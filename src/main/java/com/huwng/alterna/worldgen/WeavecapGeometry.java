package com.huwng.alterna.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Weavecap - the giant woody mushroom trees of the Lichen Savanna.
 *
 * A trunk is not one log column but a bundle of root "strands" that start splayed
 * out on the ground, twist around each other (half of them clockwise, half
 * anticlockwise, so they visibly weave through one another) and pinch into a waist
 * before flaring out again to hold up a wide, tilted, flat cap. Big trees grow 1-2
 * extra side shelves on branching strands.
 *
 * The geometry is a pure function of the seed, so the same tree can be rasterised
 * one chunk at a time ({@code clip}, see WeavecapTree) from any chunk it overlaps - this is what lets
 * 40-60 block giants generate without "setBlock in a far chunk" problems.
 */
public final class WeavecapGeometry {
    public enum Size { SMALL, MEDIUM, GIANT }

    private record Capsule(double x0, double y0, double z0, double r0, double x1, double y1, double z1, double r1) {}
    private record Cap(double cx, double cy, double cz, double radius, double thickness, double nx, double ny, double nz, long noiseSeed) {}

    private final List<Capsule> strands = new ArrayList<>();
    private final List<Cap> caps = new ArrayList<>();
    private int minX, minY, minZ, maxX, maxY, maxZ;

    private WeavecapGeometry() {}

    /** Max horizontal reach from the trunk base, used to find which trees touch a chunk. */
    public static int maxReach(Size size) {
        return switch (size) { case SMALL -> 8; case MEDIUM -> 16; case GIANT -> 30; };
    }

