package com.itmecasey.thoroughhomesteading.block;

import com.itmecasey.thoroughhomesteading.HearthTier;
import com.itmecasey.thoroughhomesteading.ModContent;
import com.itmecasey.thoroughhomesteading.ScriptEvents;
import com.itmecasey.thoroughhomesteading.item.HearthUpgradeItem;
import com.itmecasey.thoroughhomesteading.script.ScriptHost;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** A Hearth of one {@link HearthTier}: lit with flint and steel or a fire charge, fed by hand or by hopper. It has no screen. */
public class HearthBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 10.0, 16.0);

    private final HearthTier tier;

    public HearthBlock(HearthTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    public HearthTier tier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
        if (world.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModContent.HEARTH, HearthBlockEntity::serverTick);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos, net.minecraft.core.Direction direction) {
        return world.getBlockEntity(pos) instanceof HearthBlockEntity hearth ? hearth.comparatorSignal() : 0;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (placer instanceof Player player && world.getBlockEntity(pos) instanceof HearthBlockEntity hearth) {
            hearth.setOwner(player.getUUID());
        }
    }

    /** Only the owner and their Barely Questing team can break a Hearth by hand (a Hearth with no owner is open to everyone). */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter getter, BlockPos pos) {
        if (getter.getBlockEntity(pos) instanceof HearthBlockEntity hearth && !hearth.canManage(player)) {
            return 0.0F;
        }
        return super.getDestroyProgress(state, player, getter, pos);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof HearthBlockEntity hearth)) {
            return super.useItemOn(stack, state, world, pos, player, hand, hit);
        }
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
            if (state.getValue(LIT)) {
                return InteractionResult.PASS;
            }
            light(world, pos, state);
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
            if (!state.getValue(LIT)) {
                return InteractionResult.PASS;
            }
            extinguish(world, pos, state);
            return InteractionResult.SUCCESS;
        }
        if (upgrade) {
            HearthUpgradeItem item = (HearthUpgradeItem) stack.getItem();
            if (item.target().upgradesFrom(hearth.tier()) && upgrade(world, pos, item.target())) {
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
        world.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.6F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    /** Lights the Hearth (also used by scripts' tests and the self test). */
    public static void light(Level world, BlockPos pos, BlockState state) {
        world.setBlock(pos, state.setValue(LIT, true), 3);
        world.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, world.getRandom().nextFloat() * 0.4F + 0.8F);
        world.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
        fire("hearth_lit", world, pos, state);
    }

    public static void extinguish(Level world, BlockPos pos, BlockState state) {
        world.setBlock(pos, state.setValue(LIT, false), 3);
        world.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
        world.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
        fire("hearth_extinguished", world, pos, state);
    }

    private static void fire(String type, Level world, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof HearthBlock block) {
            ScriptEvents.fire(type, "x", pos.getX(), "y", pos.getY(), "z", pos.getZ(), "dimension", ScriptHost.dimension(world), "tier", block.tier().id());
        }
    }

    /** Replaces the Hearth at pos by the next tier and moves everything across (the lit state too). Returns false if the block is no Hearth. */
    public static boolean upgrade(Level world, BlockPos pos, HearthTier target) {
        BlockState old = world.getBlockState(pos);
        if (!(old.getBlock() instanceof HearthBlock) || old.getBlock() == ModContent.hearth(target)
                || !(world.getBlockEntity(pos) instanceof HearthBlockEntity oldEntity)) {
            return false;
        }
        BlockState state = ModContent.hearth(target).defaultBlockState().setValue(LIT, old.getValue(LIT));
        oldEntity.keepOnRemove();
        world.setBlock(pos, state, 3);
        if (world.getBlockEntity(pos) instanceof HearthBlockEntity fresh) {
            fresh.adopt(oldEntity);
            fresh.setChanged();
        }
        world.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 1.4F);
        return true;
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
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
