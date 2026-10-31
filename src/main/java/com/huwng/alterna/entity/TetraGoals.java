package com.huwng.alterna.entity;

import com.huwng.alterna.block.ModBlocks;
import com.huwng.alterna.block.TetraNestBlock;
import com.huwng.alterna.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/** All of the Tetra's behaviour lives here. */
final class TetraGoals {
    private TetraGoals() {}

    private static boolean freeAir(Level level, BlockPos p) {
        return level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir();
    }

    private static boolean busy(TetraEntity t) {
        return t.getTarget() != null || t.hasEgg || t.isOrderedToSit() || t.isPassenger();
    }

    /** Y of the first walkable surface below {@code from} within {@code depth} blocks, or MIN_VALUE. */
    static int floorBelow(Level level, BlockPos from, int depth) {
        BlockPos.MutableBlockPos p = from.mutable();
        for (int i = 0; i <= depth; i++) {
            BlockState s = level.getBlockState(p);
            if (!s.isAir() && (!s.getFluidState().isEmpty() || s.isSolid())) return p.getY() + 1;
            p.move(Direction.DOWN);
        }
        return Integer.MIN_VALUE;
    }

    /** A dry, open patch of ground near {@code around} where a Tetra can land. */
    static @Nullable Vec3 findLandingSpot(TetraEntity mob, Vec3 around, int radius) {
        Level level = mob.level();
        RandomSource r = mob.getRandom();
        for (int i = 0; i < 24; i++) {
            BlockPos col = BlockPos.containing(around.x + r.nextInt(radius * 2 + 1) - radius, around.y + 6,
                    around.z + r.nextInt(radius * 2 + 1) - radius);
            for (int dy = 0; dy < 40; dy++) {
                BlockPos p = col.below(dy);
                BlockState below = level.getBlockState(p.below());
                if (!level.getBlockState(p).isAir()) continue;
                if (below.isAir()) continue;
                if (!below.getFluidState().isEmpty() || !below.isFaceSturdy(level, p.below(), Direction.UP)) break;
                if (!level.getBlockState(p.above()).isAir() || !level.getBlockState(p.above(2)).isAir()) break;
                return Vec3.atBottomCenterOf(p);
            }
        }
        return null;
    }

    // ================================================================== movement
    /**
     * Grounded: plain walking. Flying: steer like a bird - turn rate is limited, the
     * body pitches toward the target and thrust is applied along that heading, so
     * with the momentum from {@link TetraEntity#travel} it carves arcs, climbs and
     * dives instead of sliding around like a bee.
     */
    static class TetraMoveControl extends MoveControl {
        private final TetraEntity tetra;
        private float flightPitch;
        private int avoidSide = 1;
        private int avoidTicks;
        // stuck detection: how far it really moved over the last second
        private @Nullable Vec3 checkPos;
        private int checkTimer;
        private int escapeTicks;
        private float escapeYaw;

        TetraMoveControl(TetraEntity tetra) {
            super(tetra);
            this.tetra = tetra;
        }

        @Override
        public void tick() {
            if (this.tetra.isPerched()) {
                this.operation = Operation.WAIT;
                this.mob.setZza(0.0F);
                this.mob.setYya(0.0F);
                return;
            }
            if (!this.tetra.isFlying()) {
                if (this.operation == Operation.MOVE_TO && this.tetra.canTakeOff() && !this.tetra.isInWater()) {
                    double dy = this.wantedY - this.mob.getY();
                    double dh = Math.hypot(this.wantedX - this.mob.getX(), this.wantedZ - this.mob.getZ());
                    if (dy > 1.6 || dh > 14.0) {
                        this.tetra.takeOff();
                        return;
                    }
                }
                super.tick();
                return;
            }
            this.tickFlying();
        }

