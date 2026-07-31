package com.huwng.alterna.worldgen;

import net.minecraft.util.RandomSource;
import java.util.ArrayList;
import java.util.List;

public final class RiftBridge {
    public final double sCenter;         // Normalized position along length (-0.75 to +0.75)
    public final int yCenter;            // Center Y height of the bridge
    public final double sHalfWidth;      // Length-wise thickness (4 to 9 blocks)
    public final double yTilt;           // Height offset from side -1 to side +1 (-6 to +6 blocks)
    public final double midThickness;    // Middle thickness (2.5 to 3.5 blocks - slim middle)
    public final double wallAnchorDepth; // Wall attachment depth (8 to 14 blocks - thick ends)
    public final RiftLedge.Variant variant; // Variant type for decoration & hollow features

    public RiftBridge(double sCenter, int yCenter, double sHalfWidth, double yTilt, double midThickness, double wallAnchorDepth, RiftLedge.Variant variant) {
        this.sCenter = sCenter;
        this.yCenter = yCenter;
        this.sHalfWidth = sHalfWidth;
        this.yTilt = yTilt;
        this.midThickness = midThickness;
        this.wallAnchorDepth = wallAnchorDepth;
        this.variant = variant;
    }

    public static RiftBridge[] buildBridges(RandomSource random, int originY, int depth, GobletTree[] gobletTrees) {
        int minBridgeY = Math.max(-330, originY - depth + 40);
        int maxBridgeY = Math.min(60, originY - 68); // Never spawn above Y=60 or ground level
        if (maxBridgeY <= minBridgeY + 20) return new RiftBridge[0];

        int targetCount = 18 + random.nextInt(10); // 18 to 27 bridges (increased quantity)
        List<RiftBridge> resultList = new ArrayList<>();
        RiftLedge.Variant[] variants = RiftLedge.Variant.values();

        for (int i = 0; i < targetCount; i++) {
            double sCenter = (random.nextDouble() - 0.5) * 1.5;
            int yCenter = minBridgeY + (int) ((i + random.nextDouble() * 0.5) / targetCount * (maxBridgeY - minBridgeY));

            // Filter out bridges that would spawn close to any GobletTree
            boolean tooCloseToTree = false;
            if (gobletTrees != null) {
                for (GobletTree tree : gobletTrees) {
                    double sGap = Math.abs(sCenter - tree.sNorm());
                    double yGap = Math.abs(yCenter - tree.wallY());
                    if (sGap < 0.20 && yGap < 16.0) {
                        tooCloseToTree = true;
                        break;
                    }
                }
            }

            if (tooCloseToTree) {
                continue;
            }

            double sHalfWidth = 4.0 + random.nextDouble() * 5.0;
            double yTilt = (random.nextDouble() - 0.5) * 12.0;
            double midThickness = 2.5 + random.nextDouble() * 1.0;     // Slim 2.5 - 3.5 blocks in the middle
            double wallAnchorDepth = 8.0 + random.nextDouble() * 6.0;   // Thick 8 - 14 blocks at the wall ends
            RiftLedge.Variant variant = variants[random.nextInt(variants.length)];
            resultList.add(new RiftBridge(sCenter, yCenter, sHalfWidth, yTilt, midThickness, wallAnchorDepth, variant));
        }
        return resultList.toArray(new RiftBridge[0]);
    }

    public static boolean isInsideBridge(RiftBridge br, double localA, double localB, int x, int y, int z, double halfLength, double effectiveHalfWidth, long roughnessSeed) {
        double ds = Math.abs(localA - br.sCenter * halfLength);
        if (ds > br.sHalfWidth) return false;

        double lengthFade = Math.cos((ds / br.sHalfWidth) * (Math.PI / 2.0));
        double wNorm = GiantCrackParams.clamp(localB / Math.max(1.0, effectiveHalfWidth), -1.0, 1.0);
        double yMid = br.yCenter + wNorm * br.yTilt;

        // Flat top walk surface
        double bridgeTop = yMid + 1.0;
        
        // Reversed arch profile: slim in the middle (midThickness), thick & heavy at two wall attachment ends (wallAnchorDepth)
        double wSq = wNorm * wNorm; // 0 in middle, 1.0 at walls
        double currentThickness = br.midThickness + (br.wallAnchorDepth - br.midThickness) * wSq;
        double archBottom = bridgeTop - currentThickness * lengthFade;

        double bottomNoise = GiantCrackParams.smoothNoise3D(x, y, z, roughnessSeed ^ 0xBF1D6E7L, 0.15) * 1.2;
        double topNoise = GiantCrackParams.smoothNoise3D(x, y, z, roughnessSeed ^ 0x9E3779BL, 0.2) * 0.2;

        return y >= (archBottom - bottomNoise) && y <= (bridgeTop + topNoise);
    }

