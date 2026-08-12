package com.huwng.alterna.block;

import com.huwng.alterna.AlternaParticles;
import com.huwng.alterna.item.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public class EnchantStoneBlock extends FallingBlock {

    public static final MapCodec<EnchantStoneBlock> CODEC = simpleCodec(EnchantStoneBlock::new);

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }

    private static List<ItemStack> getDropList() {
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(ModItems.CHILLING_ENCHANT_STONE.get(), 1));
        list.add(new ItemStack(ModItems.ELASTIC_ENCHANT_STONE.get(), 1));
        list.add(new ItemStack(ModItems.GLUTTONY_ENCHANT_STONE.get(), 1));
        list.add(new ItemStack(ModItems.WILD_ENCHANT_STONE.get(), 1));
        list.add(new ItemStack(ModItems.VAMPIRISM_ENCHANT_STONE.get(), 1));
        list.add(new ItemStack(ModItems.HEROISM_ENCHANT_STONE.get(), 1));
        list.add(new ItemStack(ModItems.DEFERRED_ENCHANT_STONE.get(), 1));
        list.add(new ItemStack(ModItems.DETONATION_ENCHANT_STONE.get(), 1));
        list.add(new ItemStack(ModItems.STORMBREAKER_ENCHANT_STONE.get(), 1));
        return list;
    }

    public EnchantStoneBlock(BlockBehaviour.Properties properties) {
        super(properties
                .instrument(NoteBlockInstrument.BASEDRUM)
                .sound(SoundType.STONE)
                .strength(5f, 5f)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .emissiveRendering((bs, br, bp) -> true)
                .isRedstoneConductor((bs, br, bp) -> false));
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return -8355712;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        if (!(builder.getLevel() instanceof ServerLevel)) {
            return super.getDrops(state, builder);
        }

        List<ItemStack> dropList = getDropList();
        if (dropList.isEmpty()) {
            return super.getDrops(state, builder);
        }

        RandomSource random = builder.getLevel().getRandom();
        ItemStack randomDrop = dropList.get(random.nextInt(dropList.size())).copy();
        return List.of(randomDrop);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);

        if (random.nextFloat() < 0.4f) {
            spawnOuterParticles(level, pos, random);
        }
    }

    private void spawnOuterParticles(Level level, BlockPos pos, RandomSource random) {
        int face = random.nextInt(6);
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        switch (face) {
            case 0:
                x += (random.nextDouble() - 0.5) * 1.1;
                y = pos.getY() + 1.05;
                z += (random.nextDouble() - 0.5) * 1.1;
                break;
            case 1:
                x += (random.nextDouble() - 0.5) * 1.1;
                y = pos.getY() - 0.05;
                z += (random.nextDouble() - 0.5) * 1.1;
                break;
            case 2:
                x += (random.nextDouble() - 0.5) * 1.1;
                y += (random.nextDouble() - 0.5) * 1.1;
                z = pos.getZ() - 0.05;
                break;
            case 3:
                x += (random.nextDouble() - 0.5) * 1.1;
                y += (random.nextDouble() - 0.5) * 1.1;
                z = pos.getZ() + 1.05;
                break;
            case 4:
                x = pos.getX() - 0.05;
                y += (random.nextDouble() - 0.5) * 1.1;
                z += (random.nextDouble() - 0.5) * 1.1;
                break;
            case 5:
                x = pos.getX() + 1.05;
                y += (random.nextDouble() - 0.5) * 1.1;
                z += (random.nextDouble() - 0.5) * 1.1;
                break;
        }

        int type = random.nextInt(3);
        if (type == 0) {
            level.addParticle(AlternaParticles.ENCHANT.get(), x, y, z, 0, 0, 0);
        } else if (type == 1) {
            level.addParticle(ParticleTypes.PORTAL, x, y, z,
                (random.nextDouble() - 0.5) * 0.05,
                (random.nextDouble() - 0.5) * 0.05,
                (random.nextDouble() - 0.5) * 0.05);
        } else {
            level.addParticle(ParticleTypes.ENCHANT, x, y, z, 0, 0.02, 0);
        }
    }
}
