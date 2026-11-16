package com.itmecasey.thoroughhomesteading.compat;

import com.itmecasey.thoroughhomesteading.HearthTier;
import com.itmecasey.thoroughhomesteading.ModContent;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * JEI: two recipe categories of our own, Settling (workstation and extra items in, the profession out, with food and tier as values) and
 * Hearth Upgrade (a Hearth and an upgrade item give the next Hearth), plus an information page on each Hearth and on each workstation that has
 * a settling recipe. The crafting recipes show in JEI's own crafting category. Loaded only when JEI is installed.
 */
@JeiPlugin
public class ThoroughHomesteadingJeiPlugin implements IModPlugin {
    private static final IRecipeType<ViewerData.Row> SETTLING = IRecipeType.create(Identifier.fromNamespaceAndPath("thoroughhomesteading", "settling"), ViewerData.Row.class);
    private static final IRecipeType<ViewerData.Upgrade> UPGRADE = IRecipeType.create(Identifier.fromNamespaceAndPath("thoroughhomesteading", "hearth_upgrade"), ViewerData.Upgrade.class);

    @Override
    public Identifier getPluginUid() {
        // JEI asks for this while mods are still loading; touching ModContent here would build the blocks too early.
        return Identifier.fromNamespaceAndPath("thoroughhomesteading", "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new Category<ViewerData.Row>(SETTLING, Component.translatable("viewer.thoroughhomesteading.settling"),
                        gui.createDrawableItemStack(new ItemStack(ModContent.hearth(HearthTier.STONE))), 120) {
                    @Override
                    public void setRecipe(IRecipeLayoutBuilder builder, ViewerData.Row row, IFocusGroup focuses) {
                        builder.addSlot(RecipeIngredientRole.INPUT, 4, 4).add(row.workstation());
                        int x = 30;
                        for (ItemStack extra : row.extras()) {
                            builder.addSlot(RecipeIngredientRole.INPUT, x, 4).add(extra);
                            x += 22;
                        }
                        builder.addSlot(RecipeIngredientRole.OUTPUT, 98, 4).add(row.result());
                    }
                },
                new Category<ViewerData.Upgrade>(UPGRADE, Component.translatable("viewer.thoroughhomesteading.hearth_upgrade"),
                        gui.createDrawableItemStack(new ItemStack(ModContent.upgrade(HearthTier.IRON))), 100) {
                    @Override
                    public void setRecipe(IRecipeLayoutBuilder builder, ViewerData.Upgrade up, IFocusGroup focuses) {
                        builder.addSlot(RecipeIngredientRole.INPUT, 4, 4).add(up.hearth());
                        builder.addSlot(RecipeIngredientRole.INPUT, 30, 4).add(up.with());
                        builder.addSlot(RecipeIngredientRole.OUTPUT, 78, 4).add(up.result());
                    }
                });
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(SETTLING, ViewerData.settlingRows());
        registration.addRecipes(UPGRADE, ViewerData.hearthUpgrades());
        for (ViewerData.Info page : ViewerData.infoPages()) {
            registration.addItemStackInfo(page.subject(), page.lines().toArray(new Component[0]));
        }
    }

    private abstract static class Category<T> implements IRecipeCategory<T> {
        private final IRecipeType<T> type;
        private final Component title;
        private final IDrawable icon;
        private final int width;

        Category(IRecipeType<T> type, Component title, IDrawable icon, int width) {
            this.type = type;
            this.title = title;
            this.icon = icon;
            this.width = width;
        }

        @Override
        public IRecipeType<T> getRecipeType() {
            return type;
        }

        @Override
        public Component getTitle() {
            return title;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return 26;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }
    }
}
