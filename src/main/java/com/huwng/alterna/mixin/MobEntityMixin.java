package com.huwng.alterna.mixin;

import com.huwng.alterna.gravity.GravityApi;
import com.huwng.alterna.gravity.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobEntityMixin {

    /**
     * Fixes mob melee attack knockback & orientation when targeting a player in custom gravity (e.g. Zombies).
     */
    @Inject(method = "doHurtTarget", at = @At("HEAD"), require = 0)
    private void alterna$onDoHurtTarget(net.minecraft.server.level.ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;
        Direction targetGravity = GravityApi.getDirection(target);
        if (targetGravity != Direction.DOWN) {
            // Adjust mob looking angle towards player eye position
            Vec3 targetEye = target.getEyePosition();
            Vec3 selfEye = self.getEyePosition();
            Vec3 diff = targetEye.subtract(selfEye);
            double dx = diff.x;
            double dy = diff.y;
            double dz = diff.z;
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            float pitch = (float) (-(Math.atan2(dy, horizontalDist) * (180.0 / Math.PI)));
            self.setYRot(yaw);
            self.setXRot(pitch);
            self.yBodyRot = yaw;
            self.yHeadRot = yaw;
        }
    }

    /**
     * Uses target's 3D eye position rather than vanilla fixed Y coordinate for Mob looking logic.
     */
    @Redirect(
            method = "lookAt(Lnet/minecraft/world/entity/Entity;FF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getEyeY()D", ordinal = 0),
            require = 0
    )
    private double alterna$redirectLookAtEyeY(LivingEntity living) {
        Direction dir = GravityApi.getDirection(living);
        return dir == Direction.DOWN ? living.getEyeY() : living.getEyePosition().y;
    }

    @Redirect(
            method = "lookAt(Lnet/minecraft/world/entity/Entity;FF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D", ordinal = 0),
            require = 0
    )
    private double alterna$redirectLookAtX(Entity entity) {
        Direction dir = GravityApi.getDirection(entity);
        return dir == Direction.DOWN ? entity.getX() : entity.getEyePosition().x;
    }

    @Redirect(
            method = "lookAt(Lnet/minecraft/world/entity/Entity;FF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D", ordinal = 0),
            require = 0
    )
    private double alterna$redirectLookAtZ(Entity entity) {
        Direction dir = GravityApi.getDirection(entity);
        return dir == Direction.DOWN ? entity.getZ() : entity.getEyePosition().z;
    }
}
