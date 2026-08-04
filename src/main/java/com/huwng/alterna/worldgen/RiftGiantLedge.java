package com.huwng.alterna.worldgen;

import net.minecraft.util.RandomSource;

/**
 * Giant Wall Ledges with Curved Stone Pedestal Base ("Bệ đá lớn có đế cong").
 * - Spawns strictly below Y = -100 (down to Y = -335 above water level).
 * - 2 to 5 giant ledges per crack segment.
 * - Massive semi-circular shape (length 50-100 blocks, reach 20-40 blocks).
 * - Flat top surface that slopes downwards at the outer rim edge.
 * - Smoothly blended curved pedestal base underneath.
 */
public final class RiftGiantLedge {
    public final int side;            // -1 (left wall) or +1 (right wall)
    public final double sCenter;      // Normalized position along length (-0.60 to +0.60)
    public final double sHalfLength;  // Half length along wall (25 to 50 blocks -> Total 50-100 blocks)
    public final int yTop;            // Top flat surface Y height (-335 to -100)
    public final double reachWidth;   // Extends 20 to 40 blocks into cavern
    public final double pedestalHeight; // Underside pedestal curve height (12 to 24 blocks)
    public final RiftLedge.Variant variant; // Variant type for decoration

    public RiftGiantLedge(int side, double sCenter, double sHalfLength, int yTop, double reachWidth, double pedestalHeight, RiftLedge.Variant variant) {
        this.side = side;
        this.sCenter = sCenter;
        this.sHalfLength = sHalfLength;
        this.yTop = yTop;
        this.reachWidth = reachWidth;
        this.pedestalHeight = pedestalHeight;
        this.variant = variant;
    }

    public static RiftGiantLedge[] buildGiantLedges(RandomSource random, int originY, int depth) {
        int minLedgeY = Math.max(-335, originY - depth + 30);
        int maxLedgeY = -100; // Strictly below Y = -100 as requested
        if (maxLedgeY <= minLedgeY + 30) return new RiftGiantLedge[0];

        int count = 6 + random.nextInt(5); // 6 to 10 standard giant ledges per crack
        RiftGiantLedge[] result = new RiftGiantLedge[count + 1];

        // 1. Exactly 1 Rootshroom Forest Giant Ledge (Y = -160 to -170, length 220 to 300 blocks, near 1 of the 2 ends)
        int forestSide = random.nextBoolean() ? 1 : -1;
        double forestSCenter = (random.nextBoolean() ? -1.0 : 1.0) * (0.65 + random.nextDouble() * 0.15); // End of crack
        double forestSHalfLength = 110.0 + random.nextDouble() * 40.0; // Half length 110-150 -> Total length 220-300 blocks!
        int forestYTop = -160 - random.nextInt(11); // Y = -160 to -170
        double forestReachWidth = 35.0 + random.nextDouble() * 15.0; // Reach width 35-50 blocks
        double forestPedestalHeight = 20.0 + random.nextDouble() * 10.0; // Pedestal height 20-30 blocks

        result[0] = new RiftGiantLedge(forestSide, forestSCenter, forestSHalfLength, forestYTop, forestReachWidth, forestPedestalHeight, RiftLedge.Variant.ROOTSHROOM_FOREST);

        RiftLedge.Variant[] standardVariants = new RiftLedge.Variant[]{
            RiftLedge.Variant.MOSS_DAZE,
            RiftLedge.Variant.CLOUDBERRY,
            RiftLedge.Variant.STARLILY,
            RiftLedge.Variant.HOLLOW
        };

        // 2. Standard giant ledges
        for (int i = 0; i < count; i++) {
            int side = random.nextBoolean() ? 1 : -1;
            double sCenter = (random.nextDouble() - 0.5) * 1.70;
            double sHalfLength = 25.0 + random.nextDouble() * 25.0; // Total length = 50 to 100 blocks
            int yTop = minLedgeY + (int) ((i + random.nextDouble() * 0.5) / count * (maxLedgeY - minLedgeY));
            double reachWidth = 20.0 + random.nextDouble() * 20.0;   // Extends 20 to 40 blocks into cavern
            double pedestalHeight = 12.0 + random.nextDouble() * 12.0; // Pedestal height = 12 to 24 blocks
            RiftLedge.Variant variant = standardVariants[random.nextInt(standardVariants.length)];
            result[i + 1] = new RiftGiantLedge(side, sCenter, sHalfLength, yTop, reachWidth, pedestalHeight, variant);
        }
        return result;
    }

