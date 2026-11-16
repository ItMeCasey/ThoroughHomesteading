package com.itmecasey.thoroughhomesteading.item;

import com.itmecasey.thoroughhomesteading.HearthTier;
import com.itmecasey.thoroughhomesteading.block.HearthBlock;
import com.itmecasey.thoroughhomesteading.block.HearthBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Raises a Hearth exactly one tier, to the tier named by the item. Contents, owner, settlers and the lit state stay. */
public class HearthUpgradeItem extends Item {
    private final HearthTier target;

    public HearthUpgradeItem(HearthTier target, Properties properties) {
        super(properties.stacksTo(16));
        this.target = target;
    }

    public HearthTier target() {
        return target;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        if (!(state.getBlock() instanceof HearthBlock hearth)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player != null && level.getBlockEntity(pos) instanceof HearthBlockEntity part && part.home() != null && !part.home().canManage(player)) {
            return InteractionResult.PASS;
        }
        if (!target.upgradesFrom(hearth.tier())) {
            return InteractionResult.FAIL;
        }
        if (HearthBlock.upgrade(level, pos, target)) {
            if (player == null || !player.hasInfiniteMaterials()) {
                context.getItemInHand().shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }
}
