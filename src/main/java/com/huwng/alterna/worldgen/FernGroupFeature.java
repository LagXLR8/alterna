package com.huwng.alterna.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class FernGroupFeature extends Feature<NoneFeatureConfiguration> {

    public FernGroupFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return placeDirect(context.level(), context.random(), context.origin());
    }

    public static boolean placeDirect(WorldGenLevel level, RandomSource random, BlockPos origin) {
        // Find solid ground
        BlockPos groundPos = origin;
        while (groundPos.getY() > level.getMinY() + 1 && level.isEmptyBlock(groundPos.below())) {
            groundPos = groundPos.below();
        }

        BlockState stateBelow = level.getBlockState(groundPos.below());
        if (stateBelow.isAir() || stateBelow.is(Blocks.WATER)) {
            return false;
        }

        boolean placedAnything = false;

        // 1. Center / Focal point: Place 1 to 2 Large Ferns in the center
        int largeFernCount = 1 + random.nextInt(2); // 1 or 2
        BlockPos centerPos = groundPos;

        for (int i = 0; i < largeFernCount; i++) {
            BlockPos targetPos = (i == 0) ? centerPos : centerPos.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
            if (canPlaceTallPlant(level, targetPos)) {
                DoublePlantBlock.placeAt(level, Blocks.LARGE_FERN.defaultBlockState(), targetPos, 2);
                placedAnything = true;
            }
        }

        // 2. Outer Ring: Surround the center with an orderly ring of small Ferns (radius 1 to 2)
        int smallFernCount = 4 + random.nextInt(4); // 4 to 7 small ferns
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) continue; // Skip center holding Large Fern

                // Keep group within radius 2
                if (dx * dx + dz * dz > 4) continue;

                BlockPos fernPos = groundPos.offset(dx, 0, dz);
                // Adjust Y to find ground surface if slight elevation change
                if (level.isEmptyBlock(fernPos) && !level.isEmptyBlock(fernPos.below())) {
                    if (random.nextFloat() < 0.7f && smallFernCount > 0) { // ~70% placement within orderly radius
                        level.setBlock(fernPos, Blocks.FERN.defaultBlockState(), 2);
                        smallFernCount--;
                        placedAnything = true;
                    }
                }
            }
        }

        return placedAnything;
    }

    private static boolean canPlaceTallPlant(WorldGenLevel level, BlockPos pos) {
        return level.isEmptyBlock(pos) 
            && level.isEmptyBlock(pos.above()) 
            && level.getBlockState(pos.below()).isSolid();
    }
}
