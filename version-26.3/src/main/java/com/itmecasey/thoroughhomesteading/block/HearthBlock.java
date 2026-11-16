package com.itmecasey.thoroughhomesteading.block;

import com.itmecasey.thoroughhomesteading.HearthTier;
import com.itmecasey.thoroughhomesteading.ModContent;
import com.itmecasey.thoroughhomesteading.ScriptEvents;
import com.itmecasey.thoroughhomesteading.item.HearthUpgradeItem;
import com.itmecasey.thoroughhomesteading.script.ScriptHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A Hearth of one {@link HearthTier}: lit with flint and steel or a fire charge, fed by hand or by hopper. It has no screen. A Hearth is two
 * blocks tall, like a door: the lower half holds the state and the fire, the upper half is the pole or the roof. Both halves carry the same
 * {@code lit} value, and anything you do to either half acts on the lower one.
 */
public class HearthBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    /** Block flags for setting a half without shape updates (they would break the pair while only one half is set). */
    private static final int SET_QUIETLY = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    /** Block flags for removing the partner half: no drops, no shape updates. */
    private static final int REMOVE_PARTNER = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_KNOWN_SHAPE;

    private final HearthTier tier;

    public HearthBlock(HearthTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    public HearthTier tier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, LIT);
    }

    // ---- The two halves ----

    /** The position of the lower half of the Hearth that a state at {@code pos} belongs to. */
    public static BlockPos lowerOf(BlockState state, BlockPos pos) {
        return state.hasProperty(HALF) && state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        // The block above must be free, like a door: a Hearth is two blocks tall.
        return !level.isOutsideBuildHeight(pos.above()) && level.getBlockState(pos.above()).canBeReplaced(context) ? defaultBlockState() : null;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos,
            BlockState neighborState, RandomSource random) {
        DoubleBlockHalf half = state.getValue(HALF);
        // A half whose partner is gone goes too.
        if (direction.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            return neighborState.is(this) && neighborState.getValue(HALF) != half ? state.setValue(LIT, neighborState.getValue(LIT)) : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
                // Breaking the upper half drops the Hearth once, as if the lower half had been broken.
                BlockPos lowerPos = pos.below();
                BlockState lower = level.getBlockState(lowerPos);
                if (lower.is(this)) {
                    if (!player.isCreative() && player.hasCorrectToolForDrops(lower)) {
                        dropResources(lower, level, lowerPos, level.getBlockEntity(lowerPos), player, player.getMainHandItem());
                    }
                    level.setBlock(lowerPos, Blocks.AIR.defaultBlockState(), REMOVE_PARTNER);
                }
            } else {
                BlockPos upperPos = pos.above();
                if (level.getBlockState(upperPos).is(this)) {
                    level.setBlock(upperPos, Blocks.AIR.defaultBlockState(), REMOVE_PARTNER);
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // ---- Shape ----

    private static final VoxelShape LOWER_BASE = box(0, 0, 0, 16, 6, 16);
    private static final VoxelShape LOWER_POST = Shapes.or(LOWER_BASE, box(1, 6, 1, 3, 16, 3));
    private static final VoxelShape UPPER_POST = Shapes.or(box(1, 0, 1, 3, 16, 3), box(3, 5, 0, 12, 14, 4));
    private static final VoxelShape LOWER_GAZEBO = Shapes.or(LOWER_BASE, box(1, 6, 1, 4, 16, 4), box(12, 6, 1, 15, 16, 4), box(1, 6, 12, 4, 16, 15),
            box(12, 6, 12, 15, 16, 15));
    private static final VoxelShape UPPER_GAZEBO = Shapes.or(box(1, 0, 1, 4, 11, 4), box(12, 0, 1, 15, 11, 4), box(1, 0, 12, 4, 11, 15), box(12, 0, 12, 15, 11, 15),
            box(0, 11, 0, 16, 16, 16));

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        boolean lower = state.getValue(HALF) == DoubleBlockHalf.LOWER;
        if (tier == HearthTier.GOLDEN) {
            return lower ? LOWER_GAZEBO : UPPER_GAZEBO;
        }
        return lower ? LOWER_POST : UPPER_POST;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HearthBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        if (world.isClientSide() || state.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return null;
        }
        return createTickerHelper(type, ModContent.HEARTH, HearthBlockEntity::serverTick);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, Direction direction) {
        return world.getBlockEntity(pos) instanceof HearthBlockEntity hearth ? hearth.comparatorSignal() : 0;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        world.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        if (placer instanceof Player player && world.getBlockEntity(pos) instanceof HearthBlockEntity hearth) {
            hearth.setOwner(player.getUUID());
        }
    }

    /** Only the owner and their Barely Questing team can break a Hearth by hand (a Hearth with no owner is open to everyone). */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter getter, BlockPos pos) {
        if (getter.getBlockEntity(pos) instanceof HearthBlockEntity part) {
            HearthBlockEntity hearth = part.home();
            if (hearth != null && !hearth.canManage(player)) {
                return 0.0F;
            }
        }
        return super.getDestroyProgress(state, player, getter, pos);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof HearthBlockEntity part) || part.home() == null) {
            return super.useItemOn(stack, state, world, pos, player, hand, hit);
        }
        HearthBlockEntity hearth = part.home();
        BlockPos home = hearth.getBlockPos();
        BlockState homeState = world.getBlockState(home);
        boolean lighter = stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE);
        boolean shovel = stack.is(ItemTags.SHOVELS);
        boolean upgrade = stack.getItem() instanceof HearthUpgradeItem;
        boolean feed = HearthBlockEntity.nutritionOf(stack) > 0 || (!world.isClientSide() && hearth.canPlaceItem(0, stack));
        if (!lighter && !shovel && !upgrade && !feed) {
            return super.useItemOn(stack, state, world, pos, player, hand, hit);
        }
        if (!hearth.canManage(player)) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (lighter) {
            if (homeState.getValue(LIT)) {
                return InteractionResult.PASS;
            }
            light(world, home, homeState);
            if (stack.is(Items.FIRE_CHARGE)) {
                if (!player.hasInfiniteMaterials()) {
                    stack.shrink(1);
                }
            } else {
                stack.hurtAndBreak(1, player, hand);
            }
            return InteractionResult.SUCCESS;
        }
        if (shovel) {
            if (!homeState.getValue(LIT)) {
                return InteractionResult.PASS;
            }
            extinguish(world, home, homeState);
            return InteractionResult.SUCCESS;
        }
        if (upgrade) {
            HearthUpgradeItem item = (HearthUpgradeItem) stack.getItem();
            if (item.target().upgradesFrom(hearth.tier()) && upgrade(world, home, item.target())) {
                if (!player.hasInfiniteMaterials()) {
                    stack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.FAIL;
        }
        ItemStack rest = hearth.insert(stack.copy());
        int moved = stack.getCount() - rest.getCount();
        if (moved <= 0) {
            return InteractionResult.FAIL;
        }
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(moved);
        }
        world.playSound(null, home, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.6F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    /** Sets {@code lit} on both halves of the Hearth that the position belongs to. */
    public static void setLit(Level world, BlockPos pos, boolean lit) {
        BlockPos low = lowerOf(world.getBlockState(pos), pos);
        for (BlockPos p : new BlockPos[] {low, low.above()}) {
            BlockState s = world.getBlockState(p);
            if (s.getBlock() instanceof HearthBlock) {
                world.setBlock(p, s.setValue(LIT, lit), SET_QUIETLY);
            }
        }
    }

    /** Lights the Hearth (also used by scripts' tests and the self test). {@code pos} may be either half. */
    public static void light(Level world, BlockPos pos, BlockState state) {
        BlockPos low = lowerOf(state, pos);
        setLit(world, low, true);
        world.playSound(null, low, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, world.getRandom().nextFloat() * 0.4F + 0.8F);
        world.gameEvent(null, GameEvent.BLOCK_CHANGE, low);
        fire("hearth_lit", world, low, world.getBlockState(low));
    }

    public static void extinguish(Level world, BlockPos pos, BlockState state) {
        BlockPos low = lowerOf(state, pos);
        setLit(world, low, false);
        world.playSound(null, low, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
        world.gameEvent(null, GameEvent.BLOCK_CHANGE, low);
        fire("hearth_extinguished", world, low, world.getBlockState(low));
    }

    private static void fire(String type, Level world, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof HearthBlock block) {
            ScriptEvents.fire(type, "x", pos.getX(), "y", pos.getY(), "z", pos.getZ(), "dimension", ScriptHost.dimension(world), "tier", block.tier().id());
        }
    }

    /** Replaces both halves of the Hearth at pos by the next tier and moves everything across (the lit state too). Returns false if the block is no Hearth. */
    public static boolean upgrade(Level world, BlockPos pos, HearthTier target) {
        BlockPos low = lowerOf(world.getBlockState(pos), pos);
        BlockState old = world.getBlockState(low);
        if (!(old.getBlock() instanceof HearthBlock) || old.getBlock() == ModContent.hearth(target)
                || !(world.getBlockEntity(low) instanceof HearthBlockEntity oldEntity)) {
            return false;
        }
        boolean lit = old.getValue(LIT);
        BlockState lower = ModContent.hearth(target).defaultBlockState().setValue(LIT, lit);
        BlockState upper = lower.setValue(HALF, DoubleBlockHalf.UPPER);
        oldEntity.keepOnRemove();
        world.setBlock(low, lower, SET_QUIETLY);
        world.setBlock(low.above(), upper, SET_QUIETLY);
        if (world.getBlockEntity(low) instanceof HearthBlockEntity fresh) {
            fresh.adopt(oldEntity);
            fresh.setChanged();
        }
        world.playSound(null, low, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 1.4F);
        return true;
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || state.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return;
        }
        if (random.nextInt(10) == 0) {
            world.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
                    0.5F + random.nextFloat(), random.nextFloat() * 0.7F + 0.6F, false);
        }
        if (random.nextInt(5) == 0) {
            world.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5 + random.nextDouble() / 2.0 * (random.nextBoolean() ? 1 : -1), pos.getY() + 0.7,
                    pos.getZ() + 0.5 + random.nextDouble() / 2.0 * (random.nextBoolean() ? 1 : -1), 0.0, 0.0, 0.0);
        }
        if (random.nextInt(4) == 0) {
            world.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 0.0, 0.05, 0.0);
        }
    }
}
