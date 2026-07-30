package com.huwng.alterna;

import com.huwng.alterna.block.ModBlocks;
import com.huwng.alterna.item.ModItems;
import com.huwng.alterna.worldgen.AlternaFeatures;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.huwng.alterna.worldgen.AlternaStructureTypes;
import com.huwng.alterna.worldgen.AlternaStructurePieceTypes;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Alterna.MODID)
public class Alterna {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "alterna";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "alterna" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "alterna" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "alterna" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Creative tab for Alterna
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ALTERNA_TAB = CREATIVE_MODE_TABS.register("alterna_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.alterna"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModBlocks.GOBLET_LOG.get().asItem().getDefaultInstance())
            .displayItems((parameters, output) -> {
                ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Alterna(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(com.huwng.alterna.event.ModCommonSetup::onCommonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        ModBlocks.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        ModItems.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        com.huwng.alterna.entity.ModEntities.register(modEventBus);
        com.huwng.alterna.block.entity.ModBlockEntities.register(modEventBus);
        // Register the Deferred Register so our fall-distance attachment type gets registered
        AlternaAttachments.ATTACHMENT_TYPES.register(modEventBus);
        // Register the rift mist particle type
        AlternaParticles.PARTICLE_TYPES.register(modEventBus);

        // Register the Deferred Registers so the huge stone spike Structure gets registered
        AlternaStructureTypes.STRUCTURE_TYPES.register(modEventBus);
        AlternaStructurePieceTypes.STRUCTURE_PIECE_TYPES.register(modEventBus);
        AlternaFeatures.FEATURES.register(modEventBus);
        // Register ourselves for server and other game events we are interested in.
        NeoForge.EVENT_BUS.register(this);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }


    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
