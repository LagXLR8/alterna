package com.huwng.alterna.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HangingMossBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MossyCarpetBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import com.huwng.alterna.block.ModBlocks;
import com.huwng.alterna.block.WildMossCarpetBlock;
import net.minecraft.world.level.block.state.properties.WallSide;

import java.util.ArrayList;
import java.util.List;

public class KapokTreeFeature extends Feature<KapokTreeConfiguration> {

    public KapokTreeFeature(Codec<KapokTreeConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<KapokTreeConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        KapokTreeConfiguration config = context.config();

        // 1. Find solid ceiling rock above origin
        BlockPos ceilingPos = null;
        for (int y = 0; y < 20; y++) {
            BlockPos checkPos = origin.above(y);
            if (level.getBlockState(checkPos).isSolid()) {
                ceilingPos = checkPos;
                break;
            }
        }

        if (ceilingPos == null) {
            ceilingPos = origin;
        }

        int treeHeight = config.minimumSize() + random.nextInt(Math.max(1, config.sizeVariation()));
        BlockPos trunkTop = ceilingPos.below(3 + random.nextInt(3));

        // 2. Inverted Mangrove Stilt Roots (Branching UPWARDS & OUTWARDS into ceiling rock)
        placeInvertedMangroveRoots(level, ceilingPos, trunkTop, random, config);

        // 3. Mangrove Central Trunk (Growing DOWNWARDS into cavern using Mangrove Wood)
        List<BlockPos> foliageNodes = new ArrayList<>();
        BlockPos currentTrunk = trunkTop;

        // Determine tilt direction for slanted canopy
        int tiltX = random.nextBoolean() ? 1 : -1;
        int tiltZ = random.nextBoolean() ? 1 : -1;

        for (int i = 0; i < treeHeight; i++) {
            currentTrunk = currentTrunk.below();
            placeLog(level, currentTrunk, random, config, Direction.Axis.Y);

            // Random Wild Moss Carpet on top of branch logs
            if (random.nextFloat() < 0.40f) {
                placeWildMossCarpetOnTop(level, currentTrunk.above());
            }

            // Mangrove diagonal side branches going downwards & outwards
            if (i >= 3 && random.nextFloat() < 0.40f) {
                Direction branchDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                int branchLen = 2 + random.nextInt(3);
                BlockPos branchPos = currentTrunk;
                for (int b = 0; b < branchLen; b++) {
                    branchPos = branchPos.relative(branchDir).below();
                    placeLog(level, branchPos, random, config, branchDir.getAxis());
                    if (random.nextFloat() < 0.50f) {
                        placeWildMossCarpetOnTop(level, branchPos.above());
                    }
                }
                foliageNodes.add(branchPos);
            }
        }
        foliageNodes.add(currentTrunk);

        // 4. Inverted Mangrove Foliage Canopy (Placed at bottom of trunk and branch tips, slightly SLANTED/TILTED)
        for (BlockPos nodePos : foliageNodes) {
            placeInvertedMangroveFoliage(level, nodePos, tiltX, tiltZ, random, config);
        }

        return true;
    }

