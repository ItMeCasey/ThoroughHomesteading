package com.itmecasey.thoroughhomesteading.compat;

import com.itmecasey.thoroughhomesteading.HearthTier;
import com.itmecasey.thoroughhomesteading.ModContent;
import com.itmecasey.thoroughhomesteading.SettlingEntry;
import com.itmecasey.thoroughhomesteading.SettlingRecipes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/**
 * What every recipe viewer shows, worked out once: the Settling rows (workstation and extras in, a named villager egg out, with the food and
 * tier as values on the egg), the Hearth upgrades and the information pages. Shared by the BEI, JEI and REI plugins.
 */
public final class ViewerData {
    /** One Settling recipe: the workstation and extra items in, the profession result out. */
    public record Row(SettlingEntry entry, ItemStack workstation, List<ItemStack> extras, ItemStack result, Component profession) {
    }

    /** A Hearth, an upgrade item and the Hearth that comes out. */
    public record Upgrade(ItemStack hearth, ItemStack with, ItemStack result) {
    }

    /** An information page: the item it belongs to and the value lines. */
    public record Info(ItemStack subject, List<Component> lines) {
    }

    private ViewerData() {
    }

    public static ItemStack stack(String id, int count) {
        Identifier identifier = Identifier.tryParse(id);
        Item item = identifier == null ? Items.AIR : BuiltInRegistries.ITEM.getOptional(identifier).orElse(Items.AIR);
        return new ItemStack(item, Math.max(1, count));
    }

    public static Component professionName(String id) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null) {
            return Component.literal(id);
        }
        return Component.translatableWithFallback("entity." + identifier.getNamespace() + ".villager." + identifier.getPath(),
                identifier.getPath().substring(0, 1).toUpperCase(java.util.Locale.ROOT) + identifier.getPath().substring(1).replace('_', ' '));
    }

    public static Component tierName(int minTier) {
        HearthTier tier = HearthTier.values()[Math.max(0, Math.min(HearthTier.values().length, minTier) - 1)];
        return Component.translatable("viewer.thoroughhomesteading.tier", tier.title());
    }

    public static List<Row> settlingRows() {
        List<Row> rows = new ArrayList<>();
        for (SettlingEntry e : SettlingRecipes.forViewers()) {
            ItemStack workstation = stack(e.workstation(), 1);
            if (workstation.isEmpty()) {
                continue;
            }
            List<ItemStack> extras = new ArrayList<>();
            for (SettlingEntry.Extra x : e.extra()) {
                extras.add(stack(x.item(), x.count()));
            }
            Component name = professionName(e.profession());
            ItemStack result = new ItemStack(Items.VILLAGER_SPAWN_EGG);
            result.set(DataComponents.ITEM_NAME, name);
            result.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.translatable("viewer.thoroughhomesteading.info.food", e.foodNeeded()),
                    tierName(e.minTier()))));
            rows.add(new Row(e, workstation, extras, result, name));
        }
        return rows;
    }

    public static List<Upgrade> hearthUpgrades() {
        List<Upgrade> out = new ArrayList<>();
        HearthTier[] tiers = HearthTier.values();
        for (int i = 1; i < tiers.length; i++) {
            out.add(new Upgrade(new ItemStack(ModContent.hearth(tiers[i - 1])), new ItemStack(ModContent.upgrade(tiers[i])), new ItemStack(ModContent.hearth(tiers[i]))));
        }
        return out;
    }

    /** One page per Hearth, and one per workstation that has a settling recipe. */
    public static List<Info> infoPages() {
        List<Info> pages = new ArrayList<>();
        for (HearthTier tier : HearthTier.values()) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("viewer.thoroughhomesteading.info.radius", tier.radius()));
            lines.add(Component.translatable("viewer.thoroughhomesteading.info.population", tier.population()));
            lines.add(Component.translatable("viewer.thoroughhomesteading.info.seconds", tier.settleTicks() / 20));
            lines.add(Component.translatable("viewer.thoroughhomesteading.info.level", tier.startLevel()));
            pages.add(new Info(new ItemStack(ModContent.hearth(tier)), lines));
        }
        Map<String, List<Component>> byWorkstation = new LinkedHashMap<>();
        for (SettlingEntry e : SettlingRecipes.forViewers()) {
            List<Component> lines = byWorkstation.computeIfAbsent(e.workstation(), k -> new ArrayList<>());
            lines.add(Component.translatable("viewer.thoroughhomesteading.info.profession", professionName(e.profession())));
            lines.add(Component.translatable("viewer.thoroughhomesteading.info.food", e.foodNeeded()));
            lines.add(tierName(e.minTier()));
        }
        for (Map.Entry<String, List<Component>> e : byWorkstation.entrySet()) {
            ItemStack subject = stack(e.getKey(), 1);
            if (!subject.isEmpty()) {
                pages.add(new Info(subject, e.getValue()));
            }
        }
        return pages;
    }
}
