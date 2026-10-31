package com.huwng.alterna.entity;

import com.huwng.alterna.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/** Peaceful day-to-day behaviour of the Cragback: grazing, drinking and staying with the herd. */
final class CragbackGoals {
    private CragbackGoals() {}

    private static boolean isEdiblePlant(BlockState state) {
        return state.is(ModBlocks.LICHEN_GRASS.get()) || state.is(ModBlocks.TALL_LICHEN_GRASS.get())
                || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN);
    }

    /** Lower the head and crop the grass (or graze the lichen turf down to dirt). */
    static class GrazeGoal extends Goal {
        private final CragbackEntity mob;
        private int timer;

        GrazeGoal(CragbackEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (this.mob.getRandom().nextInt(this.mob.isBaby() ? 60 : 500) != 0) return false;
            BlockPos pos = this.mob.blockPosition();
            return isEdiblePlant(this.mob.level().getBlockState(pos))
                    || this.mob.level().getBlockState(pos.below()).is(ModBlocks.LICHEN_TURF.get());
        }

        @Override
        public void start() {
            this.timer = this.adjustedTickDelay(CragbackEntity.EAT_TICKS);
            this.mob.level().broadcastEntityEvent(this.mob, CragbackEntity.EVENT_EAT);
            this.mob.getNavigation().stop();
        }

        @Override
        public boolean canContinueToUse() {
            return this.timer > 0;
        }

        @Override
        public void tick() {
            this.timer = Math.max(0, this.timer - 1);
            if (this.timer != this.adjustedTickDelay(4)) return;
            BlockPos pos = this.mob.blockPosition();
            ServerLevel level = (ServerLevel) this.mob.level();
            BlockState here = level.getBlockState(pos);
            if (isEdiblePlant(here)) {
                level.destroyBlock(pos, false);
                this.mob.onGrazed();
            } else {
                BlockPos below = pos.below();
                if (level.getBlockState(below).is(ModBlocks.LICHEN_TURF.get())) {
                    level.levelEvent(2001, below, Block.getId(ModBlocks.LICHEN_TURF.get().defaultBlockState()));
                    // the turf creeps back on its own (LichenTurfBlock#randomTick)
                    if (this.mob.getRandom().nextInt(3) == 0) {
                        level.setBlock(below, Blocks.DIRT.defaultBlockState(), 2);
                    }
                    this.mob.onGrazed();
                }
            }
            this.mob.playSound(SoundEvents.HORSE_EAT, 0.8F, 0.6F);
        }
    }

    /** Every so often walk to the nearest pond and drink for a while. */
    static class DrinkGoal extends Goal {
        private static final int THIRSTY = 3000;
        private final CragbackEntity mob;
        private BlockPos water;
        private BlockPos stand;
        private int drinking;
        private int timeout;

        DrinkGoal(CragbackEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.mob.thirst < THIRSTY || this.mob.getRandom().nextInt(40) != 0 || this.mob.getTarget() != null) return false;
            return this.findWater();
        }

        private boolean findWater() {
            BlockPos origin = this.mob.blockPosition();
            var level = this.mob.level();
            BlockPos best = null, bestStand = null;
            double bestDist = Double.MAX_VALUE;
            for (int i = 0; i < 160; i++) {
                BlockPos p = origin.offset(this.mob.getRandom().nextInt(33) - 16, this.mob.getRandom().nextInt(7) - 4,
                        this.mob.getRandom().nextInt(33) - 16);
                if (!level.getFluidState(p).is(FluidTags.WATER) || !level.getBlockState(p.above()).isAir()) continue;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    BlockPos s = p.relative(d);
                    if (level.getBlockState(s).isAir() && level.getBlockState(s.below()).isFaceSturdy(level, s.below(), Direction.UP)) {
                        double dist = s.distSqr(origin);
                        if (dist < bestDist) { bestDist = dist; best = p; bestStand = s; }
                    }
                }
            }
            this.water = best;
            this.stand = bestStand;
            return best != null;
        }

        @Override
        public void start() {
            this.drinking = 0;
            this.timeout = 400;
            this.mob.getNavigation().moveTo(this.stand.getX() + 0.5, this.stand.getY(), this.stand.getZ() + 0.5, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return this.timeout > 0 && (this.drinking > 0 || !this.mob.getNavigation().isDone()
                    || this.mob.blockPosition().distSqr(this.stand) <= 9);
        }

        @Override
        public void tick() {
            this.timeout--;
            Vec3 w = Vec3.atCenterOf(this.water);
            this.mob.getLookControl().setLookAt(w.x, w.y - 0.4, w.z);
            if (this.drinking > 0) {
                if (--this.drinking == 0) {
                    this.mob.thirst = 0;
                    this.timeout = 0;
                } else if (this.drinking % 15 == 0) {
                    this.mob.playSound(SoundEvents.GENERIC_DRINK.value(), 0.6F, 0.6F);
                }
                return;
            }
            if (this.mob.position().distanceToSqr(w) < 3.6 * 3.6) {
                this.mob.getNavigation().stop();
                this.drinking = this.adjustedTickDelay(CragbackEntity.DRINK_TICKS);
                this.mob.level().broadcastEntityEvent(this.mob, CragbackEntity.EVENT_DRINK);
            } else if (this.mob.getNavigation().isDone()) {
                this.mob.getNavigation().moveTo(w.x, w.y, w.z, 1.0);
            }
        }

        @Override
        public void stop() {
            this.water = null;
        }
    }

    /** Stragglers wander back toward the middle of their herd. */
    static class HerdGoal extends Goal {
        private final CragbackEntity mob;
        private Vec3 center;
        private int recheck;

        HerdGoal(CragbackEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (--this.recheck > 0) return false;
            this.recheck = this.adjustedTickDelay(60 + this.mob.getRandom().nextInt(40));
            List<CragbackEntity> herd = this.mob.level().getEntitiesOfClass(CragbackEntity.class,
                    this.mob.getBoundingBox().inflate(24.0, 8.0, 24.0), e -> e != this.mob && e.isAlive());
            if (herd.isEmpty()) return false;
            double x = 0, y = 0, z = 0;
            for (CragbackEntity e : herd) { x += e.getX(); y += e.getY(); z += e.getZ(); }
            this.center = new Vec3(x / herd.size(), y / herd.size(), z / herd.size());
            return this.mob.position().distanceToSqr(this.center) > 9 * 9;
        }

        @Override
        public void start() {
            // not the exact centre - everyone aiming at one point is how they ended up head-butting
            double a = (this.mob.getUUID().getLeastSignificantBits() & 0xFFFF) / 65536.0 * Math.PI * 2.0;
            double r = 3.5 + (this.mob.getUUID().getMostSignificantBits() & 0xFF) / 255.0 * 3.0;
            this.mob.getNavigation().moveTo(this.center.x + Math.cos(a) * r, this.center.y, this.center.z + Math.sin(a) * r, 0.9);
        }

        @Override
        public boolean canContinueToUse() {
            return !this.mob.getNavigation().isDone() && this.mob.position().distanceToSqr(this.center) > 5 * 5;
        }
    }
}
