package com.huwng.alterna.network;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.client.VoidFadeOverlay;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Alterna.MODID)
public class AlternaNetwork {

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // playToClient's handler runs on the client only, so it is safe to
        // reference client-only classes (VoidFadeOverlay) directly here even
        // though this class itself loads on both sides.
        registrar.playToClient(
                StartVoidTeleportPayload.TYPE,
                StartVoidTeleportPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(VoidFadeOverlay::startFade)
        );
    }
}