    private void placeInvertedMangroveRoots(WorldGenLevel level, BlockPos ceilingPos, BlockPos trunkTop, RandomSource random, KapokTreeConfiguration config) {
        // Main central root trunk connection
        BlockPos.betweenClosedStream(trunkTop, ceilingPos).forEach(pos -> placeLog(level, pos, random, config, Direction.Axis.Y));

        // Diagonal stilt root legs anchoring into the ceiling
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int rootLength = 3 + random.nextInt(3);
            BlockPos currentRoot = trunkTop;

            for (int r = 0; r < rootLength; r++) {
                currentRoot = currentRoot.relative(dir).above();
                placeLog(level, currentRoot, random, config, Direction.Axis.Y);

                if (random.nextBoolean()) {
                    Direction sideDir = dir.getClockWise();
                    BlockPos sideRoot = currentRoot.relative(sideDir).above();
                    placeLog(level, sideRoot, random, config, Direction.Axis.Y);
                }

                if (level.getBlockState(currentRoot.above()).isSolid()) {
                    break;
                }
            }
        }
    }

    private void placeInvertedMangroveFoliage(WorldGenLevel level, BlockPos centerPos, int tiltX, int tiltZ, RandomSource random, KapokTreeConfiguration config) {
        // Inverted Mangrove Foliage Canopy - SLANTED/TILTED along (tiltX, tiltZ)
        BlockPos.MutableBlockPos m = centerPos.mutable();

        // Upper canopy layer (Y = 0)
        placeFoliageCircle(level, m, 3, random, config);

        // Middle canopy layer (Y = -1, shifted slightly by tiltX)
        placeFoliageCircle(level, m.below().offset(tiltX, 0, 0), 2, random, config);

        // Lower canopy layer (Y = -2, shifted slightly by tiltZ)
        placeFoliageCircle(level, m.below(2).offset(tiltX, 0, tiltZ), 1, random, config);

        // Bottom tip (Y = -3)
        placeLeaves(level, m.below(3).offset(tiltX, 0, tiltZ), random, config);

        // Hanging moss / propagules underneath bottom foliage
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (random.nextFloat() < 0.35f) {
                    BlockPos leafPos = m.offset(dx, -2, dz);
                    if (level.getBlockState(leafPos).is(BlockTags.LEAVES)) {
                        int hangLen = 2 + random.nextInt(4);
                        for (int h = 1; h <= hangLen; h++) {
                            BlockPos hangPos = leafPos.below(h);
                            if (level.getBlockState(hangPos).isAir()) {
                                BlockState mossState = ModBlocks.WILD_HANGING_MOSS.get().defaultBlockState()
                                        .setValue(HangingMossBlock.TIP, (h == hangLen));
                                level.setBlock(hangPos, mossState, 2);
                            } else break;
                        }
                    }
                }
            }
        }
    }

    private void placeFoliageCircle(WorldGenLevel level, BlockPos center, int radius, RandomSource random, KapokTreeConfiguration config) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) + Math.abs(dz) <= radius + 1) {
                    BlockPos leafPos = center.offset(dx, 0, dz);
                    if (placeLeaves(level, leafPos, random, config)) {
                        // Random Wild Moss Carpet on top of leaves
                        if (random.nextFloat() < 0.25f) {
                            placeWildMossCarpetOnTop(level, leafPos.above());
                        }
                    }
                }
            }
        }
    }

    private void placeWildMossCarpetOnTop(WorldGenLevel level, BlockPos topPos) {
        if (level.getBlockState(topPos).isAir()) {
            BlockState carpetState = ModBlocks.WILD_MOSS_CARPET.get().defaultBlockState()
                    .setValue(MossyCarpetBlock.BASE, true);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos wallPos = topPos.relative(dir);
                BlockState wallState = level.getBlockState(wallPos);
                if (wallState.isSolid() || wallState.isFaceSturdy(level, wallPos, dir.getOpposite())) {
                    carpetState = carpetState.setValue(WildMossCarpetBlock.getProperty(dir), WallSide.LOW);
                }
            }
            level.setBlock(topPos, carpetState, 2);
        }
    }

    private boolean placeLog(WorldGenLevel level, BlockPos pos, RandomSource random, KapokTreeConfiguration config, Direction.Axis axis) {
        if (level.isOutsideBuildHeight(pos)) return false;
        // Use Mangrove Wood / Log default
        BlockState state = Blocks.MANGROVE_WOOD.defaultBlockState();
        if (config != null && config.trunkProvider() != null) {
            BlockState provState = config.trunkProvider().getState(level, random, pos);
            if (!provState.is(Blocks.OAK_LOG)) {
                state = provState;
            }
        }
        if (state.hasProperty(RotatedPillarBlock.AXIS)) {
            state = state.setValue(RotatedPillarBlock.AXIS, axis);
        }
        return level.setBlock(pos, state, 2);
    }

    private boolean placeLeaves(WorldGenLevel level, BlockPos pos, RandomSource random, KapokTreeConfiguration config) {
        if (level.isOutsideBuildHeight(pos)) return true;
        if (level.getBlockState(pos).canBeReplaced()) {
            // Use Mangrove Leaves default
            BlockState leafState = Blocks.MANGROVE_LEAVES.defaultBlockState();
            if (config != null && config.foliageProvider() != null) {
                BlockState provState = config.foliageProvider().getState(level, random, pos);
                if (!provState.is(Blocks.OAK_LEAVES)) {
                    leafState = provState;
                }
            }
            if (leafState.hasProperty(LeavesBlock.DISTANCE)) {
                leafState = leafState.setValue(LeavesBlock.DISTANCE, 1);
            }
            level.setBlock(pos, leafState, 2);
        }
        return true;
    }
}