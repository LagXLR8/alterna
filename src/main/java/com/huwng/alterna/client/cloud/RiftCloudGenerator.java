package com.huwng.alterna.client.cloud;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * CPU half of the rift cloud system: a straight port of Better Clouds'
 * generation model (its ChunkedGenerator/Sampler classes) scoped down to a
 * patch over the giant crack instead of the whole sky.
 *
 * Better Clouds' recipe, mirrored here with its default config values:
 *  - candidate points on a dense grid, GRID_SPACING = 5.25 blocks apart
 *    (thousands of small overlapping puffs, NOT a few big ones - this
 *    density is what its whole look comes from);
 *  - each point jittered by up to one full cell (randomPlacement = 1.0);
 *  - a smooth coverage noise field drives a per-puff LIFT with a pointiness exponent
 *    (default 3): flat wispy edges, tall bulging cores - the cumulus
 *    silhouette;
 *  - the whole field travels slowly along +X (travelSpeed = 0.03
 *    blocks/tick), which is why everything is computed in "cloud space"
 *    (world + total drift): the pattern is stable in cloud space and the
 *    world slides under it.
 *
 * The one addition BC has no equivalent of: a candidate only becomes a puff
 * if the world is actually open (carved air) at cloud height there, so the
 * layer precisely fills the rift's own footprint and never bleeds over
 * solid terrain around it.
 *
 * Output is packed straight into the texel buffer consumed by
 * rift_clouds.vsh - two RGBA8 texels per puff:
 *   texel 0: x_lo, x_hi, z_lo, z_hi  (u16 cloud-space offset / PATCH_SIZE)
 *   texel 1: lift, size, colorNoise, 255
 */
final class RiftCloudGenerator {

    // ---- Better Clouds defaults (see Config.class in its jar) ----
    static final float GRID_SPACING = 5.25F;
    static final float SIZE_XZ = 16.0F;
    static final float SIZE_Y = 6.0F;
    static final float POINTINESS = 3.0F;
    static final float SCALE_FALLOFF_MIN = 0.25F;

    // Scoped-down equivalents of yRange / render distance for the rift.
    static final float LIFT_RANGE = 26.0F;
    static final float PATCH_RADIUS = 320.0F;
    static final float PATCH_SIZE = PATCH_RADIUS * 2.0F;

    // Hard cap. A full disc at this radius/spacing holds ~11.7k
    // candidates, but only cells over carved-open rift ever become puffs -
    // the rift's footprint never fills the whole disc.
    static final int MAX_PUFFS = 12288;
    static final int BYTES_PER_PUFF = 8;

    // ---- Blanket shaping ----
    /** How far (fraction of LIFT_RANGE) weak areas sag below the base plane. */
    static final float MAX_DIP = 0.3F;
    /** Extra lift (fraction of LIFT_RANGE) at full wall contact. */
    private static final double WALL_CLIMB = 0.55;

    // ---- Per-layer knobs (one generator instance = one cloud layer) ----
    /** Coverage below this is a hole. Low = near-solid blanket with small
     * gaps; high (0.5+) = sparse, well-separated cloud groups. */
    private final double gapThreshold;
    /** Max fraction of candidates randomly dropped in thin areas. */
    private final double crowdDrop;
    /** Wall hugging on/off - off for free-floating upper layers. */
    private final boolean wallEffects;
    /** Salts the coverage noise so stacked layers never share a pattern. */
    private final int seedSalt;

    RiftCloudGenerator(double gapThreshold, double crowdDrop, boolean wallEffects, int seedSalt) {
        this.gapThreshold = gapThreshold;
        this.crowdDrop = crowdDrop;
        this.wallEffects = wallEffects;
        this.seedSalt = seedSalt * 0x1F123B;
    }

    private final BlockPos.MutableBlockPos samplePos = new BlockPos.MutableBlockPos();
    // Scratch: distSq + packed bytes per puff, so puffs can be depth-sorted
    // before writing to the GPU buffer.
    private final long[] sortKeys = new long[MAX_PUFFS];
    private final int[] packed0 = new int[MAX_PUFFS];
    private final int[] packed1 = new int[MAX_PUFFS];

    /** Cloud-space X of the patch origin (min corner) chosen by the last build. */
    private double originCsX;
    private double originCsZ;

    double originCsX() {
        return this.originCsX;
    }

    double originCsZ() {
        return this.originCsZ;
    }

