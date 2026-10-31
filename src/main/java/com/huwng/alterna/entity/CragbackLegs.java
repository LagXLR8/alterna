package com.huwng.alterna.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Client-side procedural walking for the Cragback.
 *
 * Every foot is planted at a real spot on the ground and stays there while the body moves
 * over it. When a foot falls too far behind where it should be, it takes a step: it lifts
 * in an arc and lands a little ahead of the hip (leading by the current velocity), on top
 * of whatever block is there - so the legs reach down into dips and up onto steps, and the
 * stride automatically matches the walking speed (no foot sliding). Feet move in diagonal
 * pairs like a real quadruped trot/walk. The body pitches and rolls to the ground under it.
 *
 * Only the client runs this; it is purely visual.
 */
public final class CragbackLegs {
    /** Hip offsets in blocks (entity's right, entity's forward): right-front, left-front, right-hind, left-hind. */
    private static final double[][] HIP = {{0.5, 0.81}, {-0.5, 0.81}, {0.5, -0.94}, {-0.5, -0.94}};
    /** Diagonal pairs that step together. */
    private static final int[][] PAIRS = {{0, 3}, {1, 2}};
    private static final double FRONT_TO_HIND = 1.75, LEFT_TO_RIGHT = 1.0;

    private final Vec3[] foot = new Vec3[4];
    private final Vec3[] footO = new Vec3[4];
    private final Vec3[] stepFrom = new Vec3[4];
    private final Vec3[] target = new Vec3[4];
    private final float[] stepT = new float[4];
    private final boolean[] stepping = new boolean[4];
    private int stepDuration = 8;
    private int nextPair;

    float pitch, pitchO, roll, rollO, drop, dropO, bob, bobO, sway, swayO;

    boolean ready() {
        return this.foot[0] != null;
    }

    void tick(CragbackEntity e) {
        Level level = e.level();
        Vec3 pos = e.position();
        float scale = e.getAgeScale();
        float yaw = e.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        Vec3 vel = new Vec3(pos.x - e.xo, 0, pos.z - e.zo);
        double speed = vel.length();
        // faster walking = quicker steps; a step covers roughly 2x the lead distance
        this.stepDuration = Mth.clamp((int) Math.round(10 - speed * 40), 4, 10);

        boolean reset = this.foot[0] == null || this.foot[0].distanceTo(pos) > 4.0;
        for (int i = 0; i < 4; i++) {
            this.footO[i] = this.foot[i];
            Vec3 hip = pos.add(right.scale(HIP[i][0] * scale)).add(fwd.scale(HIP[i][1] * scale));
            Vec3 lead = vel.scale(this.stepDuration * 0.9);
            Vec3 t = hip.add(lead);
            double gy = groundY(level, t.x, pos.y + 1.1 * scale, t.z, pos.y);
            this.target[i] = new Vec3(t.x, gy, t.z);
            if (reset) {
                this.foot[i] = this.footO[i] = this.target[i];
                this.stepping[i] = false;
            }
        }

        // advance steps in flight
        for (int i = 0; i < 4; i++) {
            if (!this.stepping[i]) continue;
            this.stepT[i] = Math.min(1.0F, this.stepT[i] + 1.0F / this.stepDuration);
            float t = this.stepT[i];
            float ease = t * t * (3.0F - 2.0F * t);
            Vec3 to = this.target[i];
            Vec3 p = this.stepFrom[i].lerp(to, ease);
            double arc = Mth.sin(t * Mth.PI) * (0.22 + Math.min(0.25, speed * 1.2)) * scale;
            this.foot[i] = p.add(0, arc, 0);
            if (t >= 1.0F) {
                this.stepping[i] = false;
                this.foot[i] = to;
                this.land(e, level, to, speed);
            }
        }

        // start a new pair once the other pair is down
        boolean anyStepping = false;
        for (boolean s : this.stepping) anyStepping |= s;
        if (!anyStepping) {
            double threshold = (speed > 0.01 ? 0.42 : 0.16) * scale;
            for (int k = 0; k < 2; k++) {
                int[] pair = PAIRS[(this.nextPair + k) % 2];
                double err = 0.0;
                for (int i : pair) {
                    double legErr = this.foot[i].distanceTo(this.target[i]);
                    if (Math.abs(this.foot[i].y - this.target[i].y) > 0.3) legErr += 1.0;   // hanging in the air / sunk in: fix it now
                    err = Math.max(err, legErr);
                }
                if (err > threshold) {
                    for (int i : pair) {
                        this.stepping[i] = true;
                        this.stepT[i] = 0.0F;
                        this.stepFrom[i] = this.foot[i];
                    }
                    this.nextPair = (this.nextPair + k + 1) % 2;
                    break;
                }
            }
        }

        // body attitude from the ground under the (planted) feet
        double fy0 = this.target[0].y, fy1 = this.target[1].y, fy2 = this.target[2].y, fy3 = this.target[3].y;
        double frontY = (fy0 + fy1) * 0.5, hindY = (fy2 + fy3) * 0.5;
        double leftY = (fy1 + fy3) * 0.5, rightY = (fy0 + fy2) * 0.5;
        float targetPitch = (float) Mth.clamp(Math.atan2(frontY - hindY, FRONT_TO_HIND * scale), -0.6, 0.6);
        float targetRoll = (float) Mth.clamp(Math.atan2(leftY - rightY, LEFT_TO_RIGHT * scale), -0.4, 0.4);
        // sink toward the lower feet (weighted to the lowest one) so legs on a lower block can reach it
        double lowest = Math.min(Math.min(fy0, fy1), Math.min(fy2, fy3));
        float targetDrop = (float) Mth.clamp(((fy0 + fy1 + fy2 + fy3) * 0.25 * 0.5 + lowest * 0.5) - pos.y, -1.0 * scale, 0.0);
        // a little rise and side-to-side sway with each step
        float lifting = 0.0F, side = 0.0F;
        for (int i = 0; i < 4; i++) {
            if (!this.stepping[i]) continue;
            float s = Mth.sin(this.stepT[i] * Mth.PI);
            lifting += s;
            side += (i == 0 || i == 2 ? 1 : -1) * s;   // right legs lifted -> lean left
        }
        this.pitchO = this.pitch;
        this.rollO = this.roll;
        this.dropO = this.drop;
        this.bobO = this.bob;
        this.swayO = this.sway;
        this.pitch += (targetPitch - this.pitch) * 0.35F;
        this.roll += (targetRoll - this.roll) * 0.3F;
        this.drop += (targetDrop - this.drop) * 0.4F;
        this.bob += (lifting * 0.35F - this.bob) * 0.4F;
        this.sway += (side * 0.025F - this.sway) * 0.3F;
    }

