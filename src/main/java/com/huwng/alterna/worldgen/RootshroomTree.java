package com.huwng.alterna.worldgen;

import com.huwng.alterna.block.EnokiMushroomWallBlock;
import com.huwng.alterna.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class RootshroomTree {

    public static boolean generate(LevelWriter level, RandomSource random, BlockPos origin) {
        int height = 40 + random.nextInt(31); // Height 40 to 70 blocks

        BlockState stemState = ModBlocks.ROOTSHROOM_STEM.get().defaultBlockState();
        BlockState leavesState = ModBlocks.ROOTSHROOM_LEAVES.get().defaultBlockState();
        BlockState lightState = ModBlocks.ROOTSHROOM_LIGHT.get().defaultBlockState();
        BlockState enokiWallState = ModBlocks.ENOKI_MUSHROOM_WALL.get().defaultBlockState();

        // 1. Trunk Base (Y = 0): Solid 3x3 square
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlock(origin.offset(dx, 0, dz), stemState, 3);
            }
        }

        // 2. Trunk Body (Y = 1 to height - 1): Cross / Plus shape (3x3 with 4 corners cut off)
        int[][] crossOffsets = { {0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1} };

        for (int y = 1; y < height; y++) {
            BlockPos layerCenter = origin.above(y);
            for (int[] offset : crossOffsets) {
                level.setBlock(layerCenter.offset(offset[0], 0, offset[1]), stemState, 3);
            }

            // Random Enoki Mushrooms on trunk outer walls (from Y = 2 up to height - 8)
            if (y >= 2 && y <= height - 8) {
                // Outer facing directions for the cross trunk
                Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
                for (Direction dir : directions) {
                    if (random.nextFloat() < 0.15f) {
                        BlockPos trunkOuterPos = layerCenter.relative(dir);
                        BlockPos wallAttachmentPos = trunkOuterPos.relative(dir);

                        // Place wall enoki mushroom attached to trunk face
                        BlockPos enokiPos = trunkOuterPos;
                        if (level instanceof WorldGenLevel wgl) {
                            if (wgl.getBlockState(enokiPos).isAir()) {
                                level.setBlock(enokiPos, enokiWallState.setValue(EnokiMushroomWallBlock.FACING, dir), 3);
                            }
                        } else {
                            level.setBlock(enokiPos, enokiWallState.setValue(EnokiMushroomWallBlock.FACING, dir), 3);
                        }
                    }
                }
            }
        }

        // 3. Canopy (Tán lá): Rounded cone shape (mega spruce style) starting at height > 20-30
        int startCanopyY = Math.max(20, Math.min(30, 20 + random.nextInt(11)));
        int topCanopyY = height + 3;
        int canopyHeight = topCanopyY - startCanopyY;
        double maxRadius = 7.0 + random.nextDouble() * 2.5; // Radius 7 to 9.5 at canopy base

        for (int y = startCanopyY; y <= topCanopyY; y++) {
            int dy = y - startCanopyY;
            double progress = 1.0 - ((double) dy / canopyHeight); // 1.0 at bottom, 0.0 at top
            double layerRadius = maxRadius * Math.pow(progress, 0.75); // Rounded cone curve

            int radInt = (int) Math.ceil(layerRadius);
            BlockPos canopyCenter = origin.above(y);

            for (int dx = -radInt; dx <= radInt; dx++) {
                for (int dz = -radInt; dz <= radInt; dz++) {
                    double distSq = dx * dx + dz * dz;
                    double effectiveRadSq = (layerRadius + (random.nextFloat() * 0.6 - 0.3)) * layerRadius;

                    if (distSq <= effectiveRadSq) {
                        BlockPos leafPos = canopyCenter.offset(dx, 0, dz);

                        // Don't overwrite trunk stems inside
                        if (y < height && Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && (Math.abs(dx) + Math.abs(dz) <= 1)) {
                            continue;
                        }

                        BlockState blockToPlace = (random.nextFloat() < 0.04f) ? lightState : leavesState;
                        level.setBlock(leafPos, blockToPlace, 3);
                    }
                }
            }
        }

        return true;
    }
}
