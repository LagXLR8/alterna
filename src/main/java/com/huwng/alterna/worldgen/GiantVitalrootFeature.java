package com.huwng.alterna.worldgen;

import com.huwng.alterna.block.ModBlocks;
import com.huwng.alterna.block.VitalrootFullBlock;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class GiantVitalrootFeature extends Feature<NoneFeatureConfiguration> {

    public GiantVitalrootFeature(Codec<NoneFeatureConfiguration> codec) {
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

        // Verify ground is suitable
        BlockState stateBelow = level.getBlockState(groundPos.below());
        if (stateBelow.isAir() || stateBelow.is(Blocks.WATER)) {
            return false;
        }

        // 1. Place topmost vitalroot_block at groundPos (stem=true because leaves are above)
        level.setBlock(groundPos, ModBlocks.VITALROOT_BLOCK.get().defaultBlockState().setValue(VitalrootFullBlock.STEM, true), 2);

        // 2. Extend 1 to 4 vitalroot_blocks downward into the ground
        int depth = 1 + random.nextInt(4); // 1 to 4 blocks
        for (int i = 1; i <= depth; i++) {
            BlockPos rootPos = groundPos.below(i);
            level.setBlock(rootPos, ModBlocks.VITALROOT_BLOCK.get().defaultBlockState().setValue(VitalrootFullBlock.STEM, false), 2);
        }

        // 3. Compact foliage canopy (Radius 1.5 ~ 3x3 footprint)
        // Layer 0: Ground level around vitalroot_block (3x3 ring, center skipped)
        // Layer 1: Directly above ground level (3x3 full square, center filled with leaves)
        // Layer 2: Top dome layer (corners cut off, 5-block cross shape)

        for (int dy = 0; dy <= 2; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    // Skip central block at dy=0 (holds the top vitalroot_block)
                    if (dy == 0 && dx == 0 && dz == 0) {
                        continue;
                    }

                    // Trim 4 corners for the top layer (dy = 2) to form a rounded top dome
                    if (dy == 2 && Math.abs(dx) == 1 && Math.abs(dz) == 1) {
                        continue;
                    }

                    BlockPos leafPos = groundPos.offset(dx, dy, dz);
                    BlockState currentState = level.getBlockState(leafPos);

                    if (currentState.isAir() || currentState.canBeReplaced()) {
                        // Mix mangrove_leaves and acacia_leaves
                        BlockState leafState = (random.nextBoolean() ? Blocks.MANGROVE_LEAVES : Blocks.ACACIA_LEAVES)
                                .defaultBlockState()
                                .setValue(LeavesBlock.PERSISTENT, true);
                        level.setBlock(leafPos, leafState, 2);
                    }
                }
            }
        }

        return true;
    }
}