    public static double getEdgeDistanceNorm(RiftGiantLedge gl, double localA, double localB, double halfLength, double effectiveHalfWidth) {
        double ds = Math.abs(localA - gl.sCenter * halfLength);
        if (ds > gl.sHalfLength) return 1.0;
        double lengthNorm = ds / gl.sHalfLength;
        double circularProfile = Math.sqrt(Math.max(0.0, 1.0 - lengthNorm * lengthNorm));
        double wallB = gl.side * effectiveHalfWidth;
        double distFromWall = gl.side * (wallB - localB);
        double maxTopReach = gl.reachWidth * circularProfile;
        return GiantCrackParams.clamp(distFromWall / Math.max(1.0, maxTopReach), 0.0, 1.0);
    }

    public static boolean isOuterEdge(RiftGiantLedge gl, double localA, double localB, double halfLength, double effectiveHalfWidth) {
        double ds = Math.abs(localA - gl.sCenter * halfLength);
        if (ds > gl.sHalfLength) return false;
        double lengthNorm = ds / gl.sHalfLength;
        double circularProfile = Math.sqrt(Math.max(0.0, 1.0 - lengthNorm * lengthNorm));
        double wallB = gl.side * effectiveHalfWidth;
        double distFromWall = gl.side * (wallB - localB);
        double maxTopReach = gl.reachWidth * circularProfile;
        return distFromWall >= maxTopReach - 4.0;
    }

    public static boolean isInsideGiantLedge(RiftGiantLedge gl, double localA, double localB, int x, int y, int z, double halfLength, double effectiveHalfWidth, long roughnessSeed) {
        int topY = (gl.variant == RiftLedge.Variant.HOLLOW) ? (gl.yTop - 1) : gl.yTop;
        if (y > topY || y < gl.yTop - gl.pedestalHeight - 4) return false;

        double ds = Math.abs(localA - gl.sCenter * halfLength);
        if (ds > gl.sHalfLength) return false;

        // Semi-circular rounded dome shape protruding out into the canyon
        double lengthNorm = ds / gl.sHalfLength;
        double circularProfile = Math.sqrt(Math.max(0.0, 1.0 - lengthNorm * lengthNorm));

        double wallB = gl.side * effectiveHalfWidth;
        double distFromWall = gl.side * (wallB - localB);

        // Deep 30-block wall anchor guarantees zero gaps between ledge and wall
        if (distFromWall < -30.0) return false;

        double maxTopReach = gl.reachWidth * circularProfile;
        if (distFromWall > maxTopReach) return false;

        // Outer rim sloped edge: near wall is flat, outer 45% of reach slopes downwards
        double distNorm = GiantCrackParams.clamp(distFromWall / Math.max(1.0, maxTopReach), 0.0, 1.0);
        double slopeDrop = 0.0;
        if (distNorm > 0.55 && gl.variant != RiftLedge.Variant.HOLLOW) {
            double edgeT = (distNorm - 0.55) / 0.45;
            slopeDrop = edgeT * edgeT * 3.5; // Outer edge slopes downwards by up to 3.5 blocks
        }
        double currentTopY = topY - slopeDrop;

        if (y > currentTopY) return false;

        double dy = topY - y; // distance below topY

        if (dy <= 2.0 + slopeDrop) { 
            // Top platform surface with sloped outer edge
            return true;
        } else {
            // Smoothly curved pedestal base underneath
            double normDy = GiantCrackParams.clamp((dy - (2.0 + slopeDrop)) / Math.max(1.0, gl.pedestalHeight - (2.0 + slopeDrop)), 0.0, 1.0);
            double pedestalProfile = (1.0 - normDy) * (1.0 - normDy); // Smooth parabolic concave curve
            double currentReach = maxTopReach * pedestalProfile;
            return distFromWall <= currentReach;
        }
    }

