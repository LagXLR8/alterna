package com.huwng.alterna.block;

import com.huwng.alterna.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

public class CloudberryVinesBlock extends CaveVinesBlock {
    public CloudberryVinesBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected GrowingPlantHeadBlock getHeadBlock() {
        return (GrowingPlantHeadBlock) ModBlocks.CLOUDBERRY_VINES.get();
    }

    @Override
    protected Block getBodyBlock() {
        return ModBlocks.CLOUDBERRY_VINES_PLANT.get();
    }

    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.CLOUD_BERRIES.get());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (state.getValue(BERRIES)) {
            Block.popResource(level, pos, new ItemStack(ModItems.CLOUD_BERRIES.get(), 1));
            float pitch = Mth.randomBetween(level.getRandom(), 0.8F, 1.2F);
            level.playSound(null, pos, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, pitch);
            BlockState newState = state.setValue(BERRIES, false);
            level.setBlock(pos, newState, 2);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, newState));
            return InteractionResult.SUCCESS;
        } else {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
    }

    private static int getVineLengthAbove(LevelReader level, BlockPos pos) {
        int length = 1;
        BlockPos.MutableBlockPos cursor = pos.mutable().move(Direction.UP);
        while (length < 20) {
            BlockState upperState = level.getBlockState(cursor);
            if (upperState.getBlock() instanceof CloudberryVinesPlantBlock || upperState.getBlock() instanceof CloudberryVinesBlock) {
                length++;
                cursor.move(Direction.UP);
            } else {
                break;
            }
        }
        return length;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (getVineLengthAbove(level, pos) >= 16) {
            if (!state.getValue(BERRIES) && random.nextFloat() < 0.11F) {
                level.setBlock(pos, state.setValue(BERRIES, true), 2);
            }
            return;
        }
        super.randomTick(state, level, pos, random);
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (getVineLengthAbove(level, pos) >= 16) {
            if (!state.getValue(BERRIES)) {
                level.setBlock(pos, state.setValue(BERRIES, true), 2);
            }
            return;
        }
        super.performBonemeal(level, random, pos, state);
    }
}
