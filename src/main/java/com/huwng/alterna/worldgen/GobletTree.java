package com.huwng.alterna.worldgen;

import com.huwng.alterna.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * One goblet tree: a giant tree growing sideways out of a rift wall, named for
 * its crown - a wart-block funnel holding a pool of water, like a goblet on a
 * stem.
 *
 * SHAPE, following the centreline from inside the rock outward:
 * 1. ROOT (horizontal): starts `embed` blocks BEHIND the wall point it was
 * given, at anchorY, and runs horizontally along (dirX, dirZ) - which
 * points in toward the rift's centreline - so that rootRun blocks of it
 * stick out past the face. The caller chooses embed such that the start is
 * guaranteed to be inside solid rock (see GiantCrackParams), which is what
 * makes the root reliably CONNECT to the wall instead of starting in
 * mid-air: the analytic wall point is only an estimate, but a root that
 * begins outside every possible carve boundary must cross the real face on
 * its way in, wherever that face turned out to be.
 * 2. BEND: a quarter circle of radius bendRadius rotating the centreline from
 * horizontal to vertical, consuming bendRadius blocks of horizontal
 * distance and bendRadius blocks of height.
 * 3. TRUNK (vertical): rises from the end of the bend to anchorY + height. If
 * the tree leans (see LEAN below) the trunk sweeps sideways along the way,
 * on a smoothstep curve so it joins the bend and reaches the crown without
 * a visible kink at either end.
 *
 * Thickness starts at trunkRadius * ROOT_THICKEN at the buried end, tapers to
 * trunkRadius by the time the bend finishes, then tapers to trunkRadius *
 * TRUNK_TOP_TAPER just under the crown. Root and bend cross-sections are
 * stamped in the plane perpendicular to the local tangent; the trunk's are
 * stamped horizontally (a lean of at most MAX_LEAN_SLOPE shears them
 * negligibly). Either way they're octagons rather than squares - see
 * TRUNK_CORNER_CUT.
 *
 * CROWN: a funnel centred on the trunk's top. Its surface is a paraboloid
 * rising CANOPY_LIP_HEIGHT blocks from the centre out to canopyRadius, built as
 * a CANOPY_SHELL_THICKNESS-thick shell of warped wart block, with the concave
 * interior filled with water - CANOPY_LIP_HEIGHT deep over the centre, tapering
 * to nothing at the rim. No separate containing rim is needed: the water
 * surface sits exactly at the height the bowl reaches at its narrowest possible
 * rim, so the outermost ring of the bowl is always already above the water line
 * and holds it in.
 *
 * LEAN: crowns are wide and anchors are picked blindly, so trees would
 * routinely grow their crowns into each other. withLean() tilts a tree away
 * from its neighbours - the trunk sweeps sideways and the crown, carried along,
 * tips in the same direction (tiltX/tiltZ add a linear ramp to the bowl
 * surface). The bowl is DEEPENED by exactly the drop the tilt introduces at the
 * rim, so a tilted goblet still holds its full CANOPY_LIP_HEIGHT of water on a
 * flat water surface rather than spilling it. GiantCrackParams drives the
 * search, using withLean() plus conflictsWith() to find a lean that clears
 * every tree already accepted.
 *
 * WALL ANCHOR / SIDE / SNORM: alongside the geometry above, each tree also
 * remembers the wall point it was hung from (wallX/wallZ/anchorY), which of
 * the rift's two long walls it's on (side, +-1), and where along the rift's
 * length that wall point sits (sNorm, in the same [-1, 1] units
 * GiantCrackParams
 * uses for its own boundary math). None of that feeds the tree's own shape -
 * it exists purely so GiantCrackParams.buildRootConnectors() can find, for any
 * two trees on the same wall, the same analytic wall contour between them
 * (via the shared solveWallPoint() helper) and lay a RootConnector along it.
 *
 * Everything here is a pure function of the stored parameters, and place()
 * writes only inside the chunk it's handed, so every chunk a tree overlaps can
 * build its own share independently, in any order - the same property that lets
 * the rift itself be carved chunk-by-chunk. See GobletTreeFeature for how
 * placement is scheduled, and GiantCrackParams.buildGobletTrees() for how
 * anchor points on the wall are chosen.
 */
final class GobletTree {

    // Vertical extent of the tree, from the root's anchor Y up to the crown's
    // centre.
    private static final int MIN_TREE_HEIGHT = 20;
    private static final int MAX_TREE_HEIGHT = 60;
    // Trunk half-thickness. The cross-section is an octagon, not a square: a
    // block at (u, v) from the axis is trunk only if |u| + |v| <= radius *
    // TRUNK_CORNER_CUT, which is what shaves the four corners off.
    private static final double MIN_TRUNK_RADIUS = 2.0;
    private static final double MAX_TRUNK_RADIUS = 4.0;
    private static final double TRUNK_CORNER_CUT = 1.5;
    // The root's horizontal run, i.e. how far it sticks out of the rock face
    // before it starts curving upward.
    private static final double MIN_ROOT_RUN = 5.0;
    private static final double MAX_ROOT_RUN = 10.0;
    // Radius of the quarter circle that turns the horizontal root into the
    // vertical trunk.
    private static final double MIN_BEND_RADIUS = 6.0;
    private static final double MAX_BEND_RADIUS = 10.0;
    // The root/base end is this much thicker than the trunk, tapering back to
    // trunk thickness by the time the bend finishes.
    private static final double ROOT_THICKEN = 1.6;
    private static final double TRUNK_TOP_TAPER = 0.7; // thickness multiplier just under the crown
    private static final double CURVE_STEP = 0.4; // arc-length step between stamped cross-sections
    // Crown funnel.
    private static final double MIN_CANOPY_RADIUS_FACTOR = 0.26; // of tree height
    private static final double MAX_CANOPY_RADIUS_FACTOR = 0.40;
    private static final double MIN_CANOPY_RADIUS = 7.0;
    private static final double MAX_CANOPY_RADIUS = 14.0;
    private static final int CANOPY_LIP_HEIGHT = 3; // how far the funnel curves up, centre -> rim
    private static final int CANOPY_SHELL_THICKNESS = 3; // must be >= the bowl's steepest 1-block-step slope
    private static final double CANOPY_EDGE_NOISE = 0.18; // ragged outline instead of a machined circle
    private static final double CANOPY_EDGE_NOISE_SCALE = 6.0;

