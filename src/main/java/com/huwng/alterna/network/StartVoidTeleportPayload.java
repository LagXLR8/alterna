package com.huwng.alterna.network;

import com.huwng.alterna.Alterna;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Sent to the client a split second BEFORE the server actually teleports the
 * player to the Abyss. This lets the client start covering the screen (see
 * com.huwng.alterna.client.VoidFadeOverlay) before the vanilla dimension-change
 * respawn packet arrives, so the vanilla loading screen never becomes visible.
 */
public record StartVoidTeleportPayload() implements CustomPacketPayload {

    public static final Type<StartVoidTeleportPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Alterna.MODID, "start_void_teleport"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StartVoidTeleportPayload> STREAM_CODEC =
            StreamCodec.unit(new StartVoidTeleportPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
