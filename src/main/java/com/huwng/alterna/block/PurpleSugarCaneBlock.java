package com.huwng.alterna.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.huwng.alterna.item.ModItems;

public class PurpleSugarCaneBlock extends Block {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_15;
    public static final BooleanProperty TOP = BooleanProperty.create("top");
    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public PurpleSugarCaneBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0).setValue(TOP, true));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(TOP, !context.getLevel().getBlockState(context.getClickedPos().above()).is(this));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess, BlockPos currentPos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (!state.canSurvive(level, currentPos)) {
            tickAccess.scheduleTick(currentPos, this, 1);
        }

        boolean isTop = !level.getBlockState(currentPos.above()).is(this);
        return state.setValue(TOP, isTop);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState belowState = level.getBlockState(pos.below());
        if (belowState.is(this)) {
            return true;
        } else if (isAcceptableGround(belowState)) {
            BlockPos posBelow = pos.below();
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos adjPos = posBelow.relative(direction);
                BlockState adjState = level.getBlockState(adjPos);
                FluidState fluidState = level.getFluidState(adjPos);
                if (fluidState.is(FluidTags.WATER) || fluidState.getType() == Fluids.WATER || fluidState.getType() == Fluids.FLOWING_WATER || adjState.is(Blocks.WATER) || adjState.is(Blocks.FROSTED_ICE)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isAcceptableGround(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)
                || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.MUD)
                || state.is(Blocks.MOSS_BLOCK) || state.is(ModBlocks.WILD_MOSS_BLOCK.get())
                || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.SUSPICIOUS_SAND);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isEmptyBlock(pos.above())) {
            int i = 1;
            while (level.getBlockState(pos.below(i)).is(this)) {
                i++;
            }

            if (i < 3) {
                int age = state.getValue(AGE);
                if (age == 15) {
                    level.setBlockAndUpdate(pos.above(), this.defaultBlockState().setValue(AGE, 0).setValue(TOP, true));
                    level.setBlock(pos, state.setValue(AGE, 0).setValue(TOP, false), 4);
                } else {
                    level.setBlock(pos, state.setValue(AGE, age + 1), 4);
                }
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, TOP);
    }

    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.PURPLE_SUGAR_CANE.get());
    }
}