    public static boolean isInsideHollowBasin(RiftBridge br, double localA, double localB, int x, int y, int z, double halfLength, double effectiveHalfWidth) {
        if (br.variant != RiftLedge.Variant.HOLLOW) return false;

        double ds = Math.abs(localA - br.sCenter * halfLength);
        // Leave a solid 2.2-block rim on both side edges along A
        if (ds > br.sHalfWidth - 2.2) return false;

        double wNorm = GiantCrackParams.clamp(localB / Math.max(1.0, effectiveHalfWidth), -1.0, 1.0);
        // Leave solid wall anchor rock/clay on both ends along B
        if (Math.abs(wNorm) > 0.75) return false;

        // Containment check: Ensure bridge exists at least 2 blocks further out in all 4 cardinal directions
        if (!isInsideBridge(br, localA + 2.0, localB, x, y, z, halfLength, effectiveHalfWidth, 0L) ||
            !isInsideBridge(br, localA - 2.0, localB, x, y, z, halfLength, effectiveHalfWidth, 0L) ||
            !isInsideBridge(br, localA, localB + 2.0, x, y, z, halfLength, effectiveHalfWidth, 0L) ||
            !isInsideBridge(br, localA, localB - 2.0, x, y, z, halfLength, effectiveHalfWidth, 0L)) {
            return false;
        }

        double yMid = br.yCenter + wNorm * br.yTilt;
        int troughTopY = (int) Math.floor(yMid + 1.0);
        int troughFloorY = troughTopY - 1;

        return y >= troughFloorY && y <= troughTopY;
    }

    public static boolean isHollowPartitionWall(RiftBridge br, double localA, double localB, int x, int y, int z, double halfLength, double effectiveHalfWidth) {
        if (!isInsideHollowBasin(br, localA, localB, x, y, z, halfLength, effectiveHalfWidth)) return false;

        double wNorm = GiantCrackParams.clamp(localB / Math.max(1.0, effectiveHalfWidth), -1.0, 1.0);
        long bPos = Math.round(localB);

        // End dams near canyon walls OR periodic partition walls every 7 blocks along B
        boolean isEndDam = Math.abs(wNorm) >= 0.70;
        boolean isPeriodicWall = (Math.abs(bPos) % 7 == 0);

        return isEndDam || isPeriodicWall;
    }

    public static boolean isHollowFloor(RiftBridge br, double localA, double localB, int x, int y, int z, double halfLength, double effectiveHalfWidth) {
        if (!isInsideHollowBasin(br, localA, localB, x, y, z, halfLength, effectiveHalfWidth)) return false;
        return !isInsideHollowBasin(br, localA, localB, x, y - 1, z, halfLength, effectiveHalfWidth);
    }

    public static boolean isHollowRim(RiftBridge br, double localA, double localB, int x, int y, int z, double halfLength, double effectiveHalfWidth) {
        if (br.variant != RiftLedge.Variant.HOLLOW) return false;
        double wNorm = GiantCrackParams.clamp(localB / Math.max(1.0, effectiveHalfWidth), -1.0, 1.0);
        double yMid = br.yCenter + wNorm * br.yTilt;
        int troughTopY = (int) Math.floor(yMid + 1.0);
        if (y != troughTopY) return false;

        if (isInsideHollowBasin(br, localA, localB, x, y, z, halfLength, effectiveHalfWidth)) return false;

        return isInsideHollowBasin(br, localA + 1, localB, x, y, z, halfLength, effectiveHalfWidth) ||
               isInsideHollowBasin(br, localA - 1, localB, x, y, z, halfLength, effectiveHalfWidth) ||
               isInsideHollowBasin(br, localA, localB + 1, x, y, z, halfLength, effectiveHalfWidth) ||
               isInsideHollowBasin(br, localA, localB - 1, x, y, z, halfLength, effectiveHalfWidth);
    }
}
