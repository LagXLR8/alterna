package com.huwng.alterna.worldgen;

import com.mojang.serialization.Codec;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Grows GobletTrees out of the walls of whatever giant cracks reach this chunk.
 * Uses exactly the same stateless cell lookup as GiantCrackFeature - see
 * GiantCrackParams for why that's safe to do independently per chunk.
 *
 * WHY THIS IS A SEPARATE FEATURE FROM GiantCrackFeature: carving has to be
 * completely finished for a chunk before any tree is built into it, otherwise a
 * second crack whose reach also covers this chunk would carve holes through a
 * tree the first crack had already placed. Registering this at a LATER
 * decoration step than alterna:giant_crack gets that ordering for free -
 * vanilla runs every feature of an earlier step for the chunk before any
 * feature of a later one, so all carving of this chunk (by every crack that
 * reaches it) is guaranteed done by the time place() runs here.
 *
 * See the step field in data/alterna/neoforge/biome_modifier/goblet_tree.json.
 */
public class GobletTreeFeature extends Feature<NoneFeatureConfiguration> {

    public GobletTreeFeature(Codec<NoneFeatureConfiguration> codec) {
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
                if (params.mightAffect(chunkPos)) {
                    placedAnything |= params.placeGobletTrees(level, chunkPos);
                }
            }
        }
        return placedAnything;
    }
}
