package com.huwng.alterna.network;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.client.VoidFadeOverlay;
import com.huwng.alterna.vine.VineConfig;
import com.huwng.alterna.vine.network.VineConfigSyncPayload;
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

        // Vine config sync: server -> client
        registrar.playToClient(
                VineConfigSyncPayload.TYPE,
                VineConfigSyncPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    VineConfig.setServerConfig(payload.toConfig());
                })
        );

        // Vine connections sync: server -> client
        registrar.playToClient(
                com.huwng.alterna.vine.network.VineSyncConnectionsPayload.TYPE,
                com.huwng.alterna.vine.network.VineSyncConnectionsPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    com.huwng.alterna.vine.VineClientCache.setConnections(payload.connections());
                })
        );

        // Bloodlust miss: client -> server
        registrar.playToServer(
                BloodlustMissPayload.TYPE,
                BloodlustMissPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    var sender = context.player();
                    if (sender == null) return;
                    if (sender.isCreative() || sender.isSpectator()) return;
                    // Apply miss penalty (1 heart / 2 HP true damage)
                    sender.hurt(sender.damageSources().genericKill(), 2.0f);
                })
        );
    }
}