    // ---- Lean, i.e. how trees dodge each other ----
    // Lean offsets tried, in blocks of sideways sweep from the trunk's base to
    // the crown, smallest first - a tree only leans as far as it must.
    static final double[] LEAN_MAGNITUDES = { 5.0, 9.0, 13.0, 17.0 };
    // Azimuths tried per magnitude, spread evenly around the circle from a
    // per-tree random phase.
    static final int LEAN_DIRECTIONS = 8;
    // Cap on the trunk's sweep, as a fraction of its vertical rise. Bounds how
    // far the horizontally-stamped trunk cross-sections shear.
    private static final double MAX_LEAN_SLOPE = 0.40;
    // How steeply the crown tips when the tree leans (blocks of rise per block
    // of horizontal distance across the bowl). Bounded so the bowl's shell
    // stays watertight at CANOPY_SHELL_THICKNESS.
    private static final double CANOPY_TILT = 0.22;
    // Slack demanded between two crowns, and between a crown and a stem.
    private static final double CROWN_GAP = 2.0;

    private final double startX;
    private final int anchorY;
    private final double startZ;
    private final double dirX;
    private final double dirZ;
    private final double rootRun; // the part that sticks out of the wall
    private final double horizontalRun; // embed + rootRun
    private final double bendRadius;
    private final int height;
    private final double trunkRadius;
    private final double canopyRadius;
    private final long seed;

    /**
     * Sideways sweep from the trunk's base to the crown - see LEAN in the class
     * doc.
     */
    private final double shiftX;
    private final double shiftZ;

    /** Where the bend hands off to the trunk, i.e. the un-leaned trunk axis. */
    private final double axisX;
    private final double axisZ;
    private final double bendTopY;
    private final double straightRise;

    private final int topY;
    private final double crownX;
    private final double crownZ;
    private final double tiltX;
    private final double tiltZ;
    private final double tiltDrop; // how far the tilt lowers the rim on the downhill side

    // ---- Wall anchor bookkeeping - see class doc "WALL ANCHOR / SIDE / SNORM"
    // ----
    // The wall point this tree was hung from (before subtracting embed), which
    // of the rift's two long walls it's on (+1 or -1), and its position along
    // the rift's length in the same sNorm units GiantCrackParams uses. These
    // don't affect this tree's own shape at all - they exist purely so
    // GiantCrackParams.buildRootConnectors() can match up neighbouring trees on
    // the same wall and re-solve the wall contour between them.
    private final double wallX;
    private final double wallZ;
    private final int side;
    private final double sNorm;

    private final int minChunkX, maxChunkX, minChunkZ, maxChunkZ;

    /**
     * Rolls one tree's proportions and hangs it off the wall point
     * (wallX, anchorY, wallZ), growing along the unit horizontal direction
     * (dirX, dirZ) - which must point in toward the rift's centreline. `embed`
     * is how far behind that wall point the root starts; the caller must make
     * it large enough that the start is inside solid rock.
     *
     * `side` and `sNorm` are passed straight through from the caller's own
     * wall-solve (see GiantCrackParams.buildGobletTrees()) purely so they can
     * be handed back later to buildRootConnectors() - this class never reads
     * them itself.
     *
     * The caller can still discard the result (e.g. if inwardExtent() doesn't
     * fit between the rift's walls); doing so is deterministic, since the
     * random draws happen here regardless.
     */
    static GobletTree create(RandomSource random, double wallX, int anchorY, double wallZ,
            double dirX, double dirZ, double embed, int side, double sNorm, boolean boostHeight) {
        int height = boostHeight ? (58 + random.nextInt(18)) : (MIN_TREE_HEIGHT + random.nextInt(MAX_TREE_HEIGHT - MIN_TREE_HEIGHT + 1));
        double trunkRadius = MIN_TRUNK_RADIUS + random.nextDouble() * (MAX_TRUNK_RADIUS - MIN_TRUNK_RADIUS);
        double rootRun = MIN_ROOT_RUN + random.nextDouble() * (MAX_ROOT_RUN - MIN_ROOT_RUN);
        double bendRadius = MIN_BEND_RADIUS + random.nextDouble() * (MAX_BEND_RADIUS - MIN_BEND_RADIUS);
        double canopyRadius = clamp(height * (MIN_CANOPY_RADIUS_FACTOR
                + random.nextDouble() * (MAX_CANOPY_RADIUS_FACTOR - MIN_CANOPY_RADIUS_FACTOR)),
                MIN_CANOPY_RADIUS, MAX_CANOPY_RADIUS);
        long seed = random.nextLong();

        return new GobletTree(wallX - dirX * embed, anchorY, wallZ - dirZ * embed, dirX, dirZ,
                rootRun, embed + rootRun, bendRadius, height, trunkRadius, canopyRadius, seed, 0.0, 0.0,
                wallX, wallZ, side, sNorm);
    }

    static GobletTree create(RandomSource random, double wallX, int anchorY, double wallZ,
            double dirX, double dirZ, double embed, int side, double sNorm) {
        return create(random, wallX, anchorY, wallZ, dirX, dirZ, embed, side, sNorm, false);
    }

