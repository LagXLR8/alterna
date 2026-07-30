package com.huwng.alterna.block;

import com.huwng.alterna.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class WhiteCurrantBushBlock extends SweetBerryBushBlock {
    public WhiteCurrantBushBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.WHITE_CURRANT_BERRIES.get());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        int age = state.getValue(AGE);
        boolean isFullyGrown = age == 3;
        if (age > 1) {
            int count = 1 + level.getRandom().nextInt(2) + (isFullyGrown ? 1 : 0);
            popResource(level, pos, new ItemStack(ModItems.WHITE_CURRANT_BERRIES.get(), count));
            float pitch = 0.8F + level.getRandom().nextFloat() * 0.4F;
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, pitch);
            BlockState updatedState = state.setValue(AGE, 1);
            level.setBlock(pos, updatedState, 2);
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }
}
