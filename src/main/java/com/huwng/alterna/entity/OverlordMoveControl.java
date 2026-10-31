package com.huwng.alterna.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * The Overlord never snaps around: it turns a few degrees per tick and slows down while
 * turning, so it swings its whole body around in an arc. That slowness is what lets a
 * quick player get behind it.
 */
public class OverlordMoveControl extends MoveControl {
    public OverlordMoveControl(ArmorOverlordEntity mob) {
        super(mob);
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
        float turnRate = this.mob.getTarget() != null ? 4.5F : 3.0F;
        float diff = Mth.wrapDegrees(targetYaw - yaw);
        this.mob.setYRot(this.rotlerp(yaw, targetYaw, turnRate));
        // facing away from where it wants to go -> barely creep forward while it swings around
        float turning = (float) Mth.clamp(Math.cos(diff * Mth.DEG_TO_RAD), 0.12, 1.0);
        float speed = (float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
        this.mob.setSpeed(speed * turning);
    }
}
