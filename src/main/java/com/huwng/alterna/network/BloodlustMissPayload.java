package com.huwng.alterna.network;

import com.huwng.alterna.Alterna;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * Client → Server packet: player swung at air with a full-charge weapon.
 * Used by Curse of Bloodlust to apply miss penalty server-side.
 */
public record BloodlustMissPayload() implements CustomPacketPayload {

    public static final Type<BloodlustMissPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Alterna.MODID, "bloodlust_miss"));

    public static final StreamCodec<FriendlyByteBuf, BloodlustMissPayload> CODEC =
            StreamCodec.ofMember(BloodlustMissPayload::write, BloodlustMissPayload::read);

    public static BloodlustMissPayload read(FriendlyByteBuf buf) {
        return new BloodlustMissPayload();
    }

    public void write(FriendlyByteBuf buf) {
        // no data to write
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
