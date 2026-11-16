package com.itmecasey.thoroughhomesteading.compat;

import com.itmecasey.thoroughhomesteading.HearthTier;
import com.itmecasey.thoroughhomesteading.ModContent;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * REI: three display categories of our own, Settling (workstation and extra items in, the profession out, with food and tier as values), Hearth
 * Upgrade (a Hearth and an upgrade item give the next Hearth) and Hearth (information pages for each Hearth and each workstation with a settling
 * recipe). The crafting recipes show in REI's own crafting category. Loaded only when REI is installed.
 */
@REIPluginClient
public class ThoroughHomesteadingReiPlugin implements REIClientPlugin {
    private static final CategoryIdentifier<PageDisplay> SETTLING = CategoryIdentifier.of("thoroughhomesteading", "settling");
    private static final CategoryIdentifier<PageDisplay> UPGRADE = CategoryIdentifier.of("thoroughhomesteading", "hearth_upgrade");
    private static final CategoryIdentifier<PageDisplay> INFO = CategoryIdentifier.of("thoroughhomesteading", "hearth_info");

    /** What REI shows for one entry: input items, output items and value lines. */
    public static final class PageDisplay extends BasicDisplay {
        private final CategoryIdentifier<PageDisplay> category;
        private final List<Component> lines;

        PageDisplay(CategoryIdentifier<PageDisplay> category, List<ItemStack> inputs, List<ItemStack> outputs, List<Component> lines) {
            super(inputs.stream().map(s -> EntryIngredient.of(EntryStacks.of(s))).toList(), outputs.stream().map(s -> EntryIngredient.of(EntryStacks.of(s))).toList());
            this.category = category;
            this.lines = lines;
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return category;
        }

        /** These displays are only built on the client and never saved or sent. */
        @Override
        public me.shedaniel.rei.api.common.display.DisplaySerializer<? extends me.shedaniel.rei.api.common.display.Display> getSerializer() {
            return null;
        }
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new Category(SETTLING, Component.translatable("viewer.thoroughhomesteading.settling"), EntryStacks.of(new ItemStack(ModContent.hearth(HearthTier.STONE))), 0));
        registry.add(new Category(UPGRADE, Component.translatable("viewer.thoroughhomesteading.hearth_upgrade"), EntryStacks.of(new ItemStack(ModContent.upgrade(HearthTier.IRON))), 0));
        registry.add(new Category(INFO, Component.translatable("viewer.thoroughhomesteading.info"), EntryStacks.of(new ItemStack(ModContent.hearth(HearthTier.GOLDEN))), 4));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        for (ViewerData.Row row : ViewerData.settlingRows()) {
            List<ItemStack> inputs = new ArrayList<>();
            inputs.add(row.workstation());
            inputs.addAll(row.extras());
            registry.add(new PageDisplay(SETTLING, inputs, List.of(row.result()), List.of()));
        }
        for (ViewerData.Upgrade up : ViewerData.hearthUpgrades()) {
            registry.add(new PageDisplay(UPGRADE, List.of(up.hearth(), up.with()), List.of(up.result()), List.of()));
        }
        for (ViewerData.Info page : ViewerData.infoPages()) {
            registry.add(new PageDisplay(INFO, List.of(page.subject()), List.of(), page.lines()));
        }
    }

    private record Category(CategoryIdentifier<PageDisplay> id, Component title, Renderer icon, int lineCount) implements DisplayCategory<PageDisplay> {
        @Override
        public CategoryIdentifier<? extends PageDisplay> getCategoryIdentifier() {
            return id;
        }

        @Override
        public Component getTitle() {
            return title;
        }

        @Override
        public Renderer getIcon() {
            return icon;
        }

        @Override
        public int getDisplayHeight() {
            return 36 + lineCount * 10;
        }

        @Override
        public List<Widget> setupDisplay(PageDisplay display, Rectangle bounds) {
            List<Widget> widgets = new ArrayList<>();
            widgets.add(Widgets.createRecipeBase(bounds));
            Point origin = new Point(bounds.getCenterX() - 41, bounds.y + 9);
            int x = origin.x;
            for (EntryIngredient in : display.getInputEntries()) {
                widgets.add(Widgets.createSlot(new Point(x, origin.y)).entries(in).markInput());
                x += 22;
            }
            if (!display.getOutputEntries().isEmpty()) {
                widgets.add(Widgets.createArrow(new Point(origin.x + 66, origin.y + 1)));
                widgets.add(Widgets.createSlot(new Point(origin.x + 94, origin.y)).entries(display.getOutputEntries().get(0)).markOutput());
            }
            int y = origin.y + 24;
            for (Component line : display.lines) {
                widgets.add(Widgets.createLabel(new Point(bounds.x + 8, y), line).leftAligned());
                y += 10;
            }
            return widgets;
        }
    }
}