    /** Heavy footfall: a soft thud and, at a run, a puff of dust. */
    private void land(CragbackEntity e, Level level, Vec3 at, double speed) {
        BlockPos below = BlockPos.containing(at.x, at.y - 0.05, at.z);
        BlockState ground = level.getBlockState(below);
        if (ground.isAir()) return;
        float vol = e.isBaby() ? 0.12F : 0.25F + (float) Math.min(0.3, speed * 1.5);
        level.playLocalSound(at.x, at.y, at.z, SoundEvents.RAVAGER_STEP, e.getSoundSource(), vol,
                (e.isBaby() ? 1.3F : 0.75F) + e.getRandom().nextFloat() * 0.1F, false);
        if (speed > 0.12) {
            for (int k = 0; k < 4; k++) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x + (e.getRandom().nextDouble() - 0.5) * 0.6,
                        at.y + 0.05, at.z + (e.getRandom().nextDouble() - 0.5) * 0.6, 0.0, 0.05, 0.0);
            }
        }
    }

    /** Top of the first solid surface at or below {@code fromY} (searching 4 blocks down). */
    static double groundY(Level level, double x, double fromY, double z, double fallback) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int top = Mth.floor(fromY);
        for (int y = top; y > top - 5; y--) {
            p.set(Mth.floor(x), y, Mth.floor(z));
            VoxelShape shape = level.getBlockState(p).getCollisionShape(level, p);
            if (shape.isEmpty()) continue;
            double surface = y + shape.max(Direction.Axis.Y);
            if (surface <= fromY + 0.01) return surface;
        }
        return fallback;
    }

    /** Interpolated foot position for rendering. */
    Vec3 foot(int i, float partial) {
        Vec3 a = this.footO[i] != null ? this.footO[i] : this.foot[i];
        return a.lerp(this.foot[i], partial);
    }
}
