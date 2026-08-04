package com.huwng.alterna.mixin;

import com.huwng.alterna.gravity.GravityApi;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.control.LookControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LookControl.class)
public abstract class LookControlMixin {

    @Redirect(
            method = "getWantedY",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getEyeY()D", ordinal = 0),
            require = 0
    )
    private static double alterna$redirectGetWantedY(Entity entity) {
        Direction dir = GravityApi.getDirection(entity);
        return dir == Direction.DOWN ? entity.getEyeY() : entity.getEyePosition().y;
    }

    @Redirect(
            method = "setLookAt(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D", ordinal = 0),
            require = 0
    )
    private double alterna$redirectSetLookAtX(Entity entity) {
        Direction dir = GravityApi.getDirection(entity);
        return dir == Direction.DOWN ? entity.getX() : entity.getEyePosition().x;
    }

    @Redirect(
            method = "setLookAt(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D", ordinal = 0),
            require = 0
    )
    private double alterna$redirectSetLookAtZ(Entity entity) {
        Direction dir = GravityApi.getDirection(entity);
        return dir == Direction.DOWN ? entity.getZ() : entity.getEyePosition().z;
    }

    @Redirect(
            method = "setLookAt(Lnet/minecraft/world/entity/Entity;FF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D", ordinal = 0),
            require = 0
    )
    private double alterna$redirectSetLookAtX2(Entity entity) {
        Direction dir = GravityApi.getDirection(entity);
        return dir == Direction.DOWN ? entity.getX() : entity.getEyePosition().x;
    }

    @Redirect(
            method = "setLookAt(Lnet/minecraft/world/entity/Entity;FF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D", ordinal = 0),
            require = 0
    )
    private double alterna$redirectSetLookAtZ2(Entity entity) {
        Direction dir = GravityApi.getDirection(entity);
        return dir == Direction.DOWN ? entity.getZ() : entity.getEyePosition().z;
    }
}
