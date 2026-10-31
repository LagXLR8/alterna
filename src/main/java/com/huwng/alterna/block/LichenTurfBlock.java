package com.huwng.alterna.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lichen Turf - the purple ground cover of the Lichen Savanna.
 * Unlike grass it is a lichen, so it does not need light: it slowly creeps over
 * nearby dirt in total darkness, and only dies back to dirt when smothered by a
 * solid block on top. Cragbacks graze it back down to dirt; it regrows on its own.
 */
public class LichenTurfBlock extends Block implements BonemealableBlock {
    public static final MapCodec<LichenTurfBlock> CODEC = simpleCodec(LichenTurfBlock::new);

    public LichenTurfBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    private static boolean smothered(BlockGetter level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return above.isSolidRender() || !above.getFluidState().isEmpty();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (smothered(level, pos)) {
            level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
            return;
        }
        // Creep onto one nearby dirt block per tick (lichen ignores light level).
        for (int i = 0; i < 2; i++) {
            BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
            BlockState t = level.getBlockState(target);
            if ((t.is(Blocks.DIRT) || t.is(Blocks.COARSE_DIRT)) && !smothered(level, target)) {
                level.setBlockAndUpdate(target, this.defaultBlockState());
                return;
            }
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return level.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public BonemealableBlock.Type getType() {
        return BonemealableBlock.Type.NEIGHBOR_SPREADER;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos start = pos.above();
        for (int attempt = 0; attempt < 96; attempt++) {
            BlockPos p = start;
            for (int step = 0; step < attempt / 16; step++) {
                p = p.offset(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
            }
            if (!level.getBlockState(p.below()).is(this) || !level.getBlockState(p).isAir()) continue;
            float roll = random.nextFloat();
            if (roll < 0.62f) {
                level.setBlock(p, ModBlocks.LICHEN_GRASS.get().defaultBlockState(), 3);
            } else if (roll < 0.80f) {
                if (level.getBlockState(p.above()).isAir()) {
                    DoublePlantBlock.placeAt(level, ModBlocks.TALL_LICHEN_GRASS.get().defaultBlockState(), p, 3);
                }
            } else if (roll < 0.90f) {
                level.setBlock(p, ModBlocks.EMBER_CAP.get().defaultBlockState(), 3);
            } else if (roll < 0.94f) {
                level.setBlock(p, ModBlocks.VIOLET_BUSH.get().defaultBlockState(), 3);
            }
        }
    }
}