    public static WeavecapGeometry build(long seed, BlockPos base, Size size) {
        WeavecapGeometry t = new WeavecapGeometry();
        RandomSource r = RandomSource.create(seed);
        int height; int strandCount; double baseSpread; double waist; double capRadius; double capThick;
        switch (size) {
            case SMALL -> { height = 7 + r.nextInt(5); strandCount = 3; baseSpread = 2.0; waist = 0.8; capRadius = 4.0 + r.nextDouble() * 1.5; capThick = 2.0; }
            case MEDIUM -> { height = 15 + r.nextInt(9); strandCount = 5 + r.nextInt(2); baseSpread = 5.0; waist = 1.8; capRadius = 9.0 + r.nextDouble() * 3.0; capThick = 2.5; }
            default -> { height = 36 + r.nextInt(18); strandCount = 7 + r.nextInt(3); baseSpread = 10.0; waist = 3.0; capRadius = 18.0 + r.nextDouble() * 6.0; capThick = 3.5; }
        }
        // the whole tree leans a little, like the windswept caps in the savanna
        double leanAngle = r.nextDouble() * Math.PI * 2.0;
        double leanAmount = height * (0.06 + r.nextDouble() * 0.12);
        double topX = base.getX() + 0.5 + Math.cos(leanAngle) * leanAmount;
        double topZ = base.getZ() + 0.5 + Math.sin(leanAngle) * leanAmount;
        double topY = base.getY() + height;
        double flare = Math.max(1.5, capRadius * 0.35);
        double thick = size == Size.GIANT ? 1.45 : size == Size.MEDIUM ? 1.05 : 0.8;

        int segs = Math.max(8, height);
        for (int s = 0; s < strandCount; s++) {
            double a0 = (Math.PI * 2.0 * s) / strandCount + r.nextDouble() * 0.5;
            double twist = (s % 2 == 0 ? 1.0 : -1.0) * (1.2 + r.nextDouble() * 1.4);
            double wob = r.nextDouble() * 10.0;
            double prevX = 0, prevY = 0, prevZ = 0, prevR = 0;
            // ground root running outwards first
            double rootLen = baseSpread * (1.4 + r.nextDouble() * 0.8);
            double rx = base.getX() + 0.5 + Math.cos(a0) * rootLen, rz = base.getZ() + 0.5 + Math.sin(a0) * rootLen;
            double bx = base.getX() + 0.5 + Math.cos(a0) * baseSpread, bz = base.getZ() + 0.5 + Math.sin(a0) * baseSpread;
            t.strands.add(new Capsule(rx, base.getY() - 1.5, rz, thick * 0.7, bx, base.getY() + 0.5, bz, thick * 1.15));
            for (int i = 0; i <= segs; i++) {
                double f = (double) i / segs;
                // radius profile: splayed base -> waist at ~60% -> flare under the cap
                double rad;
                if (f < 0.6) rad = baseSpread + (waist - baseSpread) * smooth(f / 0.6);
                else rad = waist + (flare - waist) * smooth((f - 0.6) / 0.4);
                double ang = a0 + twist * f + Math.sin(f * 6.0 + wob) * 0.15;
                double cx = (base.getX() + 0.5) + (topX - base.getX() - 0.5) * f;
                double cz = (base.getZ() + 0.5) + (topZ - base.getZ() - 0.5) * f;
                double x = cx + Math.cos(ang) * rad;
                double z = cz + Math.sin(ang) * rad;
                double y = base.getY() + 0.5 + (topY - base.getY()) * f;
                double sr = thick * (1.15 - 0.35 * f);
                if (i > 0) t.strands.add(new Capsule(prevX, prevY, prevZ, prevR, x, y, z, sr));
                prevX = x; prevY = y; prevZ = z; prevR = sr;
            }
            // radial rib under the cap
            double ribA = a0 + twist;
            t.strands.add(new Capsule(prevX, prevY, prevZ, thick * 0.7,
                    topX + Math.cos(ribA) * capRadius * 0.7, topY - capThick * 0.6, topZ + Math.sin(ribA) * capRadius * 0.7, thick * 0.45));
        }
        // main cap: tilted slab, with an irregular rim
        double tilt = 0.05 + r.nextDouble() * 0.12;
        double tiltDir = leanAngle + (r.nextDouble() - 0.5);
        double nx = Math.sin(tilt) * Math.cos(tiltDir), nz = Math.sin(tilt) * Math.sin(tiltDir), ny = Math.cos(tilt);
        t.caps.add(new Cap(topX, topY + capThick * 0.5, topZ, capRadius, capThick, nx, ny, nz, r.nextLong()));

        // side shelves on medium/giant trees
        int shelves = size == Size.GIANT ? 1 + r.nextInt(2) : size == Size.MEDIUM && r.nextBoolean() ? 1 : 0;
        for (int k = 0; k < shelves; k++) {
            double f = 0.32 + r.nextDouble() * 0.18 + k * 0.12;
            double ang = r.nextDouble() * Math.PI * 2.0;
            double sx = (base.getX() + 0.5) + (topX - base.getX() - 0.5) * f;
            double sz = (base.getZ() + 0.5) + (topZ - base.getZ() - 0.5) * f;
            double sy = base.getY() + height * f;
            double out = capRadius * (0.85 + r.nextDouble() * 0.3);
            double ex = sx + Math.cos(ang) * out, ez = sz + Math.sin(ang) * out, ey = sy + 3.0 + r.nextDouble() * 3.0;
            t.strands.add(new Capsule(sx, sy, sz, thick * 1.0, ex, ey, ez, thick * 0.75));
            double shelfR = capRadius * (0.38 + r.nextDouble() * 0.14);
            t.caps.add(new Cap(ex, ey + 1.0, ez, shelfR, Math.max(2.0, capThick - 1.0),
                    Math.cos(ang) * 0.18, 0.98, Math.sin(ang) * 0.18, r.nextLong()));
        }
        t.computeBounds();
        return t;
    }

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    private void computeBounds() {
        minX = minY = minZ = Integer.MAX_VALUE; maxX = maxY = maxZ = Integer.MIN_VALUE;
        for (Capsule c : strands) {
            double r = Math.max(c.r0, c.r1) + 1;
            grow(Math.min(c.x0, c.x1) - r, Math.min(c.y0, c.y1) - r, Math.min(c.z0, c.z1) - r);
            grow(Math.max(c.x0, c.x1) + r, Math.max(c.y0, c.y1) + r, Math.max(c.z0, c.z1) + r);
        }
        for (Cap c : caps) {
            double r = c.radius * 1.25 + 2;
            grow(c.cx - r, c.cy - r * 0.5, c.cz - r);
            grow(c.cx + r, c.cy + r * 0.5, c.cz + r);
        }
    }

    private void grow(double x, double y, double z) {
        minX = Math.min(minX, (int) Math.floor(x)); minY = Math.min(minY, (int) Math.floor(y)); minZ = Math.min(minZ, (int) Math.floor(z));
        maxX = Math.max(maxX, (int) Math.ceil(x)); maxY = Math.max(maxY, (int) Math.ceil(y)); maxZ = Math.max(maxZ, (int) Math.ceil(z));
    }

    /** Underside of the main cap: below this the tree is its woven root strands. */
    public int rootTopY() {
        if (caps.isEmpty()) return maxY;
        Cap c = caps.get(0);
        return (int) Math.floor(c.cy - c.thickness);
    }

