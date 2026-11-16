package com.itmecasey.thoroughhomesteading.dev;

import com.google.gson.JsonParser;
import com.itmecasey.thoroughhomesteading.BarelyQuestingBridge;
import com.itmecasey.thoroughhomesteading.Dev;
import com.itmecasey.thoroughhomesteading.HearthTier;
import com.itmecasey.thoroughhomesteading.HomeScanner;
import com.itmecasey.thoroughhomesteading.KubeJsBridge;
import com.itmecasey.thoroughhomesteading.ModContent;
import com.itmecasey.thoroughhomesteading.ScriptEvents;
import com.itmecasey.thoroughhomesteading.Settings;
import com.itmecasey.thoroughhomesteading.SettlingEntry;
import com.itmecasey.thoroughhomesteading.SettlingRecipe;
import com.itmecasey.thoroughhomesteading.SettlingRecipes;
import com.itmecasey.thoroughhomesteading.block.HearthBlock;
import com.itmecasey.thoroughhomesteading.block.HearthBlockEntity;
import com.itmecasey.thoroughhomesteading.compat.LookAt;
import com.itmecasey.thoroughhomesteading.data.HomesteadingData;
import com.itmecasey.thoroughhomesteading.script.Bridges;
import com.itmecasey.thoroughhomesteading.script.ScriptBridge;
import com.itmecasey.thoroughhomesteading.script.ScriptHost;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.TagValueInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Headless check of the whole mod on a real server: content, the settling recipe type and its codecs, the home check and every blocking
 * reason, hopper input, the comparator, the countdown, the arrival (profession, bed and workstation memories, food spent, trade files),
 * population, lost homes, upgrades, ownership, saving and loading, the look-at lines, and the scripting API, all without ModJS, KubeJS
 * or Barely Questing installed. Runs once at server start when {@code THOROUGHHOMESTEADING_SELFTEST} is set and prints {@code SELFTEST PASS/FAIL}
 * lines ending in {@code SELFTEST DONE failures=N}. It ticks block entities by hand, so it needs no running game loop.
 */
public final class SelfTest {
    private static final Logger LOGGER = LoggerFactory.getLogger("ThoroughHomesteading");
    private static ServerLevel level;
    private static MinecraftServer server;
    private static ServerPlayer owner;
    private static ServerPlayer stranger;
    private static BlockPos base;
    private static int layer;
    private static int failures;
    private static final List<Villager> VILLAGERS = new ArrayList<>();

    private SelfTest() {
    }

    public static boolean requested() {
        return System.getenv("THOROUGHHOMESTEADING_SELFTEST") != null && Dev.ENABLED;
    }

