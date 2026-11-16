package com.itmecasey.thoroughhomesteading;

import com.itmecasey.thoroughhomesteading.block.HearthBlockEntity;
import com.itmecasey.thoroughhomesteading.data.HomesteadingData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

/**
 * The home check: looks around a Hearth for free beds and workstations, light at the beds, a place to stand and a path to the bed,
 * then lets {@link HomeCheck} decide. Vertical reach is {@value #VERTICAL} blocks up and down, horizontal reach is the tier's radius.
 */
public final class HomeScanner {
    /** Blocks the scan reaches above and below the Hearth. */
    public static final int VERTICAL = 6;

    /** What a scan found: the verdict, the recipe and places the arrival would use, and the numbers behind the look-at lines. */
    public record Result(HomeCheck.Report report, SettlingEntry recipe, BlockPos bed, BlockPos workstation, BlockPos arrival) {
        public boolean passes() {
            return report.passes();
        }
    }

    private record Bed(BlockPos head, int light, double dist) {
    }

    private record Station(BlockPos pos, SettlingEntry recipe, double dist) {
    }

    private HomeScanner() {
    }

    /** Scans around the Hearth. {@code professionFilter} (such as minecraft:farmer) limits the workstations; empty takes any. */
    public static Result scan(ServerLevel level, BlockPos hearth, HearthTier tier, int settlers, HearthBlockEntity be, String professionFilter) {
        HomesteadingData.get(level.getServer());
        int radius = tier.radius();
        int population = tier.population();
        int lightMin = Settings.getInt("light_min");
        Map<String, SettlingEntry> recipes = SettlingRecipes.byWorkstation(level.getServer());

        // Beds and workstations that villagers already claimed (their home and job site memories).
        Set<BlockPos> claimedHomes = new HashSet<>();
        Set<BlockPos> claimedJobs = new HashSet<>();
        AABB around = new AABB(hearth).inflate(radius + 48.0, VERTICAL + 48.0, radius + 48.0);
        for (Villager v : level.getEntitiesOfClass(Villager.class, around)) {
            remember(v, MemoryModuleType.HOME, level, claimedHomes);
            remember(v, MemoryModuleType.JOB_SITE, level, claimedJobs);
        }

        List<Bed> beds = new ArrayList<>();
        List<Station> stations = new ArrayList<>();
        int bestLight = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        long radiusSq = (long) radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if ((long) dx * dx + (long) dz * dz > radiusSq) {
                    continue;
                }
                for (int dy = -VERTICAL; dy <= VERTICAL; dy++) {
                    p.set(hearth.getX() + dx, hearth.getY() + dy, hearth.getZ() + dz);
                    if (!level.hasChunkAt(p)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(p);
                    if (state.isAir()) {
                        continue;
                    }
                    double dist = hearth.distSqr(p);
                    if (state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD) {
                        BlockPos head = p.immutable();
                        if (freeBed(level, head, state, claimedHomes)) {
                            int light = level.getRawBrightness(head, 0);
                            beds.add(new Bed(head, light, dist));
                            bestLight = Math.max(bestLight, light);
                        }
                        continue;
                    }
                    SettlingEntry entry = recipes.get(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
                    if (entry != null && entry.allows(tier) && !claimedJobs.contains(p) && professionExists(level, entry.profession())
                            && (professionFilter.isEmpty() || professionFilter.equals(entry.profession()))) {
                        stations.add(new Station(p.immutable(), entry, dist));
                    }
                }
            }
        }
        beds.sort(Comparator.comparingDouble(Bed::dist).thenComparingInt(b -> b.head().getY()).thenComparingInt(b -> b.head().getX()).thenComparingInt(b -> b.head().getZ()));
        stations.sort(Comparator.comparingDouble(Station::dist).thenComparingInt(s -> s.pos().getY()).thenComparingInt(s -> s.pos().getX()).thenComparingInt(s -> s.pos().getZ()));
        Station station = stations.isEmpty() ? null : stations.get(0);
        SettlingEntry recipe = station == null ? null : station.recipe();

        int litBeds = 0;
        for (Bed b : beds) {
            if (b.light() >= lightMin) {
                litBeds++;
            }
        }
        int foodRequired = recipe == null ? Settings.getInt("food_required") : recipe.foodNeeded();
        int food = be.nutrition();
        if (recipe != null && !be.hasExtras(recipe)) {
            food = Math.min(food, foodRequired - 1);
        }
        HomeCheck.Report first = HomeCheck.evaluate(new HomeCheck.Inputs(settlers, population, beds.size(), litBeds, bestLight, lightMin, stations.size(), food, foodRequired));

        BlockPos bedChoice = null;
        BlockPos arrival = null;
        int freeBeds = beds.size();
        if (first.passes()) {
            arrival = findArrival(level, hearth, radius);
            if (arrival != null) {
                int tries = 0;
                for (Bed b : beds) {
                    if (b.light() < lightMin) {
                        continue;
                    }
                    if (!Settings.getBool("check_standing_room") || reachable(level, arrival, b.head())) {
                        bedChoice = b.head();
                        break;
                    }
                    if (++tries >= 4) {
                        break;
                    }
                }
            }
            if (bedChoice == null) {
                // Nobody can walk from the arrival point to a lit bed: that counts as having no usable bed.
                freeBeds = 0;
            }
        }
        HomeCheck.Report report = freeBeds == beds.size() ? first
                : HomeCheck.evaluate(new HomeCheck.Inputs(settlers, population, freeBeds, litBeds, bestLight, lightMin, stations.size(), food, foodRequired));
        return new Result(report, recipe, bedChoice, station == null ? null : station.pos(), arrival);
    }