    private GobletTree(double startX, int anchorY, double startZ, double dirX, double dirZ, double rootRun,
            double horizontalRun, double bendRadius, int height, double trunkRadius,
            double canopyRadius, long seed, double shiftX, double shiftZ,
            double wallX, double wallZ, int side, double sNorm) {
        this.startX = startX;
        this.anchorY = anchorY;
        this.startZ = startZ;
        this.dirX = dirX;
        this.dirZ = dirZ;
        this.rootRun = rootRun;
        this.horizontalRun = horizontalRun;
        this.bendRadius = bendRadius;
        this.height = height;
        this.trunkRadius = trunkRadius;
        this.canopyRadius = canopyRadius;
        this.seed = seed;
        this.wallX = wallX;
        this.wallZ = wallZ;
        this.side = side;
        this.sNorm = sNorm;

        double forward = horizontalRun + bendRadius;
        this.axisX = startX + dirX * forward;
        this.axisZ = startZ + dirZ * forward;
        this.bendTopY = anchorY + bendRadius;
        this.straightRise = Math.max(1.0, height - bendRadius);
        this.topY = anchorY + height;

        // Clamp the sweep so the trunk never leans steeper than MAX_LEAN_SLOPE.
        double requested = Math.sqrt(shiftX * shiftX + shiftZ * shiftZ);
        double allowed = MAX_LEAN_SLOPE * straightRise;
        double scale = requested > allowed && requested > 0.0 ? allowed / requested : 1.0;
        this.shiftX = shiftX * scale;
        this.shiftZ = shiftZ * scale;

        this.crownX = axisX + this.shiftX;
        this.crownZ = axisZ + this.shiftZ;

        // The crown tips in the lean's direction, by a fixed gradient rather
        // than one derived from the sweep - the trunk's smoothstep curve is
        // vertical again by the time it reaches the crown, so there's no
        // tangent to inherit.
        double leaned = Math.sqrt(this.shiftX * this.shiftX + this.shiftZ * this.shiftZ);
        if (leaned > 1.0E-6) {
            this.tiltX = CANOPY_TILT * this.shiftX / leaned;
            this.tiltZ = CANOPY_TILT * this.shiftZ / leaned;
            this.tiltDrop = CANOPY_TILT * crownOuter();
        } else {
            this.tiltX = 0.0;
            this.tiltZ = 0.0;
            this.tiltDrop = 0.0;
        }

        // Margin must cover not just the trunk/crown themselves but the furthest a
        // sapling could possibly reach from the trunk/root surface (see
        // SAPLING_MAX_EXTENT) -
        // otherwise chunks a sapling actually draws into never get visited at all,
        // which is what was silently truncating roots, stems, and canopies before.
        double margin = Math.max(trunkRadius * ROOT_THICKEN, crownOuter()) + SAPLING_MAX_EXTENT;
        double loX = Math.min(startX, Math.min(axisX, crownX)) - margin;
        double hiX = Math.max(startX, Math.max(axisX, crownX)) + margin;
        double loZ = Math.min(startZ, Math.min(axisZ, crownZ)) - margin;
        double hiZ = Math.max(startZ, Math.max(axisZ, crownZ)) + margin;
        this.minChunkX = ((int) Math.floor(loX)) >> 4;
        this.maxChunkX = ((int) Math.ceil(hiX)) >> 4;
        this.minChunkZ = ((int) Math.floor(loZ)) >> 4;
        this.maxChunkZ = ((int) Math.ceil(hiZ)) >> 4;
    }

    /** The same tree, leaning by (shiftX, shiftZ) blocks from base to crown. */
    GobletTree withLean(double newShiftX, double newShiftZ) {
        return new GobletTree(startX, anchorY, startZ, dirX, dirZ, rootRun, horizontalRun, bendRadius,
                height, trunkRadius, canopyRadius, seed, newShiftX, newShiftZ, wallX, wallZ, side, sNorm);
    }

    /**
     * How far in from the wall the tree reaches, out to the far side of
     * whichever of trunk or crown sits deepest. The rift has to be at least
     * this wide here or the tree would grow into the opposite wall.
     */
    double inwardExtent() {
        return Math.max(rootRun + bendRadius + trunkRadius, crownInward() + trunkRadius);
    }

    /** Inward distance from the wall to the crown's centre. */
    double crownInward() {
        return rootRun + bendRadius + (shiftX * dirX + shiftZ * dirZ);
    }

    /** Widest the crown's ragged outline can get. */
    double crownOuter() {
        return canopyRadius * (1.0 + CANOPY_EDGE_NOISE);
    }

    /** True if this tree's crown would run into the other tree's crown or stem. */
    boolean conflictsWith(GobletTree other) {
        return crownsOverlap(other) || crownHitsStemOf(other) || other.crownHitsStemOf(this);
    }

    private boolean crownsOverlap(GobletTree o) {
        if (crownBottomY() > o.crownTopY() || o.crownBottomY() > crownTopY()) {
            return false;
        }
        double dx = crownX - o.crownX;
        double dz = crownZ - o.crownZ;
        double need = crownOuter() + o.crownOuter() + CROWN_GAP;
        return dx * dx + dz * dz < need * need;
    }

    private boolean crownHitsStemOf(GobletTree o) {
        double lo = Math.max(crownBottomY(), o.anchorY);
        double hi = Math.min(crownTopY(), o.topY);
        if (lo > hi) {
            return false;
        }
        double need = crownOuter() + o.trunkRadius * ROOT_THICKEN + CROWN_GAP;
        for (int i = 0; i <= 2; i++) {
            double y = lo + (hi - lo) * i / 2.0;
            double[] p = o.stemPosAt(y);
            double dx = p[0] - crownX;
            double dz = p[1] - crownZ;
            if (dx * dx + dz * dz < need * need) {
                return true;
            }
        }
        return false;
    }

