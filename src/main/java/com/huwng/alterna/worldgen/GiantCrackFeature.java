package com.huwng.alterna.worldgen;

import com.mojang.serialization.Codec;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Registered as a normal Feature (see AlternaFeatures / the placed_feature +
 * biome_modifier json), so vanilla calls place() unconditionally for every
 * chunk that generates - no structure-reference searching, no distance
 * limit. Each call just asks GiantCrackParams "does the crack belonging to
 * my cell, or any of my 8 neighboring cells, reach into me?" and carves
 * whatever portion applies. See GiantCrackParams for why this is safe to
 * call independently, in any order, from any chunk.
 */
public class GiantCrackFeature extends Feature<NoneFeatureConfiguration> {

    public GiantCrackFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkPos chunkPos = new ChunkPos(context.origin().getX() >> 4, context.origin().getZ() >> 4);

        long seed = level.getLevel().getSeed();

        int cellX = Math.floorDiv(chunkPos.x(), GiantCrackParams.CELL_SIZE_CHUNKS);
        int cellZ = Math.floorDiv(chunkPos.z(), GiantCrackParams.CELL_SIZE_CHUNKS);

        boolean carvedAnything = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                GiantCrackParams params = GiantCrackParams.forCell(seed, cellX + dx, cellZ + dz);
                if (params.mightAffect(chunkPos)) {
                    params.carveChunk(level, chunkPos);
                    carvedAnything = true;
                }
            }
        }
        return carvedAnything;
    }
}