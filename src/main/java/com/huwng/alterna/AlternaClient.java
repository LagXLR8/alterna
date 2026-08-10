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
        modEventBus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(com.huwng.alterna.entity.ModEntities.THROWN_STORMBREAKER.get(), com.huwng.alterna.client.render.ThrownStormbreakerRenderer::new);
        });

        NeoForge.EVENT_BUS.addListener(IceFreezeRenderer::onRenderLiving);
        NeoForge.EVENT_BUS.addListener(TetheredEffectRenderer::onRenderLiving);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        Alterna.LOGGER.info("HELLO FROM CLIENT SETUP");
    }
}