    public BoundingBox bounds() {
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    // ------------------------------------------------------------------ rasterising
    /** What a voxel of the tree is made of. */
    public enum Kind { LOG_X, LOG_Y, LOG_Z, WOOD, CAP, GILLS, MOSS }

    /** Destination for the rasterised voxels (a level, or a plain map in tests). */
    public interface Sink {
        /** 0 = solid/blocked, 1 = free for the tree, 2 = free for roots only (soil), 3 = already this tree's wood, 4 = this tree's cap */
        int status(int x, int y, int z);
        void put(int x, int y, int z, Kind kind);
    }

    public void place(Sink sink, BoundingBox clip) {
        BoundingBox box = this.bounds();
        if (clip != null) {
            if (!box.intersects(clip)) return;
            box = new BoundingBox(Math.max(box.minX(), clip.minX()), Math.max(box.minY(), clip.minY()), Math.max(box.minZ(), clip.minZ()),
                    Math.min(box.maxX(), clip.maxX()), Math.min(box.maxY(), clip.maxY()), Math.min(box.maxZ(), clip.maxZ()));
        }
        // caps first, then wood so strands punch through the gills
        for (Cap c : caps) rasterCap(sink, c, box);
        for (Capsule c : strands) rasterCapsule(sink, c, box);
    }

    private void rasterCapsule(Sink sink, Capsule c, BoundingBox box) {
        double r = Math.max(c.r0, c.r1);
        int x0 = Math.max(box.minX(), (int) Math.floor(Math.min(c.x0, c.x1) - r));
        int x1 = Math.min(box.maxX(), (int) Math.ceil(Math.max(c.x0, c.x1) + r));
        int y0 = Math.max(box.minY(), (int) Math.floor(Math.min(c.y0, c.y1) - r));
        int y1 = Math.min(box.maxY(), (int) Math.ceil(Math.max(c.y0, c.y1) + r));
        int z0 = Math.max(box.minZ(), (int) Math.floor(Math.min(c.z0, c.z1) - r));
        int z1 = Math.min(box.maxZ(), (int) Math.ceil(Math.max(c.z0, c.z1) + r));
        if (x0 > x1 || y0 > y1 || z0 > z1) return;
        double dx = c.x1 - c.x0, dy = c.y1 - c.y0, dz = c.z1 - c.z0;
        double len2 = dx * dx + dy * dy + dz * dz;
        Kind log = Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) >= Math.abs(dz) ? Kind.LOG_Y
                : Math.abs(dx) >= Math.abs(dz) ? Kind.LOG_X : Kind.LOG_Z;
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) {
            double px = x + 0.5 - c.x0, py = y + 0.5 - c.y0, pz = z + 0.5 - c.z0;
            double t = len2 < 1e-6 ? 0 : Math.max(0, Math.min(1, (px * dx + py * dy + pz * dz) / len2));
            double qx = px - dx * t, qy = py - dy * t, qz = pz - dz * t;
            double rr = c.r0 + (c.r1 - c.r0) * t;
            double d2 = qx * qx + qy * qy + qz * qz;
            if (d2 > rr * rr) continue;
            int st = sink.status(x, y, z);
            if (st == 3) {
                sink.put(x, y, z, Kind.WOOD);             // where strands cross: solid wood knot
            } else if (st != 0) {
                sink.put(x, y, z, d2 > (rr - 1) * (rr - 1) ? log : Kind.WOOD);
            }
        }
    }

    private void rasterCap(Sink sink, Cap c, BoundingBox box) {
        double R = c.radius * 1.25 + 1;
        int x0 = Math.max(box.minX(), (int) Math.floor(c.cx - R)), x1 = Math.min(box.maxX(), (int) Math.ceil(c.cx + R));
        int z0 = Math.max(box.minZ(), (int) Math.floor(c.cz - R)), z1 = Math.min(box.maxZ(), (int) Math.ceil(c.cz + R));
        int y0 = Math.max(box.minY(), (int) Math.floor(c.cy - R * 0.4 - c.thickness)), y1 = Math.min(box.maxY(), (int) Math.ceil(c.cy + R * 0.4 + c.thickness));
        if (x0 > x1 || y0 > y1 || z0 > z1) return;
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            double ox = x + 0.5 - c.cx, oz = z + 0.5 - c.cz;
            double ang = Math.atan2(oz, ox);
            // wavy, lobed rim
            double rim = c.radius * (1.0 + 0.16 * Math.sin(ang * 3.0 + (c.noiseSeed & 7)) + 0.09 * Math.sin(ang * 7.0 + (c.noiseSeed >> 3 & 7)));
            for (int y = y0; y <= y1; y++) {
                double oy = y + 0.5 - c.cy;
                double h = ox * c.nx + oy * c.ny + oz * c.nz;                 // height along the cap normal
                double rx = ox - c.nx * h, ry = oy - c.ny * h, rz = oz - c.nz * h;
                double radial = Math.sqrt(rx * rx + ry * ry + rz * rz);
                if (radial > rim) continue;
                double edge = radial / rim;
                // thick in the middle, thin and slightly drooping at the rim
                double half = c.thickness * 0.5 * (1.0 - 0.35 * edge * edge * edge);
                double center = -edge * edge * edge * c.thickness * 0.5;
                if (h > center + half || h < center - half) continue;
                if (sink.status(x, y, z) != 1) continue;
                boolean bottom = h < center - half + 1.0;
                boolean top = h > center + half - 1.0;
                if (bottom) sink.put(x, y, z, Kind.GILLS);
                else sink.put(x, y, z, Kind.CAP);
            }
        }
    }

}
