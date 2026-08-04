package com.huwng.alterna.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.HugeMushroomBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.Map;

public class RootshroomStemBlock extends HugeMushroomBlock {
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = Map.of(
            Direction.UP, UP,
            Direction.DOWN, DOWN,
            Direction.NORTH, NORTH,
            Direction.EAST, EAST,
            Direction.SOUTH, SOUTH,
            Direction.WEST, WEST
    );

    public RootshroomStemBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, true)
                .setValue(EAST, true)
                .setValue(SOUTH, true)
                .setValue(WEST, true)
                .setValue(UP, true)
                .setValue(DOWN, true));
    }

    private static boolean isBlockingNeighbor(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);
        return neighborState.isSolid() || neighborState.isFaceSturdy(level, neighborPos, direction.getOpposite());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockGetter level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = this.defaultBlockState();

        for (Map.Entry<Direction, BooleanProperty> entry : PROPERTY_BY_DIRECTION.entrySet()) {
            Direction dir = entry.getKey();
            BooleanProperty prop = entry.getValue();
            if (isBlockingNeighbor(level, pos, dir)) {
                state = state.setValue(prop, false);
            }
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos currentPos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        BooleanProperty prop = PROPERTY_BY_DIRECTION.get(direction);
        if (prop != null) {
            // Once false, stay false permanently! If neighbor is solid, set to false.
            boolean isBlocked = isBlockingNeighbor(level, currentPos, direction);
            if (isBlocked) {
                return state.setValue(prop, false);
            }
        }
        return super.updateShape(state, level, tickAccess, currentPos, direction, neighborPos, neighborState, random);
    }
}
