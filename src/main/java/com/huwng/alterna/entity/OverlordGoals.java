package com.huwng.alterna.entity;

import com.huwng.alterna.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/** AI for the Armor Overlord: spot prey with its narrow vision, hunt it, search when it loses it. */
final class OverlordGoals {
    private OverlordGoals() {}

    /** Picks a target only from what it can actually see (or feel under its legs). */
    static class SpotPreyGoal extends Goal {
        private final ArmorOverlordEntity o;
        private @Nullable LivingEntity candidate;
        private int cooldown;

        SpotPreyGoal(ArmorOverlordEntity o) {
            this.o = o;
            this.setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (this.o.getTarget() != null || --this.cooldown > 0) return false;
            this.cooldown = 5;
            LivingEntity best = null;
            double bestScore = Double.MAX_VALUE;
            for (LivingEntity e : this.o.level().getEntitiesOfClass(LivingEntity.class,
                    this.o.getBoundingBox().inflate(ArmorOverlordEntity.SIGHT, 10.0, ArmorOverlordEntity.SIGHT),
                    e -> this.o.isPrey(e) && this.o.canSense(e))) {
                double score = this.o.distanceToSqr(e) * (e instanceof Player ? 0.6 : 1.0);
                if (score < bestScore) {
                    bestScore = score;
                    best = e;
                }
            }
            this.candidate = best;
            return best != null;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            if (this.candidate == null) return;
            this.o.spotted(this.candidate);
            this.o.roar();
        }
    }

    /** Chases what it is hunting and picks hook / slash / stomp by distance. */
    static class HuntGoal extends Goal {
        private final ArmorOverlordEntity o;
        private int repath;

        HuntGoal(ArmorOverlordEntity o) {
            this.o = o;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = this.o.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.o.attack = 0;
            this.repath = 0;
        }

        @Override
        public void stop() {
            this.o.attack = 0;
            this.o.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity t = this.o.getTarget();
            if (t == null || !(this.o.level() instanceof ServerLevel level)) return;
            boolean sensed = this.o.tickCount - this.o.lastSeenTick <= 2;
            Vec3 goal = sensed || this.o.lastKnownPos == null ? t.position() : this.o.lastKnownPos;
            this.o.getLookControl().setLookAt(goal.x, sensed ? t.getEyeY() : goal.y + 1.0, goal.z,
                    this.o.getHeadRotSpeed(), this.o.getMaxHeadXRot());

            if (this.o.roarTime > 0) {
                this.o.getNavigation().stop();
                return;
            }
            if (this.o.attack != 0) {
                this.o.getNavigation().stop();
                this.tickAttack(level, t);
                return;
            }

            double dist = Math.sqrt(t.distanceToSqr(this.o.getX(), t.getY(), this.o.getZ()));
            float facing = this.o.angleTo(t.position());
            if (sensed && Math.abs(t.getY() - this.o.getY()) < 4.0) {
                if (dist < 2.8 && this.o.stompCooldown <= 0) {
                    this.begin(3, ArmorOverlordEntity.EVENT_STOMP);
                } else if (dist < 5.0 && facing < 45.0F && this.o.slashCooldown <= 0) {
                    this.begin(2, ArmorOverlordEntity.EVENT_SLASH);
                } else if (dist > 5.2 && dist < 9.0 && facing < 22.0F && this.o.hookCooldown <= 0 && this.o.hasLineOfSight(t)) {
                    this.begin(1, ArmorOverlordEntity.EVENT_HOOK_WINDUP);
                    this.o.playSound(ModSounds.OVERLORD_AMBIENT.get(), 1.6F, 1.25F);
                }
            }
            if (this.o.attack != 0) {
                this.o.getNavigation().stop();
                return;
            }
            if (sensed && dist < 3.6) {
                // close enough: plant the legs and swing around to face it
                this.o.getNavigation().stop();
            } else if (--this.repath <= 0) {
                this.repath = 8;
                this.o.getNavigation().moveTo(goal.x, goal.y, goal.z, dist > 10.0 ? 1.4 : 1.15);
            }
        }

        private void begin(int attack, byte event) {
            this.o.attack = attack;
            this.o.attackTick = 0;
            this.o.level().broadcastEntityEvent(this.o, event);
        }