    public static boolean isInsideHollowBasin(RiftGiantLedge gl, double localA, double localB, int x, int y, int z, double halfLength, double effectiveHalfWidth) {
        if (gl.variant != RiftLedge.Variant.HOLLOW) return false;

        int waterTopY = gl.yTop - 1;
        if (y > waterTopY) return false;

        double ds = Math.abs(localA - gl.sCenter * halfLength);
        double lengthNorm = ds / gl.sHalfLength;
        if (lengthNorm >= 0.82) return false; // 18% length margin

        double circularProfile = Math.sqrt(Math.max(0.0, 1.0 - lengthNorm * lengthNorm));

        double wallB = gl.side * effectiveHalfWidth;
        double distFromWall = gl.side * (wallB - localB);

        double maxTopReach = gl.reachWidth * circularProfile;

        // Containment check: At least 3 blocks margin away from wall and 3.5 blocks inside outer reach
        double minReach = 3.0;
        double maxReach = maxTopReach - 3.5;
        if (maxReach <= minReach + 1.5) return false;

        if (distFromWall < minReach || distFromWall > maxReach) return false;

        // Dynamic containment check: Ensure solid dam rock exists 3 blocks further out towards canyon & length ends
        double outerCheckB = localB - gl.side * 3.0;
        if (!isInsideGiantLedge(gl, localA, outerCheckB, x, waterTopY, z, halfLength, effectiveHalfWidth, 0L)) {
            return false; // Water would leak out over canyon edge! Stop carving!
        }
        if (!isInsideGiantLedge(gl, localA + 3.0, localB, x, waterTopY, z, halfLength, effectiveHalfWidth, 0L) ||
            !isInsideGiantLedge(gl, localA - 3.0, localB, x, waterTopY, z, halfLength, effectiveHalfWidth, 0L)) {
            return false; // Water would leak out length ends! Stop carving!
        }

        double reachCenter = (minReach + maxReach) * 0.5;
        double reachRadius = (maxReach - minReach) * 0.5;
        double rNorm = Math.abs(distFromWall - reachCenter) / reachRadius;
        if (rNorm >= 1.0) return false;

        double bowlShape = Math.sqrt(Math.max(0.0, 1.0 - rNorm * rNorm)) * circularProfile;

        // Shallower depth: raised floor by 1 block (max depth 2 to 3 blocks)
        int maxDepth = (int) Math.min(gl.pedestalHeight - 4.0, Math.max(2.0, gl.pedestalHeight * 0.28));
        if (maxDepth < 2) maxDepth = 2;
        if (maxDepth > 3) maxDepth = 3;

        int basinDepth = (int) Math.round(maxDepth * bowlShape);
        if (basinDepth < 1) basinDepth = 1;

        int floorY = waterTopY - basinDepth;

        return y >= floorY && y <= waterTopY;
    }

    public static boolean isHollowFloor(RiftGiantLedge gl, double localA, double localB, int x, int y, int z, double halfLength, double effectiveHalfWidth) {
        if (!isInsideHollowBasin(gl, localA, localB, x, y, z, halfLength, effectiveHalfWidth)) return false;
        return !isInsideHollowBasin(gl, localA, localB, x, y - 1, z, halfLength, effectiveHalfWidth);
    }

    public static boolean isHollowRim(RiftGiantLedge gl, double localA, double localB, int x, int y, int z, double halfLength, double effectiveHalfWidth) {
        if (gl.variant != RiftLedge.Variant.HOLLOW) return false;
        int waterTopY = gl.yTop - 1;
        if (y != waterTopY) return false;

        if (isInsideHollowBasin(gl, localA, localB, x, y, z, halfLength, effectiveHalfWidth)) return false;

        // Must be adjacent to a hollow basin block
        return isInsideHollowBasin(gl, localA + 1, localB, x, y, z, halfLength, effectiveHalfWidth) ||
               isInsideHollowBasin(gl, localA - 1, localB, x, y, z, halfLength, effectiveHalfWidth) ||
               isInsideHollowBasin(gl, localA, localB + 1, x, y, z, halfLength, effectiveHalfWidth) ||
               isInsideHollowBasin(gl, localA, localB - 1, x, y, z, halfLength, effectiveHalfWidth);
    }
}
