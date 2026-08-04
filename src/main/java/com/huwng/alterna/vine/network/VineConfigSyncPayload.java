package com.huwng.alterna.vine.network;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.vine.VineConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/**
 * Syncs vine config from server to client on player join.
 * Ported from Zipline mod (MIT) — credit: Evan, Tomate0613.
 */
public record VineConfigSyncPayload(
        double snapRadius,
        double clickReach,
        boolean useAnywhere,
        double maxTurnAngle,
        double hangOffset,
        double speedMultiplier,
        boolean realisticPhysics,
        double exitJumpMultiplier,
        int releaseCooldown
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<VineConfigSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Alterna.MODID, "vine_config_sync"));

    public static final StreamCodec<FriendlyByteBuf, VineConfigSyncPayload> CODEC =
            StreamCodec.ofMember(VineConfigSyncPayload::write, VineConfigSyncPayload::read);

    public static VineConfigSyncPayload read(FriendlyByteBuf buf) {
        return new VineConfigSyncPayload(
                buf.readDouble(), buf.readDouble(), buf.readBoolean(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readBoolean(), buf.readDouble(), buf.readInt()
        );
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(this.snapRadius);
        buf.writeDouble(this.clickReach);
        buf.writeBoolean(this.useAnywhere);
        buf.writeDouble(this.maxTurnAngle);
        buf.writeDouble(this.hangOffset);
        buf.writeDouble(this.speedMultiplier);
        buf.writeBoolean(this.realisticPhysics);
        buf.writeDouble(this.exitJumpMultiplier);
        buf.writeInt(this.releaseCooldown);
    }

    public static VineConfigSyncPayload fromConfig(VineConfig config) {
        return new VineConfigSyncPayload(
                config.snapRadius, config.clickReach, config.useAnywhere,
                config.maxTurnAngle, config.hangOffset, config.speedMultiplier,
                config.realisticPhysics, config.exitJumpMultiplier, config.releaseCooldown
        );
    }

    public VineConfig toConfig() {
        VineConfig config = new VineConfig();
        config.snapRadius = this.snapRadius;
        config.clickReach = this.clickReach;
        config.useAnywhere = this.useAnywhere;
        config.maxTurnAngle = this.maxTurnAngle;
        config.hangOffset = this.hangOffset;
        config.speedMultiplier = this.speedMultiplier;
        config.realisticPhysics = this.realisticPhysics;
        config.exitJumpMultiplier = this.exitJumpMultiplier;
        config.releaseCooldown = this.releaseCooldown;
        return config;
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