        private void tickAttack(ServerLevel level, LivingEntity t) {
            int tick = ++this.o.attackTick;
            switch (this.o.attack) {
                case 1 -> {
                    if (tick == ArmorOverlordEntity.HOOK_WINDUP) {
                        level.broadcastEntityEvent(this.o, ArmorOverlordEntity.EVENT_HOOK_STRIKE);
                        this.o.hitWithHook(level, t);
                    }
                    if (tick >= ArmorOverlordEntity.HOOK_TIME) {
                        this.o.attack = 0;
                        this.o.hookCooldown = 70 + this.o.getRandom().nextInt(40);
                    }
                }
                case 2 -> {
                    if (tick == ArmorOverlordEntity.SLASH_HIT_1 || tick == ArmorOverlordEntity.SLASH_HIT_2) this.o.hitWithBlade(level, t);
                    if (tick >= ArmorOverlordEntity.SLASH_TIME) {
                        this.o.attack = 0;
                        this.o.slashCooldown = 28 + this.o.getRandom().nextInt(16);
                    }
                }
                case 3 -> {
                    if (tick == ArmorOverlordEntity.STOMP_HIT) this.o.stompNow(level);
                    if (tick >= ArmorOverlordEntity.STOMP_TIME) {
                        this.o.attack = 0;
                        this.o.stompCooldown = 60 + this.o.getRandom().nextInt(30);
                    }
                }
                default -> this.o.attack = 0;
            }
        }
    }

    /** Lost its prey: go to where it was last seen, then slowly look around before giving up. */
    static class SearchGoal extends Goal {
        private final ArmorOverlordEntity o;
        private boolean arrived;
        private int repath, lookTimer;
        private Vec3 lookAt = Vec3.ZERO;

        SearchGoal(ArmorOverlordEntity o) {
            this.o = o;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.o.getTarget() == null && this.o.searchTime > 0 && this.o.searchPos != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.arrived = false;
            this.repath = 0;
            this.lookTimer = 0;
        }

        @Override
        public void stop() {
            this.o.getNavigation().stop();
            if (this.o.getTarget() == null) {
                this.o.searchPos = null;
                this.o.searchTime = 0;
            }
        }

        @Override
        public void tick() {
            Vec3 at = this.o.searchPos;
            if (at == null) return;
            this.o.searchTime--;
            if (!this.arrived) {
                if (this.o.distanceToSqr(at) < 2.5 * 2.5 || (this.repath < 12 && this.o.getNavigation().isDone())) {
                    this.arrived = true;
                    this.o.getNavigation().stop();
                } else if (--this.repath <= 0) {
                    this.repath = 20;
                    this.o.getNavigation().moveTo(at.x, at.y, at.z, 0.9);
                }
                this.o.getLookControl().setLookAt(at.x, at.y + 1.0, at.z, this.o.getHeadRotSpeed(), this.o.getMaxHeadXRot());
                return;
            }
            if (--this.lookTimer <= 0) {
                this.lookTimer = 25 + this.o.getRandom().nextInt(25);
                float yaw = (this.o.yBodyRot + (this.o.getRandom().nextFloat() - 0.5F) * 300.0F) * Mth.DEG_TO_RAD;
                this.lookAt = this.o.position().add(-Mth.sin(yaw) * 8.0, 1.0, Mth.cos(yaw) * 8.0);
                if (this.o.getRandom().nextInt(3) == 0) this.o.playSound(ModSounds.OVERLORD_AMBIENT.get(), 1.2F, 1.0F);
            }
            this.o.getLookControl().setLookAt(this.lookAt.x, this.lookAt.y, this.lookAt.z, this.o.getHeadRotSpeed(), this.o.getMaxHeadXRot());
        }
    }

    /** Now and then it stops dead, clicks, and slowly sweeps its brain around, listening. */
    static class ListenGoal extends Goal {
        private final ArmorOverlordEntity o;
        private int time;
        private float yaw;

        ListenGoal(ArmorOverlordEntity o) {
            this.o = o;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.o.getTarget() == null && this.o.searchTime <= 0 && this.o.getRandom().nextInt(260) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return this.time > 0 && this.o.getTarget() == null;
        }

        @Override
        public void start() {
            this.time = 60 + this.o.getRandom().nextInt(60);
            this.yaw = this.o.yBodyRot + (this.o.getRandom().nextBoolean() ? 70.0F : -70.0F);
            this.o.getNavigation().stop();
            this.o.playSound(ModSounds.OVERLORD_AMBIENT.get(), 1.3F, 0.9F);
        }

        @Override
        public void tick() {
            this.time--;
            float y = this.yaw * Mth.DEG_TO_RAD;
            this.o.getLookControl().setLookAt(this.o.getX() - Mth.sin(y) * 8.0, this.o.getEyeY(), this.o.getZ() + Mth.cos(y) * 8.0,
                    this.o.getHeadRotSpeed(), this.o.getMaxHeadXRot());
        }
    }
}
