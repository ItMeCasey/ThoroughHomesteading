package com.itmecasey.thoroughhomesteading.compat;

import com.itmecasey.bei.api.BeiPlugin;
import com.itmecasey.bei.api.CustomRecipe;
import com.itmecasey.bei.api.IBeiPlugin;
import com.itmecasey.bei.api.ICategoryRegistration;
import com.itmecasey.bei.api.IRecipeRegistration;
import com.itmecasey.bei.api.RecipeCategory;
import com.itmecasey.thoroughhomesteading.HearthTier;
import com.itmecasey.thoroughhomesteading.ModContent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * BEI: three categories of our own: Settling (workstation and extra items in, the profession out, with food and tier as values), Hearth
 * Upgrade (a Hearth and an upgrade item give the next Hearth) and Hearth (information pages for each Hearth and each workstation with a
 * settling recipe). The crafting recipes show in BEI's own crafting category. Loaded only when BEI is installed.
 */
@BeiPlugin
public class ThoroughHomesteadingBeiPlugin implements IBeiPlugin {
    private static RecipeCategory settling;
    private static RecipeCategory upgrade;
    private static RecipeCategory info;

    private static RecipeCategory settlingCategory() {
        if (settling == null) {
            settling = RecipeCategory.builder(ModContent.id("settling"))
                    .title(Component.translatable("viewer.thoroughhomesteading.settling"))
                    .station(new ItemStack(ModContent.hearth(HearthTier.STONE)))
                    .size(120, 40)
                    .input(10, 12).input(36, 12).input(62, 12)
                    .output(94, 12)
                    .build();
        }
        return settling;
    }

    private static RecipeCategory upgradeCategory() {
        if (upgrade == null) {
            upgrade = RecipeCategory.builder(ModContent.id("hearth_upgrade"))
                    .title(Component.translatable("viewer.thoroughhomesteading.hearth_upgrade"))
                    .station(new ItemStack(ModContent.upgrade(HearthTier.IRON)))
                    .size(120, 40)
                    .input(10, 12).input(36, 12)
                    .output(94, 12)
                    .build();
        }
        return upgrade;
    }

    private static RecipeCategory infoCategory() {
        if (info == null) {
            info = RecipeCategory.builder(ModContent.id("hearth_info"))
                    .title(Component.translatable("viewer.thoroughhomesteading.info"))
                    .station(new ItemStack(ModContent.hearth(HearthTier.STONE)))
                    .size(120, 40)
                    .input(10, 12)
                    .output(94, 12)
                    .build();
        }
        return info;
    }

    @Override
    public void registerCategories(ICategoryRegistration registration) {
        registration.register(settlingCategory());
        registration.register(upgradeCategory());
        registration.register(infoCategory());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        int n = 0;
        for (ViewerData.Row row : ViewerData.settlingRows()) {
            List<ItemStack> inputs = new ArrayList<>();
            inputs.add(row.workstation());
            inputs.addAll(row.extras());
            registration.add(settlingCategory(), CustomRecipe.builder().id("settling_" + (n++))
                    .input(inputs).output(row.result()).build());
        }
        int u = 0;
        for (ViewerData.Upgrade up : ViewerData.hearthUpgrades()) {
            registration.add(upgradeCategory(), CustomRecipe.builder().id("upgrade_" + (u++))
                    .input(up.hearth(), up.with()).output(up.result()).build());
        }
        // An information page shows the item and, as the output, a paper named with the value lines (one stack per line would be too wide).
        int i = 0;
        for (ViewerData.Info page : ViewerData.infoPages()) {
            ItemStack paper = new ItemStack(Items.PAPER);
            paper.set(net.minecraft.core.component.DataComponents.ITEM_NAME, page.subject().getHoverName());
            paper.set(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(page.lines()));
            registration.add(infoCategory(), CustomRecipe.builder().id("info_" + (i++)).input(page.subject()).output(paper).build());
        }
    }
}
