package com.huwng.alterna.block;

import com.huwng.alterna.entity.ModEntities;
import com.huwng.alterna.entity.TetraEntity;
import com.huwng.alterna.item.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Tetra nest of twigs and lichen, usually found on top of cliff ledges above the
 * Lichen Savanna. Holds up to 3 eggs that slowly hatch into baby Tetras.
 *
 * Tetras are calm, but fiercely territorial about their nests: taking an egg or
 * breaking a nest that still has eggs makes every adult Tetra nearby attack you.
 * Eggs placed into a nest by a player are "tended"; a baby that hatches from a tended
 * nest imprints on the nearest player and comes out tame.
 */
public class TetraNestBlock extends Block {
    public static final MapCodec<TetraNestBlock> CODEC = simpleCodec(TetraNestBlock::new);
    public static final IntegerProperty EGGS = IntegerProperty.create("eggs", 0, 3);
    public static final IntegerProperty HATCH = IntegerProperty.create("hatch", 0, 2);
    public static final BooleanProperty TENDED = BooleanProperty.create("tended");
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 5.0, 16.0);

    public TetraNestBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(EGGS, 0).setValue(HATCH, 0).setValue(TENDED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EGGS, HATCH, TENDED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return direction == Direction.DOWN && !state.canSurvive(level, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (stack.is(ModItems.TETRA_EGG.get()) && state.getValue(EGGS) < 3) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(EGGS, state.getValue(EGGS) + 1).setValue(TENDED, true), 3);
                level.playSound(null, pos, SoundEvents.FROGSPAWN_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
                stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int eggs = state.getValue(EGGS);
        if (eggs <= 0) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(EGGS, eggs - 1).setValue(HATCH, 0), 3);
            if (!player.getInventory().add(new ItemStack(ModItems.TETRA_EGG.get()))) {
                player.drop(new ItemStack(ModItems.TETRA_EGG.get()), false);
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.6F, 1.0F);
            if (level instanceof ServerLevel server && !state.getValue(TENDED)) {
                TetraEntity.alertTetras(server, pos, player);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server && !player.isCreative() && !state.getValue(TENDED)) {
            TetraEntity.alertTetras(server, pos, player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(EGGS) > 0;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(2) != 0) return;
        int hatch = state.getValue(HATCH);
        if (hatch < 2) {
            level.playSound(null, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS, 0.6F, 1.1F + random.nextFloat() * 0.2F);
            level.setBlock(pos, state.setValue(HATCH, hatch + 1), 2);
            return;
        }
        int eggs = state.getValue(EGGS);
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 0.8F, 1.1F + random.nextFloat() * 0.2F);
        level.setBlock(pos, state.setValue(EGGS, eggs - 1).setValue(HATCH, 0).setValue(TENDED, eggs - 1 > 0 && state.getValue(TENDED)), 3);
        TetraEntity baby = ModEntities.TETRA.get().create(level, EntitySpawnReason.BREEDING);
        if (baby != null) {
            baby.setAge(-24000);
            baby.snapTo(pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            baby.setHomeNest(pos);
            if (state.getValue(TENDED)) {
                Player nearest = level.getNearestPlayer(baby, 16.0);
                if (nearest != null) baby.tame(nearest);
            }
            level.addFreshEntity(baby);
        }
    }
}
