package com.huwng.alterna.worldgen;

import com.huwng.alterna.block.EnokiMushroomWallBlock;
import com.huwng.alterna.block.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class RootshroomStumpFeature extends Feature<NoneFeatureConfiguration> {

    public RootshroomStumpFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return placeDirect(context.level(), context.random(), context.origin());
    }

    private static void safeSetStem(WorldGenLevel level, BlockPos pos, BlockState state, int flags) {
        try {
            BlockState existing = level.getBlockState(pos);
            if (existing.isAir() || existing.canBeReplaced() || existing.is(ModBlocks.ROOTSHROOM_LEAVES.get()) 
                    || existing.is(ModBlocks.ROOTSHROOM_LIGHT.get()) || existing.is(Blocks.MOSS_CARPET) || existing.is(Blocks.SHORT_GRASS)) {
                level.setBlock(pos, state, flags);
            }
        } catch (Exception ignored) {
        }
    }

    private static void safeSetBlock(WorldGenLevel level, BlockPos pos, BlockState state, int flags) {
        try {
            level.setBlock(pos, state, flags);
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

    private static boolean safeIsStem(WorldGenLevel level, BlockPos pos) {
        try {
            return level.getBlockState(pos).is(ModBlocks.ROOTSHROOM_STEM.get());
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean safeIsReplaceableOrAir(WorldGenLevel level, BlockPos pos) {
        try {
            BlockState state = level.getBlockState(pos);
            return state.isAir() || state.canBeReplaced() || !state.isSolid();
        } catch (Exception e) {
            return false;
        }
    }

    private static BlockState getLeafLitterState(RandomSource random) {
        try {
            net.minecraft.world.level.block.Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(net.minecraft.resources.Identifier.parse("minecraft:leaf_litter")).orElse(null);
            if (block != null && block != Blocks.AIR) {
                BlockState state = block.defaultBlockState();
                for (net.minecraft.world.level.block.state.properties.Property<?> prop : state.getProperties()) {
                    if (prop.getName().equals("facing")) {
                        @SuppressWarnings("unchecked")
                        net.minecraft.world.level.block.state.properties.Property<Direction> dirProp = (net.minecraft.world.level.block.state.properties.Property<Direction>) prop;
                        state = state.setValue(dirProp, Direction.Plane.HORIZONTAL.getRandomDirection(random));
                    } else if (prop.getName().equals("amount") || prop.getName().equals("segment_amount") || prop.getName().equals("segments")) {
                        @SuppressWarnings("unchecked")
                        net.minecraft.world.level.block.state.properties.Property<Integer> intProp = (net.minecraft.world.level.block.state.properties.Property<Integer>) prop;
                        int min = intProp.getPossibleValues().stream().min(Integer::compare).orElse(1);
                        int max = intProp.getPossibleValues().stream().max(Integer::compare).orElse(4);
                        int val = min + random.nextInt(max - min + 1);
                        state = state.setValue(intProp, val);
                    }
                }
                return state;
            }
        } catch (Exception ignored) {
        }
        return Blocks.MOSS_CARPET.defaultBlockState();
    }

    public static boolean placeDirect(WorldGenLevel level, RandomSource random, BlockPos origin) {
        int upperHeight = 2 + random.nextInt(2); // 2 or 3 layers
        int totalHeight = 2 + upperHeight; // Total height = 4 or 5 blocks tall

        BlockState stemState = ModBlocks.ROOTSHROOM_STEM.get().defaultBlockState();
        BlockState waterState = Blocks.WATER.defaultBlockState();
        BlockState enokiWallState = ModBlocks.ENOKI_MUSHROOM_WALL.get().defaultBlockState();
        BlockState mossCarpetState = ModBlocks.WILD_MOSS_CARPET.get().defaultBlockState();
        BlockState airState = Blocks.AIR.defaultBlockState();

        int[][] crossOffsets = { {1, 0}, {-1, 0}, {0, 1}, {0, -1} };

        // Ground decoration around stump base: Rooted dirt & Leaf litter (Expanded Radius & High Density)
        int groundRadius = 6;
        for (int dx = -groundRadius; dx <= groundRadius; dx++) {
            for (int dz = -groundRadius; dz <= groundRadius; dz++) {
                double distSq = dx * dx + dz * dz;
                if (distSq <= groundRadius * groundRadius + random.nextDouble() * 1.5) {
                    BlockPos groundPos = origin.offset(dx, -1, dz);
                    BlockState groundState = level.getBlockState(groundPos);
                    if (groundState.isSolid() && !groundState.is(ModBlocks.ROOTSHROOM_STEM.get()) && random.nextFloat() < 0.65f) {
                        level.setBlock(groundPos, Blocks.ROOTED_DIRT.defaultBlockState(), 2);
                    }

                    BlockPos airPos = origin.offset(dx, 0, dz);
                    BlockPos belowAir = airPos.below();
                    if (level.getBlockState(airPos).isAir() && level.getBlockState(belowAir).isSolid() 
                            && !level.getBlockState(belowAir).is(ModBlocks.ROOTSHROOM_STEM.get()) 
                            && random.nextFloat() < 0.75f) {
                        level.setBlock(airPos, getLeafLitterState(random), 2);
                    }
                }
            }
        }

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
                            safeSetBlock(level, stemPos, stemState, 3);
                        }
                    }
                }
            } else {
                break; // Whole 3x3 layer is solid ground
            }
        }

        // 1. Tầng 1 (Y = 0): Mặc định 3x3 bằng Rootshroom Stem
        BlockPos layer0 = origin.above(0);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                safeSetBlock(level, layer0.offset(dx, 0, dz), stemState, 3);
            }
        }

        // 2. Tầng 2 (Y = 1): Đổ nước ở giữa (0, 0)
        BlockPos layer1 = origin.above(1);
        safeSetBlock(level, layer1, waterState, 3);

        if (totalHeight > 4) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        safeSetBlock(level, layer1.offset(dx, 0, dz), stemState, 3);
                    }
                }
            }
        } else {
            for (int[] offset : crossOffsets) {
                safeSetBlock(level, layer1.offset(offset[0], 0, offset[1]), stemState, 3);
            }
        }

        // 3. Phía trên (Y = 2 đến totalHeight - 1): Cao 2-3 block, cắt góc
        for (int y = 2; y < totalHeight; y++) {
            BlockPos layerCenter = origin.above(y);
            boolean isTopLayer = (y == totalHeight - 1);

            // 4 vị trí thành chữ thập
            int missingCount = isTopLayer ? (random.nextInt(3)) : 0;
            int missingSkipped = 0;

            for (int[] offset : crossOffsets) {
                BlockPos wallPos = layerCenter.offset(offset[0], 0, offset[1]);
                if (isTopLayer && missingSkipped < missingCount && random.nextBoolean()) {
                    missingSkipped++;
                    safeSetBlock(level, wallPos, airState, 3);
                } else {
                    safeSetBlock(level, wallPos, stemState, 3);
                }
            }

            // Ô giữa (0, 0): Ngẫu nhiên đặt wall enoki mushroom gắn vào tường phía trong
            if (random.nextFloat() < 0.45f) {
                Direction[] innerDirs = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
                Direction chosenDir = innerDirs[random.nextInt(4)];
                BlockPos supportingWall = layerCenter.relative(chosenDir.getOpposite());
                if (safeIsStem(level, supportingWall)) {
                    safeSetBlock(level, layerCenter, enokiWallState.setValue(EnokiMushroomWallBlock.FACING, chosenDir), 3);
                } else {
                    safeSetBlock(level, layerCenter, airState, 3);
                }
            } else {
                safeSetBlock(level, layerCenter, airState, 3);
            }
        }

        // 4. Xung quanh phía ngoài ngẫu nhiên đặt enoki mushroom (gắn tường)
        for (int y = 1; y < totalHeight; y++) {
            BlockPos layerCenter = origin.above(y);
            Direction[] directions = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            for (Direction dir : directions) {
                if (random.nextFloat() < 0.25f) {
                    BlockPos enokiPos = layerCenter.relative(dir, 2);
                    if (safeIsAir(level, enokiPos)) {
                        safeSetBlock(level, enokiPos, enokiWallState.setValue(EnokiMushroomWallBlock.FACING, dir), 3);
                    }
                }
            }
        }

        // 5. Phủ Wild Moss Carpet lên tất cả các mặt stem có AIR trực tiếp phía trên
        for (int y = 0; y < totalHeight; y++) {
            BlockPos layerCenter = origin.above(y);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos stemPos = layerCenter.offset(dx, 0, dz);
                    if (safeIsStem(level, stemPos)) {
                        BlockPos abovePos = stemPos.above();
                        if (safeIsAir(level, abovePos)) {
                            safeSetBlock(level, abovePos, mossCarpetState, 3);
                        }
                    }
                }
            }
        }

        return true;
    }
}
