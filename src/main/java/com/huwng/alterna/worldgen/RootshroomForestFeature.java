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

        // 9x9 grid cells for tree/structure placement
        int gridSize = 9;
        int cellX = Math.floorDiv(x, gridSize);
        int cellZ = Math.floorDiv(z, gridSize);

        long gridHash = (long) cellX * 73856093L ^ (long) cellZ * 19349663L ^ seed;
        int targetX = cellX * gridSize + 4 + (int) ((gridHash & 3) - 1);
        int targetZ = cellZ * gridSize + 4 + (int) (((gridHash >> 2) & 3) - 1);

        if (x == targetX && z == targetZ && edgeDistNorm >= 0.12 && edgeDistNorm <= 0.88) {
            long cellIndex = (long) cellX * 31L ^ (long) cellZ ^ seed;
            int roll = (int) (Math.abs(cellIndex) % 100);

            if (roll < 30) {
                // ~30% Tree
                return RootshroomTreeFeature.placeDirect(level, random, topPos);
            } else if (roll < 50) {
                // ~20% Stump
                return RootshroomStumpFeature.placeDirect(level, random, topPos);
            } else if (roll < 65) {
                // ~15% House Structure
                return LedgeStructureFeature.placeStructure(level, random, topPos, 1, 8.0, true);
            } else if (roll < 80) {
                // ~15% Giant Vitalroot Structure
                return GiantVitalrootFeature.placeDirect(level, random, topPos);
            } else if (roll < 95) {
                // ~15% Fern Group Structure
                return FernGroupFeature.placeDirect(level, random, topPos);
            }
        }

        // Sparse ground vegetation (~25% total density)
        if (!level.getBlockState(topPos).isAir()) return true;
        float rollVeg = random.nextFloat();
        if (rollVeg < 0.04f) {
            level.setBlock(topPos, ModBlocks.ENOKI_MUSHROOM.get().defaultBlockState(), 2);
        } else if (rollVeg < 0.06f) {
            level.setBlock(topPos, ModBlocks.VITALROOT.get().defaultBlockState(), 2);
        } else if (rollVeg < 0.10f) {
            level.setBlock(topPos, Blocks.MOSS_CARPET.defaultBlockState(), 2);
        } else if (rollVeg < 0.18f) {
            level.setBlock(topPos, Blocks.SHORT_GRASS.defaultBlockState(), 2);
        } else if (rollVeg < 0.25f) {
            level.setBlock(topPos, Blocks.FERN.defaultBlockState(), 2);
        }
        return true;
    }
}
