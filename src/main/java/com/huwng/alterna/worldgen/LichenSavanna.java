package com.huwng.alterna.worldgen;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.block.ModBlocks;
import com.huwng.alterna.block.PurpleSugarCaneBlock;
import com.huwng.alterna.block.TetraNestBlock;
import com.huwng.alterna.entity.CragbackEntity;
import com.huwng.alterna.entity.ModEntities;
import com.huwng.alterna.entity.TetraEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Lichen Savanna - the purple grassland plateau deep inside the giant crack.
 *
 * Geometry (the plateau itself, its ponds) is carved per block by GiantCrackParams via
 * {@link #placeLedgeBlock}; the big things - Weavecap trees, violet trees, boulders,
 * the herds of Cragbacks, Tetra nests and flocks, and the biome itself - are placed per
 * chunk in the later decoration pass via {@link #decorateChunk}.
 */
public final class LichenSavanna {
    public static final ResourceKey<Biome> BIOME = ResourceKey.create(Registries.BIOME, Alterna.id("lichen_savanna"));
    private static final long POND_SALT = 0x5A7A11A1L;

    private LichenSavanna() {}

    /**
     * Read-only view into the crack geometry at a world position, provided by
     * GiantCrackParams (which owns the private Segment data).
     */
    public interface Probe {
        /** @return the giant-ledge info at (x, z) on the savanna top, or null when (x, z) is not open savanna ground. */
        Spot at(int x, int z);
    }

    /** Where on the plateau a column is: 0 = at the wall, 1 = at the outer rim; same along its length. */
    public record Spot(double edgeDistNorm, double lengthNorm, double distFromWall) {
        boolean interior(double maxEdge, double maxLength, double minWall) {
            return this.edgeDistNorm <= maxEdge && this.lengthNorm <= maxLength && this.distFromWall >= minWall;
        }
    }

    // ------------------------------------------------------------------ noise
    // GiantCrackParams.smoothNoise3D hashes its last coordinate very weakly, so sampled
    // as (x, 0, z) it barely changes along Z and thresholds come out as long straight
    // stripes. The savanna uses its own properly mixed 2D value noise instead.
    private static long mix64(long z) {
        z += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static double lattice(int x, int z, long seed) {
        long h = mix64(seed ^ (x * 0x632BE59BD9B4E019L));
        h = mix64(h ^ (z * 0x85157AF5L));
        return ((h >>> 40) & 0xFFFFFFL) / (double) 0xFFFFFFL * 2.0 - 1.0;
    }

    private static double smooth(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    static double valueNoise(double x, double z, long seed, double scale) {
        double gx = x / scale, gz = z / scale;
        int x0 = Mth.floor(gx), z0 = Mth.floor(gz);
        double sx = smooth(gx - x0), sz = smooth(gz - z0);
        double a = Mth.lerp(sx, lattice(x0, z0, seed), lattice(x0 + 1, z0, seed));
        double b = Mth.lerp(sx, lattice(x0, z0 + 1, seed), lattice(x0 + 1, z0 + 1, seed));
        return Mth.lerp(sz, a, b);
    }

    /** Domain-warped two-octave noise in roughly [-0.9, 0.9]: blobby, organic shapes. */
    static double organic(double x, double z, long seed, double scale) {
        double wx = valueNoise(x, z, seed ^ 0x51A7L, scale * 2.0) * scale * 0.8;
        double wz = valueNoise(x, z, seed ^ 0x2B9CL, scale * 2.0) * scale * 0.8;
        return valueNoise(x + wx, z + wz, seed, scale) * 0.65 + valueNoise(x - wz, z + wx, seed ^ 0x77L, scale * 0.5) * 0.35;
    }

    // ------------------------------------------------------------------ ponds
    /** 0, 1 or 2 blocks of water at this column. Only ever inside the flat interior so water can't leak. */
    public static int pondDepth(long seed, int x, int z, Spot spot) {
        if (spot == null || !spot.interior(0.62, 0.78, 7.0)) return 0;
        double n = organic(x, z, seed ^ POND_SALT, 13.0);
        if (n > 0.60) return 2;
        if (n > 0.44) return 1;
        return 0;
    }

    /**
     * Places one block of the savanna plateau. Returns true when it handled the block.
     *
     * @param topY the plateau's flat top Y
     */
    public static void placeLedgeBlock(WorldGenLevel level, BlockPos pos, int topY, boolean isTop, boolean isBottom,
            boolean isOuterEdge, Spot spot, int pond, boolean nearPond, long seed, BlockState wallRock) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        RandomSource random = RandomSource.create(x * 3129871L ^ y * 116129781L ^ z * 98765431L ^ seed);
        // ---- ponds
        if (pond > 0 && y > topY - pond) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
            if (y == topY && random.nextFloat() < 0.07f && level.getBlockState(pos.above()).isAir()) {
                level.setBlock(pos.above(), Blocks.LILY_PAD.defaultBlockState(), 2);
            } else if (y == topY - pond + 1 && random.nextFloat() < 0.25f) {
                level.setBlock(pos, Blocks.SEAGRASS.defaultBlockState(), 2);
            }
            return;
        }
        if (pond > 0 && y == topY - pond) {
            float r = random.nextFloat();
            level.setBlock(pos, r < 0.5f ? Blocks.MUD.defaultBlockState() : r < 0.8f ? Blocks.CLAY.defaultBlockState() : Blocks.SAND.defaultBlockState(), 2);
            return;
        }
        if (!isTop) {
            if (isBottom && random.nextFloat() < 0.10f && level.getBlockState(pos.below()).isAir()) {
                level.setBlock(pos.below(), Blocks.HANGING_ROOTS.defaultBlockState(), 2);
            }
            level.setBlock(pos, y > topY - 3 ? Blocks.DIRT.defaultBlockState()
                    : random.nextFloat() < 0.12f ? ModBlocks.LICHEN_STONE.get().defaultBlockState() : wallRock, 2);
            return;
        }
        // ---- surface
        // bare, trampled patches - irregular blobs, never lines (see organic())
        double patch = organic(x, z, seed ^ 0xC0A25EL, 8.0);
        BlockState ground;
        if (nearPond) ground = random.nextFloat() < 0.7f ? Blocks.MUD.defaultBlockState() : ModBlocks.LICHEN_TURF.get().defaultBlockState();
        else if (patch > 0.46) ground = Blocks.COARSE_DIRT.defaultBlockState();
        else if (patch > 0.40) ground = random.nextFloat() < 0.5f ? Blocks.COARSE_DIRT.defaultBlockState() : ModBlocks.LICHEN_TURF.get().defaultBlockState();
        else if (patch < -0.52) ground = Blocks.PODZOL.defaultBlockState();
        else ground = ModBlocks.LICHEN_TURF.get().defaultBlockState();
        if (isOuterEdge && random.nextFloat() < 0.3f) ground = ModBlocks.LICHEN_STONE.get().defaultBlockState();
        level.setBlock(pos, ground, 2);

        BlockPos up = pos.above();
        if (!level.getBlockState(up).isAir()) return;
        if (nearPond) {
            if (random.nextFloat() < 0.3f) {
                int h = 1 + random.nextInt(3);
                for (int i = 0; i < h; i++) {
                    if (!level.getBlockState(up.above(i)).isAir()) break;
                    level.setBlock(up.above(i), ModBlocks.PURPLE_SUGAR_CANE.get().defaultBlockState()
                            .setValue(PurpleSugarCaneBlock.TOP, i == h - 1), 2);
                }
            }
            return;
        }
        // berry thickets: sweet berries and white currants grow in scattered clumps
        double thicket = organic(x, z, seed ^ 0xBE77EEL, 7.0);
        if (thicket > 0.5 && random.nextFloat() < 0.32f && !ground.is(ModBlocks.LICHEN_STONE.get())) {
            boolean currant = organic(x, z, seed ^ 0xC0441L, 22.0) > 0.0;
            BlockState bush = (currant ? ModBlocks.WHITE_CURRANT_BERRY_BUSH.get().defaultBlockState() : Blocks.SWEET_BERRY_BUSH.defaultBlockState())
                    .setValue(SweetBerryBushBlock.AGE, 1 + random.nextInt(3));
            level.setBlock(up, bush, 2);
            return;
        }
        // grass density follows a noise field, so there are lush and sparse stretches
        double lush = organic(x, z, seed ^ 0x6A55L, 14.0);
        float roll = random.nextFloat();
        float grassChance = (float) (0.42 + 0.28 * lush);
        if (roll < grassChance) {
            if (random.nextFloat() < 0.22f && level.getBlockState(up.above()).isAir()) {
                DoublePlantBlock.placeAt(level, ModBlocks.TALL_LICHEN_GRASS.get().defaultBlockState(), up, 2);
            } else {
                level.setBlock(up, random.nextFloat() < 0.85f ? ModBlocks.LICHEN_GRASS.get().defaultBlockState() : Blocks.SHORT_GRASS.defaultBlockState(), 2);
            }
        } else if (roll < grassChance + 0.05f) {
            level.setBlock(up, ModBlocks.EMBER_CAP.get().defaultBlockState(), 2);
        } else if (roll < grassChance + 0.08f) {
            level.setBlock(up, ModBlocks.VIOLET_BUSH.get().defaultBlockState(), 2);
        } else if (roll < grassChance + 0.085f) {
            level.setBlock(up, ModBlocks.WEAVECAP_SPROUT.get().defaultBlockState(), 2);
        }
    }

    // ------------------------------------------------------------------ chunk decoration
    private static long cellHash(long seed, int cx, int cz, long salt) {
        long h = seed ^ salt;
        h ^= cx * 0x9E3779B97F4A7C15L;
        h = Long.rotateLeft(h, 23) ^ (cz * 0xC2B2AE3D27D4EB4FL);
        h *= 0x165667B19E3779F9L;
        return h ^ (h >>> 29);
    }

    /** One jittered candidate point per grid cell. */
    private interface CellVisitor { void visit(int x, int z, long hash); }

    private static void forEachCell(long seed, long salt, int cell, int margin, BoundingBox chunk, CellVisitor visitor) {
        int c0x = Math.floorDiv(chunk.minX() - margin, cell), c1x = Math.floorDiv(chunk.maxX() + margin, cell);
        int c0z = Math.floorDiv(chunk.minZ() - margin, cell), c1z = Math.floorDiv(chunk.maxZ() + margin, cell);
        for (int cx = c0x; cx <= c1x; cx++) for (int cz = c0z; cz <= c1z; cz++) {
            long h = cellHash(seed, cx, cz, salt);
            int x = cx * cell + 2 + (int) Math.floorMod(h, cell - 4);
            int z = cz * cell + 2 + (int) Math.floorMod(h >>> 16, cell - 4);
            visitor.visit(x, z, h >>> 32);
        }
    }

    /**
     * Places, for the chunk being decorated, the slice of every large savanna feature that
     * reaches into it. All feature positions are derived from the world seed + grid, so
     * every chunk agrees on where everything is.
     */
    public static void decorateChunk(WorldGenLevel level, ChunkPos chunkPos, int topY, int pedestal, long seed, Probe probe) {
        BoundingBox clip = new BoundingBox(chunkPos.getMinBlockX(), level.getMinY(), chunkPos.getMinBlockZ(),
                chunkPos.getMaxBlockX(), level.getMaxY(), chunkPos.getMaxBlockZ());
        int ground = topY + 1;

        // giant Weavecaps - the landmark trees
        forEachCell(seed, 0x61A47L, 56, 34, clip, (x, z, h) -> {
            if (Math.floorMod(h, 100) >= 70) return;
            Spot s = probe.at(x, z);
            if (s == null || !s.interior(0.55, 0.72, 14.0) || pondDepth(seed, x, z, s) > 0) return;
            WeavecapTree.placeClipped(level, h * 31 + x, new BlockPos(x, ground, z), WeavecapGeometry.Size.GIANT, clip);
        });
        // medium Weavecaps
        forEachCell(seed, 0x3ED1L, 22, 18, clip, (x, z, h) -> {
            if (Math.floorMod(h, 100) >= 45) return;
            Spot s = probe.at(x, z);
            if (s == null || !s.interior(0.7, 0.85, 8.0) || pondDepth(seed, x, z, s) > 0) return;
            WeavecapTree.placeClipped(level, h * 17 + z, new BlockPos(x, ground, z), WeavecapGeometry.Size.MEDIUM, clip);
        });
        // small Weavecaps
        forEachCell(seed, 0x5A11L, 13, 9, clip, (x, z, h) -> {
            if (Math.floorMod(h, 100) >= 22) return;
            Spot s = probe.at(x, z);
            if (s == null || !s.interior(0.82, 0.9, 4.0) || pondDepth(seed, x, z, s) > 0) return;
            WeavecapTree.placeClipped(level, h * 13 + x * 7L, new BlockPos(x, ground, z), WeavecapGeometry.Size.SMALL, clip);
        });
        // little violet-leaved trees
        forEachCell(seed, 0x710L, 11, 4, clip, (x, z, h) -> {
            if (Math.floorMod(h, 100) >= 18) return;
            Spot s = probe.at(x, z);
            if (s == null || !s.interior(0.85, 0.92, 3.0) || pondDepth(seed, x, z, s) > 0) return;
            placeVioletTree(level, new BlockPos(x, ground, z), RandomSource.create(h), clip);
        });
        // boulders, big and small
        forEachCell(seed, 0xB01DL, 17, 6, clip, (x, z, h) -> {
            if (Math.floorMod(h, 100) >= 30) return;
            Spot s = probe.at(x, z);
            if (s == null || !s.interior(0.9, 0.95, 3.0) || pondDepth(seed, x, z, s) > 0) return;
            placeBoulder(level, new BlockPos(x, ground, z), RandomSource.create(h ^ 0xB0B), clip);
        });

        // living things: herds of Cragbacks and flocks of Tetras (spawned only by the chunk that owns the spot)
        forEachCell(seed, 0x4E7DL, 40, 0, clip, (x, z, h) -> {
            if (Math.floorMod(h, 100) >= 55 || !clip.isInside(x, ground, z)) return;
            Spot s = probe.at(x, z);
            if (s == null || !s.interior(0.7, 0.85, 8.0) || pondDepth(seed, x, z, s) > 0) return;
            spawnHerd(level, new BlockPos(x, ground, z), RandomSource.create(h), clip);
        });
        forEachCell(seed, 0x7E7AL, 48, 0, clip, (x, z, h) -> {
            if (Math.floorMod(h, 100) >= 60 || !clip.isInside(x, ground, z)) return;
            Spot s = probe.at(x, z);
            if (s == null) return;
            spawnFlock(level, new BlockPos(x, ground + 12, z), RandomSource.create(h), clip);
        });
        placeTetraNests(level, chunkPos, topY, seed, clip);
        paintBiome(level, chunkPos, topY, pedestal, probe);
    }

    private static boolean canGrowOver(BlockState s) {
        return s.isAir() || s.canBeReplaced() || s.is(ModBlocks.LICHEN_GRASS.get()) || s.is(ModBlocks.TALL_LICHEN_GRASS.get())
                || s.is(ModBlocks.EMBER_CAP.get()) || s.is(ModBlocks.VIOLET_BUSH.get()) || s.is(ModBlocks.WEAVECAP_SPROUT.get());
    }

    private static void setIn(WorldGenLevel level, BoundingBox clip, BlockPos p, BlockState state, boolean force) {
        if (!clip.isInside(p)) return;
        if (force || canGrowOver(level.getBlockState(p))) level.setBlock(p, state, 2);
    }

    /** Short gnarled weavecap trunk with a round puff of violet leaves. */
    private static void placeVioletTree(WorldGenLevel level, BlockPos base, RandomSource r, BoundingBox clip) {
        int h = 3 + r.nextInt(3);
        BlockState log = ModBlocks.WEAVECAP_LOG.get().defaultBlockState();
        BlockState leaves = ModBlocks.VIOLET_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, false);
        int lx = r.nextInt(3) - 1, lz = r.nextInt(3) - 1;
        BlockPos top = base;
        for (int i = 0; i < h; i++) {
            top = base.offset(i >= h - 1 ? lx : 0, i, i >= h - 1 ? lz : 0);
            setIn(level, clip, top, log, false);
        }
        int rad = 2 + r.nextInt(2);
        for (int dx = -rad; dx <= rad; dx++) for (int dy = -1; dy <= 2; dy++) for (int dz = -rad; dz <= rad; dz++) {
            double d = dx * dx + dz * dz + (dy - 0.5) * (dy - 0.5) * 2.2;
            if (d > rad * rad + 0.6 || (d > rad * rad - 1.5 && r.nextFloat() < 0.35f)) continue;
            BlockPos p = top.offset(dx, dy + 1, dz);
            setIn(level, clip, p, leaves, false);
        }
    }

    private static void placeBoulder(WorldGenLevel level, BlockPos base, RandomSource r, BoundingBox clip) {
        BlockState[] rocks = {Blocks.TUFF.defaultBlockState(), Blocks.ANDESITE.defaultBlockState(), Blocks.COBBLESTONE.defaultBlockState(),
                ModBlocks.LICHEN_STONE.get().defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), Blocks.DEEPSLATE.defaultBlockState()};
        boolean big = r.nextFloat() < 0.35f;
        double rx = big ? 2.5 + r.nextDouble() * 2.0 : 1.0 + r.nextDouble() * 1.2;
        double ry = rx * (0.6 + r.nextDouble() * 0.35);
        double rz = rx * (0.7 + r.nextDouble() * 0.5);
        int R = (int) Math.ceil(Math.max(rx, rz)) + 1;
        for (int dx = -R; dx <= R; dx++) for (int dy = -2; dy <= (int) Math.ceil(ry); dy++) for (int dz = -R; dz <= R; dz++) {
            double v = (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry) + (dz * dz) / (rz * rz);
            if (v > 1.0 + r.nextDouble() * 0.15) continue;
            BlockPos p = base.offset(dx, dy - 1, dz);
            if (!clip.isInside(p)) continue;
            BlockState cur = level.getBlockState(p);
            if (cur.is(Blocks.WATER)) continue;
            BlockState rock = rocks[r.nextInt(rocks.length)];
            if (dy >= ry - 1 && r.nextFloat() < 0.4f) rock = ModBlocks.LICHEN_STONE.get().defaultBlockState();
            level.setBlock(p, rock, 2);
        }
    }

    private static void spawnHerd(WorldGenLevel level, BlockPos center, RandomSource r, BoundingBox clip) {
        int count = 3 + r.nextInt(3);
        for (int i = 0; i < count; i++) {
            BlockPos p = center.offset(r.nextInt(9) - 4, 0, r.nextInt(9) - 4);
            if (!clip.isInside(p)) p = center;
            // find standing room
            for (int dy = 3; dy >= -2; dy--) {
                BlockPos q = p.above(dy);
                if (level.getBlockState(q.below()).isSolid() && level.getBlockState(q).isAir() && level.getBlockState(q.above()).isAir()
                        && level.getBlockState(q.above(2)).isAir()) { p = q; break; }
            }
            CragbackEntity e = ModEntities.CRAGBACK.get().create(level.getLevel(), EntitySpawnReason.CHUNK_GENERATION);
            if (e == null) continue;
            e.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, r.nextFloat() * 360.0F, 0.0F);
            e.finalizeSpawn(level, level.getCurrentDifficultyAt(p), EntitySpawnReason.CHUNK_GENERATION, null);
            if (i == count - 1 && r.nextBoolean()) e.setAge(-24000);
            level.addFreshEntityWithPassengers(e);
        }
    }

    private static void spawnFlock(WorldGenLevel level, BlockPos center, RandomSource r, BoundingBox clip) {
        int count = 3 + r.nextInt(4);
        for (int i = 0; i < count; i++) {
            BlockPos p = center.offset(r.nextInt(7) - 3, r.nextInt(5), r.nextInt(7) - 3);
            if (!clip.isInside(p) || !level.getBlockState(p).isAir()) p = center;
            if (!level.getBlockState(p).isAir()) continue;
            TetraEntity e = ModEntities.TETRA.get().create(level.getLevel(), EntitySpawnReason.CHUNK_GENERATION);
            if (e == null) continue;
            e.snapTo(p.getX() + 0.5, p.getY() + 0.2, p.getZ() + 0.5, r.nextFloat() * 360.0F, 0.0F);
            e.finalizeSpawn(level, level.getCurrentDifficultyAt(p), EntitySpawnReason.CHUNK_GENERATION, null);
            level.addFreshEntityWithPassengers(e);
        }
    }

    /** Nests go on cliff tops above the savanna: a sturdy ledge with a sheer drop beside it. */
    private static void placeTetraNests(WorldGenLevel level, ChunkPos chunkPos, int topY, long seed, BoundingBox clip) {
        RandomSource r = RandomSource.create(cellHash(seed, chunkPos.x(), chunkPos.z(), 0x2E57L));
        if (r.nextInt(3) != 0) return;
        int placed = 0;
        for (int attempt = 0; attempt < 24 && placed < 2; attempt++) {
            int x = chunkPos.getMinBlockX() + 1 + r.nextInt(14);
            int z = chunkPos.getMinBlockZ() + 1 + r.nextInt(14);
            for (int y = topY + 70; y > topY + 6; y--) {
                BlockPos p = new BlockPos(x, y, z);
                BlockState below = level.getBlockState(p.below());
                if (!level.getBlockState(p).isAir() || !level.getBlockState(p.above()).isAir()) continue;
                if (!below.isFaceSturdy(level, p.below(), Direction.UP)) continue;
                if (!dropBeside(level, p)) break;
                level.setBlock(p, ModBlocks.TETRA_NEST.get().defaultBlockState().setValue(TetraNestBlock.EGGS, r.nextInt(4)), 2);
                int n = 1 + r.nextInt(3);
                for (int i = 0; i < n; i++) {
                    BlockPos q = p.above(1 + r.nextInt(3));
                    if (!level.getBlockState(q).isAir()) continue;
                    TetraEntity e = ModEntities.TETRA.get().create(level.getLevel(), EntitySpawnReason.CHUNK_GENERATION);
                    if (e == null) continue;
                    e.snapTo(q.getX() + 0.5, q.getY(), q.getZ() + 0.5, r.nextFloat() * 360.0F, 0.0F);
                    e.finalizeSpawn(level, level.getCurrentDifficultyAt(q), EntitySpawnReason.CHUNK_GENERATION, null);
                    e.setHomeNest(p);
                    level.addFreshEntityWithPassengers(e);
                }
                placed++;
                break;
            }
        }
    }

    private static boolean dropBeside(WorldGenLevel level, BlockPos p) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos s = p.relative(d);
            int drop = 0;
            while (drop < 8 && level.getBlockState(s.below(drop)).isAir()) drop++;
            if (drop >= 8) return true;
        }
        return false;
    }

    /** Paints the alterna:lichen_savanna biome into the 4x4x4 biome cells that cover the plateau and the air above it. */
    private static void paintBiome(WorldGenLevel level, ChunkPos chunkPos, int topY, int pedestal, Probe probe) {
        Holder<Biome> biome = level.registryAccess().lookupOrThrow(Registries.BIOME).get(BIOME).orElse(null);
        if (biome == null) return;
        ChunkAccess chunk = level.getChunk(chunkPos.x(), chunkPos.z());
        int yMin = topY - pedestal - 4, yMax = topY + 56;
        boolean[] cols = new boolean[16];
        boolean any = false;
        for (int qx = 0; qx < 4; qx++) for (int qz = 0; qz < 4; qz++) {
            int bx = chunkPos.getMinBlockX() + qx * 4 + 2, bz = chunkPos.getMinBlockZ() + qz * 4 + 2;
            Spot s = probe.at(bx, bz);
            cols[qx * 4 + qz] = s != null;
            any |= s != null;
        }
        if (!any) return;
        int qMinX = QuartPos.fromBlock(chunkPos.getMinBlockX()), qMinZ = QuartPos.fromBlock(chunkPos.getMinBlockZ());
        chunk.fillBiomesFromNoise((qx, qy, qz, sampler) -> {
            Holder<Biome> current = chunk.getNoiseBiome(qx, qy, qz);
            int by = QuartPos.toBlock(qy);
            if (by >= yMin && by <= yMax && cols[(qx - qMinX) * 4 + (qz - qMinZ)]) return biome;
            return current;
        }, null);
    }
}
