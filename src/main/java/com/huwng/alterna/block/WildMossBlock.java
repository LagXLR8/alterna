package com.huwng.alterna.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WallSide;

public class WildMossBlock extends Block implements BonemealableBlock {
    public WildMossBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return level.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int radius = 3;
        BlockState mossBlockState = this.defaultBlockState();
        BlockState carpetBlockState = ModBlocks.WILD_MOSS_CARPET.get().defaultBlockState();
        BlockState hangingMossState = ModBlocks.WILD_HANGING_MOSS.get().defaultBlockState();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > (radius + 0.5) * (radius + 0.5)) continue;

                    BlockPos targetPos = pos.offset(dx, dy, dz);
                    BlockState targetState = level.getBlockState(targetPos);

                    // 1. Convert nearby stone types (vanilla + mod stones) to WildMossBlock
                    if (isMossReplaceable(targetState)) {
                        if (random.nextFloat() < 0.65f) {
                            level.setBlock(targetPos, mossBlockState, 3);
                        }
                    } else if (targetState.isAir()) {
                        // 2. Place Wild Moss Carpet on top of solid ground
                        BlockPos belowPos = targetPos.below();
                        BlockState belowState = level.getBlockState(belowPos);
                        if (belowState.isFaceSturdy(level, belowPos, Direction.UP) && random.nextFloat() < 0.60f) {
                            BlockState carpetState = carpetBlockState.setValue(WildMossCarpetBlock.BASE, true);

                            // Check wall sides for adjacent walls
                            for (Direction dir : Direction.Plane.HORIZONTAL) {
                                BlockPos wallPos = targetPos.relative(dir);
                                BlockState wallState = level.getBlockState(wallPos);
                                if (wallState.isFaceSturdy(level, wallPos, dir.getOpposite())) {
                                    EnumProperty<WallSide> prop = WildMossCarpetBlock.getProperty(dir);
                                    carpetState = carpetState.setValue(prop, WallSide.LOW);
                                }
                            }

                            level.setBlock(targetPos, carpetState, 3);
                        }
                        // 3. Hang Wild Hanging Moss under solid ceilings
                        else {
                            BlockPos abovePos = targetPos.above();
                            BlockState aboveState = level.getBlockState(abovePos);
                            if (aboveState.isFaceSturdy(level, abovePos, Direction.DOWN) && random.nextFloat() < 0.35f) {
                                level.setBlock(targetPos, hangingMossState, 3);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isMossReplaceable(BlockState state) {
        if (state.is(BlockTags.MOSS_REPLACEABLE)) return true;
        Block b = state.getBlock();
        return b == ModBlocks.MARBLE.get()
                || b == ModBlocks.MARBLE_BRICKS.get()
                || b == ModBlocks.POLISHED_MARBLE.get()
                || b == ModBlocks.GNEISS.get()
                || b == ModBlocks.GNEISS_BRICKS.get()
                || b == ModBlocks.POLISHED_GNEISS.get()
                || b == ModBlocks.SERPENTINITE.get()
                || b == ModBlocks.SERPENTINITE_BRICKS.get()
                || b == ModBlocks.POLISHED_SERPENTINITE.get()
                || b == ModBlocks.TRENCH_STONE.get()
                || b == ModBlocks.TRENCH_STONE_BRICKS.get()
                || b == ModBlocks.POLISHED_TRENCH_STONE.get();
    }
}
