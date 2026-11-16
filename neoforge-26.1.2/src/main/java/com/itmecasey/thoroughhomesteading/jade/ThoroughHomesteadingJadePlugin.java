package com.itmecasey.thoroughhomesteading.jade;

import com.itmecasey.thoroughhomesteading.block.HearthBlockEntity;
import com.itmecasey.thoroughhomesteading.compat.LookAt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * The Thorough Homesteading lines in Jade: settlers, free beds and workstations, food, and the one missing requirement as a value. The
 * server works the numbers out (as numbers) and sends them in the block's data. Jade finds this class by its annotation,
 * so it is never loaded unless Jade is installed.
 */
@WailaPlugin
public class ThoroughHomesteadingJadePlugin implements IWailaPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath("thoroughhomesteading", "hearth");

    @Override
    public void register(IWailaCommonRegistration registration) {
        IServerDataProvider<BlockAccessor> provider = new IServerDataProvider<>() {
            @Override
            public void appendServerData(CompoundTag data, BlockAccessor accessor) {
                if (accessor.getBlockEntity() instanceof HearthBlockEntity part && accessor.getLevel() instanceof ServerLevel level) {
                    HearthBlockEntity hearth = part.home();
                    if (hearth != null) {
                        data.put("values", LookAt.toTag(LookAt.values(level, hearth)));
                    }
                }
            }

            @Override
            public Identifier getUid() {
                return UID;
            }
        };
        registration.registerBlockDataProvider(provider, HearthBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        IBlockComponentProvider provider = new IBlockComponentProvider() {
            @Override
            public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
                CompoundTag data = accessor.getServerData();
                if (!data.contains("values")) {
                    return;
                }
                for (Component line : LookAt.lines(LookAt.fromTag(data.getCompoundOrEmpty("values")))) {
                    tooltip.add(line);
                }
            }

            @Override
            public Identifier getUid() {
                return UID;
            }
        };
        registration.registerBlockComponent(provider, Block.class);
    }
}