    /**
     * Rebuilds the puff set around the camera and writes it, back-to-front,
     * into {@code buffer}. Returns the puff count.
     *
     * @param driftTotal total accumulated travel (blocks) at build time -
     *                   cloud-space X = world X + driftTotal.
     */
    int build(ClientLevel level, ByteBuffer buffer, double camX, double camZ, float cloudY, double driftTotal) {
        double csCamX = camX + driftTotal;
        double csCamZ = camZ;
        this.originCsX = csCamX - PATCH_RADIUS;
        this.originCsZ = csCamZ - PATCH_RADIUS;

        int minGX = Mth.floor((csCamX - PATCH_RADIUS) / GRID_SPACING);
        int maxGX = Mth.ceil((csCamX + PATCH_RADIUS) / GRID_SPACING);
        int minGZ = Mth.floor((csCamZ - PATCH_RADIUS) / GRID_SPACING);
        int maxGZ = Mth.ceil((csCamZ + PATCH_RADIUS) / GRID_SPACING);

        int blockY = Mth.floor(cloudY);
        int count = 0;

        for (int gx = minGX; gx <= maxGX && count < MAX_PUFFS; gx++) {
            for (int gz = minGZ; gz <= maxGZ && count < MAX_PUFFS; gz++) {
                // Scatter far beyond one cell (+-1.4 cells): neighboring
                // candidates can overlap or swap places, so the underlying
                // grid rhythm is unrecoverable by eye.
                double jx = (hash01(gx, gz, 0x51) - 0.5) * 2.8;
                double jz = (hash01(gx, gz, 0xA7) - 0.5) * 2.8;
                double csx = (gx + jx) * GRID_SPACING;
                double csz = (gz + jz) * GRID_SPACING;

                double dx = csx - csCamX;
                double dz = csz - csCamZ;
                double distSq = dx * dx + dz * dz;
                if (distSq > PATCH_RADIUS * PATCH_RADIUS) {
                    continue;
                }

                // The rift-mask: world position of this puff right now must
                // be carved open at cloud height. Everything else below
                // only shapes the blanket, it never creates puffs over
                // solid ground.
                double worldX = csx - driftTotal;
                int bx = Mth.floor(worldX);
                int bz = Mth.floor(csz);
                this.samplePos.set(bx, blockY, bz);
                if (!level.getBlockState(this.samplePos).isAir()) {
                    continue;
                }

                double strength = coverage(csx, csz);
                // Small holes only: cull just the very weakest coverage,
                // and renormalize the rest so the shaping below still uses
                // the full 0..1 range. Around each hole strength ramps up
                // from 0, which the density byte below turns into thin,
                // wispy rims instead of hard cutouts.
                if (strength < this.gapThreshold) {
                    continue;
                }
                double s = (strength - this.gapThreshold) / (1.0 - this.gapThreshold);

                // Uneven crowding: thin areas randomly drop candidates,
                // cores keep almost all of them - so density itself gets
                // patchy instead of a uniform carpet.
                if (hash01(gx, gz, 0x77) < this.crowdDrop * (1.0 - s)) {
                    continue;
                }

                // Wall proximity 0..1: solid blocks at cloud height within
                // ~6 blocks count strongly, within ~12 weakly. Drives the
                // "hugging" behaviors - puffs climb, swell and thicken
                // where the layer meets the rift walls, so the blanket
                // reads as curling up along the rock instead of being
                // sliced off by it.
                // Wall boost is randomized per puff (x0.35..x1.0): without
                // this, every puff along a wall gets the exact same
                // lift/size/density bump, which reads as a neat uniform
                // line of identical clouds tracing the rock edge.
                double wall = this.wallEffects
                        ? wallProximity(level, bx, blockY, bz) * (0.35 + 0.65 * hash01(gx, gz, 0xB2))
                        : 0.0;

                // Signed lift: strong coverage bulges up (BC's pointiness
                // curve), weak coverage sags below the base plane, and
                // wall contact climbs on top of that.
                double lift = Math.pow(s, 1.0 / POINTINESS) * s
                        - MAX_DIP * (1.0 - s)
                        + wall * WALL_CLIMB;
                lift = Mth.clamp(lift, -MAX_DIP, 1.0);

                int qx = (int) Math.round((csx - this.originCsX) / PATCH_SIZE * 65535.0);
                int qz = (int) Math.round((csz - this.originCsZ) / PATCH_SIZE * 65535.0);
                qx = Mth.clamp(qx, 0, 65535);
                qz = Mth.clamp(qz, 0, 65535);

                // Byte-encode lift over [-MAX_DIP, 1] (decoded in the vsh).
                int liftB = (int) ((lift + MAX_DIP) / (1.0 + MAX_DIP) * 255.0);
                // Hash-dominated so the shader's power-curve decode spreads
                // sizes wide (mostly small, occasional big boulders);
                // coverage/wall only nudge the odds upward.
                int sizeB = (int) (Mth.clamp(0.72 * hash01(gx, gz, 0x33) + 0.16 * s + 0.2 * wall, 0.0, 1.0) * 255.0);
                int noiseB = (int) (hash01(gx, gz, 0xEE) * 255.0);
                // Density: thick opaque cores, translucent thin areas,
                // extra body where the layer presses against a wall.
                int densB = (int) (Mth.clamp(0.3 + 0.7 * s + 0.35 * wall, 0.0, 1.0) * 255.0);

                this.packed0[count] = (qx & 0xFF) | ((qx >> 8) << 8) | ((qz & 0xFF) << 16) | ((qz >> 8) << 24);
                this.packed1[count] = liftB | (sizeB << 8) | (noiseB << 16) | (densB << 24);
                // Distance in the high bits, index in the low bits, negated
                // ordering handled below - one primitive sort, no allocs.
                this.sortKeys[count] = ((long) distSq << 14) | count;
                count++;
            }
        }

        // Back-to-front for translucent blending; sort ascending then walk
        // backwards (farthest written first).
        Arrays.sort(this.sortKeys, 0, count);
        for (int i = count - 1; i >= 0; i--) {
            int idx = (int) (this.sortKeys[i] & 0x3FFF);
            putIntLE(buffer, this.packed0[idx]);
            putIntLE(buffer, this.packed1[idx]);
        }
        return count;
    }

