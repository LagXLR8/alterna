package com.huwng.alterna.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ember Cap - a tuft of tiny glowing orange mushrooms that dots the savanna grass.
 * Gives off a faint light and the occasional drifting spark. Edible (see ModItems).
 */
public class EmberCapBlock extends VegetationBlock {
    public static final MapCodec<EmberCapBlock> CODEC = simpleCodec(EmberCapBlock::new);
    private static final VoxelShape SHAPE = Block.column(10.0, 0.0, 6.0);

    public EmberCapBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends VegetationBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(18) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME,
                    pos.getX() + 0.3 + random.nextDouble() * 0.4,
                    pos.getY() + 0.35,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4,
                    0.0, 0.01, 0.0);
        }
    }
}
