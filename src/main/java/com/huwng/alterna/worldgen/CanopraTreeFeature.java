package com.huwng.alterna.worldgen;

import com.huwng.alterna.block.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CanopraTreeFeature extends Feature<NoneFeatureConfiguration> {

    public static final Identifier[] CANOPRA_TREE_STRUCTURES = new Identifier[]{
            Identifier.fromNamespaceAndPath("alterna", "canopra_tree_1"),
            Identifier.fromNamespaceAndPath("alterna", "canopra_tree_2"),
            Identifier.fromNamespaceAndPath("alterna", "canopra_tree_3"),
            Identifier.fromNamespaceAndPath("alterna", "canopra_tree_4")
    };

    public static final Identifier CANOPRA_SUPPORT = Identifier.fromNamespaceAndPath("alterna", "canopra_support");

    // Shroomlight center marker located at (14, 0, 13) across all 4 tree templates
    public static final BlockPos SHROOMLIGHT_PIVOT = new BlockPos(14, 0, 13);
    // Shroomlight center marker in canopra_support located at (5, 10, 5)
    public static final BlockPos SUPPORT_SHROOMLIGHT_PIVOT = new BlockPos(5, 10, 5);

    private static final ConcurrentHashMap<Identifier, StructureTemplate> TEMPLATE_FALLBACK_CACHE = new ConcurrentHashMap<>();

    public CanopraTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return placeDirect(context.level(), context.random(), context.origin(), true);
    }

    public static boolean placeDirect(WorldGenLevel level, RandomSource random, BlockPos origin) {
        return placeDirect(level, random, origin, true);
    }

    public static boolean placeDirect(WorldGenLevel level, RandomSource random, BlockPos origin, boolean placeSupport) {
        // Randomly pick one of the 4 tree templates
        Identifier chosen = CANOPRA_TREE_STRUCTURES[random.nextInt(CANOPRA_TREE_STRUCTURES.length)];
        StructureTemplate treeTemplate = getOrLoadTemplate(level, chosen);
        if (treeTemplate == null) return false;

        // Random 4-way rotation and optional mirror for natural variation
        Rotation rotation = Rotation.getRandom(random);
        Mirror mirror = (random.nextInt(2) == 0) ? Mirror.FRONT_BACK : Mirror.NONE;

        // 1. Place canopra_support only when natural worldgen generates the tree
        if (placeSupport) {
            StructureTemplate supportTemplate = getOrLoadTemplate(level, CANOPRA_SUPPORT);
            if (supportTemplate != null) {
                StructurePlaceSettings supportSettings = new StructurePlaceSettings()
                        .setRotation(rotation)
                        .setMirror(mirror)
                        .setIgnoreEntities(true)
                        .setKnownShape(true)
                        .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);

                BlockPos rotatedSupportOffset = StructureTemplate.calculateRelativePosition(supportSettings, SUPPORT_SHROOMLIGHT_PIVOT);
                BlockPos supportOrigin = origin.subtract(rotatedSupportOffset);
                supportTemplate.placeInWorld(level, supportOrigin, supportOrigin, supportSettings, random, 2);
            }
        }

        // 2. Place the Canopra tree
        StructurePlaceSettings treeSettings = new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(mirror)
                .setIgnoreEntities(true)
                .setKnownShape(true)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);

        // Align the shroomlight pivot (14, 0, 13) exactly to target origin
        BlockPos rotatedPivotOffset = StructureTemplate.calculateRelativePosition(treeSettings, SHROOMLIGHT_PIVOT);
        BlockPos placeOrigin = origin.subtract(rotatedPivotOffset);

        // Place the structure into the world
        boolean placed = treeTemplate.placeInWorld(level, placeOrigin, placeOrigin, treeSettings, random, 2);

        // 3. Replace any shroomlight marker at origin or in the root cluster with solid Canopra Wood
        if (level.getBlockState(origin).is(Blocks.SHROOMLIGHT)) {
            level.setBlock(origin, ModBlocks.CANOPRA_WOOD.get().defaultBlockState(), 2);
        }

        int searchMinY = placeSupport ? -10 : -1;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = searchMinY; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (level.getBlockState(p).is(Blocks.SHROOMLIGHT)) {
                        level.setBlock(p, ModBlocks.CANOPRA_WOOD.get().defaultBlockState(), 2);
                    }
                }
            }
        }

        return placed;
    }

    private static StructureTemplate getOrLoadTemplate(WorldGenLevel level, Identifier id) {
        if (level.getLevel() != null && level.getLevel().getServer() != null) {
            StructureTemplateManager manager = level.getLevel().getServer().getStructureManager();
            Optional<StructureTemplate> opt = manager.get(id);
            if (opt.isPresent()) {
                return opt.get();
            }
        }

        // Fallback: load directly from resource stream
        return TEMPLATE_FALLBACK_CACHE.computeIfAbsent(id, key -> {
            String[] paths = new String[]{
                    "/data/alterna/structure/" + key.getPath() + ".nbt",
                    "/data/alterna/structures/" + key.getPath() + ".nbt",
                    "data/alterna/structure/" + key.getPath() + ".nbt"
            };

            for (String resPath : paths) {
                try (InputStream is = CanopraTreeFeature.class.getResourceAsStream(resPath)) {
                    if (is != null) {
                        CompoundTag tag = NbtIo.readCompressed(is, NbtAccounter.unlimitedHeap());
                        StructureTemplate t = new StructureTemplate();
                        t.load(BuiltInRegistries.BLOCK, tag);
                        return t;
                    }
                } catch (Exception ignored) {
                }
            }
            return null;
        });
    }
}
