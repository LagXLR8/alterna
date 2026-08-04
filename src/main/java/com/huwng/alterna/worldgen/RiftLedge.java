package com.huwng.alterna.worldgen;

import net.minecraft.util.RandomSource;

public final class RiftLedge {
    public enum Variant {
        MOSS_DAZE,
        CLOUDBERRY,
        STARLILY,
        HOLLOW,
        ROOTSHROOM_FOREST
    }

    public final int side;            // -1 (left wall) or +1 (right wall)
    public final double sCenter;      // Normalized position along length (-0.80 to +0.80)
    public final double sHalfLength;  // Half length along wall (8 to 25 blocks)
    public final int yCenter;         // Center Y height
    public final double yHalfHeight;  // Vertical thickness
    public final double reachWidth;   // Extends 3 to 10 blocks into cavern
    public final boolean isBoulder;   // false = flat platform; true = rounded boulder protrusion
    public final Variant variant;     // Variant type for decoration

    public RiftLedge(int side, double sCenter, double sHalfLength, int yCenter, double yHalfHeight, double reachWidth, boolean isBoulder, Variant variant) {
        this.side = side;
        this.sCenter = sCenter;
        this.sHalfLength = sHalfLength;
        this.yCenter = yCenter;
        this.yHalfHeight = yHalfHeight;
        this.reachWidth = reachWidth;
        this.isBoulder = isBoulder;
        this.variant = variant;
    }

    public static RiftLedge[] buildLedges(RandomSource random, int originY, int depth) {
        int minLedgeY = Math.max(-335, originY - depth + 25);
        int maxLedgeY = Math.min(-50, originY - 68); // Never spawn higher than Y = -50
        if (maxLedgeY <= minLedgeY + 20) return new RiftLedge[0];

        int count = 55 + random.nextInt(26); // 55 to 80 ledges (spans to crack ends)
        RiftLedge[] result = new RiftLedge[count];
        Variant[] standardVariants = new Variant[]{Variant.MOSS_DAZE, Variant.CLOUDBERRY, Variant.STARLILY};

        for (int i = 0; i < count; i++) {
            int side = random.nextBoolean() ? 1 : -1;
            double sCenter = (random.nextDouble() - 0.5) * 1.90;
            double sHalfLength = 8.0 + random.nextDouble() * 17.0;
            int yCenter = minLedgeY + (int) ((i + random.nextDouble() * 0.5) / count * (maxLedgeY - minLedgeY));
            boolean isBoulder = random.nextBoolean();
            double yHalfHeight = isBoulder ? (2.5 + random.nextDouble() * 3.0) : (1.0 + random.nextDouble() * 1.5);
            double reachWidth = 3.0 + random.nextDouble() * 7.0;
            Variant variant = standardVariants[random.nextInt(standardVariants.length)];
            result[i] = new RiftLedge(side, sCenter, sHalfLength, yCenter, yHalfHeight, reachWidth, isBoulder, variant);
        }
        return result;
    }

    public static double getEdgeDistanceNorm(RiftLedge lg, double localA, double localB, double halfLength, double effectiveHalfWidth) {
        double ds = Math.abs(localA - lg.sCenter * halfLength);
        if (ds > lg.sHalfLength) return 1.0;
        double lengthProfile = 1.0 - (ds / lg.sHalfLength) * (ds / lg.sHalfLength);
        double maxReach = lg.reachWidth * lengthProfile;
        double wallB = lg.side * effectiveHalfWidth;
        double distFromWall = lg.side * (wallB - localB);
        return GiantCrackParams.clamp(distFromWall / Math.max(1.0, maxReach), 0.0, 1.0);
    }

    public static boolean isOuterEdge(RiftLedge lg, double localA, double localB, double halfLength, double effectiveHalfWidth) {
        double ds = Math.abs(localA - lg.sCenter * halfLength);
        if (ds > lg.sHalfLength) return false;
        double lengthProfile = 1.0 - (ds / lg.sHalfLength) * (ds / lg.sHalfLength);
        double maxReach = lg.reachWidth * lengthProfile;
        double wallB = lg.side * effectiveHalfWidth;
        double distFromWall = lg.side * (wallB - localB);
        return distFromWall >= maxReach - 2.0;
    }

    public static boolean isInsideLedge(RiftLedge lg, double localA, double localB, int x, int y, int z, double halfLength, double effectiveHalfWidth, long roughnessSeed) {
        double ds = Math.abs(localA - lg.sCenter * halfLength);
        if (ds > lg.sHalfLength) return false;

        double lengthProfile = 1.0 - (ds / lg.sHalfLength) * (ds / lg.sHalfLength);
        double wallB = lg.side * effectiveHalfWidth;
        double distFromWall = lg.side * (wallB - localB);
        double maxReach = lg.reachWidth * lengthProfile;

        // Ensure it starts deep inside the wall (-6.0) so it's always firmly anchored and never floats in air!
        if (distFromWall >= -6.0 && distFromWall <= maxReach) {
            double reachNorm = GiantCrackParams.clamp(distFromWall / Math.max(1.0, maxReach), 0.0, 1.0);

            if (lg.isBoulder) {
                // Boulder: rounded sides, flattened top surface
                double heightMax = lg.yHalfHeight * Math.sqrt(Math.max(0.0, 1.0 - reachNorm * reachNorm)) * Math.sqrt(lengthProfile);
                double bottomY = lg.yCenter - heightMax * 1.2;
                double topY = lg.yCenter + heightMax * 0.4; // Flatten top half
                double bNoise = GiantCrackParams.smoothNoise3D(x, y, z, roughnessSeed ^ 0x1E6D6E7L, 0.2) * 0.8;
                return y >= bottomY - bNoise && y <= topY + 0.3;
            } else {
                // Flat platform: completely flat top for walking, tapered bottom
                double heightMax = lg.yHalfHeight * (1.0 - 0.2 * reachNorm) * Math.sqrt(lengthProfile);
                double topY = lg.yCenter + 1.0; // Flat top
                double bottomY = lg.yCenter - heightMax * 1.5; // Tapered bottom into wall
                double bNoise = GiantCrackParams.smoothNoise3D(x, y, z, roughnessSeed ^ 0x1E6D6E7L, 0.2) * 0.6;
                return y >= bottomY - bNoise && y <= topY + 0.2;
            }
        }
        return false;
    }
}
