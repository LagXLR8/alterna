package com.huwng.alterna.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class EnokiMushroomBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty CLOSED = BooleanProperty.create("closed");
    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

    public EnokiMushroomBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CLOSED, false).setValue(WATERLOGGED, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isSolid() || state.is(Blocks.MYCELIUM) || state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.NETHERRACK) || state.is(Blocks.CRIMSON_NYLIUM) || state.is(Blocks.WARPED_NYLIUM)
                || state.is(Blocks.STONE);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        return this.mayPlaceOn(level.getBlockState(belowPos), level, belowPos);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState().setValue(CLOSED, false).setValue(WATERLOGGED,
                fluidstate.getType() == Fluids.WATER);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        handleEntityInteraction(state, level, pos, entity);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        handleEntityInteraction(state, level, pos, entity);
        super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
    }

    private void handleEntityInteraction(BlockState state, Level level, BlockPos pos, Entity entity) {
        // Ignore non-living entities (e.g. dropped items, projectiles)
        if (entity instanceof LivingEntity livingEntity && !livingEntity.isSpectator()) {
            double dx = livingEntity.getX() - (pos.getX() + 0.5D);
            double dz = livingEntity.getZ() - (pos.getZ() + 0.5D);
            double distSq = dx * dx + dz * dz;

            boolean isNearCenter = distSq <= (0.35D * 0.35D); // Within 0.35 blocks of center

            if (!state.getValue(CLOSED)) {
                if (isNearCenter) {
                    closeTrap(state, level, pos);
                } else if (level instanceof ServerLevel serverLevel) {
                    if (!serverLevel.getBlockTicks().hasScheduledTick(pos, this)) {
                        serverLevel.scheduleTick(pos, this, 20); // 1-second delay
                    }
                }
            } else {
                // Reduce movement speed by 70% (retain 30% speed)
                livingEntity.makeStuckInBlock(state, new Vec3(0.40D, 0.40D, 0.40D));
                Vec3 movement = livingEntity.getDeltaMovement();
                livingEntity.setDeltaMovement(movement.x * 0.40D, movement.y < 0 ? movement.y : 0.0D,
                        movement.z * 0.40D);
            }
        }
    }

    private void closeTrap(BlockState state, Level level, BlockPos pos) {
        level.setBlock(pos, state.setValue(CLOSED, true), 3);
        level.playSound(null, pos, SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 0.8F, 1.2F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.scheduleTick(pos, this, 20);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class,
                new AABB(pos).inflate(0.2D, 0.5D, 0.2D));

        if (!state.getValue(CLOSED)) {
            // 1-second delay expired: close trap if a LivingEntity is still present
            if (!entities.isEmpty()) {
                closeTrap(state, level, pos);
            }
        } else {
            // Closed state monitoring: reopen if empty, otherwise keep monitoring
            if (entities.isEmpty()) {
                level.setBlock(pos, state.setValue(CLOSED, false), 3);
                level.playSound(null, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 0.6F, 1.4F);
            } else {
                level.scheduleTick(pos, this, 20);
            }
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess,
            BlockPos currentPos, Direction direction, BlockPos neighborPos, BlockState neighborState,
            RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            tickAccess.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return !state.canSurvive(level, currentPos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, tickAccess, currentPos, direction, neighborPos, neighborState,
                        random);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CLOSED, WATERLOGGED);
    }
}
