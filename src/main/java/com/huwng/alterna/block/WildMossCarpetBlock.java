package com.huwng.alterna.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.MossyCarpetBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WallSide;

public class WildMossCarpetBlock extends MossyCarpetBlock {
    public WildMossCarpetBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public static EnumProperty<WallSide> getProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> throw new IllegalArgumentException("Unsupported direction " + direction);
        };
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos currentPos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        BlockState updatedState = super.updateShape(state, level, tickAccess, currentPos, direction, neighborPos, neighborState, random);
        if (updatedState.isAir()) return updatedState;

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            EnumProperty<WallSide> prop = getProperty(dir);
            WallSide currentSide = updatedState.getValue(prop);

            if (currentSide != WallSide.NONE) {
                BlockPos wallPos = currentPos.relative(dir);
                boolean wallSturdy = level.getBlockState(wallPos).isFaceSturdy(level, wallPos, dir.getOpposite());

                if (!wallSturdy) {
                    updatedState = updatedState.setValue(prop, WallSide.NONE);
                } else {
                    BlockPos abovePos = currentPos.above();
                    BlockState aboveState = level.getBlockState(abovePos);
                    boolean hasCarpetAbove = aboveState.getBlock() instanceof MossyCarpetBlock && aboveState.getValue(prop) != WallSide.NONE;

                    updatedState = updatedState.setValue(prop, hasCarpetAbove ? WallSide.TALL : WallSide.LOW);
                }
            }
        }

        return updatedState;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockState newState = state;
        boolean updated = false;

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            EnumProperty<WallSide> prop = getProperty(dir);
            WallSide side = state.getValue(prop);

            if (side == WallSide.LOW) {
                // Place / update Wild Moss Carpet on the wall above in direction dir
                BlockPos abovePos = pos.above();
                BlockPos wallPosAbove = abovePos.relative(dir);
                if (level.getBlockState(wallPosAbove).isFaceSturdy(level, wallPosAbove, dir.getOpposite())) {
                    BlockState existingAboveState = level.getBlockState(abovePos);
                    if (existingAboveState.isAir()) {
                        BlockState newAboveState = this.defaultBlockState()
                                .setValue(BASE, false)
                                .setValue(prop, WallSide.LOW);
                        level.setBlock(abovePos, newAboveState, 3);
                        newState = newState.setValue(prop, WallSide.TALL);
                        updated = true;
                    } else if (existingAboveState.getBlock() instanceof MossyCarpetBlock) {
                        BlockState newAboveState = existingAboveState.setValue(prop, WallSide.LOW);
                        level.setBlock(abovePos, newAboveState, 3);
                        newState = newState.setValue(prop, WallSide.TALL);
                        updated = true;
                    }
                }
            } else if (side == WallSide.NONE) {
                BlockPos wallPos = pos.relative(dir);
                if (level.getBlockState(wallPos).isFaceSturdy(level, wallPos, dir.getOpposite())) {
                    newState = newState.setValue(prop, WallSide.LOW);
                    updated = true;
                }
            }
        }

        if (updated) {
            level.setBlock(pos, newState, 3);
        } else {
            // Fallback: spread wild moss carpet to adjacent ground blocks
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos targetPos = pos.relative(dir);
                if (level.getBlockState(targetPos).isAir()) {
                    BlockPos belowTarget = targetPos.below();
                    if (level.getBlockState(belowTarget).isFaceSturdy(level, belowTarget, Direction.UP)) {
                        level.setBlock(targetPos, this.defaultBlockState().setValue(BASE, true), 3);
                        break;
                    }
                }
            }
        }
    }
}
