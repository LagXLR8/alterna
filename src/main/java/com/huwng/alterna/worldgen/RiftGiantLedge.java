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

        int count = 6 + random.nextInt(5); // 6 to 10 giant ledges per crack
        RiftGiantLedge[] result = new RiftGiantLedge[count];
        RiftLedge.Variant[] variants = RiftLedge.Variant.values();

        for (int i = 0; i < count; i++) {
            int side = random.nextBoolean() ? 1 : -1;
            double sCenter = (random.nextDouble() - 0.5) * 1.70;
            double sHalfLength = 25.0 + random.nextDouble() * 25.0; // Total length = 50 to 100 blocks
            int yTop = minLedgeY + (int) ((i + random.nextDouble() * 0.5) / count * (maxLedgeY - minLedgeY));
            double reachWidth = 20.0 + random.nextDouble() * 20.0;   // Extends 20 to 40 blocks into cavern
            double pedestalHeight = 12.0 + random.nextDouble() * 12.0; // Pedestal height = 12 to 24 blocks
            RiftLedge.Variant variant = variants[random.nextInt(variants.length)];
            result[i] = new RiftGiantLedge(side, sCenter, sHalfLength, yTop, reachWidth, pedestalHeight, variant);
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
        if (y > gl.yTop || y < gl.yTop - gl.pedestalHeight - 4) return false;

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
        if (distNorm > 0.55) {
            double edgeT = (distNorm - 0.55) / 0.45;
            slopeDrop = edgeT * edgeT * 3.5; // Outer edge slopes downwards by up to 3.5 blocks
        }
        double currentTopY = gl.yTop - slopeDrop;

        if (y > currentTopY) return false;

        double dy = gl.yTop - y; // distance below yTop

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
}
