package com.huwng.alterna.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class TrenchSandBlock extends FallingBlock {
    public static final MapCodec<TrenchSandBlock> CODEC = simpleCodec(TrenchSandBlock::new);
    private final int dustColor;

    public TrenchSandBlock(int dustColor, BlockBehaviour.Properties properties) {
        super(properties);
        this.dustColor = dustColor;
    }

    public TrenchSandBlock(BlockBehaviour.Properties properties) {
        this(0x1a1f2e, properties);
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }

    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return this.dustColor;
    }
}