    private static void putIntLE(ByteBuffer buffer, int packed) {
        buffer.put((byte) packed)
                .put((byte) (packed >> 8))
                .put((byte) (packed >> 16))
                .put((byte) (packed >> 24));
    }

    /**
     * Solid-neighbor scan at cloud height: 4 directions at ~6 blocks
     * (strong) and 4 at ~12 (weak), clamped to 1. Zero in open air, rises
     * as the layer approaches the rift walls.
     */
    private double wallProximity(ClientLevel level, int bx, int blockY, int bz) {
        double wall = 0.0;
        for (int i = 0; i < 4; i++) {
            int dx = (i == 0 ? 6 : i == 1 ? -6 : 0);
            int dz = (i == 2 ? 6 : i == 3 ? -6 : 0);
            this.samplePos.set(bx + dx, blockY, bz + dz);
            if (!level.getBlockState(this.samplePos).isAir()) {
                wall += 0.30;
            }
            this.samplePos.set(bx + dx * 2, blockY, bz + dz * 2);
            if (!level.getBlockState(this.samplePos).isAir()) {
                wall += 0.12;
            }
        }
        return Math.min(wall, 1.0);
    }

    /**
     * Two octaves of coherent value noise in cloud space, 0..1. Coarse
     * octave (~140 block features) forms the banks; fine octave (~35
     * blocks) roughens their outline. Salted per layer so stacked layers
     * get independent patterns.
     */
    private double coverage(double csx, double csz) {
        double coarse = valueNoise(csx / 140.0, csz / 140.0, 0xC10D ^ this.seedSalt);
        double fine = valueNoise(csx / 35.0, csz / 35.0, 0xF13E ^ this.seedSalt);
        return coarse * 0.72 + fine * 0.28;
    }

    private static double valueNoise(double x, double z, int seed) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        double fx = smooth(x - x0);
        double fz = smooth(z - z0);
        double c00 = hash01(x0, z0, seed);
        double c10 = hash01(x0 + 1, z0, seed);
        double c01 = hash01(x0, z0 + 1, seed);
        double c11 = hash01(x0 + 1, z0 + 1, seed);
        return Mth.lerp(fz, Mth.lerp(fx, c00, c10), Mth.lerp(fx, c01, c11));
    }

    private static double smooth(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double hash01(int x, int z, int seed) {
        // Each axis multiplied by its own constant BEFORE combining, plus a
        // double finalizer round. The previous version's pre-mix state was
        // linear in (x, z), which leaked faint diagonal alignment into
        // jitter and sizes.
        long h = x * 0x9E3779B97F4A7C15L
                ^ z * 0xC2B2AE3D27D4EB4FL
                ^ seed * 0x165667B19E3779F9L;
        h ^= (h >>> 33);
        h *= 0xFF51AFD7ED558CCDL;
        h ^= (h >>> 33);
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= (h >>> 33);
        return ((h >>> 40) & 0xFFFFFF) / (double) 0xFFFFFF;
    }
}
