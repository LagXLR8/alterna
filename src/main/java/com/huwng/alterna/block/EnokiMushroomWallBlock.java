package com.huwng.alterna.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Map;

public class EnokiMushroomWallBlock extends Block implements SimpleWaterloggedBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    // Số nhịp heartbeat (10 tick/nhịp) còn lại trước khi hết cooldown.
    // 40 nhịp * 10 tick = 400 tick = 20 giây.
    private static final int COOLDOWN_HEARTBEATS = 40;
    private static final int HEARTBEAT_INTERVAL = 10;
    public static final IntegerProperty COOLDOWN_TICKS = IntegerProperty.create("cooldown_ticks", 0,
            COOLDOWN_HEARTBEATS);

    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.NORTH, Block.box(2.0, 2.0, 4.0, 14.0, 14.0, 16.0),
            Direction.SOUTH, Block.box(2.0, 2.0, 0.0, 14.0, 14.0, 12.0),
            Direction.WEST, Block.box(4.0, 2.0, 2.0, 16.0, 14.0, 14.0),
            Direction.EAST, Block.box(0.0, 2.0, 2.0, 12.0, 14.0, 14.0));

    public EnokiMushroomWallBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(COOLDOWN_TICKS, 0)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.getOrDefault(state.getValue(FACING), SHAPES.get(Direction.NORTH));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction direction = state.getValue(FACING);
        BlockPos behindPos = pos.relative(direction.getOpposite());
        BlockState behindState = level.getBlockState(behindPos);
        return behindState.isFaceSturdy(level, behindPos, direction);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.getAxis().isHorizontal()) {
                Direction opposite = direction.getOpposite();
                state = state.setValue(FACING, opposite).setValue(COOLDOWN_TICKS, 0);
                if (state.canSurvive(level, pos)) {
                    FluidState fluidstate = context.getLevel().getFluidState(pos);
                    return state.setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
                }
            }
        }

        return null;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            // Chỉ có DUY NHẤT một heartbeat cho block này tại mọi thời điểm.
            if (!serverLevel.getBlockTicks().hasScheduledTick(pos, this)) {
                serverLevel.scheduleTick(pos, this, HEARTBEAT_INTERVAL);
            }
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            if (entity instanceof LivingEntity livingEntity && !livingEntity.isSpectator()) {
                if (state.getValue(COOLDOWN_TICKS) == 0) {
                    triggerSporeDefense(state, serverLevel, pos, List.of(livingEntity));
                }
            }
        }
        super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int remaining = state.getValue(COOLDOWN_TICKS);

        if (remaining > 0) {
            // Đang trong thời gian hồi chiêu -> đếm ngược 1 nhịp.
            level.setBlock(pos, state.setValue(COOLDOWN_TICKS, remaining - 1), 3);
        } else {
            // Hết cooldown -> quét khu vực 4x4 quanh block.
            AABB area = new AABB(pos).inflate(2.0D, 1.0D, 2.0D);
            List<LivingEntity> mobs = level.getEntitiesOfClass(LivingEntity.class, area, e -> !e.isSpectator());

            if (!mobs.isEmpty()) {
                triggerSporeDefense(state, level, pos, mobs);
            }
        }

        // Luôn duy trì đúng MỘT heartbeat kế tiếp, dù nhánh nào ở trên chạy.
        level.scheduleTick(pos, this, HEARTBEAT_INTERVAL);
    }

    private void triggerSporeDefense(BlockState state, ServerLevel level, BlockPos pos, List<LivingEntity> mobs) {
        if (state.getValue(COOLDOWN_TICKS) > 0) {
            return; // Đã trong cooldown!
        }

        // Bật cooldown = 40 nhịp (400 tick / 20 giây). KHÔNG tự lên lịch tick ở đây
        // - heartbeat trong tick() sẽ tự đếm ngược, tránh 2 tick chạy song song.
        level.setBlock(pos, state.setValue(COOLDOWN_TICKS, COOLDOWN_HEARTBEATS), 3);

        double cx = pos.getX() + 0.5D;
        double cy = pos.getY() + 0.5D;
        double cz = pos.getZ() + 0.5D;
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, cx, cy, cz, 40, 0.5D, 0.5D, 0.5D, 0.05D);
        level.sendParticles(ParticleTypes.MYCELIUM, cx, cy, cz, 30, 0.4D, 0.4D, 0.4D, 0.02D);
        level.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.3F);

        for (LivingEntity mob : mobs) {
            mob.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess,
            BlockPos currentPos, Direction direction, BlockPos neighborPos, BlockState neighborState,
            RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            tickAccess.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return direction.getOpposite() == state.getValue(FACING) && !state.canSurvive(level, currentPos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, tickAccess, currentPos, direction, neighborPos, neighborState,
                        random);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, COOLDOWN_TICKS, WATERLOGGED);
    }
}