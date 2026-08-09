package com.huwng.alterna.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class VitalrootFullBlock extends Block {
    public static final BooleanProperty STEM = BooleanProperty.create("stem");

    public VitalrootFullBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(STEM, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean isAirAbove = context.getLevel().getBlockState(context.getClickedPos().above()).isAir();
        return this.defaultBlockState().setValue(STEM, isAirAbove);
    }

    @Override
    public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos currentPos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        BlockState updatedState = super.updateShape(state, level, tickAccess, currentPos, direction, neighborPos, neighborState, random);
        if (direction == Direction.UP) {
            boolean isAirAbove = neighborState.isAir();
            return updatedState.setValue(STEM, isAirAbove);
        }
        return updatedState;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STEM);
    }
}
