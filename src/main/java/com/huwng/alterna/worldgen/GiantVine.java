package com.huwng.alterna.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Giant Vine growing along the walls of a giant crack.
 * 
 * Starts at Y = 60 and grows down along the crack wall.
 * Features procedural Giant Fruits hanging underneath horizontal sections and ledge junctions.
 */
public final class GiantVine {

    public static class VineNode {
        public final double x, y, z;
        public final boolean isJunction;

        public VineNode(double x, double y, double z, boolean isJunction) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.isJunction = isJunction;
        }
    }

    public static class GiantFruitPoint {
        public final double x, y, z;
        public final long fruitSeed;

        public GiantFruitPoint(double x, double y, double z, long fruitSeed) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.fruitSeed = fruitSeed;
        }
    }

    private static final double STEP = 0.6; // Step size along line segments
    private final List<List<VineNode>> polylines; // Main path + sub-branches
    private final List<GiantFruitPoint> fruitPoints; // Hanging giant fruits
    private final double radius;
    private final long seed;

    private final int minChunkX, maxChunkX, minChunkZ, maxChunkZ;

    public GiantVine(List<List<VineNode>> polylines, double radius, long seed) {
        this.polylines = polylines;
        this.radius = radius;
        this.seed = seed;
        this.fruitPoints = new ArrayList<>();

        double loX = Double.POSITIVE_INFINITY, hiX = Double.NEGATIVE_INFINITY;
        double loZ = Double.POSITIVE_INFINITY, hiZ = Double.NEGATIVE_INFINITY;

        int nodeIdx = 0;
        for (List<VineNode> path : polylines) {
            for (VineNode n : path) {
                loX = Math.min(loX, n.x);
                hiX = Math.max(hiX, n.x);
                loZ = Math.min(loZ, n.z);
                hiZ = Math.max(hiZ, n.z);

                // Add Giant Fruit at selected ledge junctions or horizontal branch points
                if (n.isJunction || (nodeIdx % 12 == 0 && n.y < 40)) {
                    long nodeHash = ((long) n.x * 3129871L ^ (long) n.y * 1168189L ^ (long) n.z * 999983L ^ seed ^ nodeIdx);
                    nodeHash ^= nodeHash >>> 16;
                    if ((Math.abs(nodeHash) % 100) < 40) {
                        fruitPoints.add(new GiantFruitPoint(n.x, n.y, n.z, nodeHash));
                    }
                }
                nodeIdx++;
            }
        }

        double margin = radius + 6.0;
        this.minChunkX = ((int) Math.floor(loX - margin)) >> 4;
        this.maxChunkX = ((int) Math.ceil(hiX + margin)) >> 4;
        this.minChunkZ = ((int) Math.floor(loZ - margin)) >> 4;
        this.maxChunkZ = ((int) Math.ceil(hiZ + margin)) >> 4;
    }

    public boolean mightAffect(ChunkPos chunkPos) {
        return chunkPos.x() >= minChunkX && chunkPos.x() <= maxChunkX
                && chunkPos.z() >= minChunkZ && chunkPos.z() <= maxChunkZ;
    }

    public void place(WorldGenLevel level, ChunkPos chunkPos) {
        int chunkMinX = chunkPos.getMinBlockX();
        int chunkMaxX = chunkPos.getMaxBlockX();
        int chunkMinZ = chunkPos.getMinBlockZ();
        int chunkMaxZ = chunkPos.getMaxBlockZ();

        // 1. Stamp Giant Vine segments
        for (List<VineNode> path : polylines) {
            for (int i = 0; i + 1 < path.size(); i++) {
                VineNode from = path.get(i);
                VineNode to = path.get(i + 1);
                stampSegment(level, from, to, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
            }
        }

        // 2. Stamp Giant Fruit structures
        for (GiantFruitPoint fruit : fruitPoints) {
            placeFruit(level, fruit, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        }
    }

    private void stampSegment(WorldGenLevel level, VineNode from, VineNode to,
                              int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = Math.max(1, (int) Math.ceil(len / STEP));

        for (int i = 0; i <= steps; i++) {
            double f = (double) i / steps;
            double px = from.x + dx * f;
            double py = from.y + dy * f;
            double pz = from.z + dz * f;

            if (px + radius + 1.5 < chunkMinX || px - radius - 1.5 > chunkMaxX
                    || pz + radius + 1.5 < chunkMinZ || pz - radius - 1.5 > chunkMaxZ) {
                continue;
            }

            stampBall(level, px, py, pz, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        }
    }

    private void stampBall(WorldGenLevel level, double px, double py, double pz,
                           int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        int minX = Math.max(chunkMinX, (int) Math.floor(px - radius));
        int maxX = Math.min(chunkMaxX, (int) Math.ceil(px + radius));
        int minY = (int) Math.floor(py - radius);
        int maxY = (int) Math.ceil(py + radius);
        int minZ = Math.max(chunkMinZ, (int) Math.floor(pz - radius));
        int maxZ = Math.min(chunkMaxZ, (int) Math.ceil(pz + radius));

        double rSq = radius * radius;

        for (int x = minX; x <= maxX; x++) {
            double dx = x + 0.5 - px;
            for (int y = minY; y <= maxY; y++) {
                double dy = y + 0.5 - py;
                for (int z = minZ; z <= maxZ; z++) {
                    double dz = z + 0.5 - pz;
                    double distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq > rSq) {
                        continue;
                    }

                    placeVineBlock(level, x, y, z, distSq / rSq);
                }
            }
        }
    }

    private void placeVineBlock(WorldGenLevel level, int x, int y, int z, double distNorm) {
        if (y <= level.getMinY() || y >= level.getMaxY()) {
            return;
        }

        BlockPos pos = new BlockPos(x, y, z);
        BlockState existing = level.getBlockState(pos);

        if (existing.is(Blocks.BEDROCK) || (existing.isSolidRender() && !existing.is(Blocks.SANDSTONE) && !existing.is(Blocks.STONE))) {
            return;
        }

        long posHash = (x * 3129871L ^ y * 1168189L ^ z * 999983L ^ seed);
        posHash ^= posHash >>> 16;
        double rnd = ((posHash & 0xFFFF) / 65535.0);

        BlockState state;

        if (rnd < 0.008 && distNorm < 0.2) {
            state = Blocks.SHROOMLIGHT.defaultBlockState();
        } else {
            // Smooth organic leaf transition around Y = -120 (from Y = -105 to Y = -135)
            double t = GiantCrackParams.clamp((y - (-135.0)) / 30.0, 0.0, 1.0);
            double noise = GiantCrackParams.smoothNoise3D(x, y, z, seed ^ 0x7EAF1EAFL, 0.15) * 0.25;
            double azaleaChance = GiantCrackParams.clamp(t + noise, 0.0, 1.0);

            if (rnd < azaleaChance) {
                // Azalea / Flowering Azalea leaves (Upper Cavern)
                double subRnd = ((posHash ^ 0x91811L) & 0xFFFF) / 65535.0;
                if (subRnd > 0.70) {
                    state = Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState();
                } else {
                    state = Blocks.AZALEA_LEAVES.defaultBlockState();
                }
            } else {
                // Mangrove / Pale Oak leaves (Lower Deep Cavern)
                long clusterHash = ((x >> 2) * 8831L ^ (y >> 3) * 3571L ^ (z >> 2) * 999983L ^ seed);
                clusterHash ^= clusterHash >>> 16;
                double clusterRnd = ((clusterHash & 0xFFFF) / 65535.0);

                if (clusterRnd < 0.08) {
                    state = getPaleOakState();
                } else {
                    state = Blocks.MANGROVE_LEAVES.defaultBlockState();
                }
            }

            if (state.hasProperty(LeavesBlock.PERSISTENT)) {
                state = state.setValue(LeavesBlock.PERSISTENT, true);
            }
            if (state.hasProperty(LeavesBlock.DISTANCE)) {
                state = state.setValue(LeavesBlock.DISTANCE, 1);
            }
        }

        level.setBlock(pos, state, 2);
    }

    /**
     * Stamps a procedural Giant Fruit hanging beneath the vine.
     * Stem: Mangrove Roots (1-2 blocks wide, 3-5 blocks long, slanted).
     * Fruit: Layer 1 = Melon 2x2; Layer 2 = Melon 4x4x4 (corners cut) with 2x2 core; Layer 3 = Melon 2x2.
     */
    private void placeFruit(WorldGenLevel level, GiantFruitPoint fruit,
                            int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        long rnd = Math.abs(fruit.fruitSeed);
        int stemLength = 3 + (int) (rnd % 3); // 3 to 5 blocks long
        double slantX = ((rnd & 1) == 0 ? 0.35 : -0.35);
        double slantZ = (((rnd >> 1) & 1) == 0 ? 0.35 : -0.35);

        double curX = fruit.x;
        double curY = fruit.y;
        double curZ = fruit.z;

        BlockState stemState = Blocks.MANGROVE_ROOTS.defaultBlockState();

        // 1. Stem (Mangrove Roots 1-2 blocks wide, 3-5 blocks long, slanted down)
        for (int s = 1; s <= stemLength; s++) {
            curX += slantX;
            curY -= 1.0;
            curZ += slantZ;

            int stemX = (int) Math.round(curX);
            int stemY = (int) Math.round(curY);
            int stemZ = (int) Math.round(curZ);

            int widthX = (rnd & 2) != 0 ? 1 : 0;
            int widthZ = (rnd & 4) != 0 ? 1 : 0;

            for (int dx = 0; dx <= widthX; dx++) {
                for (int dz = 0; dz <= widthZ; dz++) {
                    int bx = stemX + dx;
                    int bz = stemZ + dz;
                    if (bx >= chunkMinX && bx <= chunkMaxX && bz >= chunkMinZ && bz <= chunkMaxZ) {
                        tryPlaceFruitBlock(level, bx, stemY, bz, stemState);
                    }
                }
            }
        }

        // 2. Fruit Body underneath stem
        int originX = (int) Math.round(curX) - 1; // 4x4 bounding box centered around stem base
        int originZ = (int) Math.round(curZ) - 1;
        int topY = (int) Math.round(curY) - 1;

        BlockState melonState = Blocks.MELON.defaultBlockState();
        BlockState netherWartState = Blocks.NETHER_WART_BLOCK.defaultBlockState();
        BlockState rawGoldState = Blocks.RAW_GOLD_BLOCK.defaultBlockState();

        // 15% chance for Raw Gold Block core, 85% chance for Nether Wart Block core
        double coreRnd = ((rnd ^ 0x5DEECE66DL) & 0xFFFF) / 65535.0;
        BlockState coreState = (coreRnd < 0.15) ? rawGoldState : netherWartState;

        // Layer 1 (Top Layer): Melon 2x2
        int l1Y = topY;
        for (int dx = 1; dx <= 2; dx++) {
            for (int dz = 1; dz <= 2; dz++) {
                int bx = originX + dx;
                int bz = originZ + dz;
                if (bx >= chunkMinX && bx <= chunkMaxX && bz >= chunkMinZ && bz <= chunkMaxZ) {
                    tryPlaceFruitBlock(level, bx, l1Y, bz, melonState);
                }
            }
        }

        // Layer 2 (Middle Body: 4x4x4 height with 1 block cut at each of the 8 3D corners on top/bottom Y slices)
        for (int yOff = 1; yOff <= 4; yOff++) {
            int l2Y = topY - yOff;
            boolean isTopOrBottomSlice = (yOff == 1 || yOff == 4);
            for (int dx = 0; dx < 4; dx++) {
                for (int dz = 0; dz < 4; dz++) {
                    // Cut 1 block at the 8 outer 3D corners (4 on top slice yOff=1, 4 on bottom slice yOff=4)
                    if (isTopOrBottomSlice && (dx == 0 || dx == 3) && (dz == 0 || dz == 3)) {
                        continue;
                    }

                    int bx = originX + dx;
                    int bz = originZ + dz;
                    if (bx < chunkMinX || bx > chunkMaxX || bz < chunkMinZ || bz > chunkMaxZ) {
                        continue;
                    }

                    boolean isCore = (dx == 1 || dx == 2) && (dz == 1 || dz == 2);
                    BlockState bState = isCore ? coreState : melonState;
                    tryPlaceFruitBlock(level, bx, l2Y, bz, bState);
                }
            }
        }

        // Layer 3 (Bottom Layer): Melon 2x2
        int l3Y = topY - 5;
        for (int dx = 1; dx <= 2; dx++) {
            for (int dz = 1; dz <= 2; dz++) {
                int bx = originX + dx;
                int bz = originZ + dz;
                if (bx >= chunkMinX && bx <= chunkMaxX && bz >= chunkMinZ && bz <= chunkMaxZ) {
                    tryPlaceFruitBlock(level, bx, l3Y, bz, melonState);
                }
            }
        }
    }

    private void tryPlaceFruitBlock(WorldGenLevel level, int x, int y, int z, BlockState state) {
        if (y <= level.getMinY() || y >= level.getMaxY()) {
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);
        BlockState existing = level.getBlockState(pos);

        if (existing.is(Blocks.BEDROCK) || (existing.isSolidRender() && !existing.is(Blocks.SANDSTONE) && !existing.is(Blocks.STONE))) {
            return;
        }

        level.setBlock(pos, state, 2);
    }

    private static BlockState paleOakStateCache = null;

    private static BlockState getPaleOakState() {
        if (paleOakStateCache != null) {
            return paleOakStateCache;
        }
        try {
            paleOakStateCache = Blocks.PALE_OAK_LEAVES.defaultBlockState();
        } catch (Throwable t) {
            paleOakStateCache = Blocks.BIRCH_LEAVES.defaultBlockState();
        }
        return paleOakStateCache;
    }
}
