package com.huwng.alterna.event;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.block.ModBlocks;
import com.huwng.alterna.item.ModItems;
import net.minecraft.world.level.block.ComposterBlock;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public class ModCommonSetup {

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        Alterna.LOGGER.info("HELLO FROM COMMON SETUP");

        event.enqueueWork(() -> {
            ComposterBlock.COMPOSTABLES.put(ModItems.PURPLE_SUGAR_CANE.get(), 0.65F);
            ComposterBlock.COMPOSTABLES.put(ModItems.ENOKI_MUSHROOM.get(), 0.65F);
            ComposterBlock.COMPOSTABLES.put(ModItems.CLOUD_BERRIES.get(), 0.30F);
            ComposterBlock.COMPOSTABLES.put(ModItems.WHITE_CURRANT_BERRIES.get(), 0.30F);

            ComposterBlock.COMPOSTABLES.put(ModBlocks.ROOTSHROOM_STEM.get().asItem(), 0.85F);
            ComposterBlock.COMPOSTABLES.put(ModBlocks.ROOTSHROOM_LEAVES.get().asItem(), 0.30F);
            ComposterBlock.COMPOSTABLES.put(ModBlocks.ROOTSHROOM_FUNGUS.get().asItem(), 0.65F);
            ComposterBlock.COMPOSTABLES.put(ModBlocks.ROOTSHROOM_LIGHT.get().asItem(), 0.65F);

            ComposterBlock.COMPOSTABLES.put(ModBlocks.WILD_MOSS_BLOCK.get().asItem(), 0.65F);
            ComposterBlock.COMPOSTABLES.put(ModBlocks.WILD_MOSS_CARPET.get().asItem(), 0.30F);
            ComposterBlock.COMPOSTABLES.put(ModBlocks.WILD_HANGING_MOSS.get().asItem(), 0.30F);

            ComposterBlock.COMPOSTABLES.put(ModBlocks.STAR_LILY.get().asItem(), 0.65F);
            ComposterBlock.COMPOSTABLES.put(ModBlocks.STAR_LILY_VINE.get().asItem(), 0.30F);
            ComposterBlock.COMPOSTABLES.put(ModBlocks.SHORT_DAZE.get().asItem(), 0.30F);
            ComposterBlock.COMPOSTABLES.put(ModBlocks.TALL_DAZE.get().asItem(), 0.65F);
        });
    }
}
