package com.huwng.alterna.worldgen;

import com.mojang.serialization.Codec;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Places GiantVines along the inner walls of whatever giant cracks reach this chunk.
 * 
 * Runs at a later decoration step than giant_crack so all carving and ledges
 * are fully completed before vines are generated into the chunk.
 */
public class GiantVineFeature extends Feature<NoneFeatureConfiguration> {

    public GiantVineFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkPos chunkPos = new ChunkPos(context.origin().getX() >> 4, context.origin().getZ() >> 4);

        long seed = level.getLevel().getSeed();

        int cellX = Math.floorDiv(chunkPos.x(), GiantCrackParams.CELL_SIZE_CHUNKS);
        int cellZ = Math.floorDiv(chunkPos.z(), GiantCrackParams.CELL_SIZE_CHUNKS);

        boolean placedAnything = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                GiantCrackParams params = GiantCrackParams.forCell(seed, cellX + dx, cellZ + dz);
                if (params != null && params.mightAffect(chunkPos)) {
                    placedAnything |= params.placeGiantVines(level, chunkPos);
                }
            }
        }
        return placedAnything;
    }
}
