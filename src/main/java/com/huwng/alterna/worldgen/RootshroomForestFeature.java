package com.huwng.alterna.worldgen;

import com.huwng.alterna.block.ModBlocks;
import com.huwng.alterna.entity.ArmorOverlordEntity;
import com.huwng.alterna.entity.ModEntities;
import net.minecraft.world.entity.EntitySpawnReason;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class RootshroomForestFeature extends Feature<NoneFeatureConfiguration> {
    /** One possible Armor Overlord lair per 64x64 area of forest. */
    private static final int OVERLORD_CELL = 64;

    public RootshroomForestFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return placeDirect(context.level(), context.random(), context.origin(), 0.5, context.random().nextLong());
    }

    public static boolean placeDirect(WorldGenLevel level, RandomSource random, BlockPos topPos, double edgeDistNorm, long seed) {
        int x = topPos.getX();
        int z = topPos.getZ();
        trySpawnOverlord(level, topPos, edgeDistNorm, seed);

        // 9x9 grid cells for tree/structure placement
        int gridSize = 9;
        int cellX = Math.floorDiv(x, gridSize);
        int cellZ = Math.floorDiv(z, gridSize);

        long gridHash = (long) cellX * 73856093L ^ (long) cellZ * 19349663L ^ seed;
        int targetX = cellX * gridSize + 4 + (int) ((gridHash & 3) - 1);
        int targetZ = cellZ * gridSize + 4 + (int) (((gridHash >> 2) & 3) - 1);

        if (x == targetX && z == targetZ && edgeDistNorm >= 0.12 && edgeDistNorm <= 0.88) {
            long cellIndex = (long) cellX * 31L ^ (long) cellZ ^ seed;
            int roll = (int) (Math.abs(cellIndex) % 100);

            if (roll < 30) {
                // ~30% Tree
                return RootshroomTreeFeature.placeDirect(level, random, topPos);
            } else if (roll < 50) {
                // ~20% Stump
                return RootshroomStumpFeature.placeDirect(level, random, topPos);
            } else if (roll < 65) {
                // ~15% House Structure
                return LedgeStructureFeature.placeStructure(level, random, topPos, 1, 8.0, true);
            } else if (roll < 80) {
                // ~15% Giant Vitalroot Structure
                return GiantVitalrootFeature.placeDirect(level, random, topPos);
            } else if (roll < 95) {
                // ~15% Fern Group Structure
                return FernGroupFeature.placeDirect(level, random, topPos);
            }
        }

        // Sparse ground vegetation (~25% total density)
        if (!level.getBlockState(topPos).isAir()) return true;
        float rollVeg = random.nextFloat();
        if (rollVeg < 0.04f) {
            level.setBlock(topPos, ModBlocks.ENOKI_MUSHROOM.get().defaultBlockState(), 2);
        } else if (rollVeg < 0.06f) {
            level.setBlock(topPos, ModBlocks.VITALROOT.get().defaultBlockState(), 2);
        } else if (rollVeg < 0.10f) {
            level.setBlock(topPos, Blocks.MOSS_CARPET.defaultBlockState(), 2);
        } else if (rollVeg < 0.18f) {
            level.setBlock(topPos, Blocks.SHORT_GRASS.defaultBlockState(), 2);
        } else if (rollVeg < 0.25f) {
            level.setBlock(topPos, Blocks.FERN.defaultBlockState(), 2);
        }
        return true;
    }

    /**
     * The forest's apex hunter. Each 64x64 cell picks one column (always on the 9-block grid
     * lines, so never inside a tree trunk) and, 40% of the time, an Armor Overlord waits there.
     */
    private static void trySpawnOverlord(WorldGenLevel level, BlockPos topPos, double edgeDistNorm, long seed) {
        int x = topPos.getX(), z = topPos.getZ();
        if (Math.floorMod(x, 9) != 0 || Math.floorMod(z, 9) != 0) return;
        if (edgeDistNorm < 0.25 || edgeDistNorm > 0.75) return;
        int cx = Math.floorDiv(x, OVERLORD_CELL), cz = Math.floorDiv(z, OVERLORD_CELL);
        long h = (cx * 341873128712L) ^ (cz * 132897987541L) ^ (seed * 0x9E3779B97F4A7C15L);
        h ^= (h >>> 29);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 32);
        if (Math.floorMod(h, 100) >= 40) return;
        int px = cx * OVERLORD_CELL + 9 * (1 + (int) Math.floorMod(h >>> 8, 6L));
        int pz = cz * OVERLORD_CELL + 9 * (1 + (int) Math.floorMod(h >>> 16, 6L));
        if (x != Math.floorDiv(px, 9) * 9 || z != Math.floorDiv(pz, 9) * 9) return;
        // room to stand: 3x3 footprint, 5 blocks of headroom
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy < 5; dy++) {
                    p.set(x + dx, topPos.getY() + dy, z + dz);
                    if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) return;
                }
            }
        }
        if (!level.getBlockState(topPos.below()).isSolid()) return;
        ArmorOverlordEntity e = ModEntities.ARMOR_OVERLORD.get().create(level.getLevel(), EntitySpawnReason.CHUNK_GENERATION);
        if (e == null) return;
        e.snapTo(x + 0.5, topPos.getY(), z + 0.5, (float) Math.floorMod(h >>> 24, 360L), 0.0F);
        e.finalizeSpawn(level, level.getCurrentDifficultyAt(topPos), EntitySpawnReason.CHUNK_GENERATION, null);
        e.setPersistenceRequired();
        level.addFreshEntityWithPassengers(e);
    }
}
