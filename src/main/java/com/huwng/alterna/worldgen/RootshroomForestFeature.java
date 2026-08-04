package com.huwng.alterna.worldgen;

import com.huwng.alterna.block.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class RootshroomForestFeature extends Feature<NoneFeatureConfiguration> {

    public RootshroomForestFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return placeDirect(context.level(), context.random(), context.origin(), 0.5, context.random().nextLong());
    }

    public static boolean placeDirect(WorldGenLevel level, RandomSource random, BlockPos topPos, double edgeDistNorm, long seed) {
        int x = topPos.getX();
        int z = topPos.getZ();

        // 10x10 block grid cells for dense, guaranteed tree & stump spawning
        int gridSize = 10;
        int cellX = Math.floorDiv(x, gridSize);
        int cellZ = Math.floorDiv(z, gridSize);

        long gridHash = (long) cellX * 73856093L ^ (long) cellZ * 19349663L ^ seed;
        int targetX = cellX * gridSize + 5 + (int) ((gridHash & 3) - 1);
        int targetZ = cellZ * gridSize + 5 + (int) (((gridHash >> 2) & 3) - 1);

        if (x == targetX && z == targetZ && edgeDistNorm >= 0.02 && edgeDistNorm <= 0.98) {
            // Guaranteed 2 Trees : 1 Stump ratio (33% Stumps, 67% Trees) across grid cells
            long cellIndex = (long) cellX * 31L ^ (long) cellZ;
            boolean isStump = (Math.abs(cellIndex + (seed & 7)) % 3 == 0);

            if (isStump) {
                return RootshroomStumpFeature.placeDirect(level, random, topPos);
            } else {
                return RootshroomTreeFeature.placeDirect(level, random, topPos);
            }
        } else {
            // Sparse ground vegetation (~16% total density)
            // Only place if air — don't overwrite stems/canopy from trees placed by adjacent chunks
            if (!level.getBlockState(topPos).isAir()) return true;
            float roll = random.nextFloat();
            if (roll < 0.04f) {
                // Enoki Mushroom (~4%)
                level.setBlock(topPos, ModBlocks.ENOKI_MUSHROOM.get().defaultBlockState(), 2);
            } else if (roll < 0.08f) {
                // Vanilla Moss Carpet (~4%)
                level.setBlock(topPos, Blocks.MOSS_CARPET.defaultBlockState(), 2);
            } else if (roll < 0.16f) {
                // Vanilla Short Grass (~8%)
                level.setBlock(topPos, Blocks.SHORT_GRASS.defaultBlockState(), 2);
            }
            return true;
        }
    }
}
