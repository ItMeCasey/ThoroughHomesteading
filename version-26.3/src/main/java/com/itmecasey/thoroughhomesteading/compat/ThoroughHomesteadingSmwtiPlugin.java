package com.itmecasey.thoroughhomesteading.compat;

import com.itmecasey.smwti.api.CommonRegistration;
import com.itmecasey.smwti.api.ISmwtiPlugin;
import com.itmecasey.smwti.api.SmwtiPlugin;
import com.itmecasey.thoroughhomesteading.block.HearthBlockEntity;
import net.minecraft.server.level.ServerLevel;

/**
 * The Thorough Homesteading lines in Show Me What This Is: settlers, free beds and workstations, food, and the one missing requirement as a
 * value. The server works the lines out. SMWTI finds this class by its annotation; nothing calls into it, and it is never loaded unless SMWTI
 * is installed.
 */
@SmwtiPlugin
public class ThoroughHomesteadingSmwtiPlugin implements ISmwtiPlugin {
    @Override
    public void register(CommonRegistration registration) {
        registration.serverBlock((tooltip, level, pos, entity, detailed) -> {
            if (entity instanceof HearthBlockEntity hearth && level instanceof ServerLevel server) {
                LookAt.lines(server, hearth).forEach(tooltip::add);
            }
        });
    }
}
