package com.huwng.alterna.worldgen;

import com.huwng.alterna.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Một đoạn rễ chạy dọc bề mặt vách đá, nối 2 GobletTree kế nhau trên cùng
 * một bên rift, để một hàng cây đọc thành một hệ rễ liền mạch thay vì các
 * thân cây rời rạc. Đường đi được tính sẵn một lần trong
 * GiantCrackParams.buildRootConnector() bằng cách lấy mẫu lại vách đá theo
 * từng bước nhỏ dọc chiều dài giữa 2 anchor (xem solveWallPoint()), nên ống
 * rễ này bám đúng theo mặt đá thật - kể cả phần gồ ghề do harmonics/roughness
 * gây ra - thay vì cắt thẳng xuyên không khí hoặc xuyên đá giữa 2 điểm.
 *
 * Thuần là hàm của polyline đã lưu sẵn: place() chỉ đọc/ghi block trong
 * đúng chunk được truyền vào, nên - giống GobletTree - bất kỳ chunk nào mà
 * bounding box của rễ này chạm tới đều có thể tự dựng phần của mình, độc
 * lập, theo bất kỳ thứ tự nào.
 */
final class RootConnector {

    private static final double STEP = 0.5; // bước theo chiều dài cung giữa các lát cắt được đặt

    private final double[][] points; // {x, y, z}, theo thứ tự từ cây A sang cây B
    private final double radius;

    private final int minChunkX, maxChunkX, minChunkZ, maxChunkZ;

    RootConnector(double[][] points, double radius) {
        this.points = points;
        this.radius = radius;

        double loX = Double.POSITIVE_INFINITY, hiX = Double.NEGATIVE_INFINITY;
        double loZ = Double.POSITIVE_INFINITY, hiZ = Double.NEGATIVE_INFINITY;
        for (double[] p : points) {
            loX = Math.min(loX, p[0]);
            hiX = Math.max(hiX, p[0]);
            loZ = Math.min(loZ, p[2]);
            hiZ = Math.max(hiZ, p[2]);
        }
        double margin = radius + 2.0;
        this.minChunkX = ((int) Math.floor(loX - margin)) >> 4;
        this.maxChunkX = ((int) Math.ceil(hiX + margin)) >> 4;
        this.minChunkZ = ((int) Math.floor(loZ - margin)) >> 4;
        this.maxChunkZ = ((int) Math.ceil(hiZ + margin)) >> 4;
    }

    boolean mightAffect(ChunkPos chunkPos) {
        return chunkPos.x() >= minChunkX && chunkPos.x() <= maxChunkX
                && chunkPos.z() >= minChunkZ && chunkPos.z() <= maxChunkZ;
    }

    void place(WorldGenLevel level, ChunkPos chunkPos) {
        int chunkMinX = chunkPos.getMinBlockX();
        int chunkMaxX = chunkPos.getMaxBlockX();
        int chunkMinZ = chunkPos.getMinBlockZ();
        int chunkMaxZ = chunkPos.getMaxBlockZ();

        for (int i = 0; i + 1 < points.length; i++) {
            stampSegment(level, points[i], points[i + 1], chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        }
    }

    private void stampSegment(WorldGenLevel level, double[] from, double[] to,
                              int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        double dx = to[0] - from[0];
        double dy = to[1] - from[1];
        double dz = to[2] - from[2];
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = Math.max(1, (int) Math.ceil(len / STEP));

        for (int i = 0; i <= steps; i++) {
            double f = (double) i / steps;
            double px = from[0] + dx * f;
            double py = from[1] + dy * f;
            double pz = from[2] + dz * f;

            if (px + radius + 1.0 < chunkMinX || px - radius - 1.0 > chunkMaxX
                    || pz + radius + 1.0 < chunkMinZ || pz - radius - 1.0 > chunkMaxZ) {
                continue;
            }
            stampBall(level, px, py, pz, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        }
    }

    /**
     * Một khối cầu gỗ nhỏ tại (px, py, pz) - đơn giản hơn nhiều so với
     * lát cắt bát giác của GobletTree, chấp nhận được vì rễ nối mảnh hơn
     * hẳn so với 2 cái cây nó nối.
     */
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
                    if (dx * dx + dy * dy + dz * dz > rSq) {
                        continue;
                    }
                    placeWood(level, x, y, z);
                }
            }
        }
    }

    private static void placeWood(WorldGenLevel level, int x, int y, int z) {
        if (y <= level.getMinY() || y >= level.getMaxY()) {
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).is(Blocks.BEDROCK)) {
            return; // không bao giờ phá vỡ tường an toàn của rift
        }
        BlockState state = useStripped(x, y, z)
                ? ModBlocks.GOBLET_STRIPPED_WOOD.get().defaultBlockState()
                : ModBlocks.GOBLET_WOOD.get().defaultBlockState();
        level.setBlock(pos, state, 2);
    }

    private static boolean useStripped(int x, int y, int z) {
        long h = (x * 1000003L + y * 999983L + z * 999979L);
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h & 0xFFL) < 38L; // ~15%
    }
}