package com.huwng.alterna.worldgen;

import com.huwng.alterna.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;

import com.huwng.alterna.entity.ModEntities;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;

@SuppressWarnings("unchecked")
public class LedgeStructureFeature {

    private static final ResourceKey<LootTable>[] DUNGEON_LOOT_TABLES = new ResourceKey[] {
            ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse("minecraft:chests/simple_dungeon")),
            ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse("minecraft:chests/abandoned_mineshaft")),
            ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse("minecraft:chests/desert_pyramid")),
            ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse("minecraft:chests/jungle_temple")),
            ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse("minecraft:chests/trial_chambers/reward"))
    };

    private static EntityType<?> pickRandomSpawnerEntity(RandomSource random) {
        EntityType<?>[] pool = new EntityType<?>[] {
                ModEntities.CLIMBING_ZOMBIE.get(),
                EntityType.CAVE_SPIDER,
                EntityType.STRAY,
                EntityType.BOGGED
        };
        return pool[random.nextInt(pool.length)];
    }

    private static final Identifier[] SMALL_HOUSES = new Identifier[]{
            Identifier.fromNamespaceAndPath("alterna", "small_gneiss_house_1"),
            Identifier.fromNamespaceAndPath("alterna", "small_gneiss_house_2"),
            Identifier.fromNamespaceAndPath("alterna", "small_gneiss_house_3")
    };

    private static final Identifier[] BIG_HOUSES = new Identifier[]{
            Identifier.fromNamespaceAndPath("alterna", "big_gneiss_house_1"),
            Identifier.fromNamespaceAndPath("alterna", "big_gneiss_house_2"),
            Identifier.fromNamespaceAndPath("alterna", "big_gneiss_house_3")
    };

    public static boolean placeStructure(WorldGenLevel level, RandomSource random, BlockPos topPos, int side, double reachWidth, boolean isGiantLedge) {
        if (level.getLevel() == null || level.getLevel().getServer() == null) return false;
        StructureTemplateManager manager = level.getLevel().getServer().getStructureManager();

        // Measure available vertical headroom above topPos
        int availableHeight = 0;
        for (int y = 1; y <= 14; y++) {
            if (level.getBlockState(topPos.above(y)).isSolid()) {
                break;
            }
            availableHeight++;
        }
        if (availableHeight < 4) return false; // Needs at least 4-5 blocks height headroom

        StructureTemplate finalTemplate = null;
        Vec3i finalSize = null;
        Rotation finalRotation = Rotation.NONE;

        // Determine candidate pools. Give big houses a chance when reachWidth & headroom allow,
        // but fall back to small houses if headroom or reachWidth is too tight for big houses.
        boolean preferBig = (isGiantLedge || reachWidth >= 8.5) && availableHeight >= 9 && random.nextFloat() < 0.60f;

        Identifier[] primaryPool = preferBig ? BIG_HOUSES : SMALL_HOUSES;
        Identifier[] secondaryPool = preferBig ? SMALL_HOUSES : BIG_HOUSES;

        // Try primary pool first, then secondary pool
        for (Identifier[] pool : new Identifier[][]{primaryPool, secondaryPool}) {
            int[] order = shuffledIndices(random, pool.length);
            for (int idx : order) {
                Identifier houseId = pool[idx];
                Optional<StructureTemplate> tOpt = manager.get(houseId);
                if (tOpt.isEmpty()) continue;
                StructureTemplate t = tOpt.get();
                Vec3i s = t.getSize();

                // Check height requirement
                if (s.getY() > availableHeight) continue;

                // Try all 4 rotations
                for (Rotation rot : shuffledRotations(random)) {
                    int rx = (rot == Rotation.CLOCKWISE_90 || rot == Rotation.COUNTERCLOCKWISE_90) ? s.getZ() : s.getX();
                    int rz = (rot == Rotation.CLOCKWISE_90 || rot == Rotation.COUNTERCLOCKWISE_90) ? s.getX() : s.getZ();
                    int minFootprint = Math.min(rx, rz);
                    if (reachWidth >= minFootprint * 0.70) {
                        finalTemplate = t;
                        finalSize = s;
                        finalRotation = rot;
                        break;
                    }
                }
                if (finalTemplate != null) break;
            }
            if (finalTemplate != null) break;
        }

        if (finalTemplate == null || finalSize.getX() <= 0 || finalSize.getZ() <= 0) return false;

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(finalRotation)
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(true)
                .setKnownShape(true)
                .addProcessor(BlockIgnoreProcessor.AIR);

        // Compute rotated relative bounding box
        int rawSX = finalSize.getX();
        int rawSZ = finalSize.getZ();

        BlockPos relC0 = StructureTemplate.calculateRelativePosition(settings, new BlockPos(0, 0, 0));
        BlockPos relC1 = StructureTemplate.calculateRelativePosition(settings, new BlockPos(rawSX - 1, 0, 0));
        BlockPos relC2 = StructureTemplate.calculateRelativePosition(settings, new BlockPos(0, 0, rawSZ - 1));
        BlockPos relC3 = StructureTemplate.calculateRelativePosition(settings, new BlockPos(rawSX - 1, 0, rawSZ - 1));

        int minRelX = Math.min(Math.min(relC0.getX(), relC1.getX()), Math.min(relC2.getX(), relC3.getX()));
        int maxRelX = Math.max(Math.max(relC0.getX(), relC1.getX()), Math.max(relC2.getX(), relC3.getX()));
        int minRelZ = Math.min(Math.min(relC0.getZ(), relC1.getZ()), Math.min(relC2.getZ(), relC3.getZ()));
        int maxRelZ = Math.max(Math.max(relC0.getZ(), relC1.getZ()), Math.max(relC2.getZ(), relC3.getZ()));

        // Center structure's rotated footprint around topPos
        int centerOffsetX = (minRelX + maxRelX) / 2;
        int centerOffsetZ = (minRelZ + maxRelZ) / 2;
        BlockPos originPos = topPos.offset(-centerOffsetX, 0, -centerOffsetZ);

        // 1. Clear vegetation / non-solid replaceable blocks inside house volume
        for (int dx = 0; dx < rawSX; dx++) {
            for (int dz = 0; dz < rawSZ; dz++) {
                for (int dy = 0; dy < finalSize.getY(); dy++) {
                    BlockPos relPos = StructureTemplate.calculateRelativePosition(settings, new BlockPos(dx, dy, dz));
                    BlockPos wPos = originPos.offset(relPos);
                    BlockState current = level.getBlockState(wPos);
                    if (!current.isAir() && (!current.isSolid() || current.canBeReplaced())) {
                        level.setBlock(wPos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // 2. Build foundation underneath exact rotated template floor footprint (below dy = 1)
        BlockState foundationState = ModBlocks.GNEISS_BRICKS.get().defaultBlockState();
        for (int dx = 0; dx < rawSX; dx++) {
            for (int dz = 0; dz < rawSZ; dz++) {
                BlockPos relPos = StructureTemplate.calculateRelativePosition(settings, new BlockPos(dx, 0, dz));
                BlockPos floorPos = originPos.offset(relPos);
                for (int dy = 1; dy <= 4; dy++) {
                    BlockPos subPos = floorPos.below(dy);
                    BlockState current = level.getBlockState(subPos);
                    if (current.isAir() || !current.isSolid() || current.canBeReplaced()) {
                        level.setBlock(subPos, foundationState, 2);
                    } else {
                        break;
                    }
                }
            }
        }

        boolean placed = finalTemplate.placeInWorld(level, originPos, originPos, settings, random, 2);

        if (placed) {
            // Post-process container blocks & spawners inside placed structure footprint
            for (int dx = 0; dx < rawSX; dx++) {
                for (int dz = 0; dz < rawSZ; dz++) {
                    for (int dy = 0; dy < finalSize.getY(); dy++) {
                        BlockPos relPos = StructureTemplate.calculateRelativePosition(settings, new BlockPos(dx, dy, dz));
                        BlockPos wPos = originPos.offset(relPos);
                        BlockState st = level.getBlockState(wPos);

                        // 1. Chest, Trapped Chest, Barrel -> Assign random Loot Table
                        if (st.is(Blocks.CHEST) || st.is(Blocks.TRAPPED_CHEST) || st.is(Blocks.BARREL)) {
                            if (level.getBlockEntity(wPos) instanceof RandomizableContainerBlockEntity containerBE) {
                                ResourceKey<LootTable> lootKey = DUNGEON_LOOT_TABLES[random.nextInt(DUNGEON_LOOT_TABLES.length)];
                                containerBE.setLootTable(lootKey, random.nextLong());
                            }
                        } 
                        // 2. Decorated Pot -> Assign random Loot Table
                        else if (st.is(Blocks.DECORATED_POT)) {
                            if (level.getBlockEntity(wPos) instanceof DecoratedPotBlockEntity potBE) {
                                ResourceKey<LootTable> lootKey = DUNGEON_LOOT_TABLES[random.nextInt(DUNGEON_LOOT_TABLES.length)];
                                potBE.setLootTable(lootKey, random.nextLong());
                            }
                        }
                        // 3. Monster Spawner -> Randomly assign Climbing Zombie, Cave Spider, Stray, or Bogged
                        else if (st.is(Blocks.SPAWNER)) {
                            if (level.getBlockEntity(wPos) instanceof SpawnerBlockEntity spawnerBE) {
                                EntityType<?> chosenMob = pickRandomSpawnerEntity(random);
                                spawnerBE.setEntityId(chosenMob, random);
                            }
                        }
                    }
                }
            }
        }

        return placed;
    }

    /** Returns a shuffled copy of [0..n-1] using the given random source. */
    private static int[] shuffledIndices(RandomSource random, int n) {
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) arr[i] = i;
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
        }
        return arr;
    }

    /** Returns all 4 Rotation values in a random order. */
    private static Rotation[] shuffledRotations(RandomSource random) {
        Rotation[] rots = Rotation.values().clone();
        for (int i = rots.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Rotation tmp = rots[i]; rots[i] = rots[j]; rots[j] = tmp;
        }
        return rots;
    }
}
