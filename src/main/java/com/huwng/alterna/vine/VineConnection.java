package com.huwng.alterna.vine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Represents a 3D vine connection between two block positions.
 */
public record VineConnection(UUID id, BlockPos posA, BlockPos posB, Vec3 startVec, Vec3 endVec) {

    public static final Codec<VineConnection> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("id").forGetter(VineConnection::id),
                    BlockPos.CODEC.fieldOf("posA").forGetter(VineConnection::posA),
                    BlockPos.CODEC.fieldOf("posB").forGetter(VineConnection::posB)
            ).apply(instance, VineConnection::new)
    );

    public VineConnection(UUID id, BlockPos posA, BlockPos posB) {
        this(
                id,
                posA,
                posB,
                Vec3.atCenterOf(posA),
                Vec3.atCenterOf(posB)
        );
    }

    public VineConnection(BlockPos posA, BlockPos posB) {
        this(
                UUID.randomUUID(),
                posA,
                posB
        );
    }
}
