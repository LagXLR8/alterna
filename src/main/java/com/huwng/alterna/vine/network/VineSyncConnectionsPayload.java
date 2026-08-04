package com.huwng.alterna.vine.network;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.vine.VineConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Network payload to sync all vine connections in a world to the client.
 */
public record VineSyncConnectionsPayload(List<VineConnection> connections) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<VineSyncConnectionsPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Alterna.MODID, "vine_sync_connections"));

    public static final StreamCodec<FriendlyByteBuf, VineSyncConnectionsPayload> CODEC =
            StreamCodec.ofMember(VineSyncConnectionsPayload::write, VineSyncConnectionsPayload::read);

    public static VineSyncConnectionsPayload read(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<VineConnection> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID id = buf.readUUID();
            BlockPos posA = buf.readBlockPos();
            BlockPos posB = buf.readBlockPos();
            list.add(new VineConnection(id, posA, posB));
        }
        return new VineSyncConnectionsPayload(list);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(connections.size());
        for (VineConnection conn : connections) {
            buf.writeUUID(conn.id());
            buf.writeBlockPos(conn.posA());
            buf.writeBlockPos(conn.posB());
        }
    }

    public static VineSyncConnectionsPayload fromConnections(Collection<VineConnection> connections) {
        return new VineSyncConnectionsPayload(new ArrayList<>(connections));
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
