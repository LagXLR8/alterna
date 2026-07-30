package com.huwng.alterna.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class StarLilyVineBlock extends VineBlock implements BonemealableBlock {
    public static final int MAX_LENGTH = 10;

    public StarLilyVineBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int currentLength = getVineLength(level, pos);
        if (currentLength >= MAX_LENGTH) {
            return;
        }
        super.randomTick(state, level, pos, random);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        BlockPos lowestPos = getLowestVinePos(level, pos);
        BlockPos targetBelow = lowestPos.below();
        return getVineLength(level, lowestPos) < MAX_LENGTH && level.getBlockState(targetBelow).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos lowestPos = getLowestVinePos(level, pos);
        BlockPos growPos = lowestPos.below();
        BlockState lowestState = level.getBlockState(lowestPos);

        if (level.getBlockState(growPos).isAir() && getVineLength(level, lowestPos) < MAX_LENGTH) {
            BlockState newVineState = this.defaultBlockState();
            boolean copiedFace = false;

            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (lowestState.hasProperty(getPropertyForFace(dir)) && lowestState.getValue(getPropertyForFace(dir))) {
                    newVineState = newVineState.setValue(getPropertyForFace(dir), true);
                    copiedFace = true;
                }
            }

            if (!copiedFace) {
                newVineState = newVineState.setValue(UP, true);
            }

            level.setBlock(growPos, newVineState, 3);
        }
    }

    private static BlockPos getLowestVinePos(LevelReader level, BlockPos pos) {
        BlockPos current = pos;
        while (level.getBlockState(current.below()).getBlock() instanceof StarLilyVineBlock) {
            current = current.below();
        }
        return current;
    }

    private static int getVineLength(LevelReader level, BlockPos pos) {
        int length = 1;
        BlockPos checkPos = pos.above();
        while (level.getBlockState(checkPos).getBlock() instanceof StarLilyVineBlock && length < 20) {
            length++;
            checkPos = checkPos.above();
        }
        return length;
    }
}