    public static int run(MinecraftServer started) {
        server = started;
        level = server.overworld();
        failures = 0;
        ScriptHost.start(server);
        // Listeners of a ModJS script (when it is installed) must survive the whole run, so the tests never clear them.
        owner = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "selftest_owner"), ClientInformation.createDefault());
        stranger = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "selftest_other"), ClientInformation.createDefault());
        BlockPos spawn = level.getRespawnData().pos();
        int x = ((spawn.getX() >> 4) << 4) + 8;
        int z = ((spawn.getZ() >> 4) << 4) + 8;
        base = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 12, z);
        try {
            Settings.reset();
            step("content_and_recipes", SelfTest::content);
            step("recipe_codecs", SelfTest::codecs);
            step("lighting_and_events", SelfTest::lighting);
            step("requirements_in_order", SelfTest::requirements);
            step("hopper_input_and_comparator", SelfTest::hoppersAndComparator);
            step("arrival", SelfTest::arrival);
            step("population", SelfTest::population);
            step("before_settle_cancel", SelfTest::cancel);
            step("countdown_and_blocked_events", SelfTest::countdown);
            step("unlit_hearth", SelfTest::unlit);
            step("standing_room", SelfTest::standingRoom);
            step("lost_home", SelfTest::lostHome);
            step("upgrades", SelfTest::upgrades);
            step("ownership", SelfTest::ownership);
            step("using_items_on_a_hearth", SelfTest::useItems);
            step("two_blocks_tall", SelfTest::twoBlocksTall);
            step("save_and_load", SelfTest::saveAndLoad);
            step("trade_files", SelfTest::trades);
            step("look_at_lines", SelfTest::lookAt);
            step("scripting_api", SelfTest::scripting);
            step("optional_mods_absent", SelfTest::optionalAbsent);
        } finally {
            for (Villager v : VILLAGERS) {
                v.discard();
            }
            Settings.reset();
            ScriptEvents.clear();
            HomesteadingData.setTestTrades(null);
        }
        LOGGER.info("SELFTEST DONE failures={}", failures);
        return failures;
    }

    // ---- helpers ----

    private static void step(String name, Runnable body) {
        try {
            Settings.reset();
            // The light engine works in the background, so the light at a freshly built bed is not settled yet; the light rule has its own check.
            Settings.set("light_min", 0);
            body.run();
        } catch (RuntimeException | LinkageError e) {
            LOGGER.error("SELFTEST error in " + name, e);
            verdict(name + "_ran_clean", false, e.toString());
        }
    }

    private static void verdict(String name, boolean ok, String detail) {
        if (ok) {
            LOGGER.info("SELFTEST PASS {} ({})", name, detail);
        } else {
            failures++;
            LOGGER.error("SELFTEST FAIL {} ({})", name, detail);
        }
    }

    /** A fresh platform 25 by 25 blocks with eight blocks of air above it, 16 blocks higher than the last. */
    private static BlockPos platform() {
        BlockPos c = base.above(16 * layer++);
        for (int dx = -12; dx <= 12; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                level.setBlock(c.offset(dx, -1, dz), Blocks.STONE.defaultBlockState(), 2);
                for (int dy = 0; dy <= 8; dy++) {
                    level.setBlock(c.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        return c;
    }

    private static HearthBlockEntity hearth(BlockPos pos, HearthTier tier, boolean lit) {
        BlockState lower = ModContent.hearth(tier).defaultBlockState().setValue(HearthBlock.LIT, lit);
        level.setBlock(pos, lower, 3);
        level.setBlock(pos.above(), lower.setValue(HearthBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 3);
        HearthBlockEntity be = (HearthBlockEntity) level.getBlockEntity(pos);
        be.setOwner(owner.getUUID());
        return be;
    }

    /** A bed whose foot is at the position and head to the east of it; returns the head position. */
    private static BlockPos bed(BlockPos foot) {
        BlockPos head = foot.east();
        level.setBlock(foot, Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.FOOT), 3);
        level.setBlock(head, Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.EAST).setValue(BedBlock.PART, BedPart.HEAD), 3);
        return head;
    }

    private static void station(BlockPos pos, net.minecraft.world.level.block.Block block) {
        level.setBlock(pos, block.defaultBlockState(), 3);
    }

    private static void food(HearthBlockEntity h, int breads) {
        h.insert(new ItemStack(Items.BREAD, breads));
    }

    private static Map<String, Object> report(HearthBlockEntity h) {
        return h.report(level);
    }

    private static String blocked(HearthBlockEntity h) {
        return String.valueOf(report(h).get("blocked"));
    }

    private static HearthBlockEntity rescan(HearthBlockEntity h) {
        // result() re-scans when the cache is older than a second of game time; loading the block entity data clears it.
        CompoundTag tag = h.saveWithoutMetadata(level.registryAccess());
        h.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        return h;
    }

    private static Villager villager(String uuid) {
        Villager v = level.getEntity(UUID.fromString(uuid)) instanceof Villager found ? found : null;
        if (v != null) {
            VILLAGERS.add(v);
        }
        return v;
    }

    // ---- steps ----

    private static void content() {
        verdict("three_hearth_blocks", ModContent.blocks().size() == 3, "blocks=" + ModContent.blocks().size());
        verdict("five_items", ModContent.items().size() == 5, "items=" + ModContent.items().size());
        verdict("recipe_type_registered", BuiltInRegistries.RECIPE_TYPE.getOptional(ModContent.id("settling")).isPresent(), "");
        verdict("recipe_serializer_registered", BuiltInRegistries.RECIPE_SERIALIZER.getOptional(ModContent.id("settling")).isPresent(), "");
        List<SettlingEntry> entries = SettlingRecipes.entries(server);
        verdict("thirteen_settling_recipes_loaded", entries.size() == 13, "n=" + entries.size());
        boolean farmer = entries.stream().anyMatch(e -> e.workstation().equals("minecraft:composter") && e.profession().equals("minecraft:farmer"));
        verdict("composter_makes_a_farmer", farmer, "");
        for (String name : List.of("stone_hearth", "iron_hearth_upgrade", "golden_hearth_upgrade")) {
            Object holder = server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, ModContent.id(name))).orElse(null);
            verdict("crafting_recipe_" + name, holder != null, "");
        }
        verdict("hearth_blocks_need_a_pickaxe", ModContent.hearth(HearthTier.STONE).defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE), "");
        verdict("creative_tab_builds", ModContent.creativeTab() != null, "");
    }

    private static void codecs() {
        String json = "{\"type\":\"thoroughhomesteading:settling\",\"workstation\":\"minecraft:composter\",\"profession\":\"minecraft:farmer\","
                + "\"min_tier\":2,\"food\":25,\"extra\":[{\"item\":\"minecraft:emerald\",\"count\":3}]}";
        var ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        var parsed = SettlingRecipe.CODEC.codec().parse(ops, JsonParser.parseString(json));
        SettlingRecipe recipe = parsed.result().orElse(null);
        verdict("settling_json_parses", recipe != null, parsed.error().map(Object::toString).orElse(""));
        if (recipe == null) {
            return;
        }
        SettlingEntry e = recipe.toEntry();
        verdict("settling_fields", e.minTier() == 2 && e.food() == 25 && e.extra().size() == 1 && e.extra().get(0).count() == 3
                && e.extra().get(0).item().equals("minecraft:emerald"), e.toString());
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        SettlingRecipe.STREAM_CODEC.encode(buf, recipe);
        SettlingRecipe back = SettlingRecipe.STREAM_CODEC.decode(buf);
        verdict("settling_stream_roundtrip", back.equals(recipe), back.toString());
        var minimal = SettlingRecipe.CODEC.codec().parse(ops, JsonParser.parseString(
                "{\"type\":\"thoroughhomesteading:settling\",\"workstation\":\"minecraft:loom\",\"profession\":\"minecraft:shepherd\"}"));
        verdict("settling_defaults_fill_in", minimal.result().map(r -> r.minTier() == 1 && r.food() == 0 && r.extra().isEmpty()).orElse(false), "");
        var bad = SettlingRecipe.CODEC.codec().parse(ops, JsonParser.parseString("{\"type\":\"thoroughhomesteading:settling\",\"workstation\":\"minecraft:loom\"}"));
        verdict("settling_without_profession_is_refused", bad.result().isEmpty(), "");
    }

    private static void lighting() {
        BlockPos c = platform();
        List<String> events = new ArrayList<>();
        ScriptEvents.on("hearth_lit", e -> events.add("lit " + e.string("tier") + " " + (int) e.number("x")));
        ScriptEvents.on("hearth_extinguished", e -> events.add("out " + e.string("tier")));
        HearthBlockEntity h = hearth(c, HearthTier.STONE, false);
        BlockState state = level.getBlockState(c);
        verdict("new_hearth_is_unlit", !state.getValue(HearthBlock.LIT) && state.getLightEmission() == 0, "");
        HearthBlock.light(level, c, state);
        verdict("lighting_sets_lit_and_light", level.getBlockState(c).getValue(HearthBlock.LIT) && level.getBlockState(c).getLightEmission() == 13, "");
        HearthBlock.extinguish(level, c, level.getBlockState(c));
        verdict("extinguishing_clears_lit", !level.getBlockState(c).getValue(HearthBlock.LIT), "");
        verdict("lit_and_out_events_fire", events.equals(List.of("lit stone " + c.getX(), "out stone")), events.toString());
        verdict("both_halves_have_a_shape", level.getBlockState(c).getShape(level, c).max(Direction.Axis.Y) > 0.3
                && level.getBlockState(c.above()).getShape(level, c.above()).max(Direction.Axis.Y) > 0.9, "");
        verdict("hearth_has_a_block_entity", h != null && h.tier() == HearthTier.STONE, "");
    }

    private static void requirements() {
        BlockPos c = platform();
        HearthBlockEntity h = hearth(c, HearthTier.STONE, true);
        verdict("no_bed_blocks_on_beds", blocked(h).equals("beds"), blocked(h));
        BlockPos head = bed(c.offset(3, 0, 0));
        rescan(h);
        verdict("bed_found_then_workstations_block", blocked(h).equals("workstations") && (int) report(h).get("beds") == 1, report(h).toString());
        station(c.offset(0, 0, 3), Blocks.COMPOSTER);
        rescan(h);
        verdict("workstation_found_then_food_blocks", blocked(h).equals("food") && (int) report(h).get("workstations") == 1, report(h).toString());
        verdict("light_value_is_read_from_the_bed", (int) report(h).get("light") == level.getRawBrightness(head, 0), "light=" + report(h).get("light"));
        food(h, 3);
        rescan(h);
        verdict("not_enough_food_still_blocks", blocked(h).equals("food") && (int) report(h).get("food") == 15, report(h).toString());
        food(h, 1);
        rescan(h);
        verdict("everything_present_passes", blocked(h).isEmpty() && report(h).get("profession").equals("minecraft:farmer"), report(h).toString());
        // A workstation of a tier the Hearth cannot use does not count; a settling recipe names the lowest tier.
        station(c.offset(0, 0, -4), Blocks.LECTERN);
        rescan(h);
        verdict("nearest_workstation_wins", report(h).get("profession").equals("minecraft:farmer") && (int) report(h).get("workstations") == 2, report(h).toString());
        // Too dark: a bed in a closed box cannot be lit by the open sky. The light level is read straight from the world, so the rule is
        // checked on the real number: the setting decides.
        Settings.set("light_min", 15);
        rescan(h);
        int light = (int) report(h).get("light");
        verdict("light_min_is_compared_to_the_bed_light", (light >= 15) == blocked(h).isEmpty(), "light=" + light + " blocked=" + blocked(h));
    }

    private static void hoppersAndComparator() {
        BlockPos c = platform();
        HearthBlockEntity h = hearth(c, HearthTier.STONE, true);
        verdict("hoppers_see_the_hearth_as_a_container", HopperBlockEntity.getContainerAt(level, c) == h, "");
        ItemStack rest = HopperBlockEntity.addItem(null, h, new ItemStack(Items.BREAD, 5), Direction.UP);
        verdict("hopper_inserts_food_from_above", rest.isEmpty() && h.nutrition() == 25, "nutrition=" + h.nutrition());
        ItemStack side = HopperBlockEntity.addItem(null, h, new ItemStack(Items.COOKED_BEEF, 2), Direction.NORTH);
        verdict("hopper_inserts_food_from_the_sides", side.isEmpty() && h.nutrition() == 41, "nutrition=" + h.nutrition());
        ItemStack below = HopperBlockEntity.addItem(null, h, new ItemStack(Items.BREAD, 1), Direction.DOWN);
        verdict("nothing_goes_in_from_below", !below.isEmpty(), "");
        ItemStack stone = HopperBlockEntity.addItem(null, h, new ItemStack(Items.STONE, 4), Direction.UP);
        verdict("non_food_is_refused", stone.getCount() == 4, "");
        verdict("nothing_comes_out_through_any_face", !h.canTakeItemThroughFace(0, h.getItem(0), Direction.DOWN) && !h.canTakeItemThroughFace(0, h.getItem(0), Direction.UP)
                && !h.canTakeItemThroughFace(0, h.getItem(0), Direction.NORTH), "");
        verdict("down_face_has_no_slots", h.getSlotsForFace(Direction.DOWN).length == 0 && h.getSlotsForFace(Direction.UP).length == 9, "");
        // Comparator: nine slots, signal by fullness.
        h.clearContent();
        verdict("empty_hearth_reads_zero", level.getBlockState(c).getAnalogOutputSignal(level, c, Direction.NORTH) == 0, "");
        for (int i = 0; i < 9; i++) {
            h.setItem(i, new ItemStack(Items.BREAD, 64));
        }
        verdict("full_hearth_reads_fifteen", level.getBlockState(c).getAnalogOutputSignal(level, c, Direction.NORTH) == 15, "");
        h.clearContent();
        h.setItem(0, new ItemStack(Items.BREAD, 64));
        int one = level.getBlockState(c).getAnalogOutputSignal(level, c, Direction.NORTH);
        verdict("one_full_slot_reads_between", one >= 1 && one < 15, "signal=" + one);
        verdict("hearth_uses_a_comparator", level.getBlockState(c).hasAnalogOutputSignal(), "");
    }

    /** A complete home: bed, composter, light, five bread. Returns the Hearth. */
    private static HearthBlockEntity home(BlockPos c, HearthTier tier, boolean lit) {
        HearthBlockEntity h = hearth(c, tier, lit);
        bed(c.offset(3, 0, 0));
        station(c.offset(0, 0, 3), Blocks.COMPOSTER);
        food(h, 5);
        return h;
    }

    private static void arrival() {
        BlockPos c = platform();
        List<String> events = new ArrayList<>();
        ScriptEvents.on("settler_arrived", e -> events.add(e.string("profession") + " " + e.string("tier")));
        HearthBlockEntity h = home(c, HearthTier.STONE, true);
        int before = h.nutrition();
        boolean ok = h.settleNow(level, "");
        verdict("settle_now_brings_a_settler", ok && h.settlerCount() == 1, "ok=" + ok);
        if (h.settlerIds().isEmpty()) {
            return;
        }
        Villager v = villager(h.settlerIds().get(0));
        verdict("the_settler_is_a_vanilla_villager", v != null && v.isAlive(), "");
        if (v == null) {
            return;
        }
        verdict("profession_comes_from_the_workstation", v.getVillagerData().profession().is(VillagerProfession.FARMER), v.getVillagerData().profession().toString());
        verdict("settler_starts_at_level_one", v.getVillagerData().level() == 1, "level=" + v.getVillagerData().level());
        BlockPos bedHead = c.offset(4, 0, 0);
        verdict("home_memory_is_the_bed", v.getBrain().getMemory(MemoryModuleType.HOME).map(g -> g.pos().equals(bedHead)).orElse(false), "");
        verdict("job_site_memory_is_the_workstation", v.getBrain().getMemory(MemoryModuleType.JOB_SITE).map(g -> g.pos().equals(c.offset(0, 0, 3))).orElse(false), "");
        verdict("food_is_spent", before - h.nutrition() >= 20 && h.nutrition() < before, "before=" + before + " after=" + h.nutrition());
        verdict("arrival_event_fires", events.equals(List.of("minecraft:farmer stone")), events.toString());
        verdict("ledger_counts_the_owner", ScriptHost.ledger().count(owner.getUUID(), "minecraft:farmer") == 1, "");
        verdict("the_settler_stands_on_solid_ground", level.getBlockState(v.blockPosition().below()).isSolid(), v.blockPosition().toString());
        verdict("the_settler_has_trades", !v.getOffers().isEmpty(), "offers=" + v.getOffers().size());
        // The bed and workstation are claimed now: a second pass finds neither free.
        rescan(h);
        verdict("claimed_bed_and_workstation_are_not_free", (int) report(h).get("beds") == 0 && (int) report(h).get("workstations") == 0, report(h).toString());
        verdict("claimed_home_blocks_on_beds", blocked(h).equals("beds"), blocked(h));
        verdict("hearth_remembers_the_settler", h.settlerIds().get(0).equals(v.getUUID().toString()), "");
    }

    private static void population() {
        BlockPos c = platform();
        HearthBlockEntity h = hearth(c, HearthTier.STONE, true);
        bed(c.offset(3, 0, 0));
        bed(c.offset(3, 0, 2));
        bed(c.offset(3, 0, 4));
        station(c.offset(0, 0, 3), Blocks.COMPOSTER);
        station(c.offset(0, 0, -4), Blocks.LECTERN);
        station(c.offset(-5, 0, 0), Blocks.LOOM);
        food(h, 64);
        boolean a = h.settleNow(level, "");
        boolean b = h.settleNow(level, "");
        boolean third = h.settleNow(level, "");
        verdict("stone_hearth_keeps_two_settlers", a && b && !third && h.settlerCount() == 2, "a=" + a + " b=" + b + " third=" + third);
        rescan(h);
        verdict("full_hearth_reports_population", blocked(h).equals("population") && (int) report(h).get("population") == 2, report(h).toString());
        for (String id : h.settlerIds()) {
            villager(id);
        }
        List<String> professions = new ArrayList<>();
        for (String id : h.settlerIds()) {
            Villager v = (Villager) level.getEntity(UUID.fromString(id));
            professions.add(v.getVillagerData().profession().getRegisteredName());
        }
        verdict("the_two_nearest_workstations_were_used", professions.contains("minecraft:farmer") && professions.size() == 2, professions.toString());
        Settings.set("population.stone", 3);
        verdict("population_setting_applies", HearthTier.STONE.population() == 3, "");
    }

    private static void cancel() {
        BlockPos c = platform();
        List<String> seen = new ArrayList<>();
        boolean[] active = {true};
        ScriptEvents.on("before_settle", e -> {
            if (active[0]) {
                seen.add(e.string("profession"));
                e.cancel();
            }
        });
        HearthBlockEntity h = home(c, HearthTier.STONE, true);
        int before = h.nutrition();
        boolean ok = h.settleNow(level, "");
        verdict("a_script_can_cancel_an_arrival", !ok && h.settlerCount() == 0 && h.nutrition() == before, "seen=" + seen);
        verdict("cancel_listener_saw_the_profession", seen.equals(List.of("minecraft:farmer")), seen.toString());
        active[0] = false;
        boolean later = h.settleNow(level, "");
        verdict("without_the_listener_it_goes_through", later && h.settlerCount() == 1, "");
        for (String id : h.settlerIds()) {
            villager(id);
        }
    }

    private static void countdown() {
        BlockPos c = platform();
        List<String> blocked = new ArrayList<>();
        ScriptEvents.on("settling_blocked", e -> blocked.add(e.string("reason")));
        Settings.set("settle_ticks.stone", 400);
        Settings.set("scan_interval", 200);
        HearthBlockEntity h = hearth(c, HearthTier.STONE, true);
        h.tickScan(level, 200);
        h.tickScan(level, 200);
        verdict("blocked_event_fires_once_per_reason", blocked.equals(List.of("beds")), blocked.toString());
        verdict("countdown_waits_while_blocked", h.progress() == 0, "progress=" + h.progress());
        bed(c.offset(3, 0, 0));
        station(c.offset(0, 0, 3), Blocks.COMPOSTER);
        h.tickScan(level, 200);
        verdict("blocked_event_fires_when_the_reason_changes", blocked.equals(List.of("beds", "food")), blocked.toString());
        food(h, 5);
        h.tickScan(level, 200);
        verdict("countdown_runs_while_the_check_passes", h.progress() == 200 && h.settlerCount() == 0, "progress=" + h.progress());
        // The countdown pauses when a requirement stops being true and keeps its progress.
        level.setBlock(c.offset(0, 0, 3), Blocks.AIR.defaultBlockState(), 3);
        h.tickScan(level, 200);
        verdict("countdown_pauses_without_resetting", h.progress() == 200 && h.settlerCount() == 0, "progress=" + h.progress());
        station(c.offset(0, 0, 3), Blocks.COMPOSTER);
        h.tickScan(level, 200);
        verdict("the_settler_arrives_when_the_countdown_ends", h.settlerCount() == 1 && h.progress() == 0, "settlers=" + h.settlerCount());
        for (String id : h.settlerIds()) {
            villager(id);
        }
    }

    private static void unlit() {
        BlockPos c = platform();
        Settings.set("settle_ticks.stone", 200);
        HearthBlockEntity h = home(c, HearthTier.STONE, false);
        h.tickScan(level, 200);
        verdict("an_unlit_hearth_does_nothing", h.settlerCount() == 0 && h.progress() == 0, "");
        verdict("settle_now_needs_a_lit_hearth", !h.settleNow(level, ""), "");
        Settings.set("allow_unlit_hearth_scan", true);
        h.tickScan(level, 200);
        verdict("unlit_scan_updates_the_report_but_not_the_countdown", h.settlerCount() == 0 && h.progress() == 0 && blocked(h).isEmpty(), report(h).toString());
        // Ticking through the block's own ticker honors the setting too.
        HearthBlockEntity.serverTick(level, c, level.getBlockState(c), h);
        verdict("the_ticker_runs_cleanly", h.settlerCount() == 0, "");
    }

    private static void standingRoom() {
        BlockPos c = platform();
        HearthBlockEntity h = hearth(c, HearthTier.STONE, true);
        station(c.offset(0, 0, 3), Blocks.COMPOSTER);
        food(h, 5);
        // The bed sits on a pillar four blocks up with nothing leading to it.
        BlockPos foot = c.offset(5, 4, 0);
        for (int dy = 0; dy < 4; dy++) {
            level.setBlock(c.offset(5, dy, 0), Blocks.STONE.defaultBlockState(), 2);
            level.setBlock(c.offset(6, dy, 0), Blocks.STONE.defaultBlockState(), 2);
        }
        bed(foot);
        rescan(h);
        verdict("an_unreachable_bed_counts_as_no_bed", blocked(h).equals("beds") && (int) report(h).get("beds") == 0, report(h).toString());
        Settings.set("check_standing_room", false);
        rescan(h);
        verdict("without_the_standing_room_check_it_passes", blocked(h).isEmpty(), report(h).toString());
        BlockPos stand = HomeScanner.findArrival(level, c, 8);
        verdict("arrival_point_is_the_nearest_standing_block", stand != null && stand.distSqr(c) <= 2.0, String.valueOf(stand));
    }

    private static void lostHome() {
        BlockPos c = platform();
        HearthBlockEntity h = home(c, HearthTier.STONE, true);
        h.settleNow(level, "");
        Villager v = villager(h.settlerIds().get(0));
        verdict("a_settler_is_counted", h.settlerCount() == 1 && v != null, "");
        // Vanilla clears the bed memory when the bed is broken; the Hearth then stops counting the settler.
        v.getBrain().eraseMemory(MemoryModuleType.HOME);
        rescan(h);
        report(h);
        verdict("a_settler_without_a_bed_stops_counting", (int) report(h).get("settlers") == 0 && h.settlerCount() == 0, report(h).toString());
        verdict("the_villager_stays_a_normal_villager", v.isAlive() && v.getVillagerData().profession().is(VillagerProfession.FARMER), "");
        // A replacement can come: a new bed and food.
        bed(c.offset(3, 0, 2));
        station(c.offset(-3, 0, 0), Blocks.LOOM);
        food(h, 5);
        boolean second = h.settleNow(level, "");
        verdict("a_replacement_may_be_brought", second && h.settlerCount() == 1, "");
        for (String id : h.settlerIds()) {
            villager(id);
        }
        // A settler that died is dropped too.
        Villager newest = (Villager) level.getEntity(UUID.fromString(h.settlerIds().get(0)));
        newest.discard();
        rescan(h);
        report(h);
        verdict("a_dead_settler_stops_counting", h.settlerCount() == 0, "settlers=" + h.settlerCount());
    }

    private static void upgrades() {
        BlockPos c = platform();
        HearthBlockEntity stone = home(c, HearthTier.STONE, true);
        verdict("stone_radius_is_eight", HearthTier.STONE.radius() == 8 && report(stone).get("tier").equals("stone"), "");
        var golden = ModContent.upgrade(HearthTier.GOLDEN);
        var iron = ModContent.upgrade(HearthTier.IRON);
        verdict("golden_cannot_skip_a_level", !HearthTier.GOLDEN.upgradesFrom(HearthTier.STONE) && HearthBlock.upgrade(level, c, HearthTier.IRON), "");
        HearthBlockEntity upgraded = (HearthBlockEntity) level.getBlockEntity(c);
        verdict("upgrade_replaces_the_block", level.getBlockState(c).getBlock() == ModContent.hearth(HearthTier.IRON), "");
        verdict("upgrade_keeps_lit_food_and_owner", level.getBlockState(c).getValue(HearthBlock.LIT) && upgraded.nutrition() == 25 && upgraded.owner().equals(owner.getUUID()), "");
        verdict("iron_tier_numbers", upgraded.tier() == HearthTier.IRON && HearthTier.IRON.population() == 4 && HearthTier.IRON.radius() == 12, "");
        verdict("upgrade_items_exist", golden.target() == HearthTier.GOLDEN && iron.target() == HearthTier.IRON, "");
        boolean ironSettles = upgraded.settleNow(level, "");
        verdict("iron_hearth_brings_a_settler", ironSettles && upgraded.settlerCount() == 1, "");
        villager(upgraded.settlerIds().get(0));
        String first = upgraded.settlerIds().get(0);
        verdict("upgrading_to_the_same_tier_does_nothing", !HearthBlock.upgrade(level, c, HearthTier.IRON) && level.getBlockEntity(c) == upgraded, "");
        HearthBlock.upgrade(level, c, HearthTier.GOLDEN);
        HearthBlockEntity gold = (HearthBlockEntity) level.getBlockEntity(c);
        verdict("golden_keeps_everything", gold.tier() == HearthTier.GOLDEN && gold.nutrition() > 0 && level.getBlockState(c).getValue(HearthBlock.LIT)
                && gold.owner().equals(owner.getUUID()), "");
        verdict("settlers_stay_with_the_hearth_across_an_upgrade", gold.settlerIds().equals(List.of(first)), gold.settlerIds().toString());
        bed(c.offset(3, 0, 2));
        station(c.offset(-3, 0, 0), Blocks.LOOM);
        food(gold, 5);
        boolean ok = gold.settleNow(level, "");
        verdict("golden_settlers_arrive", ok && gold.settlerCount() == 2, "");
        Villager v = villager(gold.settlerIds().get(1));
        verdict("golden_settlers_start_at_level_two", v != null && v.getVillagerData().level() == 2, v == null ? "none" : "level=" + v.getVillagerData().level());
        verdict("golden_population_is_eight", HearthTier.GOLDEN.population() == 8 && (int) report(gold).get("population") == 8, "");
    }

    private static void ownership() {
        BlockPos c = platform();
        HearthBlockEntity h = hearth(c, HearthTier.STONE, false);
        BlockState state = level.getBlockState(c);
        verdict("the_owner_can_manage", h.canManage(owner), "");
        verdict("a_stranger_cannot_manage", !h.canManage(stranger), "");
        verdict("the_owner_can_break_by_hand", state.getDestroyProgress(owner, level, c) > 0.0F, "");
        verdict("a_stranger_cannot_break_by_hand", state.getDestroyProgress(stranger, level, c) == 0.0F, "");
        h.setOwner(null);
        verdict("a_hearth_without_an_owner_is_open_to_everyone", h.canManage(stranger) && state.getDestroyProgress(stranger, level, c) > 0.0F, "");
        verdict("without_barely_questing_there_are_no_teams", !BarelyQuestingBridge.sameTeam(owner.getUUID(), stranger.getUUID()), "");
        // Hoppers insert whoever owns the Hearth.
        h.setOwner(owner.getUUID());
        verdict("hoppers_ignore_ownership", HopperBlockEntity.addItem(null, h, new ItemStack(Items.BREAD, 2), Direction.UP).isEmpty(), "");
    }

    private static net.minecraft.world.InteractionResult use(BlockPos c, ItemStack stack, ServerPlayer who) {
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(c), Direction.UP, c, false);
        return level.getBlockState(c).useItemOn(stack, level, who, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
    }

    private static void useItems() {
        BlockPos c = platform();
        List<String> events = new ArrayList<>();
        ScriptEvents.on("hearth_lit", e -> events.add("lit"));
        ScriptEvents.on("hearth_extinguished", e -> events.add("out"));
        HearthBlockEntity h = hearth(c, HearthTier.STONE, false);
        ItemStack steel = new ItemStack(Items.FLINT_AND_STEEL);
        verdict("a_stranger_cannot_light_it", !level.getBlockState(c).getValue(HearthBlock.LIT) && !use(c, steel, stranger).consumesAction()
                && !level.getBlockState(c).getValue(HearthBlock.LIT), "");
        var lit = use(c, steel, owner);
        verdict("flint_and_steel_lights_it", lit.consumesAction() && level.getBlockState(c).getValue(HearthBlock.LIT) && steel.getDamageValue() == 1, "damage=" + steel.getDamageValue());
        verdict("lighting_fires_the_event", events.equals(List.of("lit")), events.toString());
        ItemStack bread = new ItemStack(Items.BREAD, 6);
        verdict("food_by_hand_goes_in_for_the_owner", use(c, bread, owner).consumesAction() && bread.isEmpty() && h.nutrition() == 30, "n=" + h.nutrition());
        ItemStack more = new ItemStack(Items.BREAD, 2);
        verdict("a_stranger_cannot_feed_it", !use(c, more, stranger).consumesAction() && more.getCount() == 2 && h.nutrition() == 30, "");
        ItemStack junk = new ItemStack(Items.STONE, 2);
        use(c, junk, owner);
        verdict("non_food_by_hand_is_not_taken", junk.getCount() == 2 && h.nutrition() == 30, "");
        verdict("a_shovel_puts_it_out", use(c, new ItemStack(Items.IRON_SHOVEL), owner).consumesAction() && !level.getBlockState(c).getValue(HearthBlock.LIT), "");
        verdict("putting_it_out_fires_the_event", events.equals(List.of("lit", "out")), events.toString());
        ItemStack charge = new ItemStack(Items.FIRE_CHARGE, 2);
        use(c, charge, owner);
        verdict("a_fire_charge_lights_it_and_is_used_up", level.getBlockState(c).getValue(HearthBlock.LIT) && charge.getCount() == 1, "");
        ItemStack goldUp = new ItemStack(ModContent.upgrade(HearthTier.GOLDEN));
        use(c, goldUp, owner);
        verdict("the_golden_item_does_not_skip_a_tier", level.getBlockState(c).getBlock() == ModContent.hearth(HearthTier.STONE), "");
        ItemStack ironUp = new ItemStack(ModContent.upgrade(HearthTier.IRON), 2);
        verdict("a_stranger_cannot_upgrade", !use(c, ironUp, stranger).consumesAction() && ironUp.getCount() == 2, "");
        use(c, ironUp, owner);
        HearthBlockEntity after = (HearthBlockEntity) level.getBlockEntity(c);
        verdict("the_iron_item_upgrades_and_is_used_up", level.getBlockState(c).getBlock() == ModContent.hearth(HearthTier.IRON) && ironUp.getCount() == 1
                && after.nutrition() == 30 && level.getBlockState(c).getValue(HearthBlock.LIT), "");
        // Placing sets the owner.
        BlockPos p = c.offset(5, 0, 5);
        level.setBlock(p, ModContent.hearth(HearthTier.STONE).defaultBlockState(), 3);
        ModContent.hearth(HearthTier.STONE).setPlacedBy(level, p, level.getBlockState(p), stranger, new ItemStack(ModContent.item("stone_hearth")));
        verdict("placing_sets_the_owner", stranger.getUUID().equals(((HearthBlockEntity) level.getBlockEntity(p)).owner()), "");
        verdict("placing_adds_the_upper_half", level.getBlockState(p.above()).getBlock() == ModContent.hearth(HearthTier.STONE)
                && level.getBlockState(p.above()).getValue(HearthBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER, "");
    }

    private static void twoBlocksTall() {
        var upperHalf = net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER;
        BlockPos c = platform();
        HearthBlockEntity h = hearth(c, HearthTier.STONE, false);
        BlockPos up = c.above();
        verdict("a_hearth_is_two_blocks_tall", level.getBlockState(up).getBlock() == ModContent.hearth(HearthTier.STONE)
                && level.getBlockState(up).getValue(HearthBlock.HALF) == upperHalf
                && level.getBlockState(c).getValue(HearthBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER, "");
        verdict("the_upper_half_forwards_to_the_lower_one", level.getBlockEntity(up) instanceof HearthBlockEntity part && part.isUpper() && part.home() == h
                && h.home() == h, "");
        ItemStack steel = new ItemStack(Items.FLINT_AND_STEEL);
        verdict("lighting_the_upper_half_lights_both", use(up, steel, owner).consumesAction() && level.getBlockState(c).getValue(HearthBlock.LIT)
                && level.getBlockState(up).getValue(HearthBlock.LIT), "");
        verdict("only_the_lower_half_gives_light", level.getBlockState(c).getLightEmission() == 13 && level.getBlockState(up).getLightEmission() == 0, "");
        ItemStack bread = new ItemStack(Items.BREAD, 2);
        verdict("food_into_the_upper_half_goes_to_the_lower_one", use(up, bread, owner).consumesAction() && h.nutrition() == 10, "n=" + h.nutrition());
        HearthBlockEntity part = (HearthBlockEntity) level.getBlockEntity(up);
        verdict("hoppers_can_fill_it_through_the_upper_half", HopperBlockEntity.addItem(null, part, new ItemStack(Items.BREAD, 1), Direction.UP).isEmpty()
                && h.nutrition() == 15, "n=" + h.nutrition());
        verdict("the_upper_half_reads_the_same_comparator_signal", part.comparatorSignal() == h.comparatorSignal() && h.comparatorSignal() > 0, "");
        verdict("the_upper_half_holds_nothing_itself", !part.isEmpty() == !h.isEmpty() && part.getContainerSize() == HearthBlockEntity.SLOTS, "");
        verdict("a_shovel_on_the_upper_half_puts_both_out", use(up, new ItemStack(Items.IRON_SHOVEL), owner).consumesAction()
                && !level.getBlockState(c).getValue(HearthBlock.LIT) && !level.getBlockState(up).getValue(HearthBlock.LIT), "");
        use(up, steel, owner);
        ItemStack ironUp = new ItemStack(ModContent.upgrade(HearthTier.IRON));
        verdict("an_upgrade_on_the_upper_half_replaces_both", use(up, ironUp, owner).consumesAction()
                && level.getBlockState(c).getBlock() == ModContent.hearth(HearthTier.IRON) && level.getBlockState(up).getBlock() == ModContent.hearth(HearthTier.IRON)
                && level.getBlockState(up).getValue(HearthBlock.HALF) == upperHalf && level.getBlockState(c).getValue(HearthBlock.LIT)
                && level.getBlockState(up).getValue(HearthBlock.LIT) && ((HearthBlockEntity) level.getBlockEntity(c)).nutrition() == 15, "");
        // Placement needs the block above free.
        BlockPos p = c.offset(5, 0, 0);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(p.below()), Direction.UP, p.below(), false);
        var ctx = new net.minecraft.world.item.context.BlockPlaceContext(level, owner, net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(ModContent.item("stone_hearth")), hit);
        level.setBlock(p.above(), Blocks.STONE.defaultBlockState(), 3);
        verdict("placement_needs_the_block_above_free", ModContent.hearth(HearthTier.STONE).getStateForPlacement(ctx) == null, "");
        level.setBlock(p.above(), Blocks.AIR.defaultBlockState(), 3);
        verdict("placement_works_with_room_above", ModContent.hearth(HearthTier.STONE).getStateForPlacement(ctx) != null, "");
        // Breaking either half removes both and drops the Hearth once.
        BlockPos q = c.offset(0, 0, 5);
        hearth(q, HearthTier.STONE, false);
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(q).inflate(3);
        owner.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        // Without a pickaxe nothing drops from either half (a Hearth needs the right tool, like stone).
        BlockPos w = c.offset(-5, 0, 0);
        hearth(w, HearthTier.STONE, false);
        owner.gameMode.destroyBlock(w.above());
        verdict("without_a_pickaxe_nothing_drops_and_both_halves_go", level.getBlockState(w).isAir() && level.getBlockState(w.above()).isAir()
                && level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(w).inflate(3),
                        e -> e.getItem().is(ModContent.item("stone_hearth"))).isEmpty(), "");
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        owner.gameMode.destroyBlock(q.above());
        List<net.minecraft.world.entity.item.ItemEntity> dropped = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box,
                e -> e.getItem().is(ModContent.item("stone_hearth")));
        verdict("breaking_the_upper_half_removes_both_and_drops_one",
                level.getBlockState(q).isAir() && level.getBlockState(q.above()).isAir() && dropped.size() == 1 && dropped.get(0).getItem().getCount() == 1,
                "drops=" + dropped.size());
        dropped.forEach(net.minecraft.world.entity.Entity::discard);
        BlockPos r = c.offset(0, 0, -5);
        hearth(r, HearthTier.STONE, false);
        owner.gameMode.destroyBlock(r);
        List<net.minecraft.world.entity.item.ItemEntity> dropped2 = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(r).inflate(3), e -> e.getItem().is(ModContent.item("stone_hearth")));
        verdict("breaking_the_lower_half_removes_both_and_drops_one",
                level.getBlockState(r).isAir() && level.getBlockState(r.above()).isAir() && dropped2.size() == 1, "drops=" + dropped2.size());
        dropped2.forEach(net.minecraft.world.entity.Entity::discard);
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        // A half whose partner is gone goes too.
        BlockPos t = c.offset(5, 0, 5);
        hearth(t, HearthTier.STONE, false);
        level.setBlock(t.above(), Blocks.AIR.defaultBlockState(), 3);
        verdict("a_half_without_its_partner_goes_too", level.getBlockState(t).isAir(), "");
    }

    private static void saveAndLoad() {
        BlockPos c = platform();
        HearthBlockEntity h = home(c, HearthTier.STONE, true);
        bed(c.offset(3, 0, 2));
        station(c.offset(-3, 0, 0), Blocks.LOOM);
        food(h, 10);
        h.settleNow(level, "");
        Villager v = villager(h.settlerIds().get(0));
        Settings.set("settle_ticks.stone", 6000);
        h.tickScan(level, 200);
        verdict("the_countdown_started_again_for_the_next_settler", h.progress() == 200, "progress=" + h.progress());
        CompoundTag saved = h.saveWithoutMetadata(level.registryAccess());
        HearthBlockEntity copy = new HearthBlockEntity(c, level.getBlockState(c));
        copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
        verdict("save_keeps_the_food", copy.nutrition() == h.nutrition() && copy.nutrition() > 0, "n=" + copy.nutrition());
        verdict("save_keeps_the_owner", owner.getUUID().equals(copy.owner()), "");
        verdict("save_keeps_the_settlers", copy.settlerIds().equals(h.settlerIds()) && v != null, "");
        verdict("save_keeps_the_countdown", copy.progress() == h.progress(), "progress=" + copy.progress());
        CompoundTag empty = new CompoundTag();
        HearthBlockEntity blank = new HearthBlockEntity(c, level.getBlockState(c));
        blank.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), empty));
        verdict("loading_nothing_gives_an_empty_hearth", blank.isEmpty() && blank.owner() == null && blank.settlerCount() == 0, "");
    }

    private static void trades() {
        BlockPos c = platform();
        MerchantOffer mine = new MerchantOffer(new ItemCost(Items.DIRT, 3), new ItemStack(Items.DIAMOND, 2), 5, 1, 0.05F);
        MerchantOffer extra = new MerchantOffer(new ItemCost(Items.COBBLESTONE, 1), new ItemStack(Items.EMERALD, 1), 9, 1, 0.05F);
        Identifier src = ModContent.id("test");
        HomesteadingData.setTestTrades(Map.of("minecraft:farmer", List.of(new HomesteadingData.TradeFile(src, "replace", 1, List.of(mine)))));
        HearthBlockEntity h = home(c, HearthTier.STONE, true);
        h.settleNow(level, "");
        Villager v = villager(h.settlerIds().get(0));
        verdict("a_replace_file_takes_the_place_of_level_one_trades",
                v != null && v.getOffers().size() == 1 && v.getOffers().get(0).getResult().is(Items.DIAMOND), v == null ? "none" : "offers=" + v.getOffers().size());
        // Add mode keeps the vanilla trades and appends.
        BlockPos d = platform();
        HomesteadingData.setTestTrades(Map.of("minecraft:farmer", List.of(new HomesteadingData.TradeFile(src, "add", 1, List.of(extra)))));
        HearthBlockEntity h2 = home(d, HearthTier.STONE, true);
        h2.settleNow(level, "");
        Villager v2 = villager(h2.settlerIds().get(0));
        int size = v2 == null ? -1 : v2.getOffers().size();
        verdict("an_add_file_appends_to_the_vanilla_trades", v2 != null && size >= 2 && v2.getOffers().get(size - 1).getResult().is(Items.EMERALD), "offers=" + size);
        // Golden settlers start at level two, so only level two files apply to them.
        BlockPos e = platform();
        HomesteadingData.setTestTrades(Map.of("minecraft:farmer", List.of(
                new HomesteadingData.TradeFile(src, "replace", 1, List.of(mine)),
                new HomesteadingData.TradeFile(src, "replace", 2, List.of(extra)))));
        HearthBlockEntity h3 = home(e, HearthTier.GOLDEN, true);
        h3.settleNow(level, "");
        Villager v3 = villager(h3.settlerIds().get(0));
        verdict("a_golden_settler_uses_the_level_two_file", v3 != null && v3.getOffers().size() == 1 && v3.getOffers().get(0).getResult().is(Items.EMERALD), v3 == null ? "none" : "offers=" + v3.getOffers().size());
        HomesteadingData.setTestTrades(null);
        var data = HomesteadingData.get(server);
        verdict("datapack_trades_are_empty_by_default", data.trades().isEmpty() && data.tierOverrides().isEmpty(), data.toString());
    }

    private static void lookAt() {
        BlockPos c = platform();
        HearthBlockEntity h = hearth(c, HearthTier.STONE, true);
        var lines = LookAt.lines(level, h);
        verdict("look_at_has_the_value_lines", lines.size() == 5, lines.size() + " lines");
        verdict("look_at_names_the_missing_bed", lines.get(lines.size() - 1).toString().contains("need.beds"), lines.get(lines.size() - 1).toString());
        bed(c.offset(3, 0, 0));
        rescan(h);
        var withBed = LookAt.lines(level, h);
        verdict("look_at_names_the_missing_workstation", withBed.get(withBed.size() - 1).toString().contains("need.workstations"), withBed.toString());
        station(c.offset(0, 0, 3), Blocks.COMPOSTER);
        rescan(h);
        var noFood = LookAt.lines(level, h);
        verdict("a_food_block_adds_no_extra_line", noFood.size() == 4, noFood.size() + " lines");
        var tag = LookAt.toTag(LookAt.values(level, h));
        var round = LookAt.fromTag(tag);
        verdict("look_at_values_survive_the_data_tag", round.equals(LookAt.values(level, h)), round.toString());
    }

    private static void scripting() {
        BlockPos c = platform();
        HearthBlockEntity h = home(c, HearthTier.STONE, true);
        var api = Bridges.API;
        int x = c.getX();
        int y = c.getY();
        int z = c.getZ();
        Map<String, Object> rep = api.report(x, y, z);
        verdict("report_has_every_requirement", rep != null && rep.keySet().containsAll(List.of("tier", "lit", "settlers", "population", "beds", "workstations", "light", "food",
                "foodRequired", "progress", "settleTicks", "blocked", "settlerIds")), String.valueOf(rep));
        verdict("report_is_empty_where_there_is_no_hearth", api.report(x + 100, y, z) == null, "");
        verdict("settlers_of_is_empty_before_arrival", api.settlersOf(x, y, z).isEmpty(), "");
        verdict("settle_now_by_profession_refuses_a_missing_one", !api.settleNow(x, y, z, "minecraft:librarian"), "");
        boolean ok = api.settleNow(x, y, z, "minecraft:farmer");
        verdict("settle_now_brings_the_named_profession", ok && api.settlersOf(x, y, z).size() == 1, "");
        for (String id : api.settlersOf(x, y, z)) {
            villager(id);
        }
        verdict("report_counts_the_settler", (int) api.report(x, y, z).get("settlers") == 1, "");
        verdict("recipes_list_shows_thirteen", api.recipes().size() == 13 && api.recipes().get(0).containsKey("workstation"), "");
        verdict("tiers_and_events_lists", api.tiers().equals(List.of("stone", "iron", "golden")) && api.events().size() == 5, "");
        verdict("settings_can_be_read_and_set", api.settingKeys().contains("radius.stone") && api.setting("radius.stone").equals("8"), "");
        api.setSetting("radius.stone", "9");
        verdict("set_setting_changes_the_tier", HearthTier.STONE.radius() == 9, "");
        boolean threw = false;
        try {
            api.setSetting("nope", "1");
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        verdict("unknown_settings_throw", threw, "");
        verdict("api_report_matches_the_block_entity", api.report(x, y, z).get("settleTicks").equals(HearthTier.STONE.settleTicks()), "");
        verdict("api_works_with_a_dimension", api.report("minecraft:overworld", x, y, z) != null && api.report("minecraft:the_nether", x, y, z) == null, "");
    }

    private static void optionalAbsent() {
        boolean withModjs = System.getenv("THOROUGHHOMESTEADING_MODJS_JAR") != null;
        verdict("modjs_presence_is_detected_and_nothing_breaks", ScriptBridge.install() == withModjs, "modjs=" + withModjs);
        verdict("kubejs_is_not_installed_and_nothing_breaks", !KubeJsBridge.present(), "");
        verdict("barely_questing_is_not_installed_and_nothing_breaks", !BarelyQuestingBridge.present() && !BarelyQuestingBridge.install(ScriptHost::ledger), "");
        verdict("bridges_install_reports_nothing_hooked", Bridges.install(java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "th-selftest-nokube")).isBlank() != withModjs, "");
        verdict("the_viewer_fallback_is_the_defaults_table", SettlingRecipes.forViewers().size() == 13, "");
        List<SettlingEntry> live = new ArrayList<>(SettlingRecipes.entries(server));
        SettlingRecipes.setClient(live.subList(0, 2));
        verdict("viewers_use_what_the_server_sent", SettlingRecipes.forViewers().size() == 2, "");
        SettlingRecipes.clearClient();
        verdict("viewers_fall_back_after_leaving", SettlingRecipes.forViewers().size() == 13, "");
        RecipeHolder<?> any = server.getRecipeManager().getRecipes().stream().filter(r -> r.value() instanceof SettlingRecipe).findFirst().orElse(null);
        verdict("settling_recipes_live_in_the_recipe_manager", any != null, "");
    }
}
