package com.huwng.alterna.mixin;

import com.huwng.alterna.gravity.GravityApi;
import com.huwng.alterna.gravity.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Projectile.class)
public abstract class ProjectileMixin {

    /**
     * Adjust projectile shooting direction based on shooter's gravity orientation.
     */
    @ModifyVariable(
            method = "shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V",
            at = @At("HEAD"),
            ordinal = 0
    )
    private float alterna$modifyShootPitch(float pitch, Entity shooter) {
        Direction dir = GravityApi.getDirection(shooter);
        return dir == Direction.DOWN ? pitch : RotationUtil.rotPlayerToWorld(shooter.getYRot(), shooter.getXRot(), dir).y;
    }

    @ModifyVariable(
            method = "shootFromRotation(Lnet/minecraft/world/entity/Entity;FFFFF)V",
            at = @At("HEAD"),
            ordinal = 1
    )
    private float alterna$modifyShootYaw(float yaw, Entity shooter) {
        Direction dir = GravityApi.getDirection(shooter);
        return dir == Direction.DOWN ? yaw : RotationUtil.rotPlayerToWorld(shooter.getYRot(), shooter.getXRot(), dir).x;
    }
}