    private static void remember(Villager v, net.minecraft.world.entity.ai.memory.MemoryModuleType<GlobalPos> type, ServerLevel level, Set<BlockPos> into) {
        Optional<GlobalPos> memory = v.getBrain().getMemory(type);
        if (memory.isPresent() && memory.get().dimension().equals(level.dimension())) {
            into.add(memory.get().pos());
        }
    }

    private static boolean professionExists(ServerLevel level, String id) {
        Identifier identifier = Identifier.tryParse(id);
        return identifier != null && level.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION)
                .get(ResourceKey.create(Registries.VILLAGER_PROFESSION, identifier)).isPresent();
    }

    /** A bed no villager claimed, with both halves, nobody in it and room above both halves. */
    private static boolean freeBed(ServerLevel level, BlockPos head, BlockState headState, Set<BlockPos> claimed) {
        if (headState.getValue(BedBlock.OCCUPIED)) {
            return false;
        }
        Direction toFoot = BedBlock.getConnectedDirection(headState);
        BlockPos foot = head.relative(toFoot);
        BlockState footState = level.getBlockState(foot);
        if (!(footState.getBlock() instanceof BedBlock) || footState.getValue(BedBlock.PART) != BedPart.FOOT) {
            return false;
        }
        if (claimed.contains(head) || claimed.contains(foot)) {
            return false;
        }
        return open(level, head.above()) && open(level, foot.above());
    }

    private static boolean open(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
    }

    /** The nearest block to the Hearth where a villager can stand: solid floor, two open blocks above, no fluid. Null if there is none. */
    public static BlockPos findArrival(ServerLevel level, BlockPos hearth, int radius) {
        for (int ring = 1; ring <= radius; ring++) {
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    for (int dy = -VERTICAL; dy <= VERTICAL; dy++) {
                        BlockPos feet = hearth.offset(dx, dy, dz);
                        if (!level.hasChunkAt(feet) || !standable(level, feet)) {
                            continue;
                        }
                        double d = feet.distSqr(hearth);
                        if (d < bestDist) {
                            bestDist = d;
                            best = feet;
                        }
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    private static boolean standable(ServerLevel level, BlockPos feet) {
        BlockPos below = feet.below();
        BlockState floor = level.getBlockState(below);
        return floor.isFaceSturdy(level, below, Direction.UP) && floor.getFluidState().isEmpty() && open(level, feet) && open(level, feet.above())
                && !(floor.getBlock() instanceof BedBlock);
    }

    /** Whether a villager standing at the arrival point can walk to the bed. */
    static boolean reachable(ServerLevel level, BlockPos arrival, BlockPos bedHead) {
        Villager probe = net.minecraft.world.entity.EntityType.VILLAGER.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (probe == null) {
            return true;
        }
        probe.snapTo(arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5, 0.0F, 0.0F);
        // Path finding refuses an entity that is not on the ground, and this one has never ticked.
        probe.setOnGround(true);
        Path path = probe.getNavigation().createPath(bedHead, 1);
        return path != null && path.canReach();
    }

    /** The profession key for a settling recipe's profession id, or null when the profession is not registered. */
    public static ResourceKey<VillagerProfession> professionKey(String id) {
        Identifier identifier = Identifier.tryParse(id);
        return identifier == null ? null : ResourceKey.create(Registries.VILLAGER_PROFESSION, identifier);
    }
}
