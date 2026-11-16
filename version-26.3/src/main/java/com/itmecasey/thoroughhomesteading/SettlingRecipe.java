package com.itmecasey.thoroughhomesteading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The {@code thoroughhomesteading:settling} recipe: a workstation block, the profession a settler gets from it, the lowest Hearth tier that
 * may use it, the nutrition of food it spends (0 or missing means the {@code food_required} setting) and extra items spent with the food.
 * Datapacks, ModJS and KubeJS add, change and remove these like any recipe.
 */
public record SettlingRecipe(Identifier workstation, Identifier profession, int minTier, int food, List<Extra> extra) implements Recipe<SingleRecipeInput> {
    /** An extra item spent on arrival. */
    public record Extra(Identifier item, int count) {
        public static final Codec<Extra> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("item").forGetter(Extra::item),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Extra::count)).apply(i, Extra::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Extra> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Extra::item, ByteBufCodecs.VAR_INT, Extra::count, Extra::new);
    }

    public static final MapCodec<SettlingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Identifier.CODEC.fieldOf("workstation").forGetter(SettlingRecipe::workstation),
            Identifier.CODEC.fieldOf("profession").forGetter(SettlingRecipe::profession),
            Codec.INT.optionalFieldOf("min_tier", 1).forGetter(SettlingRecipe::minTier),
            Codec.INT.optionalFieldOf("food", 0).forGetter(SettlingRecipe::food),
            Extra.CODEC.listOf().optionalFieldOf("extra", List.of()).forGetter(SettlingRecipe::extra)).apply(i, SettlingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SettlingRecipe> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, SettlingRecipe::workstation,
            Identifier.STREAM_CODEC, SettlingRecipe::profession,
            ByteBufCodecs.VAR_INT, SettlingRecipe::minTier,
            ByteBufCodecs.VAR_INT, SettlingRecipe::food,
            Extra.STREAM_CODEC.apply(ByteBufCodecs.list()), SettlingRecipe::extra,
            SettlingRecipe::new);

    /** The plain-data form the home check and the viewers use. */
    public SettlingEntry toEntry() {
        List<SettlingEntry.Extra> out = new ArrayList<>();
        for (Extra e : extra) {
            out.add(new SettlingEntry.Extra(e.item().toString(), e.count()));
        }
        return new SettlingEntry(workstation.toString(), profession.toString(), Math.max(1, minTier), Math.max(0, food), out);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<? extends Recipe<SingleRecipeInput>> getSerializer() {
        return ModContent.SETTLING_SERIALIZER;
    }

    @Override
    public RecipeType<? extends Recipe<SingleRecipeInput>> getType() {
        return ModContent.SETTLING_TYPE;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