    private double crownBottomY() {
        return topY - CANOPY_SHELL_THICKNESS - 1;
    }

    private double crownTopY() {
        // Uphill side of a tilted rim is the highest point of the whole tree.
        return topY + CANOPY_LIP_HEIGHT + 2.0 * tiltDrop + 1.0;
    }

    /** Horizontal centre of the stem at world height y, as {x, z}. */
    private double[] stemPosAt(double y) {
        if (y <= bendTopY) {
            // Invert the bend: rise = bendRadius * (1 - cos a).
            double rise = clamp(y - anchorY, 0.0, bendRadius);
            double cos = 1.0 - rise / bendRadius;
            double sin = Math.sqrt(Math.max(0.0, 1.0 - cos * cos));
            double forward = horizontalRun + bendRadius * sin;
            return new double[] { startX + dirX * forward, startZ + dirZ * forward };
        }
        double e = clamp((y - bendTopY) / straightRise, 0.0, 1.0);
        double g = e * e * (3.0 - 2.0 * e); // smoothstep: no kink at either end of the sweep
        return new double[] { axisX + shiftX * g, axisZ + shiftZ * g };
    }

    boolean mightAffect(ChunkPos chunkPos) {
        return chunkPos.x() >= minChunkX && chunkPos.x() <= maxChunkX
                && chunkPos.z() >= minChunkZ && chunkPos.z() <= maxChunkZ;
    }

    /** Which of the rift's two long walls this tree hangs from (+1 or -1). */
    int side() {
        return side;
    }

    /**
     * Position along the rift's length this tree's wall anchor sits at, in the same
     * [-1, 1] sNorm units GiantCrackParams uses.
     */
    double sNorm() {
        return sNorm;
    }

    /**
     * World Y of the wall anchor (same value as anchorY - exposed under this name
     * for RootConnector matching).
     */
    int wallY() {
        return anchorY;
    }

    double wallX() {
        return wallX;
    }

    double wallZ() {
        return wallZ;
    }

