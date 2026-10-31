package com.huwng.alterna.entity;

import com.huwng.alterna.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Procedural walking for the Armor Overlord. Runs on BOTH sides: the client uses it to draw the
 * legs, the server uses it for the leg hitboxes and - more importantly - to steer the body.
 *
 * The four claws are planted on the real ground and stay there while the body moves over them.
 * A claw that falls too far from its rest spot takes a step: it lifts high in an arc and comes
 * down slightly ahead (leading by the current walking AND turning speed), on top of whatever
 * block is there. Stalking: one leg at a time. Running or turning: diagonal pairs.
 *
 * Turning is done by the legs: {@link #supportYaw} measures which way the planted claws are
 * "holding" the body, and the entity may only twist a little past that. So to turn around it
 * twists, re-plants a pair of claws in the new direction, twists further, re-plants the other
 * pair, and so on - the body can never spin faster than its legs can walk it round.
 */
public final class OverlordLegs {
    private static final double S = ArmorOverlordEntity.SCALE;
    /** Rest spots of the claws in blocks (entity's right, entity's forward): RF, LF, RH, LH. */
    private static final double[][] REST = {{2.45 * S, 2.65 * S}, {-2.45 * S, 2.65 * S}, {2.45 * S, -2.35 * S}, {-2.45 * S, -2.35 * S}};
    private static final int[][] PAIRS = {{0, 3}, {1, 2}};
    private static final double FRONT_TO_HIND = 5.0 * S, LEFT_TO_RIGHT = 4.9 * S;

    private final Vec3[] foot = new Vec3[4];
    private final Vec3[] footO = new Vec3[4];
    private final Vec3[] stepFrom = new Vec3[4];
    private final Vec3[] target = new Vec3[4];
    private final float[] stepT = new float[4];
    private final boolean[] stepping = new boolean[4];
    private int stepDuration = 12;
    private int nextPair;
    private int lastLeg = -1;
    private float yawVel;

    float pitch, pitchO, roll, rollO, drop, dropO, bob, bobO, sway, swayO;

    boolean ready() {
        return this.foot[0] != null;
    }

    void tick(ArmorOverlordEntity e) {
        Level level = e.level();
        Vec3 pos = e.position();
        float yawDeg = e.getYRot();
        Vec3 vel = new Vec3(pos.x - e.xo, 0, pos.z - e.zo);
        double speed = vel.length();
        this.yawVel += (Mth.wrapDegrees(yawDeg - e.yRotO) - this.yawVel) * 0.5F;
        // how far the body is twisted past the planted claws
        float twist = this.ready() ? Mth.wrapDegrees(yawDeg - this.supportYaw(pos, yawDeg)) : 0.0F;
        boolean turning = Math.abs(this.yawVel) > 1.2F || Math.abs(twist) > 6.0F;
        boolean pairs = speed > 0.1 || turning;
        this.stepDuration = Mth.clamp((int) Math.round(14 - speed * 45 - Math.abs(this.yawVel) * 0.8), 7, 14);
        // one leg at a time: each leg steps every 4 steps, so it must reach further ahead
        double leadTicks = pairs ? this.stepDuration * 1.0 : this.stepDuration * 1.8;
        // place the claws ahead in the turn too, so each re-plant carries the turn on
        float yaw = (yawDeg + Mth.clamp(this.yawVel * (float) leadTicks + twist * 0.6F, -22.0F, 22.0F)) * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));

        boolean reset = this.foot[0] == null || this.foot[0].distanceTo(pos) > 9.0;
        for (int i = 0; i < 4; i++) {
            this.footO[i] = this.foot[i];
            Vec3 rest = pos.add(right.scale(REST[i][0])).add(fwd.scale(REST[i][1]));
            Vec3 t = rest.add(vel.scale(leadTicks));
            double gy = groundY(level, t.x, pos.y + 2.6, t.z, pos.y, 8);
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
            double height = 0.8 + Math.min(0.6, speed * 3.0) + Math.max(0.0, to.y - this.stepFrom[i].y);
            double arc = Mth.sin((float) Math.pow(t, 0.8) * Mth.PI) * height;
            this.foot[i] = p.add(0, arc, 0);
            if (t >= 1.0F) {
                this.stepping[i] = false;
                this.foot[i] = to;
                if (level.isClientSide()) this.land(e, level, to, speed);
            }
        }

        if (e.isAlive()) {
            if (pairs) this.stepPairs(turning); else this.stepSingle(speed);
        }

        // body attitude from the ground under the claws
        double fy0 = this.target[0].y, fy1 = this.target[1].y, fy2 = this.target[2].y, fy3 = this.target[3].y;
        double frontY = (fy0 + fy1) * 0.5, hindY = (fy2 + fy3) * 0.5;
        double leftY = (fy1 + fy3) * 0.5, rightY = (fy0 + fy2) * 0.5;
        float targetPitch = (float) Mth.clamp(Math.atan2(frontY - hindY, FRONT_TO_HIND), -0.45, 0.45);
        float targetRoll = (float) Mth.clamp(Math.atan2(leftY - rightY, LEFT_TO_RIGHT), -0.35, 0.35);
        double lowest = Math.min(Math.min(fy0, fy1), Math.min(fy2, fy3));
        double mean = (fy0 + fy1 + fy2 + fy3) * 0.25;
        float targetDrop = (float) Mth.clamp((mean * 0.6 + lowest * 0.4) - pos.y, -1.6, 0.4);
        float lifting = 0.0F, side = 0.0F;
        for (int i = 0; i < 4; i++) {
            if (!this.stepping[i]) continue;
            float s = Mth.sin(this.stepT[i] * Mth.PI);
            lifting += s;
            side += (i == 0 || i == 2 ? 1 : -1) * s;
        }
        this.pitchO = this.pitch;
        this.rollO = this.roll;
        this.dropO = this.drop;
        this.bobO = this.bob;
        this.swayO = this.sway;
        this.pitch += (targetPitch - this.pitch) * 0.2F;
        this.roll += (targetRoll - this.roll) * 0.2F;
        this.drop += (targetDrop - this.drop) * 0.25F;
        this.bob += (lifting * 0.22F - this.bob) * 0.3F;
        this.sway += (side * 0.03F - this.sway) * 0.25F;
    }

    private boolean anyStepping() {
        for (boolean s : this.stepping) if (s) return true;
        return false;
    }

    private double error(int i) {
        double err = this.foot[i].distanceTo(this.target[i]);
        if (Math.abs(this.foot[i].y - this.target[i].y) > 0.45) err += 2.0;   // hanging in the air / sunk in
        return err;
    }

    /** Stalking: one leg at a time, always the one that lags the most (never the same leg twice in a row). */
    private void stepSingle(double speed) {
        if (this.anyStepping()) return;
        double threshold = speed > 0.01 ? 0.75 : 0.3;
        int best = -1;
        double bestErr = threshold;
        for (int i = 0; i < 4; i++) {
            double err = this.error(i) * (i == this.lastLeg ? 0.5 : 1.0);
            if (err > bestErr) {
                bestErr = err;
                best = i;
            }
        }
        if (best >= 0) this.begin(best);
    }

    /** Running / turning: diagonal pairs, alternating. */
    private void stepPairs(boolean turning) {
        if (this.anyStepping()) return;
        double threshold = turning ? 0.45 : 0.7;
        for (int k = 0; k < 2; k++) {
            int[] pair = PAIRS[(this.nextPair + k) % 2];
            double err = Math.max(this.error(pair[0]), this.error(pair[1]));
            if (err > threshold) {
                for (int i : pair) this.begin(i);
                this.nextPair = (this.nextPair + k + 1) % 2;
                return;
            }
        }
    }

    private void begin(int i) {
        this.stepping[i] = true;
        this.stepT[i] = 0.0F;
        this.stepFrom[i] = this.foot[i];
        this.lastLeg = i;
    }

    /**
     * The yaw (degrees) at which the planted claws hold the body: the rotation that best maps the
     * rest layout of the claws onto where they are actually planted (translation removed).
     * Returns {@code fallback} if fewer than two claws are on the ground.
     */
    float supportYaw(Vec3 pos, float fallback) {
        if (!this.ready()) return fallback;
        double cqx = 0, cqz = 0, cpx = 0, cpz = 0;
        int n = 0;
        for (int i = 0; i < 4; i++) {
            if (this.stepping[i]) continue;
            // rest layout at yaw 0: right = -x, forward = +z
            cqx += -REST[i][0];
            cqz += REST[i][1];
            cpx += this.foot[i].x - pos.x;
            cpz += this.foot[i].z - pos.z;
            n++;
        }
        if (n < 2) return fallback;
        cqx /= n; cqz /= n; cpx /= n; cpz /= n;
        double num = 0, den = 0;
        for (int i = 0; i < 4; i++) {
            if (this.stepping[i]) continue;
            double qx = -REST[i][0] - cqx, qz = REST[i][1] - cqz;
            double px = this.foot[i].x - pos.x - cpx, pz = this.foot[i].z - pos.z - cpz;
            num += qx * pz - qz * px;
            den += qx * px + qz * pz;
        }
        float yaw = (float) (Math.atan2(num, den) * Mth.RAD_TO_DEG);
        // keep it on the same 360-degree turn as the entity's yaw
        return fallback + Mth.wrapDegrees(yaw - fallback);
    }

    /** Heavy footfall: a deep thud and a puff of the ground (client only). */
    private void land(ArmorOverlordEntity e, Level level, Vec3 at, double speed) {
        BlockPos below = BlockPos.containing(at.x, at.y - 0.05, at.z);
        BlockState ground = level.getBlockState(below);
        if (ground.isAir()) return;
        float vol = 1.4F + (float) Math.min(0.6, speed * 3.0);
        level.playLocalSound(at.x, at.y, at.z, ModSounds.OVERLORD_STEP.get(), e.getSoundSource(), vol,
                0.85F + e.getRandom().nextFloat() * 0.2F, false);
        // plus the ground's own footstep (grass, dirt, stone...), pitched down for the weight
        net.minecraft.world.level.block.SoundType type = ground.getSoundType(level, below, e);
        level.playLocalSound(at.x, at.y, at.z, type.getStepSound(), e.getSoundSource(), type.getVolume() * 1.2F,
                type.getPitch() * 0.55F, false);
        int n = speed > 0.08 ? 8 : 4;
        for (int k = 0; k < n; k++) {
            level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x + (e.getRandom().nextDouble() - 0.5) * 0.7,
                    at.y + 0.05, at.z + (e.getRandom().nextDouble() - 0.5) * 0.7, 0.0, 0.05, 0.0);
        }
    }

    /** Top of the first solid surface at or below {@code fromY}, searching {@code depth} blocks down. */
    static double groundY(Level level, double x, double fromY, double z, double fallback, int depth) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int top = Mth.floor(fromY);
        for (int y = top; y > top - depth; y--) {
            p.set(Mth.floor(x), y, Mth.floor(z));
            BlockState state = level.getBlockState(p);
            if (state.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock) continue;   // it tramples through leaves
            VoxelShape shape = state.getCollisionShape(level, p);
            if (shape.isEmpty()) continue;
            double surface = y + shape.max(Direction.Axis.Y);
            if (surface <= fromY + 0.01) return surface;
        }
        return fallback;
    }

    /** Interpolated claw position for rendering. */
    Vec3 foot(int i, float partial) {
        Vec3 a = this.footO[i] != null ? this.footO[i] : this.foot[i];
        return a.lerp(this.foot[i], partial);
    }

    /** Current claw position (for hitboxes). */
    Vec3 foot(int i) {
        return this.foot[i];
    }
}
