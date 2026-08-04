package com.huwng.alterna.worldgen;

import com.huwng.alterna.block.EnokiMushroomWallBlock;
import com.huwng.alterna.block.ModBlocks;
import com.huwng.alterna.vine.VineConnection;
import com.huwng.alterna.vine.VineSavedData;
import com.huwng.alterna.vine.network.VineSyncConnectionsPayload;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.network.PacketDistributor;

public class RootshroomTreeFeature extends Feature<NoneFeatureConfiguration> {

    public RootshroomTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return placeDirect(context.level(), context.random(), context.origin());
    }

    private static void safeSetStem(WorldGenLevel level, BlockPos pos, BlockState state, int flags) {
        try {
            BlockState existing = level.getBlockState(pos);
            // Do NOT overwrite existing ROOTSHROOM_STEM (other trees/stumps) or water (stump pools)
            if (existing.is(ModBlocks.ROOTSHROOM_STEM.get()) || existing.is(Blocks.WATER)) return;
            if (existing.isAir() || existing.canBeReplaced() || existing.is(ModBlocks.ROOTSHROOM_LEAVES.get())
                    || existing.is(ModBlocks.ROOTSHROOM_LIGHT.get()) || existing.is(Blocks.MOSS_CARPET) || existing.is(Blocks.SHORT_GRASS)) {
                level.setBlock(pos, state, flags);
            }
        } catch (Exception ignored) {
        }
    }

    private static void safeSetLeaf(WorldGenLevel level, BlockPos pos, BlockState state, int flags) {
        try {
            BlockState existing = level.getBlockState(pos);
            // Do NOT overwrite existing stems (other trees/stumps) or water (stump pools)
            if (existing.is(ModBlocks.ROOTSHROOM_STEM.get()) || existing.is(Blocks.WATER)) return;
            if (existing.isAir() || existing.canBeReplaced()) {
                level.setBlock(pos, state, flags);
            }
        } catch (Exception ignored) {
        }
    }

    private static void safeSetDecoration(WorldGenLevel level, BlockPos pos, BlockState state, int flags) {
        try {
            BlockState existing = level.getBlockState(pos);
            if (existing.isAir()) {
                level.setBlock(pos, state, flags);
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean safeIsAir(WorldGenLevel level, BlockPos pos) {
        try {
            return level.getBlockState(pos).isAir();
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean safeIsReplaceableOrAir(WorldGenLevel level, BlockPos pos) {
        try {
            BlockState state = level.getBlockState(pos);
            // Treat existing stems and water as solid/non-replaceable to avoid punching through other features
            if (state.is(ModBlocks.ROOTSHROOM_STEM.get()) || state.is(Blocks.WATER)) return false;
            return state.isAir() || state.canBeReplaced() || !state.isSolid();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean placeDirect(WorldGenLevel level, RandomSource random, BlockPos origin) {
        int height = 40 + random.nextInt(16); // Height 40 to 55 blocks

        BlockState stemState = ModBlocks.ROOTSHROOM_STEM.get().defaultBlockState();
        BlockState leavesState = ModBlocks.ROOTSHROOM_LEAVES.get().defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, true)
                .setValue(LeavesBlock.DISTANCE, 1);
        BlockState lightState = ModBlocks.ROOTSHROOM_LIGHT.get().defaultBlockState();
        BlockState enokiWallState = ModBlocks.ENOKI_MUSHROOM_WALL.get().defaultBlockState();

        int cornerCutYStart = (height > 48) ? 4 : 2;

        // 0. Base downward support: If air is detected anywhere in the 3x3 layer below, place rootshroom stems down
        for (int dy = 1; dy <= 20; dy++) {
            boolean hasAirInLayer = false;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos checkPos = origin.offset(dx, -dy, dz);
                    if (safeIsReplaceableOrAir(level, checkPos)) {
                        hasAirInLayer = true;
                        break;
                    }
                }
                if (hasAirInLayer) break;
            }

            if (hasAirInLayer) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos stemPos = origin.offset(dx, -dy, dz);
                        if (safeIsReplaceableOrAir(level, stemPos)) {
                            safeSetStem(level, stemPos, stemState, 3);
                        }
                    }
                }
            } else {
                break; // Whole 3x3 layer is solid ground
            }
        }

        // 1. Trunk Base: Solid 3x3 square from Y = 0 up to cornerCutYStart
        for (int y = 0; y < cornerCutYStart && y < height; y++) {
            BlockPos layerCenter = origin.above(y);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    safeSetStem(level, layerCenter.offset(dx, 0, dz), stemState, 3);
                }
            }
        }

        // 2. Trunk Body: Cross / Plus shape (3x3 with 4 corners cut off) from cornerCutYStart to height - 1
        int[][] crossOffsets = { {0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1} };

        for (int y = cornerCutYStart; y < height; y++) {
            BlockPos layerCenter = origin.above(y);
            for (int[] offset : crossOffsets) {
                safeSetStem(level, layerCenter.offset(offset[0], 0, offset[1]), stemState, 3);
            }

            // Random Enoki Mushrooms attached to outer trunk walls (from Y = 2 up to height - 8)
            if (y >= 2 && y <= height - 8) {
                Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
                for (Direction dir : directions) {
                    if (random.nextFloat() < 0.15f) {
                        BlockPos enokiPos = layerCenter.relative(dir, 2);
                        if (safeIsAir(level, enokiPos)) {
                            safeSetDecoration(level, enokiPos, enokiWallState.setValue(EnokiMushroomWallBlock.FACING, dir), 3);
                        }
                    }
                }
            }
        }

        // 3. Canopy (Tán lá): Starts at height 10 to 20 above ground
        int startCanopyY = 10 + random.nextInt(11); // Y = 10..20
        int topCanopyY = height + 2;

        for (int y = startCanopyY; y <= topCanopyY; y++) {
            int layerIndex = y - startCanopyY;
            int step = layerIndex % 3;
            int cycle = (layerIndex / 3) % 3;

            int baseR = (step == 0) ? 4 : (step == 1) ? 5 : 3;
            int radius = Math.max(1, baseR - cycle);

            BlockPos canopyCenter = origin.above(y);

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int distSq = dx * dx + dz * dz;
                    if (distSq <= radius * radius + (random.nextInt(2))) {
                        BlockPos leafPos = canopyCenter.offset(dx, 0, dz);

                        // Don't overwrite central stem
                        if (y < height && Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && (Math.abs(dx) + Math.abs(dz) <= 1)) {
                            continue;
                        }

                        BlockState blockToPlace = (random.nextFloat() < 0.04f) ? lightState : leavesState;
                        safeSetLeaf(level, leafPos, blockToPlace, 3);
                    }
                }
            }
        }

        // 4. Natural Vine Zipline Generation (Height Y = 10 to 30 on trunk)
        if (level.getLevel() != null) {
            ServerLevel serverLevel = level.getLevel();
            int vineCount = 1 + random.nextInt(2); // 1 or 2 zipline vines per tree

            for (int v = 0; v < vineCount; v++) {
                int startY = 10 + random.nextInt(20);
                BlockPos startPos = origin.above(startY);

                BlockPos targetPos = null;
                double angle = random.nextDouble() * Math.PI * 2.0;

                // Try 4 directional angles scanning downwards diagonally toward ground / ledge / wall
                for (int attempt = 0; attempt < 4; attempt++) {
                    double currentAngle = angle + (attempt * Math.PI / 2.0);
                    double dirX = Math.cos(currentAngle);
                    double dirZ = Math.sin(currentAngle);
                    double slopeY = -0.35 - random.nextDouble() * 0.35; // Downward slant

                    for (int dist = 5; dist <= 28; dist++) {
                        int checkX = (int) Math.round(startPos.getX() + dirX * dist);
                        int checkY = (int) Math.round(startPos.getY() + slopeY * dist);
                        int checkZ = (int) Math.round(startPos.getZ() + dirZ * dist);
                        BlockPos checkPos = new BlockPos(checkX, checkY, checkZ);

                        try {
                            BlockState st = level.getBlockState(checkPos);
                            if (st.isSolid() || st.is(ModBlocks.ROOTSHROOM_STEM.get()) || st.is(ModBlocks.ROOTSHROOM_LEAVES.get())) {
                                targetPos = checkPos;
                                break;
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    if (targetPos != null) break;
                }

                if (targetPos != null) {
                    try {
                        VineSavedData data = VineSavedData.get(serverLevel);
                        VineConnection conn = new VineConnection(startPos, targetPos);
                        if (data.addConnection(conn)) {
                            PacketDistributor.sendToPlayersInDimension(serverLevel, VineSyncConnectionsPayload.fromConnections(data.getConnections()));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        return true;
    }
}
