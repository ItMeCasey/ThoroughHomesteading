package com.itmecasey.thoroughhomesteading;

import com.itmecasey.thoroughhomesteading.block.HearthBlock;
import com.itmecasey.thoroughhomesteading.block.HearthBlockEntity;
import com.itmecasey.thoroughhomesteading.item.HearthUpgradeItem;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Every block, item, block entity type and recipe type the mod adds, built once and handed to the loader's registry event. */
public final class ModContent {
    public static final String MODID = ThoroughHomesteading.MODID;

    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    private static final Map<HearthTier, HearthBlock> HEARTHS = new EnumMap<>(HearthTier.class);
    private static final Map<HearthTier, HearthUpgradeItem> UPGRADES = new EnumMap<>(HearthTier.class);
    public static final BlockEntityType<HearthBlockEntity> HEARTH;
    public static final RecipeType<SettlingRecipe> SETTLING_TYPE = RecipeType.simple(id("settling"));
    public static final RecipeSerializer<SettlingRecipe> SETTLING_SERIALIZER = new RecipeSerializer<>(SettlingRecipe.CODEC, SettlingRecipe.STREAM_CODEC);

    static {
        MapColor[] colors = {MapColor.STONE, MapColor.METAL, MapColor.GOLD};
        for (HearthTier tier : HearthTier.values()) {
            String name = tier.blockName();
            HearthBlock block = new HearthBlock(tier, BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id(name)))
                    .mapColor(colors[tier.ordinal()]).strength(3.0F + tier.ordinal(), 6.0F + 3 * tier.ordinal()).sound(SoundType.STONE)
                    .requiresCorrectToolForDrops().noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.IMMOVEABLE)
                    .lightLevel(s -> s.getValue(HearthBlock.LIT)
                            && s.getValue(HearthBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER ? 13 : 0));
            HEARTHS.put(tier, block);
            BLOCKS.put(name, block);
            ITEMS.put(name, new BlockItem(block, itemProps(name).useBlockDescriptionPrefix()));
        }
        HEARTH = new BlockEntityType<>(HearthBlockEntity::new, Set.copyOf(HEARTHS.values()));
        for (HearthTier tier : HearthTier.values()) {
            if (tier.level() > 1) {
                String name = tier.upgradeName();
                HearthUpgradeItem item = new HearthUpgradeItem(tier, itemProps(name));
                UPGRADES.put(tier, item);
                ITEMS.put(name, item);
            }
        }
    }

    private ModContent() {
    }

    public static Item.Properties itemProps(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(name)));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static HearthBlock hearth(HearthTier tier) {
        return HEARTHS.get(tier);
    }

    public static HearthUpgradeItem upgrade(HearthTier tier) {
        return UPGRADES.get(tier);
    }

    public static Item item(String name) {
        return ITEMS.get(name);
    }

    public static Map<String, Block> blocks() {
        return BLOCKS;
    }

    public static Map<String, Item> items() {
        return ITEMS;
    }

    public static CreativeModeTab creativeTab() {
        return CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.thoroughhomesteading"))
                .icon(() -> new ItemStack(ITEMS.get("stone_hearth")))
                .displayItems((params, output) -> ITEMS.values().forEach(output::accept))
                .build();
    }

    public static void registerBlocks(BiConsumer<String, Block> out) {
        BLOCKS.forEach(out);
    }

    public static void registerItems(BiConsumer<String, Item> out) {
        ITEMS.forEach(out);
    }

    public static void registerBlockEntities(BiConsumer<String, BlockEntityType<?>> out) {
        out.accept("hearth", HEARTH);
    }

    public static void registerRecipeTypes(BiConsumer<String, RecipeType<?>> out) {
        out.accept("settling", SETTLING_TYPE);
    }

    public static void registerRecipeSerializers(BiConsumer<String, RecipeSerializer<?>> out) {
        out.accept("settling", SETTLING_SERIALIZER);
    }
}