        private void tickFlying() {
            Level level = this.mob.level();
            Vec3 pos = this.mob.position();
            boolean hasGoal = this.operation == Operation.MOVE_TO;
            this.operation = Operation.WAIT;
            double tx = this.wantedX, ty = this.wantedY, tz = this.wantedZ, speedMod = this.speedModifier;
            Vec3 land = this.tetra.landTarget;
            if (land != null) {
                tx = land.x; ty = land.y; tz = land.z;
                speedMod = Math.max(speedMod, 1.0);
            } else if (!hasGoal || speedMod <= 0.0 || pos.distanceToSqr(tx, ty, tz) < 4.0) {
                // nobody is steering: keep gliding forward
                Vec3 fwd = Vec3.directionFromRotation(0.0F, this.mob.getYRot());
                tx = pos.x + fwd.x * 10.0; ty = pos.y; tz = pos.z + fwd.z * 10.0;
                speedMod = 0.85;
            }
            if (land == null && !this.tetra.isDiving()) {
                // keep clear of the ground unless coming in to land or diving at prey...
                int floor = floorBelow(level, this.mob.blockPosition(), 6);
                if (floor != Integer.MIN_VALUE && ty < floor + 3.5) ty = floor + 3.5;
                // ...and never try to climb into a ceiling or overhang
                int ceiling = ceilingAbove(level, this.mob.blockPosition(), 5);
                if (ceiling != Integer.MAX_VALUE) {
                    double maxY = ceiling - 1.6;
                    if (floor != Integer.MIN_VALUE && floor + 3.5 > maxY) maxY = (floor + ceiling) * 0.5 - 0.5;
                    if (ty > maxY) ty = maxY;
                }
            }

            // stuck against something (under a ledge, in a corner): turn around and drop out of it
            if (--this.checkTimer <= 0) {
                if (this.checkPos != null && this.escapeTicks <= 0 && land == null
                        && pos.distanceTo(this.checkPos) < 2.5) {
                    this.escapeTicks = 25;
                    this.escapeYaw = this.mob.getYRot() + 150.0F + this.mob.getRandom().nextFloat() * 60.0F;
                    this.tetra.flightStuck = true;
                }
                this.checkPos = pos;
                this.checkTimer = 20;
            }

            double dx = tx - pos.x, dy = ty - pos.y, dz = tz - pos.z;
            double dh = Math.sqrt(dx * dx + dz * dz);
            double dist = Math.sqrt(dh * dh + dy * dy);
            float yaw = this.mob.getYRot();
            float targetYaw = dh > 0.25 ? (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F : yaw;
            float turnRate = this.tetra.isDiving() ? 9.0F : land != null && dist < 6.0 ? 20.0F : 12.0F;
            // target (almost) straight above: climb steeply on the current heading instead of
            // chasing a heading that keeps flipping around - that is what made it spiral up
            boolean steepClimb = dy > 1.5 && dh < dy * 0.8;
            if (steepClimb) targetYaw = yaw;

            // whiskers: about to fly into a cliff -> bank hard to one side and climb
            if (land == null && this.blockedAhead(level, pos)) {
                if (this.avoidTicks <= 0) this.avoidSide = this.mob.getRandom().nextBoolean() ? 1 : -1;
                this.avoidTicks = 12;
            }
            if (this.avoidTicks > 0) {
                this.avoidTicks--;
                targetYaw = yaw + 90.0F * this.avoidSide;
                turnRate = 22.0F;
                if (ceilingAbove(level, this.mob.blockPosition(), 3) == Integer.MAX_VALUE) dy = Math.max(dy, 3.0);
            }
            if (this.escapeTicks > 0) {
                this.escapeTicks--;
                targetYaw = this.escapeYaw;
                turnRate = 25.0F;
                dy = ceilingAbove(level, this.mob.blockPosition(), 3) != Integer.MAX_VALUE ? -2.0 : 2.0;
                dh = 4.0;
                steepClimb = false;
            }
            this.mob.setYRot(this.rotlerp(yaw, targetYaw, turnRate));
            this.mob.yBodyRot = this.mob.getYRot();

            float targetPitch = (float) (-Mth.atan2(dy, Math.max(dh, 0.001)) * Mth.RAD_TO_DEG);
            targetPitch = Mth.clamp(targetPitch, steepClimb ? -80.0F : -55.0F, this.tetra.isDiving() || land != null ? 80.0F : 55.0F);
            // a sharp turn bleeds off climb/dive
            float turnDiff = Math.abs(Mth.wrapDegrees(targetYaw - yaw));
            if (turnDiff > 90.0F) targetPitch *= 0.4F;
            this.flightPitch = this.rotlerp(this.flightPitch, targetPitch, steepClimb ? 14.0F : 9.0F);

            float speed = (float) (speedMod * this.mob.getAttributeValue(Attributes.FLYING_SPEED));
            if (land != null && dist < 7.0) {
                speed *= (float) Math.max(0.2, dist / 7.0);   // flare and slow down for the landing
            } else if (turnDiff > 100.0F) {
                speed *= 0.7F;
            }
            this.mob.setSpeed(speed);
            float p = this.flightPitch * Mth.DEG_TO_RAD;
            this.mob.setZza(Mth.cos(p));
            this.mob.setYya(-Mth.sin(p));
            this.mob.setXxa(0.0F);
        }

        /** Y of the lowest solid block above {@code from} within {@code height}, or MAX_VALUE. */
        private static int ceilingAbove(Level level, BlockPos from, int height) {
            BlockPos.MutableBlockPos p = from.mutable();
            for (int i = 1; i <= height; i++) {
                p.move(Direction.UP);
                if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) return p.getY();
            }
            return Integer.MAX_VALUE;
        }

