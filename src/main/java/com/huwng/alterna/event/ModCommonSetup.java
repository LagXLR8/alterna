package com.huwng.alterna.event;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.Config;
import com.huwng.alterna.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public class ModCommonSetup {

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        Alterna.LOGGER.info("HELLO FROM COMMON SETUP");

        event.enqueueWork(() -> {
            ComposterBlock.COMPOSTABLES.put(ModItems.PURPLE_SUGAR_CANE.get(), 0.65F);
        });

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            Alterna.LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        Alterna.LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> Alterna.LOGGER.info("ITEM >> {}", item));
    }
}
