package com.huwng.alterna.worldgen;

import com.huwng.alterna.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Level-side glue for {@link WeavecapGeometry}: block choices, saplings, clipped worldgen placement. */
public final class WeavecapTree {
    private WeavecapTree() {}

    private static boolean replaceable(BlockState s) {
        return s.isAir() || s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(ModBlocks.LICHEN_GRASS.get())
                || s.is(ModBlocks.TALL_LICHEN_GRASS.get()) || s.is(ModBlocks.EMBER_CAP.get()) || s.is(ModBlocks.VIOLET_BUSH.get())
                || s.is(ModBlocks.WEAVECAP_SPROUT.get());
    }

    private static boolean soil(BlockState s) {
        return s.is(ModBlocks.LICHEN_TURF.get()) || s.is(Blocks.DIRT) || s.is(Blocks.COARSE_DIRT)
                || s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.MUD);
    }

    /** Sink writing into a real level. */
    static WeavecapGeometry.Sink levelSink(LevelAccessor level) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        return new WeavecapGeometry.Sink() {
            @Override
            public int status(int x, int y, int z) {
                BlockState s = level.getBlockState(m.set(x, y, z));
                if (s.is(ModBlocks.WEAVECAP_LOG.get()) || s.is(ModBlocks.WEAVECAP_WOOD.get())) return 3;
                if (s.is(ModBlocks.WEAVECAP_CAP.get()) || s.is(ModBlocks.WEAVECAP_GILLS.get())) return 4;
                if (replaceable(s)) return 1;
                if (soil(s)) return 2;
                return 0;
            }

            @Override
            public void put(int x, int y, int z, WeavecapGeometry.Kind kind) {
                BlockState st = switch (kind) {
                    case LOG_X -> ModBlocks.WEAVECAP_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
                    case LOG_Y -> ModBlocks.WEAVECAP_LOG.get().defaultBlockState();
                    case LOG_Z -> ModBlocks.WEAVECAP_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
                    case WOOD -> ModBlocks.WEAVECAP_WOOD.get().defaultBlockState();
                    case CAP -> ModBlocks.WEAVECAP_CAP.get().defaultBlockState();
                    case GILLS -> ModBlocks.WEAVECAP_GILLS.get().defaultBlockState();
                    case MOSS -> ModBlocks.LICHEN_TURF.get().defaultBlockState();
                };
                level.setBlock(m.set(x, y, z), st, 2);
            }
        };
    }

    public static boolean hasRoomForSapling(ServerLevel level, BlockPos pos) {
        for (int y = 1; y <= 9; y++) {
            if (!level.getBlockState(pos.above(y)).isAir()) return false;
        }
        return true;
    }

    public static boolean growFromSapling(ServerLevel level, BlockPos pos, RandomSource random) {
        long seed = random.nextLong();
        WeavecapGeometry tree = WeavecapGeometry.build(seed, pos, WeavecapGeometry.Size.SMALL);
        tree.place(levelSink(level), null);
        drapeVines(level, tree, tree.bounds(), seed);
        return true;
    }

    /** Worldgen entry: place the slice of the tree inside the chunk currently generating. */
    public static void placeClipped(WorldGenLevel level, long seed, BlockPos base, WeavecapGeometry.Size size, BoundingBox chunkClip) {
        WeavecapGeometry tree = WeavecapGeometry.build(seed, base, size);
        tree.place(levelSink(level), chunkClip);
        BoundingBox b = tree.bounds();
        if (!b.intersects(chunkClip)) return;
        drapeVines(level, tree, new BoundingBox(Math.max(b.minX(), chunkClip.minX()), b.minY(), Math.max(b.minZ(), chunkClip.minZ()),
                Math.min(b.maxX(), chunkClip.maxX()), b.maxY(), Math.min(b.maxZ(), chunkClip.maxZ())), seed);
    }

    private static boolean isTreeWood(BlockState s) {
        return s.is(ModBlocks.WEAVECAP_LOG.get()) || s.is(ModBlocks.WEAVECAP_WOOD.get());
    }

    /**
     * Hangs vines all over the woven root strands (below the cap). Vines take the
     * biome's foliage tint, so in the savanna they come out dusky red. Everything
     * placed stays inside {@code box}, so worldgen never writes outside its chunk.
     */
    private static void drapeVines(LevelAccessor level, WeavecapGeometry tree, BoundingBox box, long seed) {
        int top = tree.rootTopY();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos v = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
            for (int y = box.minY(); y <= Math.min(top, box.maxY()); y++) {
                if (!isTreeWood(level.getBlockState(p.set(x, y, z)))) continue;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    int vx = x + d.getStepX(), vz = z + d.getStepZ();
                    if (vx < box.minX() || vx > box.maxX() || vz < box.minZ() || vz > box.maxZ()) continue;
                    long h = (vx * 3129871L) ^ (y * 116129781L) ^ (vz * 98765431L) ^ seed ^ d.ordinal() * 0x9E3779B9L;
                    h = h * 0x5DEECE66DL + 0xBL;
                    if (Math.floorMod(h >>> 16, 100) >= 45) continue;
                    if (!level.getBlockState(v.set(vx, y, vz)).isAir()) continue;
                    BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(d.getOpposite()), true);
                    int len = 1 + (int) Math.floorMod(h >>> 24, 5);
                    for (int i = 0; i < len; i++) {
                        v.set(vx, y - i, vz);
                        if (!level.getBlockState(v).isAir()) break;
                        // lower segments hang from the one above (same face, like vanilla hanging vines)
                        level.setBlock(v, vine, 2);
                    }
                }
            }
        }
    }
}