    void place(WorldGenLevel level, ChunkPos chunkPos) {
        int chunkMinX = chunkPos.getMinBlockX();
        int chunkMaxX = chunkPos.getMaxBlockX();
        int chunkMinZ = chunkPos.getMinBlockZ();
        int chunkMaxZ = chunkPos.getMaxBlockZ();

        // Wood first, then the crown, whose leaves and water only ever fill
        // air: that lets the trunk show through the bottom of the bowl and
        // keeps water from being written into solid blocks.
        placeRootAndBend(level, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        placeTrunk(level, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        placeCanopy(level, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        // Small goblet saplings growing outward from trunk/root surface.
        placeSaplings(level, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
    }

    private void placeRootAndBend(WorldGenLevel level, int chunkMinX, int chunkMaxX,
            int chunkMinZ, int chunkMaxZ) {
        double bendEnd = horizontalRun + bendRadius * Math.PI * 0.5;

        for (double s = 0.0; s <= bendEnd; s += CURVE_STEP) {
            double angle;
            double px;
            double py;
            double pz;
            if (s < horizontalRun) {
                angle = 0.0;
                px = startX + dirX * s;
                pz = startZ + dirZ * s;
                py = anchorY;
            } else {
                angle = (s - horizontalRun) / bendRadius;
                double forward = horizontalRun + bendRadius * Math.sin(angle);
                px = startX + dirX * forward;
                pz = startZ + dirZ * forward;
                py = anchorY + bendRadius * (1.0 - Math.cos(angle));
            }

            // Thickest at the buried end, back to trunk thickness by the time
            // the bend finishes.
            double f = s / bendEnd;
            double radius = trunkRadius * (ROOT_THICKEN + (1.0 - ROOT_THICKEN) * f);
            if (outsideChunk(px, pz, radius, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ)) {
                continue;
            }

            // Root and bend stay in the vertical plane spanned by (dirX, dirZ)
            // and up, so one basis vector of the cross-section is the constant
            // horizontal perpendicular (dirZ, 0, -dirX). The other is the
            // in-plane normal, which rotates with the tangent: straight up
            // while the root is horizontal, back along -dir at the bend's top.
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);
            stampTiltedSection(level, px, py, pz, -sin * dirX, cos, -sin * dirZ, radius,
                    chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        }
    }

    private void placeTrunk(WorldGenLevel level, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        for (double y = bendTopY; y <= topY + 1.0E-9; y += 0.5) {
            double e = clamp((y - bendTopY) / straightRise, 0.0, 1.0);
            double radius = trunkRadius * (1.0 + (TRUNK_TOP_TAPER - 1.0) * e * e);
            double[] p = stemPosAt(y);
            if (outsideChunk(p[0], p[1], radius, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ)) {
                continue;
            }
            stampHorizontalSection(level, p[0], y, p[1], radius, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        }
    }

    /**
     * Cheap reject: true when a cross-section of this radius can't reach the
     * chunk. Most samples of a tall tree fall outside, so this is what keeps
     * the per-chunk cost down.
     */
    private static boolean outsideChunk(double px, double pz, double radius,
            int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        return px + radius + 1.0 < chunkMinX || px - radius - 1.0 > chunkMaxX
                || pz + radius + 1.0 < chunkMinZ || pz - radius - 1.0 > chunkMaxZ;
    }

    /**
     * Fills one octagonal cross-section in the plane spanned by the constant
     * horizontal basis vector (dirZ, 0, -dirX) and the supplied in-plane normal
     * (both unit length and mutually perpendicular). Sampled at half-block
     * steps because that basis is generally not axis-aligned, and whole-block
     * steps would leave gaps in the surface.
     */
    private void stampTiltedSection(WorldGenLevel level, double px, double py, double pz,
            double normalX, double normalY, double normalZ, double radius,
            int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        double cut = radius * TRUNK_CORNER_CUT;
        for (double u = -radius; u <= radius + 1.0E-9; u += 0.5) {
            for (double v = -radius; v <= radius + 1.0E-9; v += 0.5) {
                if (Math.abs(u) + Math.abs(v) > cut) {
                    continue; // shaves the corners off - the "cut corner" profile
                }
                int x = (int) Math.round(px + u * dirZ + v * normalX);
                int y = (int) Math.round(py + v * normalY);
                int z = (int) Math.round(pz - u * dirX + v * normalZ);
                if (x < chunkMinX || x > chunkMaxX || z < chunkMinZ || z > chunkMaxZ) {
                    continue;
                }
                placeWood(level, x, y, z);
            }
        }
    }

    /**
     * Same octagon, but axis-aligned in XZ - used for the (possibly leaning) trunk.
     */
    private void stampHorizontalSection(WorldGenLevel level, double px, double py, double pz, double radius,
            int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        double cut = radius * TRUNK_CORNER_CUT;
        int y = (int) Math.round(py);
        int minX = Math.max(chunkMinX, (int) Math.floor(px - radius));
        int maxX = Math.min(chunkMaxX, (int) Math.ceil(px + radius));
        int minZ = Math.max(chunkMinZ, (int) Math.floor(pz - radius));
        int maxZ = Math.min(chunkMaxZ, (int) Math.ceil(pz + radius));
        for (int x = minX; x <= maxX; x++) {
            double du = Math.abs(x + 0.5 - px);
            if (du > radius) {
                continue;
            }
            for (int z = minZ; z <= maxZ; z++) {
                double dv = Math.abs(z + 0.5 - pz);
                if (dv > radius || du + dv > cut) {
                    continue;
                }
                placeWood(level, x, y, z);
            }
        }
    }

    private void placeCanopy(WorldGenLevel level, int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        double outer = crownOuter();
        int minX = Math.max(chunkMinX, (int) Math.floor(crownX - outer));
        int maxX = Math.min(chunkMaxX, (int) Math.ceil(crownX + outer));
        int minZ = Math.max(chunkMinZ, (int) Math.floor(crownZ - outer));
        int maxZ = Math.min(chunkMaxZ, (int) Math.ceil(crownZ + outer));

        // Deepening the bowl by exactly the drop the tilt causes at the widest
        // possible rim keeps the downhill rim at or above the water line, so a
        // tilted goblet holds a full, flat pool instead of spilling it.
        double bowlDepth = CANOPY_LIP_HEIGHT + tiltDrop;
        int waterY = topY + CANOPY_LIP_HEIGHT;

        for (int x = minX; x <= maxX; x++) {
            double dx = x + 0.5 - crownX;
            for (int z = minZ; z <= maxZ; z++) {
                double dz = z + 0.5 - crownZ;
                double rho = Math.sqrt(dx * dx + dz * dz);
                if (rho > outer) {
                    continue;
                }

                // Ragged outline: the bowl's radius wobbles with coherent noise
                // so the crown isn't a perfect circle.
                double edge = canopyRadius * (1.0 + CANOPY_EDGE_NOISE
                        * GiantCrackParams.smoothNoise3D(x, topY, z, seed, CANOPY_EDGE_NOISE_SCALE));
                if (rho > edge) {
                    continue;
                }

                double f = rho / Math.max(1.0, edge);
                int surfaceY = (int) Math.round(topY + bowlDepth * f * f + tiltX * dx + tiltZ * dz);
                for (int dy = 0; dy < CANOPY_SHELL_THICKNESS; dy++) {
                    placeLeaf(level, x, surfaceY - dy, z);
                }
                for (int y = surfaceY + 1; y <= waterY; y++) {
                    placeWater(level, x, y, z);
                }
            }
        }
    }

    // ── Block placement helpers ─────────────────────────────────────────────

    /**
     * Places goblet log. ~15% chance of using stripped goblet log as a
     * natural bark-patch pattern, derived from block coordinates so the
     * result is deterministic and chunk-order-independent.
     */
    private static void placeWood(WorldGenLevel level, int x, int y, int z) {
        if (y <= level.getMinY() || y >= level.getMaxY() || !level.hasChunk(x >> 4, z >> 4))
            return;
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).is(Blocks.BEDROCK))
            return;
        BlockState state = useStripped(x, y, z, 0L)
                ? ModBlocks.GOBLET_STRIPPED_LOG.get().defaultBlockState()
                : ModBlocks.GOBLET_LOG.get().defaultBlockState();
        level.setBlock(pos, state, 2);
    }

    /**
     * Same as placeWood but used by the small saplings (always GOBLET_LOG, no
     * strip).
     */
    private static void placeSaplingWood(WorldGenLevel level, int x, int y, int z) {
        if (y <= level.getMinY() || y >= level.getMaxY() || !level.hasChunk(x >> 4, z >> 4))
            return;
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).is(Blocks.BEDROCK))
            return;
        level.setBlock(pos, ModBlocks.GOBLET_LOG.get().defaultBlockState(), 2);
    }

    private static void placeLeaf(WorldGenLevel level, int x, int y, int z) {
        if (y <= level.getMinY() || y >= level.getMaxY() || !level.hasChunk(x >> 4, z >> 4))
            return;
        BlockPos pos = new BlockPos(x, y, z);
        BlockState existing = level.getBlockState(pos);
        if (existing.isAir() || !existing.getFluidState().isEmpty()) {
            level.setBlock(pos, ModBlocks.GOBLET_MEMBRANE.get().defaultBlockState(), 2);
        }
    }

    private static void placeWater(WorldGenLevel level, int x, int y, int z) {
        if (y <= level.getMinY() || y >= level.getMaxY() || !level.hasChunk(x >> 4, z >> 4))
            return;
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).isAir()) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
        }
    }

    // ── Small goblet sapling generation ────────────────────────────────────
    //
    // 1–5 little saplings sprout from the parent tree's root or trunk surface:
    // root: 1–2 block thick, 4–7 block long, growing outward
    // trunk: 1–2 block thick, 5–9 block tall, curving upward
    // canopy: 2–4 block radius funnel of GOBLET_MEMBRANE, full of water
    //
    // Unlike the main tree, saplings never try to dodge solid blocks - they
    // just carve straight through, exactly like the parent root/trunk does.
    // That's deliberate: a "try to deflect, give up if still blocked" loop is
    // what used to make saplings wander off in random directions and quit
    // partway through. Every sapling below places its full rolled length,
    // every time.
    //
    // What they DO dodge is each other and the main crown: before a sapling's
    // blocks are ever placed, its whole path is grown "on paper" and checked
    // against the main crown and every previously-accepted sapling on this
    // same tree. A few alternate azimuths are tried around the same attach
    // point before the sapling is simply dropped, so overlaps get resolved
    // the same way every time this tree is generated, in any chunk order.

    // ── Small goblet sapling generation ────────────────────────────────────
    //
    // 1–5 small saplings sprout from the parent tree's root or trunk surface:
    //  - Root section: 2 blocks thick (with random organic corner cuts), 2–4 blocks long
    //  - Stem section: 2 blocks thick (with random organic corner cuts), 3–6 blocks high
    //  - Canopy: 2–4 blocks radius funnel of GOBLET_MEMBRANE, filled with water
    //  - Attachment: starts 0.5 blocks EMBEDDED inside the parent wood so it never floats
    //  - Wall avoidance: deflects away from rock walls into open rift air

    private static final int SAPLING_MIN_COUNT = 2;
    private static final int SAPLING_MAX_COUNT = 5;
    private static final double SAPLING_CROWN_CLEARANCE = 1.5;
    private static final double SAPLING_GAP = 2.0;
    private static final int SAPLING_MAX_ATTEMPTS = 6;
    private static final double SAPLING_MAX_EXTENT = 25.0;

    private void placeSaplings(WorldGenLevel level,
            int chunkMinX, int chunkMaxX,
            int chunkMinZ, int chunkMaxZ) {
        RandomSource rng = RandomSource.create(seed ^ 0xC0FFEEL);
        int count = SAPLING_MIN_COUNT + rng.nextInt(SAPLING_MAX_COUNT - SAPLING_MIN_COUNT + 1);

        List<double[]> acceptedCanopies = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            // Alternate evenly between trunk and root so both get saplings
            boolean onTrunk = (i % 2 == 0);
            double basePhi = rng.nextDouble() * Math.PI * 2.0;
            int rootLen = 2 + rng.nextInt(3);     // 2–4 blocks long
            int stemHeight = 3 + rng.nextInt(4);  // 3–6 blocks high
            int thickness = 2;                     // 2 blocks thick (with corner cuts)
            double miniRadius = 2.0 + rng.nextDouble() * 2.0; // 2–4 blocks wide
            double wobbleX = (rng.nextDouble() - 0.5) * 0.4;
            double wobbleZ = (rng.nextDouble() - 0.5) * 0.4;

            double[] base = onTrunk ? trunkAttachPoint(rng) : rootAttachPoint(rng);
            if (base == null) {
                base = rootAttachPoint(rng); // fallback to root if trunk too short
            }
            if (base == null) continue;

            double[][] acceptedPath = null;
            for (int attempt = 0; attempt < SAPLING_MAX_ATTEMPTS; attempt++) {
                double phi = basePhi + attempt * (Math.PI / 3.0);
                double[] out = onTrunk ? radialAroundVertical(phi) : radialAroundRoot(phi);

                // Start 0.5 blocks EMBEDDED inside parent wood so root never floats in mid-air
                double ax = base[0] + out[0] * (base[3] - 0.5);
                double ay = base[1] + out[1] * (base[3] - 0.5);
                double az = base[2] + out[2] * (base[3] - 0.5);

                if (overlapsMainCrown(ax, ay, az)) continue;

                double[][] path = growSaplingPath(level, ax, ay, az, out[0], out[1], out[2],
                        wobbleX, wobbleZ, rootLen, stemHeight);
                if (path.length < 2) continue; // blocked immediately at root

                double[] tip = path[path.length - 1];
                double canopyY = tip[1] + 1.0;

                if (overlapsMainCrown(tip[0], canopyY, tip[2])) continue;
                if (conflictsWithAccepted(tip[0], canopyY, tip[2], miniRadius, acceptedCanopies)) continue;
                if (pathHitsMainStemLate(path)) continue;

                acceptedPath = path;
                acceptedCanopies.add(new double[] { tip[0], canopyY, tip[2], miniRadius });
                break;
            }

            if (acceptedPath != null) {
                placeSaplingFromPath(level, acceptedPath, thickness, miniRadius,
                        chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
            }
        }
    }

    private boolean overlapsMainCrown(double x, double y, double z) {
        double dx = x - crownX;
        double dz = z - crownZ;
        double crownClear = crownOuter() + SAPLING_CROWN_CLEARANCE;
        return y >= crownBottomY() - 2.0 && dx * dx + dz * dz < crownClear * crownClear;
    }

    private boolean tooCloseToMainStem(double x, double y, double z) {
        double cy = clamp(y, anchorY, topY);
        double[] stem = stemPosAt(cy);
        double dx = x - stem[0];
        double dz = z - stem[1];
        double need = mainStemRadiusApprox(cy) + 0.8;
        return dx * dx + dz * dz < need * need;
    }

    private double mainStemRadiusApprox(double y) {
        if (y <= bendTopY) {
            return trunkRadius * ROOT_THICKEN;
        }
        double e = clamp((y - bendTopY) / straightRise, 0.0, 1.0);
        return trunkRadius * (1.0 + (TRUNK_TOP_TAPER - 1.0) * e * e);
    }

    private static boolean conflictsWithAccepted(double x, double y, double z, double radius,
            List<double[]> accepted) {
        for (double[] prev : accepted) {
            double dx = x - prev[0];
            double dy = y - prev[1];
            double dz = z - prev[2];
            double need = radius + prev[3] + SAPLING_GAP;
            if (dx * dx + dy * dy + dz * dz < need * need) {
                return true;
            }
        }
        return false;
    }

    /** Only checks late steps (step >= 3) to prevent stem from curving back INTO parent stem. */
    private boolean pathHitsMainStemLate(double[][] path) {
        for (int i = 3; i < path.length; i++) {
            double[] p = path[i];
            if (tooCloseToMainStem(p[0], p[1], p[2])) {
                return true;
            }
        }
        return false;
    }

    private double[] rootAttachPoint(RandomSource rng) {
        double s = rng.nextDouble() * horizontalRun;
        double px = startX + dirX * s;
        double pz = startZ + dirZ * s;
        return new double[] { px, anchorY, pz, trunkRadius * ROOT_THICKEN };
    }

    private double[] trunkAttachPoint(RandomSource rng) {
        double lo = bendTopY + 1.0;
        double hi = topY - 5.0;
        if (hi <= lo) {
            return rootAttachPoint(rng);
        }
        double y = lo + rng.nextDouble() * (hi - lo);
        double e = clamp((y - bendTopY) / straightRise, 0.0, 1.0);
        double radius = trunkRadius * (1.0 + (TRUNK_TOP_TAPER - 1.0) * e * e);
        double[] p = stemPosAt(y);
        return new double[] { p[0], y, p[1], radius };
    }

    private static double[] radialAroundVertical(double phi) {
        return new double[] { Math.cos(phi), 0.0, Math.sin(phi) };
    }

    private double[] radialAroundRoot(double phi) {
        double side = Math.cos(phi);
        double up = Math.abs(Math.sin(phi));
        double vx = dirZ * side;
        double vy = up;
        double vz = -dirX * side;
        double len = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (len < 1.0E-6) {
            return new double[] { dirZ, 0.0, -dirX };
        }
        return new double[] { vx / len, vy / len, vz / len };
    }

    /**
     * Grows sapling path with WALL AVOIDANCE (Né tường):
     * If a step hits solid stone/terrain, it deflects towards open rift air (dirX, dirZ, up).
     * If blocked repeatedly by wall, growth stops at the wall face.
     */
    private double[][] growSaplingPath(WorldGenLevel level,
            double ax, double ay, double az,
            double outX, double outY, double outZ,
            double wobbleX, double wobbleZ,
            int rootLen, int stemHeight) {
        int maxSteps = rootLen + stemHeight;
        List<double[]> steps = new ArrayList<>();

        double px = ax, py = ay, pz = az;
        double curDX = outX, curDY = outY, curDZ = outZ;

        for (int step = 0; step < maxSteps; step++) {
            double targetX, targetY, targetZ;

            if (step < rootLen) {
                double f = rootLen <= 1 ? 1.0 : (double) step / (rootLen - 1);
                double bit = 0.1 * f;
                targetX = curDX * (1.0 - bit) + wobbleX * bit;
                targetY = curDY * (1.0 - bit);
                targetZ = curDZ * (1.0 - bit) + wobbleZ * bit;
            } else {
                double e = stemHeight <= 1 ? 1.0 : (double) (step - rootLen) / (stemHeight - 1);
                double g = e * e * (3.0 - 2.0 * e);
                double outWeight = 1.0 - 0.5 * g;
                targetX = curDX * outWeight + wobbleX * (1.0 - outWeight);
                targetY = curDY * (1.0 - g) + g * 1.4;
                targetZ = curDZ * outWeight + wobbleZ * (1.0 - outWeight);
            }

            double len = Math.sqrt(targetX * targetX + targetY * targetY + targetZ * targetZ);
            if (len < 1.0E-6) break;
            targetX /= len; targetY /= len; targetZ /= len;

            // Check for wall / stone collision & deflect away into open rift air
            double nextX = px + targetX;
            double nextY = py + targetY;
            double nextZ = pz + targetZ;

            if (step > 1 && isSolidRock(level, nextX, nextY, nextZ)) {
                // Deflect vector towards rift center (dirX, 0.4, dirZ)
                targetX = targetX * 0.4 + dirX * 0.6;
                targetY = Math.max(targetY, 0.4);
                targetZ = targetZ * 0.4 + dirZ * 0.6;
                len = Math.sqrt(targetX * targetX + targetY * targetY + targetZ * targetZ);
                targetX /= len; targetY /= len; targetZ /= len;
                nextX = px + targetX; nextY = py + targetY; nextZ = pz + targetZ;

                if (isSolidRock(level, nextX, nextY, nextZ)) {
                    break; // blocked by deep wall rock - stop stem here
                }
            }

            px = nextX; py = nextY; pz = nextZ;
            steps.add(new double[] { px, py, pz });
        }

        return steps.toArray(new double[0][]);
    }

    private static boolean isSolidRock(WorldGenLevel level, double x, double y, double z) {
        int bx = (int) Math.round(x);
        int by = (int) Math.round(y);
        int bz = (int) Math.round(z);
        if (by <= level.getMinY() || by >= level.getMaxY()) return true;
        if (!level.hasChunk(bx >> 4, bz >> 4)) return true;
        BlockPos pos = new BlockPos(bx, by, bz);
        BlockState st = level.getBlockState(pos);
        if (st.isAir() || st.is(ModBlocks.GOBLET_LOG.get()) || st.is(ModBlocks.GOBLET_STRIPPED_LOG.get())
                || st.is(ModBlocks.GOBLET_WOOD.get()) || st.is(ModBlocks.GOBLET_MEMBRANE.get())) {
            return false;
        }
        return st.isSolid();
    }

    private void placeSaplingFromPath(WorldGenLevel level, double[][] path, int thickness, double miniRadius,
            int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        for (double[] p : path) {
            if (p[0] < chunkMinX - 2 || p[0] > chunkMaxX + 2 || p[2] < chunkMinZ - 2 || p[2] > chunkMaxZ + 2) {
                continue;
            }
            placeThickStem(level, p[0], p[1], p[2], thickness, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
        }
        double[] tip = path[path.length - 1];
        placeMiniCanopy(level, tip[0], tip[1] + 1.0, tip[2], miniRadius, chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ);
    }

    /**
     * Stamps a 3D stem cross-section, 2x2x2 blocks thick with random organic corner cuts
     * so it forms a round 3D cylinder rather than a flat 1-layer slice.
     */
    private static void placeThickStem(WorldGenLevel level, double px, double py, double pz, int thickness,
            int chunkMinX, int chunkMaxX, int chunkMinZ, int chunkMaxZ) {
        int baseX = (int) Math.floor(px - 0.5);
        int baseY = (int) Math.floor(py - 0.5);
        int baseZ = (int) Math.floor(pz - 0.5);

        for (int dx = 0; dx <= 1; dx++) {
            int x = baseX + dx;
            if (x < chunkMinX || x > chunkMaxX) continue;
            for (int dy = 0; dy <= 1; dy++) {
                int y = baseY + dy;
                if (y <= level.getMinY() || y >= level.getMaxY()) continue;
                for (int dz = 0; dz <= 1; dz++) {
                    int z = baseZ + dz;
                    if (z < chunkMinZ || z > chunkMaxZ) continue;

                    // Organic 3D corner cut: skip when 2 or more offsets are 1 (~25% chance)
                    if ((dx + dy + dz >= 2) && isCornerCut(x, y, z)) {
                        continue;
                    }
                    placeSaplingWood(level, x, y, z);
                }
            }
        }
    }

    private static boolean isCornerCut(int x, int y, int z) {
        long h = (x * 31213L + y * 99989L + z * 65537L);
        return (h & 3L) == 0L; // 25% corner cut
    }

    /**
     * Places a small paraboloid funnel of GOBLET_MEMBRANE with water in its basin
     * (radius 2-4 blocks).
     */
    private static void placeMiniCanopy(WorldGenLevel level,
            double cx, double cy, double cz,
            double radius,
            int chunkMinX, int chunkMaxX,
            int chunkMinZ, int chunkMaxZ) {
        int miniLipH = Math.max(1, (int) Math.round(radius * 0.5));
        int waterY = (int) Math.round(cy) + miniLipH;

        int minX = Math.max(chunkMinX, (int) Math.floor(cx - radius) - 1);
        int maxX = Math.min(chunkMaxX, (int) Math.ceil(cx + radius) + 1);
        int minZ = Math.max(chunkMinZ, (int) Math.floor(cz - radius) - 1);
        int maxZ = Math.min(chunkMaxZ, (int) Math.ceil(cz + radius) + 1);

        for (int x = minX; x <= maxX; x++) {
            double dx = x + 0.5 - cx;
            for (int z = minZ; z <= maxZ; z++) {
                double dz = z + 0.5 - cz;
                double rho = Math.sqrt(dx * dx + dz * dz);
                if (rho > radius + 0.5)
                    continue;

                double f = rho / Math.max(1.0, radius);
                int surfY = (int) Math.round(cy + miniLipH * f * f);

                // Force the shell in, unlike the big crown's placeLeaf (air/fluid only) -
                // a mini canopy sits right on top of its own stem, so "only fill air"
                // used to leave the bottom of the bowl missing wherever the stem's last
                // block poked up into it. Shell first, water after, same as the big crown.
                placeMembraneForce(level, x, surfY, z);
                placeMembraneForce(level, x, surfY - 1, z);

                for (int y = surfY + 1; y <= waterY; y++) {
                    placeWater(level, x, y, z);
                }
            }
        }
    }

    private static void placeMembraneForce(WorldGenLevel level, int x, int y, int z) {
        if (y <= level.getMinY() || y >= level.getMaxY())
            return;
        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).is(Blocks.BEDROCK))
            return;
        level.setBlock(pos, ModBlocks.GOBLET_MEMBRANE.get().defaultBlockState(), 2);
    }

    // ── Utilities ───────────────────────────────────────────────────────────

    /**
     * Returns true ~15% of the time based on a stable hash of the block
     * position XORed with an extra salt. Used to scatter stripped-log
     * bark patches without a mutable RNG.
     */
    private static boolean useStripped(int x, int y, int z, long salt) {
        long h = (x * 1000003L + y * 999983L + z * 999979L) ^ salt;
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h & 0xFFL) < 38L; // 38/256 ≈ 15 %
    }

    private static double clamp(double v, double min, double max) {
        return v < min ? min : Math.min(v, max);
    }
}