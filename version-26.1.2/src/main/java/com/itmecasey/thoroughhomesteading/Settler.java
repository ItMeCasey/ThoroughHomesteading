package com.itmecasey.thoroughhomesteading;

import com.itmecasey.thoroughhomesteading.block.HearthBlockEntity;
import com.itmecasey.thoroughhomesteading.data.HomesteadingData;
import com.itmecasey.thoroughhomesteading.script.ScriptHost;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;

/** Bringing a settler: a vanilla villager with the profession of the workstation, the bed and workstation as its home and job site. */
public final class Settler {
    /** A settler a Hearth brought: who it is, where it was last seen, the trade level and offer count already handled, and its profession. */
    public record Ref(UUID id, BlockPos pos, int level, int offers, String profession) {
        public String encode() {
            return id + "|" + pos.getX() + "|" + pos.getY() + "|" + pos.getZ() + "|" + level + "|" + offers + "|" + profession;
        }

        public static Ref decode(String text) {
            String[] p = text.split("\\|");
            if (p.length != 7) {
                return null;
            }
            try {
                return new Ref(UUID.fromString(p[0]), new BlockPos(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3])),
                        Integer.parseInt(p[4]), Integer.parseInt(p[5]), p[6]);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private Settler() {
    }

    /** A settler counts as living at home while it still has both a bed and a workstation (vanilla clears the memory when one is broken). */
    public static boolean keepsHome(Villager villager) {
        return villager.getBrain().getMemory(MemoryModuleType.HOME).isPresent() && villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).isPresent();
    }

    /** Updates the last seen position and applies trade files to levels the villager gained since the last look. */
    public static Ref update(ServerLevel level, Villager villager, Ref ref) {
        int current = villager.getVillagerData().level();
        int offers = ref.offers();
        int seen = ref.level();
        if (current > seen) {
            int size = villager.getOffers().size();
            offers = HomesteadingData.applyLevel(level.getServer(), villager, ref.profession(), current, Math.min(offers, size));
            seen = current;
        }
        return new Ref(ref.id(), villager.blockPosition(), seen, offers, ref.profession());
    }

    /** Brings the settler the scan chose. Returns false when a script cancelled it, the profession is unknown or nothing could be spawned. */
    public static boolean arrive(ServerLevel level, HearthBlockEntity hearth, HomeScanner.Result r) {
        SettlingEntry recipe = r.recipe();
        BlockPos pos = hearth.getBlockPos();
        HearthTier tier = hearth.tier();
        String dimension = ScriptHost.dimension(level);
        if (ScriptEvents.fire("before_settle", "x", pos.getX(), "y", pos.getY(), "z", pos.getZ(), "dimension", dimension,
                "tier", tier.id(), "profession", recipe.profession())) {
            return false;
        }
        ResourceKey<VillagerProfession> key = HomeScanner.professionKey(recipe.profession());
        if (key == null) {
            return false;
        }
        Holder<VillagerProfession> profession = level.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION).get(key).orElse(null);
        Villager villager = net.minecraft.world.entity.EntityType.VILLAGER.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (profession == null || villager == null) {
            return false;
        }
        BlockPos at = r.arrival();
        double dx = pos.getX() - at.getX();
        double dz = pos.getZ() - at.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        villager.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0.0F);
        int startLevel = tier.startLevel();
        villager.setVillagerData(new VillagerData(villager.getVillagerData().type(), profession, startLevel));
        villager.setVillagerXp(VillagerData.getMinXpPerLevel(startLevel));
        villager.getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(level.dimension(), r.bed()));
        villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(level.dimension(), r.workstation()));
        int offers = HomesteadingData.applyLevel(level.getServer(), villager, recipe.profession(), startLevel, 0);
        hearth.spend(recipe.foodNeeded(), recipe);
        level.addFreshEntity(villager);
        hearth.addSettler(new Ref(villager.getUUID(), at, startLevel, offers, recipe.profession()));
        trail(level, pos, at);
        level.playSound(null, at, SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        UUID owner = hearth.owner();
        if (owner != null) {
            ScriptHost.record(owner, recipe.profession());
        }
        ScriptEvents.fire("settler_arrived", "settler", villager.getUUID().toString(), "x", pos.getX(), "y", pos.getY(), "z", pos.getZ(),
                "dimension", dimension, "tier", tier.id(), "profession", recipe.profession(), "owner", owner == null ? "" : owner.toString());
        return true;
    }

    /** A short line of green sparkles from the Hearth to the arrival point, and a burst where the settler stands. */
    private static void trail(ServerLevel level, BlockPos from, BlockPos to) {
        for (int i = 0; i <= 8; i++) {
            double t = i / 8.0;
            double x = from.getX() + 0.5 + (to.getX() - from.getX()) * t;
            double y = from.getY() + 0.9 + (to.getY() - from.getY()) * t;
            double z = from.getZ() + 0.5 + (to.getZ() - from.getZ()) * t;
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 2, 0.1, 0.1, 0.1, 0.0);
        }
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, to.getX() + 0.5, to.getY() + 1.0, to.getZ() + 0.5, 12, 0.4, 0.5, 0.4, 0.02);
    }
}
