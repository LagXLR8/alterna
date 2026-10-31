package com.huwng.alterna.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

/**
 * Heavy-animal steering: the Cragback turns slowly (a few degrees per tick) and slows down
 * while turning, so it walks around in an arc instead of snapping on the spot. It also
 * keeps its distance from herd mates: it veers away from any Cragback in front of it and
 * slows down rather than walking into it.
 */
public class CragbackMoveControl extends MoveControl {
    private final CragbackEntity cragback;

    public CragbackMoveControl(CragbackEntity mob) {
        super(mob);
        this.cragback = mob;
    }

    @Override
    public void tick() {
        if (this.operation != Operation.MOVE_TO) {
            super.tick();
            return;
        }
        this.operation = Operation.WAIT;
        double dx = this.wantedX - this.mob.getX();
        double dz = this.wantedZ - this.mob.getZ();
        double dy = this.wantedY - this.mob.getY();
        if (dx * dx + dy * dy + dz * dz < 2.5000003E-7) {
            this.mob.setZza(0.0F);
            return;
        }
        float yaw = this.mob.getYRot();
        float targetYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;

        // herd spacing
        float crowd = 1.0F;
        float avoid = 0.0F;
        Vec3 fwd = Vec3.directionFromRotation(0.0F, yaw);
        for (CragbackEntity other : this.mob.level().getEntitiesOfClass(CragbackEntity.class, this.mob.getBoundingBox().inflate(3.5),
                e -> e != this.mob && e.isAlive())) {
            Vec3 to = other.position().subtract(this.mob.position()).multiply(1, 0, 1);
            double d = to.length();
            if (d < 1.0E-3) continue;
            Vec3 dir = to.scale(1.0 / d);
            double ahead = dir.dot(fwd);
            if (ahead < 0.2) continue;                       // only those in front matter
            double gap = d - (this.mob.getBbWidth() + other.getBbWidth()) * 0.5;
            double cross = fwd.x * dir.z - fwd.z * dir.x;    // which side the other one is on
            float push = (float) Mth.clamp((2.5 - gap) * 18.0, 0.0, 50.0) * (float) ahead;
            avoid += cross > 0 ? -push : push;
            if (ahead > 0.7) crowd = Math.min(crowd, (float) Mth.clamp(gap / 2.0, 0.0, 1.0));
        }
        targetYaw += Mth.clamp(avoid, -70.0F, 70.0F);

        float turnRate = this.cragback.isCharging() ? 9.0F : 5.0F;
        float diff = Mth.wrapDegrees(targetYaw - yaw);
        this.mob.setYRot(this.rotlerp(yaw, targetYaw, turnRate));
        // big turn -> walk slowly around it; facing the right way -> full pace
        float turning = (float) Mth.clamp(Math.cos(diff * Mth.DEG_TO_RAD), 0.25, 1.0);
        float speed = (float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
        this.mob.setSpeed(speed * turning * Math.max(crowd, 0.05F));
    }
}
