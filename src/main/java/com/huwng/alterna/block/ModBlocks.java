package com.huwng.alterna.block;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.item.ModItems;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

public class ModBlocks {
        public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Alterna.MODID);

        public static final DeferredBlock<RotatedPillarBlock> GOBLET_LOG = registerBlock("goblet_log",
                        RotatedPillarBlock::new,
                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).mapColor(MapColor.COLOR_PURPLE));

        public static final DeferredBlock<RotatedPillarBlock> GOBLET_STRIPPED_LOG = registerBlock("goblet_stripped_log",
                        RotatedPillarBlock::new,
                        BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG).mapColor(MapColor.COLOR_PURPLE));

        public static final DeferredBlock<RotatedPillarBlock> GOBLET_WOOD = registerBlock("goblet_wood",
                        RotatedPillarBlock::new,
                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD).mapColor(MapColor.COLOR_PURPLE));

        public static final DeferredBlock<RotatedPillarBlock> GOBLET_STRIPPED_WOOD = registerBlock(
                        "goblet_stripped_wood",
                        RotatedPillarBlock::new,
                        BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD).mapColor(MapColor.COLOR_PURPLE));

        public static final DeferredBlock<Block> GOBLET_PLANKS = registerBlock("goblet_planks",
                        Block::new,
                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(MapColor.COLOR_PURPLE));

        public static final DeferredBlock<StairBlock> GOBLET_STAIRS = registerBlock("goblet_stairs",
                        p -> new StairBlock(GOBLET_PLANKS.get().defaultBlockState(), p),
                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS).mapColor(MapColor.COLOR_PURPLE));

        public static final DeferredBlock<SlabBlock> GOBLET_SLAB = registerBlock("goblet_slab",
                        SlabBlock::new,
                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB).mapColor(MapColor.COLOR_PURPLE));

        public static final DeferredBlock<FenceBlock> GOBLET_FENCE = registerBlock("goblet_fence",
                        FenceBlock::new,
                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE).mapColor(MapColor.COLOR_PURPLE));

        public static final DeferredBlock<FenceGateBlock> GOBLET_FENCE_GATE = registerBlock("goblet_fence_gate",
                        p -> new FenceGateBlock(WoodType.OAK, p),
                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE).mapColor(MapColor.COLOR_PURPLE));

        public static final DeferredBlock<DoorBlock> GOBLET_DOOR = registerBlock("goblet_door",
                        p -> new DoorBlock(BlockSetType.OAK, p),
                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR).mapColor(MapColor.COLOR_PURPLE));

        public static final DeferredBlock<TrapDoorBlock> GOBLET_TRAPDOOR = registerBlock("goblet_trapdoor",
                        p -> new TrapDoorBlock(BlockSetType.OAK, p),
                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR).mapColor(MapColor.COLOR_PURPLE));

        public static final DeferredBlock<ButtonBlock> GOBLET_BUTTON = registerBlock("goblet_button",
                        p -> new ButtonBlock(BlockSetType.OAK, 30, p),
                        BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON).mapColor(MapColor.COLOR_PURPLE));

        public static final DeferredBlock<PressurePlateBlock> GOBLET_PRESSURE_PLATE = registerBlock(
                        "goblet_pressure_plate",
                        p -> new PressurePlateBlock(BlockSetType.OAK, p), BlockBehaviour.Properties
                                        .ofFullCopy(Blocks.OAK_PRESSURE_PLATE).mapColor(MapColor.COLOR_PURPLE));

        public static final DeferredBlock<LadderBlock> GOBLET_LADDER = registerBlock("goblet_ladder",
                        LadderBlock::new,
                        BlockBehaviour.Properties.ofFullCopy(Blocks.LADDER).mapColor(MapColor.COLOR_PURPLE));

        public static final DeferredBlock<GobletMembraneBlock> GOBLET_MEMBRANE = registerBlock("goblet_membrane",
                        GobletMembraneBlock::new,
                        BlockBehaviour.Properties.ofFullCopy(Blocks.HONEY_BLOCK).mapColor(MapColor.COLOR_PURPLE));

        // ---- MARBLE SET (Tuff sound, Hardness 2.0, Blast Resistance 4.0) ----
        public static final DeferredBlock<Block> MARBLE = registerBlock("marble",
                        Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.TUFF).mapColor(MapColor.QUARTZ)
                                        .strength(2.0f, 4.0f));
        public static final DeferredBlock<StairBlock> MARBLE_STAIRS = registerBlock("marble_stairs",
                        p -> new StairBlock(MARBLE.get().defaultBlockState(), p), BlockBehaviour.Properties
                                        .ofFullCopy(Blocks.TUFF_STAIRS).mapColor(MapColor.QUARTZ).strength(2.0f, 4.0f));
        public static final DeferredBlock<SlabBlock> MARBLE_SLAB = registerBlock("marble_slab",
                        SlabBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.TUFF_SLAB).mapColor(MapColor.QUARTZ)
                                        .strength(2.0f, 4.0f));
        public static final DeferredBlock<WallBlock> MARBLE_WALL = registerBlock("marble_wall",
                        WallBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.TUFF_WALL).mapColor(MapColor.QUARTZ)
                                        .strength(2.0f, 4.0f));

        public static final DeferredBlock<Block> POLISHED_MARBLE = registerBlock("polished_marble",
                        Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.TUFF).mapColor(MapColor.QUARTZ)
                                        .strength(2.0f, 4.0f));
        public static final DeferredBlock<StairBlock> POLISHED_MARBLE_STAIRS = registerBlock("polished_marble_stairs",
                        p -> new StairBlock(POLISHED_MARBLE.get().defaultBlockState(), p), BlockBehaviour.Properties
                                        .ofFullCopy(Blocks.TUFF_STAIRS).mapColor(MapColor.QUARTZ).strength(2.0f, 4.0f));
        public static final DeferredBlock<SlabBlock> POLISHED_MARBLE_SLAB = registerBlock("polished_marble_slab",
                        SlabBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.TUFF_SLAB).mapColor(MapColor.QUARTZ)
                                        .strength(2.0f, 4.0f));
        public static final DeferredBlock<WallBlock> POLISHED_MARBLE_WALL = registerBlock("polished_marble_wall",
                        WallBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.TUFF_WALL).mapColor(MapColor.QUARTZ)
                                        .strength(2.0f, 4.0f));

        public static final DeferredBlock<Block> MARBLE_BRICKS = registerBlock("marble_bricks",
                        Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.TUFF).mapColor(MapColor.QUARTZ)
                                        .strength(2.0f, 4.0f));
        public static final DeferredBlock<StairBlock> MARBLE_BRICK_STAIRS = registerBlock("marble_brick_stairs",
                        p -> new StairBlock(MARBLE_BRICKS.get().defaultBlockState(), p), BlockBehaviour.Properties
                                        .ofFullCopy(Blocks.TUFF_STAIRS).mapColor(MapColor.QUARTZ).strength(2.0f, 4.0f));
        public static final DeferredBlock<SlabBlock> MARBLE_BRICK_SLAB = registerBlock("marble_brick_slab",
                        SlabBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.TUFF_SLAB).mapColor(MapColor.QUARTZ)
                                        .strength(2.0f, 4.0f));
        public static final DeferredBlock<WallBlock> MARBLE_BRICK_WALL = registerBlock("marble_brick_wall",
                        WallBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.TUFF_WALL).mapColor(MapColor.QUARTZ)
                                        .strength(2.0f, 4.0f));

        // ---- GNEISS SET (Hardness 5.0, Blast Resistance 9.0) ----
        public static final DeferredBlock<Block> GNEISS = registerBlock("gneiss",
                        Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.COLOR_GRAY)
                                        .strength(5.0f, 9.0f));
        public static final DeferredBlock<StairBlock> GNEISS_STAIRS = registerBlock("gneiss_stairs",
                        p -> new StairBlock(GNEISS.get().defaultBlockState(), p),
                        BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_STAIRS).mapColor(MapColor.COLOR_GRAY)
                                        .strength(5.0f, 9.0f));
        public static final DeferredBlock<SlabBlock> GNEISS_SLAB = registerBlock("gneiss_slab",
                        SlabBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_SLAB)
                                        .mapColor(MapColor.COLOR_GRAY).strength(5.0f, 9.0f));
        public static final DeferredBlock<WallBlock> GNEISS_WALL = registerBlock("gneiss_wall",
                        WallBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_WALL)
                                        .mapColor(MapColor.COLOR_GRAY).strength(5.0f, 9.0f));

        public static final DeferredBlock<Block> POLISHED_GNEISS = registerBlock("polished_gneiss",
                        Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.COLOR_GRAY)
                                        .strength(5.0f, 9.0f));
        public static final DeferredBlock<StairBlock> POLISHED_GNEISS_STAIRS = registerBlock("polished_gneiss_stairs",
                        p -> new StairBlock(POLISHED_GNEISS.get().defaultBlockState(), p),
                        BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_STAIRS).mapColor(MapColor.COLOR_GRAY)
                                        .strength(5.0f, 9.0f));
        public static final DeferredBlock<SlabBlock> POLISHED_GNEISS_SLAB = registerBlock("polished_gneiss_slab",
                        SlabBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_SLAB)
                                        .mapColor(MapColor.COLOR_GRAY).strength(5.0f, 9.0f));
        public static final DeferredBlock<WallBlock> POLISHED_GNEISS_WALL = registerBlock("polished_gneiss_wall",
                        WallBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_WALL)
                                        .mapColor(MapColor.COLOR_GRAY).strength(5.0f, 9.0f));

        public static final DeferredBlock<Block> GNEISS_BRICKS = registerBlock("gneiss_bricks",
                        Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.COLOR_GRAY)
                                        .strength(5.0f, 9.0f));
        public static final DeferredBlock<StairBlock> GNEISS_BRICK_STAIRS = registerBlock("gneiss_brick_stairs",
                        p -> new StairBlock(GNEISS_BRICKS.get().defaultBlockState(), p),
                        BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_STAIRS).mapColor(MapColor.COLOR_GRAY)
                                        .strength(5.0f, 9.0f));
        public static final DeferredBlock<SlabBlock> GNEISS_BRICK_SLAB = registerBlock("gneiss_brick_slab",
                        SlabBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_SLAB)
                                        .mapColor(MapColor.COLOR_GRAY).strength(5.0f, 9.0f));
        public static final DeferredBlock<WallBlock> GNEISS_BRICK_WALL = registerBlock("gneiss_brick_wall",
                        WallBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICK_WALL)
                                        .mapColor(MapColor.COLOR_GRAY).strength(5.0f, 9.0f));

        // ---- SERPENTINITE SET (Deepslate sound, Hardness 4.0, Blast Resistance 6.0)
        // ----
        public static final DeferredBlock<Block> SERPENTINITE = registerBlock("serpentinite",
                        Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
                                        .mapColor(MapColor.COLOR_LIGHT_GREEN).strength(4.0f, 6.0f));
        public static final DeferredBlock<StairBlock> SERPENTINITE_STAIRS = registerBlock("serpentinite_stairs",
                        p -> new StairBlock(SERPENTINITE.get().defaultBlockState(), p),
                        BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).mapColor(MapColor.COLOR_LIGHT_GREEN)
                                        .strength(4.0f, 6.0f));
        public static final DeferredBlock<SlabBlock> SERPENTINITE_SLAB = registerBlock("serpentinite_slab",
                        SlabBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
                                        .mapColor(MapColor.COLOR_LIGHT_GREEN).strength(4.0f, 6.0f));
        public static final DeferredBlock<WallBlock> SERPENTINITE_WALL = registerBlock("serpentinite_wall",
                        WallBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
                                        .mapColor(MapColor.COLOR_LIGHT_GREEN).strength(4.0f, 6.0f));

        public static final DeferredBlock<Block> POLISHED_SERPENTINITE = registerBlock("polished_serpentinite",
                        Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
                                        .mapColor(MapColor.COLOR_LIGHT_GREEN).strength(4.0f, 6.0f));
        public static final DeferredBlock<StairBlock> POLISHED_SERPENTINITE_STAIRS = registerBlock(
                        "polished_serpentinite_stairs",
                        p -> new StairBlock(POLISHED_SERPENTINITE.get().defaultBlockState(), p),
                        BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).mapColor(MapColor.COLOR_LIGHT_GREEN)
                                        .strength(4.0f, 6.0f));
        public static final DeferredBlock<SlabBlock> POLISHED_SERPENTINITE_SLAB = registerBlock(
                        "polished_serpentinite_slab",
                        SlabBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
                                        .mapColor(MapColor.COLOR_LIGHT_GREEN).strength(4.0f, 6.0f));
        public static final DeferredBlock<WallBlock> POLISHED_SERPENTINITE_WALL = registerBlock(
                        "polished_serpentinite_wall",
                        WallBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
                                        .mapColor(MapColor.COLOR_LIGHT_GREEN).strength(4.0f, 6.0f));

        public static final DeferredBlock<Block> SERPENTINITE_BRICKS = registerBlock("serpentinite_bricks",
                        Block::new, BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
                                        .mapColor(MapColor.COLOR_LIGHT_GREEN).strength(4.0f, 6.0f));
        public static final DeferredBlock<StairBlock> SERPENTINITE_BRICK_STAIRS = registerBlock(
                        "serpentinite_brick_stairs",
                        p -> new StairBlock(SERPENTINITE_BRICKS.get().defaultBlockState(), p),
                        BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).mapColor(MapColor.COLOR_LIGHT_GREEN)
                                        .strength(4.0f, 6.0f));
        public static final DeferredBlock<SlabBlock> SERPENTINITE_BRICK_SLAB = registerBlock("serpentinite_brick_slab",
                        SlabBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
                                        .mapColor(MapColor.COLOR_LIGHT_GREEN).strength(4.0f, 6.0f));
        public static final DeferredBlock<WallBlock> SERPENTINITE_BRICK_WALL = registerBlock("serpentinite_brick_wall",
                        WallBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)
                                        .mapColor(MapColor.COLOR_LIGHT_GREEN).strength(4.0f, 6.0f));

        // ---- WILD MOSS SET ----
        public static final DeferredBlock<WildMossBlock> WILD_MOSS_BLOCK = registerBlock("wild_moss_block",
                        WildMossBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_MOSS_BLOCK));

        public static final DeferredBlock<WildMossCarpetBlock> WILD_MOSS_CARPET = registerBlock("wild_moss_carpet",
                        WildMossCarpetBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_MOSS_CARPET));

        public static final DeferredBlock<HangingMossBlock> WILD_HANGING_MOSS = registerBlock("wild_hanging_moss",
                        HangingMossBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_HANGING_MOSS));

        // ---- CLOUDBERRY VINES SET ----
        public static final DeferredBlock<CloudberryVinesBlock> CLOUDBERRY_VINES = registerBlockOnly("cloudberry_vines",
                        CloudberryVinesBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CAVE_VINES));

        public static final DeferredBlock<CloudberryVinesPlantBlock> CLOUDBERRY_VINES_PLANT = registerBlockOnly(
                        "cloudberry_vines_plant",
                        CloudberryVinesPlantBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CAVE_VINES_PLANT));

        // ---- WHITE CURRANT BERRIES SET ----
        public static final DeferredBlock<WhiteCurrantBushBlock> WHITE_CURRANT_BERRY_BUSH = registerBlockOnly(
                        "white_currant_berry_bush",
                        WhiteCurrantBushBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH));

        // ---- STAR LILY SET ----
        public static final DeferredBlock<StarLilyBlock> STAR_LILY = registerBlock("star_lily",
                        StarLilyBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.PINK_PETALS));

        public static final DeferredBlock<StarLilyVineBlock> STAR_LILY_VINE = registerBlock("star_lily_vine",
                        StarLilyVineBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.VINE));

        public static final DeferredBlock<HangingStarLilyBlock> HANGING_STAR_LILY = registerBlockOnly("hanging_star_lily",
                        HangingStarLilyBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_HANGING_MOSS));

        public static final DeferredBlock<HangingStarLilyPlantBlock> HANGING_STAR_LILY_PLANT = registerBlockOnly("hanging_star_lily_plant",
                        HangingStarLilyPlantBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_HANGING_MOSS));

        // ---- DAZE FLOWERS ----
        public static final DeferredBlock<ShortDazeBlock> SHORT_DAZE = registerBlock("short_daze",
                        ShortDazeBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS));

        public static final DeferredBlock<TallDazeBlock> TALL_DAZE = registerBlock("tall_daze",
                        TallDazeBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.ROSE_BUSH));

        public static final DeferredBlock<PurpleSugarCaneBlock> PURPLE_SUGAR_CANE = registerBlockOnly("purple_sugar_cane",
                        PurpleSugarCaneBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.SUGAR_CANE));

        // ---- GRAVITY CORE BLOCK ----
        public static final DeferredBlock<GravityCoreBlock> GRAVITY_CORE_BLOCK = registerBlock("gravity_core_block",
                        GravityCoreBlock::new, BlockBehaviour.Properties.of()
                                        .mapColor(MapColor.COLOR_PURPLE)
                                        .strength(2.0f)
                                        .requiresCorrectToolForDrops()
                                        .noOcclusion());


        private static <B extends Block> DeferredBlock<B> registerBlockOnly(String name,
                        Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties) {
                return BLOCKS.registerBlock(name, factory, () -> properties);
        }

        private static <B extends Block> DeferredBlock<B> registerBlock(String name,
                        Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties) {
                DeferredBlock<B> toReturn = BLOCKS.registerBlock(name, factory, () -> properties);
                registerBlockItem(name, toReturn);
                return toReturn;
        }

        private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
                ModItems.ITEMS.registerSimpleBlockItem(name, block);
        }

        public static void register(IEventBus eventBus) {
                BLOCKS.register(eventBus);
        }
}
