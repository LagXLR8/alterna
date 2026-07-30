package com.huwng.alterna.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBedBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;

public class StarLilyBlock extends FlowerBedBlock {
    public StarLilyBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getClickedFace() == Direction.DOWN) {
            return ModBlocks.HANGING_STAR_LILY.get().defaultBlockState();
        }
        return super.getStateForPlacement(context);
    }

    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ModBlocks.STAR_LILY.get());
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int amount = state.getValue(AMOUNT);
        if (amount < 4) {
            level.setBlock(pos, state.setValue(AMOUNT, amount + 1), 2);
        } else {
            popResource(level, pos, new ItemStack(ModBlocks.STAR_LILY.get(), 1));
        }
    }
}