        private boolean blockedAhead(Level level, Vec3 pos) {
            Vec3 v = this.mob.getDeltaMovement();
            double h = Math.sqrt(v.x * v.x + v.z * v.z);
            if (h < 0.05) return false;
            for (int i = 2; i <= 5; i += 3) {
                BlockPos p = BlockPos.containing(pos.x + v.x / h * i, pos.y + 0.5 + v.y * i, pos.z + v.z / h * i);
                if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) return true;
            }
            return false;
        }
    }

    // ================================================================== basic
    /** Never sit in water: flap straight back up. */
    static class EscapeWaterGoal extends Goal {
        private final TetraEntity mob;
        EscapeWaterGoal(TetraEntity mob) { this.mob = mob; this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP)); }
        @Override public boolean canUse() { return this.mob.isInWater(); }
        @Override public void tick() {
            this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0, 0.12, 0));
            if (!this.mob.isFlying()) this.mob.takeOff();
        }
    }

    /** Walk around a little while resting on the ground. */
    static class GroundStrollGoal extends WaterAvoidingRandomStrollGoal {
        private final TetraEntity tetra;
        GroundStrollGoal(TetraEntity mob) { super(mob, 1.0, 0.01F); this.tetra = mob; }
        @Override public boolean canUse() { return !this.tetra.isFlying() && !this.tetra.isPerched() && super.canUse(); }
        @Override public boolean canContinueToUse() { return !this.tetra.isFlying() && super.canContinueToUse(); }
    }

    /**
     * Take off again once it has rested, when its flock leaves, or when a player
     * comes too close (wild birds are skittish).
     */
    static class TakeOffGoal extends Goal {
        private final TetraEntity mob;
        TakeOffGoal(TetraEntity mob) { this.mob = mob; this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP)); }

        @Override
        public boolean canUse() {
            if (this.mob.isFlying() || this.mob.isPerched() || !this.mob.canTakeOff() || !this.mob.onGround()) return false;
            if (this.mob.isTame()) return false;   // tame ones take off when following the owner
            if (this.mob.restTime <= 0) return this.mob.getRandom().nextInt(20) == 0;
            TetraEntity leader = this.mob.flockLeader;
            if (leader != null && leader != this.mob && leader.isAlive() && leader.isFlying() && leader.distanceToSqr(this.mob) > 36) {
                return this.mob.getRandom().nextInt(8) == 0;
            }
            Player near = this.mob.level().getNearestPlayer(this.mob, 5.0);
            return near != null && !near.isSpectator() && !near.isCreative() && !near.isDiscrete();
        }

        @Override public boolean canContinueToUse() { return false; }

        @Override public void start() { this.mob.takeOff(); }
    }

    // ================================================================== flock flight
    /**
     * The flock: the Tetra with the lowest UUID nearby leads and picks long, sweeping
     * routes through the rift. The others hold a loose V behind it and steer with the
     * classic boid rules (stay with the group, match its heading, don't collide).
     */
    static class FlockFlightGoal extends Goal {
        private final TetraEntity mob;
        private @Nullable Vec3 waypoint;
        private int waypointTimer;
        private int relead;
        private double cruise = 1.0;
        private final Vec3 slot;

        FlockFlightGoal(TetraEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE));
            RandomSource r = RandomSource.create(mob.getUUID().getLeastSignificantBits());
            // a slot in a loose V behind the leader: (sideways, up, back)
            double side = (r.nextBoolean() ? 1 : -1) * (2.0 + r.nextDouble() * 4.0);
            this.slot = new Vec3(side, (r.nextDouble() - 0.5) * 2.5, 2.0 + Math.abs(side) * 0.9);
        }

        @Override
        public boolean canUse() {
            return this.mob.isFlying() && !busy(this.mob) && !(this.mob.isTame() && this.mob.getOwner() != null);
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        private TetraEntity findLeader() {
            List<TetraEntity> near = this.mob.level().getEntitiesOfClass(TetraEntity.class, this.mob.getBoundingBox().inflate(32.0, 20.0, 32.0),
                    t -> t.isAlive() && !t.isTame() && !t.isBaby() && (t.isFlying() || t.isPerched() || t == this.mob.flockLeader));
            near.add(this.mob);
            return near.stream().min(Comparator.comparing(TetraEntity::getUUID, UUID::compareTo)).orElse(this.mob);
        }

        @Override
        public void tick() {
            if (--this.relead <= 0) {
                this.mob.flockLeader = this.mob.isBaby() ? this.mob : this.findLeader();
                this.relead = 40 + this.mob.getRandom().nextInt(20);
            }
            TetraEntity leader = this.mob.flockLeader;
            if (leader == null || leader == this.mob || !leader.isAlive() || !leader.isFlying()) {
                this.lead();
            } else {
                this.follow(leader);
            }
        }

        private void lead() {
            if (this.mob.flightStuck) {
                this.mob.flightStuck = false;
                this.waypoint = null;
            }
            if (this.waypoint == null || --this.waypointTimer <= 0 || this.mob.position().distanceToSqr(this.waypoint) < 36.0) {
                this.waypoint = this.pickWaypoint();
                this.waypointTimer = 300;
                // vary the pace: lazy circling sometimes, quick dashes other times
                this.cruise = 0.8 + this.mob.getRandom().nextDouble() * 0.5;
            }
            if (this.waypoint != null) {
                this.mob.getMoveControl().setWantedPosition(this.waypoint.x, this.waypoint.y, this.waypoint.z, this.cruise);
            }
        }

        private void follow(TetraEntity leader) {
            // the slot rotates with the leader's heading
            float yaw = leader.getYRot() * Mth.DEG_TO_RAD;
            double sin = Mth.sin(yaw), cos = Mth.cos(yaw);
            // leader's forward is (-sin, cos); its right is (-cos, -sin)
            double ox = -cos * this.slot.x + sin * this.slot.z;
            double oz = -sin * this.slot.x - cos * this.slot.z;
            Vec3 lead = leader.getDeltaMovement().scale(6.0);
            Vec3 target = leader.position().add(lead).add(ox, this.slot.y, oz);
            // separation from close neighbours
            Vec3 push = Vec3.ZERO;
            for (TetraEntity other : this.mob.level().getEntitiesOfClass(TetraEntity.class, this.mob.getBoundingBox().inflate(2.5),
                    t -> t != this.mob && t.isFlying())) {
                Vec3 away = this.mob.position().subtract(other.position());
                double d = Math.max(0.3, away.length());
                push = push.add(away.scale(1.0 / (d * d)));
            }
            target = target.add(push.scale(3.0));
            double dist = this.mob.position().distanceTo(target);
            double speed = dist > 14 ? 1.6 : dist > 6 ? 1.25 : dist > 2.5 ? 1.05 : 0.9;
            this.mob.getMoveControl().setWantedPosition(target.x, target.y, target.z, speed);
        }

        private @Nullable Vec3 pickWaypoint() {
            RandomSource r = this.mob.getRandom();
            Level level = this.mob.level();
            BlockPos home = this.mob.homeNest != null ? this.mob.homeNest : this.mob.blockPosition();
            Vec3 eye = this.mob.position().add(0, 0.45, 0);
            for (int i = 0; i < 24; i++) {
                double a = r.nextDouble() * Math.PI * 2.0;
                double d = 18 + r.nextDouble() * 30;
                BlockPos p = home.offset((int) (Math.cos(a) * d), r.nextInt(15) - 6, (int) (Math.sin(a) * d));
                if (!freeAir(level, p)) continue;
                // open sky above it: no ceilings or overhangs to get trapped under
                boolean open = true;
                for (int up = 2; up <= 5 && open; up++) open = level.getBlockState(p.above(up)).isAir();
                if (!open) continue;
                // 6-28 blocks above the ground, in the open air of the rift
                int air = 0;
                BlockPos q = p.below();
                while (air < 28 && level.getBlockState(q).isAir() && q.getY() > level.getMinY()) { q = q.below(); air++; }
                if (air < 6 || air >= 28) continue;
                // and a clear straight line to fly there
                Vec3 c = Vec3.atCenterOf(p);
                if (level.clip(new ClipContext(eye, c, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.mob)).getType()
                        != HitResult.Type.MISS) continue;
                return c;
            }
            return null;
        }
    }

    /**
     * Deliberate landing. The leader decides when the flock rests; the rest of the
     * flock comes down around it. Tame Tetras land when told to sit.
     */
    static class LandGoal extends Goal {
        private final TetraEntity mob;
        private int timeout;

        LandGoal(TetraEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!this.mob.isFlying() || this.mob.getTarget() != null || this.mob.hasEgg || this.mob.timeFlying < 60) return false;
            Vec3 around = null;
            if (this.mob.isOrderedToSit()) {
                around = this.mob.position();
            } else if (this.mob.isTame()) {
                return false;
            } else {
                TetraEntity leader = this.mob.flockLeader;
                if (leader != null && leader != this.mob && leader.isAlive() && !leader.isFlying() && !leader.isPerched()
                        && leader.distanceToSqr(this.mob) < 40 * 40) {
                    around = leader.position();
                } else if ((leader == null || leader == this.mob) && this.mob.timeFlying > 500
                        && this.mob.getRandom().nextInt(400) == 0) {
                    around = this.mob.position();
                }
            }
            if (around == null) return false;
            Vec3 spot = findLandingSpot(this.mob, around, this.mob.isOrderedToSit() ? 2 : 8);
            if (spot == null) return false;
            this.mob.landTarget = spot;
            return true;
        }

        @Override
        public void start() {
            this.timeout = 400;
        }

        @Override
        public boolean canContinueToUse() {
            return this.mob.isFlying() && this.mob.landTarget != null && --this.timeout > 0 && this.mob.getTarget() == null;
        }

        @Override
        public void tick() {
            Vec3 t = this.mob.landTarget;
            if (t == null) return;
            this.mob.getMoveControl().setWantedPosition(t.x, t.y, t.z, 1.0);
            // touch down as soon as it is right above the spot
            if (this.mob.onGround() || this.mob.position().distanceToSqr(t) < 0.8 * 0.8) {
                this.mob.land(400 + this.mob.getRandom().nextInt(600));
            }
        }

        @Override
        public void stop() {
            this.mob.landTarget = null;
        }
    }

    // ================================================================== owner
    /** Tame Tetras circle above their owner and land beside them when the owner stops. */
    static class FollowOwnerFlightGoal extends Goal {
        private final TetraEntity mob;
        private @Nullable LivingEntity owner;
        private double angle;

        FollowOwnerFlightGoal(TetraEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity o = this.mob.getOwner();
            if (o == null || this.mob.unableToMoveToOwner() || this.mob.getTarget() != null) return false;
            if (this.mob.distanceToSqr(o) < (this.mob.isFlying() ? 4 * 4 : 7 * 7)) return false;
            this.owner = o;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.owner != null && this.owner.isAlive() && !this.mob.unableToMoveToOwner() && this.mob.getTarget() == null
                    && this.mob.distanceToSqr(this.owner) > 3 * 3;
        }

        @Override
        public void start() {
            this.angle = this.mob.getRandom().nextDouble() * Math.PI * 2.0;
            this.mob.takeOff();
        }

        @Override
        public void tick() {
            LivingEntity o = this.owner;
            if (o == null) return;
            if (this.mob.shouldTryTeleportToOwner() && this.mob.distanceToSqr(o) > 36 * 36) {
                this.mob.tryToTeleportToOwner();
                return;
            }
            this.mob.getLookControl().setLookAt(o, 10.0F, this.mob.getMaxHeadXRot());
            boolean ownerMoving = o.getDeltaMovement().horizontalDistanceSqr() > 0.003;
            if (!ownerMoving && this.mob.distanceToSqr(o) < 8 * 8 && this.mob.isFlying() && this.mob.landTarget == null) {
                this.mob.landTarget = findLandingSpot(this.mob, o.position(), 2);
            }
            if (this.mob.landTarget != null) {
                Vec3 t = this.mob.landTarget;
                this.mob.getMoveControl().setWantedPosition(t.x, t.y, t.z, 1.0);
                if (this.mob.onGround() || this.mob.position().distanceToSqr(t) < 0.8 * 0.8) this.mob.land(40);
                if (ownerMoving) this.mob.landTarget = null;
                return;
            }
            this.angle += 0.06;
            Vec3 c = o.position().add(Math.cos(this.angle) * 4.0, 3.5, Math.sin(this.angle) * 4.0);
            double d = this.mob.position().distanceTo(c);
            this.mob.getMoveControl().setWantedPosition(c.x, c.y, c.z, d > 12 ? 1.7 : 1.15);
        }

        @Override
        public void stop() {
            this.owner = null;
            this.mob.landTarget = null;
        }
    }

    // ================================================================== cliff perching
    /** Fly to a vertical rock face, cling to it for a while looking around, then leave. */
    static class PerchOnCliffGoal extends Goal {
        private final TetraEntity mob;
        private @Nullable BlockPos spot;
        private @Nullable Direction wall;
        private int approach;
        private int clingTime;

        PerchOnCliffGoal(TetraEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.mob.isFlying() || this.mob.perchCooldown > 0 || busy(this.mob) || this.mob.isBaby()) return false;
            if (this.mob.isTame() && this.mob.getOwner() != null) return false;
            if (this.mob.timeFlying < 100 || this.mob.getRandom().nextInt(60) != 0) return false;
            return this.findSpot();
        }

        private boolean findSpot() {
            Level level = this.mob.level();
            RandomSource r = this.mob.getRandom();
            BlockPos origin = this.mob.blockPosition();
            for (int i = 0; i < 40; i++) {
                BlockPos p = origin.offset(r.nextInt(33) - 16, r.nextInt(15) - 7, r.nextInt(33) - 16);
                if (!level.getBlockState(p).isAir() || !level.getBlockState(p.above()).isAir()) continue;
                // must be a sheer face: open air below as well, not a floor
                if (!level.getBlockState(p.below()).isAir() || !level.getBlockState(p.below(2)).isAir()) continue;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos w = p.relative(d);
                    BlockState ws = level.getBlockState(w);
                    if (ws.isFaceSturdy(level, w, d.getOpposite()) && level.getBlockState(w.above()).isFaceSturdy(level, w.above(), d.getOpposite())
                            && level.getBlockState(p.relative(d.getOpposite())).isAir()
                            && level.getBlockState(p.relative(d.getOpposite(), 2)).isAir()) {
                        this.spot = p;
                        this.wall = d;
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void start() {
            this.approach = 300;
            this.clingTime = 0;
        }

        @Override
        public boolean canContinueToUse() {
            if (this.mob.getTarget() != null) return false;
            return this.mob.isPerched() ? this.clingTime > 0 : this.mob.isFlying() && this.approach > 0 && this.spot != null;
        }

        @Override
        public void tick() {
            if (this.mob.isPerched()) {
                this.clingTime--;
                if (this.mob.getRandom().nextInt(40) == 0) {
                    // glance around at the surroundings
                    Vec3 look = this.mob.position().add((this.mob.getRandom().nextDouble() - 0.5) * 16, (this.mob.getRandom().nextDouble() - 0.6) * 8,
                            (this.mob.getRandom().nextDouble() - 0.5) * 16);
                    this.mob.getLookControl().setLookAt(look.x, look.y, look.z);
                }
                return;
            }
            if (this.spot == null || this.wall == null) return;
            this.approach--;
            // come in from the open side so it doesn't slam into the rock edge-on
            Vec3 c = Vec3.atCenterOf(this.spot);
            double far = this.mob.position().distanceToSqr(c);
            if (far > 6 * 6) {
                Vec3 front = c.add(-this.wall.getStepX() * 4.0, 0.5, -this.wall.getStepZ() * 4.0);
                this.mob.getMoveControl().setWantedPosition(front.x, front.y, front.z, 1.15);
            } else {
                this.mob.landTarget = c;
                this.mob.getMoveControl().setWantedPosition(c.x, c.y, c.z, 1.0);
            }
            if (far < 1.3 * 1.3 && this.mob.level().getBlockState(this.spot).isAir()) {
                this.mob.landTarget = null;
                this.mob.perch(this.spot, this.wall);
                this.clingTime = this.adjustedTickDelay(200 + this.mob.getRandom().nextInt(400));
                this.mob.playSound(ModSounds.TETRA_AMBIENT.get(), 0.7F, 0.95F);
            }
        }

        @Override
        public void stop() {
            this.mob.landTarget = null;
            if (this.mob.isPerched()) this.mob.unperch();
            else this.mob.perchCooldown = 300;
            this.spot = null;
        }
    }

    // ================================================================== fishing
    /** Circle above a fish in a pond, fold the wings, dive and snatch it. */
    static class HuntFishGoal extends Goal {
        private final TetraEntity mob;
        private @Nullable AbstractFish prey;
        private int timeout;
        private double circle;

        HuntFishGoal(TetraEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.mob.fishCooldown > 0 || this.mob.isPerched() || busy(this.mob)) return false;
            if (!this.mob.isFlying() && this.mob.restTime > 0) return false;
            if (this.mob.getRandom().nextInt(30) != 0) return false;
            List<AbstractFish> fish = this.mob.level().getEntitiesOfClass(AbstractFish.class,
                    this.mob.getBoundingBox().inflate(24.0, 20.0, 24.0), f -> f.isAlive() && f.isInWater());
            if (fish.isEmpty()) return false;
            this.prey = fish.stream().min(Comparator.comparingDouble(f -> f.distanceToSqr(this.mob))).get();
            return true;
        }

        @Override
        public void start() {
            this.timeout = 400;
            this.circle = this.mob.getRandom().nextDouble() * Math.PI * 2.0;
            this.mob.takeOff();
        }

        @Override
        public boolean canContinueToUse() {
            return this.prey != null && this.prey.isAlive() && this.timeout > 0 && this.mob.getTarget() == null && this.mob.isFlying();
        }

        @Override
        public void tick() {
            if (this.prey == null) return;
            this.timeout--;
            double horiz = Math.hypot(this.mob.getX() - this.prey.getX(), this.mob.getZ() - this.prey.getZ());
            this.mob.getLookControl().setLookAt(this.prey);
            if (!this.mob.isDiving()) {
                // circle over the pond, then dive once lined up
                this.circle += 0.07;
                Vec3 above = this.prey.position().add(Math.cos(this.circle) * 5.0, 7.0, Math.sin(this.circle) * 5.0);
                this.mob.getMoveControl().setWantedPosition(above.x, above.y, above.z, 1.15);
                if (horiz < 7.0 && this.mob.getY() > this.prey.getY() + 4.0 && this.mob.getRandom().nextInt(15) == 0) {
                    this.mob.setDiving(true);
                    this.mob.playSound(SoundEvents.PHANTOM_SWOOP, 0.6F, 1.7F);
                }
            } else {
                this.mob.getMoveControl().setWantedPosition(this.prey.getX(), this.prey.getY(), this.prey.getZ(), 2.0);
                if (this.mob.distanceToSqr(this.prey) < 1.8 * 1.8 && this.mob.level() instanceof ServerLevel server) {
                    this.prey.hurtServer(server, this.mob.damageSources().mobAttack(this.mob), 50.0F);
                    this.mob.eatFish();
                    this.mob.setDiving(false);
                    this.mob.setDeltaMovement(this.mob.getDeltaMovement().multiply(1.0, 0.0, 1.0).add(0, 0.55, 0));
                    this.timeout = 0;
                } else if (this.mob.getY() < this.prey.getY() - 1.0 || this.mob.isInWater()) {
                    this.mob.setDiving(false);   // missed: pull up and try again
                }
            }
        }

        @Override
        public void stop() {
            this.mob.setDiving(false);
            this.prey = null;
        }
    }

    // ================================================================== berries
    /**
     * Pluck a ripe berry (sweet berry or white currant) from a bush, flying in from above
     * so it never has to push through the thorns. The berry is carried in the beak; the
     * Tetra eats it later or lets it fall somewhere along the way (see TetraEntity).
     */
    static class ForageBerryGoal extends Goal {
        private final TetraEntity mob;
        private @Nullable BlockPos bush;
        private int timeout;

        ForageBerryGoal(TetraEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private static boolean ripe(BlockState s) {
            return s.getBlock() instanceof SweetBerryBushBlock && s.getValue(SweetBerryBushBlock.AGE) >= 2;
        }

        @Override
        public boolean canUse() {
            if (this.mob.isHoldingBerry() || this.mob.forageCooldown > 0 || this.mob.isPerched() || busy(this.mob)) return false;
            if (this.mob.getRandom().nextInt(80) != 0) return false;
            Level level = this.mob.level();
            BlockPos origin = this.mob.blockPosition();
            BlockPos best = null;
            double bestD = Double.MAX_VALUE;
            for (BlockPos p : BlockPos.betweenClosed(origin.offset(-14, -8, -14), origin.offset(14, 4, 14))) {
                if (!ripe(level.getBlockState(p)) || !level.getBlockState(p.above()).isAir()) continue;
                double d = p.distSqr(origin);
                if (d < bestD) { bestD = d; best = p.immutable(); }
            }
            if (best == null) {
                this.mob.forageCooldown = 200;
                return false;
            }
            this.bush = best;
            return true;
        }

        @Override
        public void start() {
            this.timeout = 400;
        }

        @Override
        public boolean canContinueToUse() {
            return this.bush != null && --this.timeout > 0 && !this.mob.isHoldingBerry() && this.mob.getTarget() == null
                    && ripe(this.mob.level().getBlockState(this.bush));
        }

        @Override
        public void tick() {
            if (this.bush == null) return;
            Vec3 top = Vec3.atBottomCenterOf(this.bush).add(0, 1.0, 0);
            this.mob.getLookControl().setLookAt(top.x, top.y - 0.5, top.z);
            double d2 = this.mob.position().distanceToSqr(top);
            if (this.mob.isFlying()) {
                if (d2 > 9 * 9) {
                    this.mob.getMoveControl().setWantedPosition(top.x, top.y + 3.0, top.z, 1.1);
                } else {
                    this.mob.landTarget = top;   // slow, controlled approach from above
                    this.mob.getMoveControl().setWantedPosition(top.x, top.y, top.z, 1.0);
                }
            } else if (this.mob.getNavigation().isDone() || this.mob.tickCount % 20 == 0) {
                this.mob.getNavigation().moveTo(top.x, this.bush.getY(), top.z, 1.0);
            }
            if (d2 < 1.6 * 1.6) this.pluck();
        }

        private void pluck() {
            Level level = this.mob.level();
            BlockState st = level.getBlockState(this.bush);
            if (!ripe(st)) return;
            int berry = st.is(ModBlocks.WHITE_CURRANT_BERRY_BUSH.get()) ? TetraEntity.BERRY_WHITE_CURRANT : TetraEntity.BERRY_SWEET;
            level.setBlock(this.bush, st.setValue(SweetBerryBushBlock.AGE, 1), 2);
            level.playSound(null, this.bush, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 0.8F, 1.1F);
            this.mob.holdBerry(berry);
            this.mob.forageCooldown = 900 + this.mob.getRandom().nextInt(1200);
            this.mob.landTarget = null;
            if (this.mob.isFlying()) this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0, 0.3, 0));
        }

        @Override
        public void stop() {
            this.mob.landTarget = null;
            this.bush = null;
        }
    }

    // ================================================================== attack
    /** Circle high above the target, then fold the wings and dive into it. */
    static class SwoopAttackGoal extends Goal {
        private final TetraEntity mob;
        private int phaseTimer;
        private boolean swooping;
        private double circleAngle;

        SwoopAttackGoal(TetraEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = this.mob.getTarget();
            return t != null && t.isAlive() && !this.mob.isOrderedToSit();
        }

        @Override
        public void start() {
            this.mob.unperch();
            this.mob.takeOff();
            this.swooping = false;
            this.phaseTimer = 30 + this.mob.getRandom().nextInt(30);
            this.circleAngle = this.mob.getRandom().nextDouble() * Math.PI * 2.0;
        }

        @Override
        public void tick() {
            LivingEntity t = this.mob.getTarget();
            if (t == null) return;
            if (!this.mob.isFlying()) this.mob.takeOff();
            this.mob.getLookControl().setLookAt(t);
            if (!this.swooping) {
                this.circleAngle += 0.07;
                Vec3 c = t.position().add(Math.cos(this.circleAngle) * 8.0, 7.0, Math.sin(this.circleAngle) * 8.0);
                this.mob.getMoveControl().setWantedPosition(c.x, c.y, c.z, 1.35);
                if (--this.phaseTimer <= 0) {
                    this.swooping = true;
                    this.mob.setDiving(true);
                    this.phaseTimer = 60;
                    this.mob.playSound(SoundEvents.PHANTOM_SWOOP, 1.0F, 1.5F);
                }
            } else {
                this.mob.getMoveControl().setWantedPosition(t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 2.2);
                if (this.mob.getBoundingBox().inflate(0.5).intersects(t.getBoundingBox()) && this.mob.level() instanceof ServerLevel server) {
                    this.mob.doHurtTarget(server, t);
                    this.pullUp();
                } else if (--this.phaseTimer <= 0) {
                    this.pullUp();
                }
            }
        }

        private void pullUp() {
            this.swooping = false;
            this.mob.setDiving(false);
            this.phaseTimer = 40 + this.mob.getRandom().nextInt(40);
            this.mob.setDeltaMovement(this.mob.getDeltaMovement().add(0, 0.6, 0));
        }

        @Override
        public void stop() {
            this.mob.setDiving(false);
        }
    }

    // ================================================================== nesting
    /** Carry an egg to a nest on a cliff ledge, building a new nest if none is near. */
    static class LayEggGoal extends Goal {
        private final TetraEntity mob;
        private @Nullable BlockPos nest;
        private int timeout;

        LayEggGoal(TetraEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!this.mob.hasEgg || this.mob.getTarget() != null) return false;
            this.nest = this.findNest();
            return this.nest != null;
        }

        private @Nullable BlockPos findNest() {
            if (this.mob.homeNest != null && this.mob.isUsableNest(this.mob.homeNest)) return this.mob.homeNest;
            BlockPos origin = this.mob.blockPosition();
            for (BlockPos p : BlockPos.withinManhattan(origin, 20, 10, 20)) {
                if (this.mob.isUsableNest(p)) return p.immutable();
            }
            // build a new one on a high ledge
            Level level = this.mob.level();
            RandomSource r = this.mob.getRandom();
            for (int i = 0; i < 60; i++) {
                BlockPos col = origin.offset(r.nextInt(33) - 16, 12, r.nextInt(33) - 16);
                for (int dy = 0; dy < 30; dy++) {
                    BlockPos p = col.below(dy);
                    BlockState below = level.getBlockState(p.below());
                    if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
                            && below.isFaceSturdy(level, p.below(), Direction.UP) && !below.is(ModBlocks.TETRA_NEST.get())) {
                        if (isCliffTop(level, p)) return p.immutable();
                        break;
                    }
                }
            }
            return null;
        }

        /** A ledge with a drop of at least 5 blocks right next to it. */
        static boolean isCliffTop(Level level, BlockPos p) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos side = p.relative(d);
                int drop = 0;
                while (drop < 6 && level.getBlockState(side.below(drop)).isAir()) drop++;
                if (drop >= 6) return true;
            }
            return false;
        }

        @Override
        public void start() {
            this.timeout = 700;
            this.mob.takeOff();
        }

        @Override
        public boolean canContinueToUse() {
            return this.mob.hasEgg && this.nest != null && this.timeout > 0;
        }

        @Override
        public void tick() {
            if (this.nest == null) return;
            this.timeout--;
            Vec3 c = Vec3.atBottomCenterOf(this.nest).add(0, 0.6, 0);
            double d2 = this.mob.position().distanceToSqr(c);
            if (this.mob.isFlying()) {
                if (d2 < 10 * 10) this.mob.landTarget = c;
                this.mob.getMoveControl().setWantedPosition(c.x, c.y, c.z, 1.1);
            } else {
                this.mob.getMoveControl().setWantedPosition(c.x, c.y, c.z, 1.0);
            }
            if (d2 < 1.5 * 1.5) {
                Level level = this.mob.level();
                BlockState st = level.getBlockState(this.nest);
                if (!st.is(ModBlocks.TETRA_NEST.get())) {
                    if (!st.isAir()) { this.nest = null; return; }
                    st = ModBlocks.TETRA_NEST.get().defaultBlockState();
                    this.mob.playSound(SoundEvents.GRASS_PLACE, 1.0F, 1.0F);
                }
                int eggs = st.getValue(TetraNestBlock.EGGS);
                if (eggs < 3) {
                    level.setBlock(this.nest, st.setValue(TetraNestBlock.EGGS, eggs + 1).setValue(TetraNestBlock.TENDED, false), 3);
                    this.mob.playSound(SoundEvents.TURTLE_LAY_EGG, 0.8F, 1.3F);
                }
                this.mob.homeNest = this.nest;
                this.mob.hasEgg = false;
                if (this.mob.isFlying()) this.mob.land(300 + this.mob.getRandom().nextInt(300));
            }
        }

        @Override
        public void stop() {
            this.mob.landTarget = null;
        }
    }
}
