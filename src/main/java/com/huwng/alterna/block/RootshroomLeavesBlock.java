package com.huwng.alterna.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class RootshroomLeavesBlock extends LeavesBlock {
    public static final MapCodec<RootshroomLeavesBlock> CODEC = simpleCodec(RootshroomLeavesBlock::new);

    public RootshroomLeavesBlock(BlockBehaviour.Properties properties) {
        super(0.01F, properties);
    }

    @Override
    public MapCodec<RootshroomLeavesBlock> codec() {
        return CODEC;
    }

    @Override
    protected void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
    }
}
