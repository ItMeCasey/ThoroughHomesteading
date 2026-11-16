package com.itmecasey.thoroughhomesteading.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.itmecasey.thoroughhomesteading.HearthTier;
import com.itmecasey.thoroughhomesteading.Settings;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * The two datapack folders besides recipes: {@code data/<namespace>/thoroughhomesteading/trades/<profession>.json} (add or replace a
 * profession's trades at one level) and {@code .../thoroughhomesteading/hearth/<tier>.json} (radius, population and settle_ticks of a
 * tier). Read from the server's resource manager whenever it changes, so /reload picks up edits with no listener to register.
 */
public final class HomesteadingData {
    private static final Logger LOG = Logger.getLogger("thoroughhomesteading");

    /** One trade file: how it applies and the offers it carries. */
    public record TradeFile(Identifier source, String mode, int level, List<MerchantOffer> offers) {
        public boolean replaces() {
            return "replace".equals(mode);
        }
    }

    /** Everything read from the datapacks. */
    public record Data(Map<String, List<TradeFile>> trades, Map<String, Integer> tierOverrides) {
    }

    private static volatile Map<String, List<TradeFile>> testTrades;
    private static volatile ResourceManager lastManager;
    private static volatile Data lastData = new Data(Map.of(), Map.of());

    private HomesteadingData() {
    }

    /** The current data; reloads it when the server's resource manager is a new one (after /reload) and applies the tier overrides. */
    /** Self test hook: trade files used instead of the datapacks' until it is called again with null. */
    public static void setTestTrades(Map<String, List<TradeFile>> trades) {
        testTrades = trades;
    }

    public static synchronized Data get(MinecraftServer server) {
        if (testTrades != null) {
            return new Data(testTrades, lastData.tierOverrides());
        }
        ResourceManager manager = server.getResourceManager();
        if (manager != lastManager) {
            lastData = load(server, manager);
            lastManager = manager;
            Settings.setPackOverrides(lastData.tierOverrides());
        }
        return lastData;
    }

    private static Data load(MinecraftServer server, ResourceManager manager) {
        Map<String, List<TradeFile>> trades = new LinkedHashMap<>();
        var ops = server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        Map<Identifier, Resource> found = manager.listResources("thoroughhomesteading/trades", id -> id.getPath().endsWith(".json"));
        List<Identifier> ids = new ArrayList<>(found.keySet());
        ids.sort(java.util.Comparator.comparing(Identifier::toString));
        for (Identifier id : ids) {
            String path = id.getPath().substring("thoroughhomesteading/trades/".length(), id.getPath().length() - ".json".length());
            String first = path.contains("/") ? path.substring(0, path.indexOf('/')) : path;
            String profession = id.getNamespace() + ":" + first;
            try (Reader reader = found.get(id).openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                String mode = root.has("mode") ? root.get("mode").getAsString() : "add";
                int level = root.has("level") ? Math.max(1, Math.min(5, root.get("level").getAsInt())) : 1;
                List<MerchantOffer> offers = new ArrayList<>();
                JsonArray array = root.has("trades") ? root.getAsJsonArray("trades") : new JsonArray();
                for (JsonElement el : array) {
                    var parsed = MerchantOffer.CODEC.parse(ops, el);
                    var offer = parsed.result();
                    if (offer.isPresent()) {
                        offers.add(offer.get());
                    } else {
                        LOG.warning("ThoroughHomesteading: a trade in " + id + " is not valid: " + parsed.error().map(e -> e.message()).orElse("?"));
                    }
                }
                trades.computeIfAbsent(profession, k -> new ArrayList<>()).add(new TradeFile(id, mode, level, offers));
            } catch (IOException | RuntimeException e) {
                LOG.warning("ThoroughHomesteading: could not read the trade file " + id + ": " + e);
            }
        }
        Map<String, Integer> overrides = new LinkedHashMap<>();
        Map<Identifier, Resource> tierFiles = manager.listResources("thoroughhomesteading/hearth", id -> id.getPath().endsWith(".json"));
        List<Identifier> tierIds = new ArrayList<>(tierFiles.keySet());
        tierIds.sort(java.util.Comparator.comparing(Identifier::toString));
        for (Identifier id : tierIds) {
            String name = id.getPath().substring("thoroughhomesteading/hearth/".length(), id.getPath().length() - ".json".length());
            HearthTier tier = HearthTier.byId(name);
            if (tier == null) {
                continue;
            }
            try (Reader reader = tierFiles.get(id).openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                for (String key : new String[] {"radius", "population", "settle_ticks"}) {
                    if (root.has(key)) {
                        Settings.Def def = Settings.def(key + "." + tier.id());
                        int value = (int) Math.max(def.min(), Math.min(def.max(), root.get(key).getAsInt()));
                        overrides.put(key + "." + tier.id(), value);
                    }
                }
            } catch (IOException | RuntimeException e) {
                LOG.warning("ThoroughHomesteading: could not read the hearth file " + id + ": " + e);
            }
        }
        return new Data(trades, overrides);
    }

    /**
     * Applies the trade files for one level to the offers {@code from} onward (the offers the game just added for that level): a
     * replace file takes the place of them, an add file appends. Returns the new number of offers.
     */
    public static int applyLevel(MinecraftServer server, Villager villager, String profession, int level, int from) {
        MerchantOffers offers = villager.getOffers();
        List<TradeFile> files = get(server).trades().get(profession);
        if (files == null) {
            return offers.size();
        }
        List<MerchantOffer> replacement = new ArrayList<>();
        boolean replace = false;
        List<MerchantOffer> added = new ArrayList<>();
        for (TradeFile f : files) {
            if (f.level() != level) {
                continue;
            }
            if (f.replaces()) {
                replace = true;
                replacement.addAll(f.offers());
            } else {
                added.addAll(f.offers());
            }
        }
        if (replace && from <= offers.size()) {
            offers.subList(from, offers.size()).clear();
            offers.addAll(replacement);
        }
        offers.addAll(added);
        return offers.size();
    }
}
