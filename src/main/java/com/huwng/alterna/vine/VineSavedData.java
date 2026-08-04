package com.huwng.alterna.vine;

import com.huwng.alterna.Alterna;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

/**
 * Persisted level storage for all active vine connections in a world.
 */
public class VineSavedData extends SavedData {

    private static final String DATA_NAME = "alterna_vine_connections";

    public static final Codec<VineSavedData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    VineConnection.CODEC.listOf().fieldOf("connections").forGetter(data -> new ArrayList<>(data.connections.values()))
            ).apply(instance, list -> {
                VineSavedData data = new VineSavedData();
                for (VineConnection conn : list) {
                    data.connections.put(conn.id(), conn);
                }
                return data;
            })
    );

    public static final SavedDataType<VineSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Alterna.MODID, DATA_NAME),
            VineSavedData::new,
            CODEC,
            null
    );

    private final Map<UUID, VineConnection> connections = new HashMap<>();

    public VineSavedData() {}

    public static VineSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public Collection<VineConnection> getConnections() {
        return Collections.unmodifiableCollection(connections.values());
    }

    public boolean addConnection(VineConnection connection) {
        if (!connections.containsKey(connection.id())) {
            connections.put(connection.id(), connection);
            setDirty();
            return true;
        }
        return false;
    }

    public boolean removeConnection(UUID id) {
        if (connections.remove(id) != null) {
            setDirty();
            return true;
        }
        return false;
    }

    public boolean removeConnectionsAt(BlockPos pos) {
        boolean removed = connections.values().removeIf(conn ->
                conn.posA().equals(pos) || conn.posB().equals(pos)
        );
        if (removed) {
            setDirty();
        }
        return removed;
    }
}
