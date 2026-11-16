package com.itmecasey.thoroughhomesteading.compat;

import com.itmecasey.thoroughhomesteading.HomeCheck;
import com.itmecasey.thoroughhomesteading.block.HearthBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * The look-at lines for a Hearth, shared by Show Me What This Is and Jade: names and values only. The server works the numbers out
 * ({@link #values}); the lines are built from them with translation keys, so every client shows its own language. When the home is
 * incomplete, the single missing requirement follows as a value such as {@code Workstations 0 of 1}.
 */
public final class LookAt {
    private LookAt() {
    }

    /** The numbers behind the lines. {@code reason} is empty when nothing blocks; {@code have} and {@code need} belong to the reason. */
    public record Values(int settlers, int population, int beds, int workstations, int food, int foodRequired, String reason, int have, int need) {
    }

    public static Values values(ServerLevel level, HearthBlockEntity hearth) {
        HomeCheck.Report report = hearth.result(level).report();
        HomeCheck.Inputs in = report.in();
        int[] missing = report.missing();
        return new Values(in.settlers(), in.population(), in.freeBeds(), in.freeWorkstations(), in.food(), in.foodRequired(), report.reason().id(), missing[0], missing[1]);
    }

    public static List<Component> lines(Values v) {
        List<Component> out = new ArrayList<>();
        out.add(Component.translatable("smwti.thoroughhomesteading.settlers", v.settlers(), v.population()));
        out.add(Component.translatable("smwti.thoroughhomesteading.beds", v.beds()));
        out.add(Component.translatable("smwti.thoroughhomesteading.workstations", v.workstations()));
        out.add(Component.translatable("smwti.thoroughhomesteading.food", v.food(), v.foodRequired()));
        // Settlers and food already show their own "x of y"; the other blockers get one extra line.
        if (v.reason().equals("beds") || v.reason().equals("workstations") || v.reason().equals("light")) {
            out.add(Component.translatable("smwti.thoroughhomesteading.need." + v.reason(), v.have(), v.need()));
        }
        return out;
    }

    public static List<Component> lines(ServerLevel level, HearthBlockEntity hearth) {
        return lines(values(level, hearth));
    }

    public static CompoundTag toTag(Values v) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("settlers", v.settlers());
        tag.putInt("population", v.population());
        tag.putInt("beds", v.beds());
        tag.putInt("workstations", v.workstations());
        tag.putInt("food", v.food());
        tag.putInt("foodRequired", v.foodRequired());
        tag.putString("reason", v.reason());
        tag.putInt("have", v.have());
        tag.putInt("need", v.need());
        return tag;
    }

    public static Values fromTag(CompoundTag t) {
        return new Values(t.getIntOr("settlers", 0), t.getIntOr("population", 0), t.getIntOr("beds", 0), t.getIntOr("workstations", 0),
                t.getIntOr("food", 0), t.getIntOr("foodRequired", 0), t.getStringOr("reason", ""), t.getIntOr("have", 0), t.getIntOr("need", 0));
    }
}
