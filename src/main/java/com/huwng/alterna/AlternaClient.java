package com.huwng.alterna;

import com.huwng.alterna.client.IceFreezeRenderer;
import com.huwng.alterna.client.TetheredEffectRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Alterna.MODID, dist = Dist.CLIENT)
public class AlternaClient {
    public AlternaClient(ModContainer container, IEventBus modEventBus) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(this::onClientSetup);
        // Entity renderers + model layers (Stormbreaker is registered by
        // ClientEnchantStoneEvents.ModBusClientEvents, which is an @EventBusSubscriber).
        modEventBus.addListener(com.huwng.alterna.client.ModClientEntityEvents::registerEntityRenderers);
        modEventBus.addListener(com.huwng.alterna.client.ModClientEntityEvents::registerLayerDefinitions);

        // Bio-Scanner: HUD layers (mod bus) + scanning logic (game bus)
        modEventBus.addListener(com.huwng.alterna.client.scanner.ScannerClient::registerLayers);
        modEventBus.addListener(com.huwng.alterna.client.scanner.ScannerClient::addLayers);
        NeoForge.EVENT_BUS.addListener(com.huwng.alterna.client.scanner.ScannerClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(com.huwng.alterna.client.scanner.ScannerFx::onRenderLevel);

        NeoForge.EVENT_BUS.addListener(IceFreezeRenderer::onRenderLiving);
        NeoForge.EVENT_BUS.addListener(TetheredEffectRenderer::onRenderLiving);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        Alterna.LOGGER.info("HELLO FROM CLIENT SETUP");
    }
}
