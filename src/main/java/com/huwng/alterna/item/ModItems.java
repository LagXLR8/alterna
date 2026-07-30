package com.huwng.alterna.item;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.block.ModBlocks;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Alterna.MODID);

    public static final DeferredItem<RiftDetectorItem> RIFT_DETECTOR = registerItem("rift_detector",
            RiftDetectorItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<BlockItem> PURPLE_SUGAR_CANE = registerItem("purple_sugar_cane",
            p -> new BlockItem(ModBlocks.PURPLE_SUGAR_CANE.get(), p),
            new Item.Properties());

    public static final DeferredItem<BlockItem> CLOUD_BERRIES = registerItem("cloud_berries",
            p -> new BlockItem(ModBlocks.CLOUDBERRY_VINES.get(), p),
            new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.4F).build()));

    public static final DeferredItem<BlockItem> WHITE_CURRANT_BERRIES = registerItem("white_currant_berries",
            p -> new BlockItem(ModBlocks.WHITE_CURRANT_BERRY_BUSH.get(), p),
            new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.4F).build()));

    private static <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, I> factory, Item.Properties properties) {
        return ITEMS.registerItem(name, factory, () -> properties);
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
