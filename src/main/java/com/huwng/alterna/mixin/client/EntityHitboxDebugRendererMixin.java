package com.huwng.alterna.mixin.client;

import com.huwng.alterna.gravity.GravityApi;
import com.huwng.alterna.gravity.RotationUtil;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityHitboxDebugRenderer.class)
public abstract class EntityHitboxDebugRendererMixin {

    @Redirect(method = "showHitboxes", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"), require = 0)
    private Vec3 alterna$redirectEyePositionInHitboxDebug(Vec3 currentPosition, double x, double y, double z, Entity entity, float partialTicks, boolean isServerEntity) {
        Direction gravityDirection = GravityApi.getDirection(entity);
        if (gravityDirection == Direction.DOWN) {
            return currentPosition.add(x, y, z);
        }
        Vec3 eyeOffsetWorld = RotationUtil.vecPlayerToWorld(0.0, entity.getEyeHeight(), 0.0, gravityDirection);
        return currentPosition.add(eyeOffsetWorld);
    }

    @Redirect(method = "showHitboxes", at = @At(value = "NEW", target = "(DDDDDD)Lnet/minecraft/world/phys/AABB;"), require = 0)
    private AABB alterna$redirectRedEyeBoxInHitboxDebug(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, Entity entity, float partialTicks, boolean isServerEntity) {
        Direction gravityDirection = GravityApi.getDirection(entity);
        if (gravityDirection == Direction.DOWN) {
            return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }

        AABB playerBoxLocal = RotationUtil.boxWorldToPlayer(entity.getBoundingBox(), gravityDirection);
        double localEyeY = playerBoxLocal.minY + entity.getEyeHeight();
        AABB redBoxLocal = new AABB(playerBoxLocal.minX, localEyeY - 0.01, playerBoxLocal.minZ, playerBoxLocal.maxX, localEyeY + 0.01, playerBoxLocal.maxZ);

        Vec3 latestPosition = entity.position();
        Vec3 currentPosition = entity.getPosition(partialTicks);
        Vec3 offset = currentPosition.subtract(latestPosition);

        return RotationUtil.boxPlayerToWorld(redBoxLocal, gravityDirection).move(offset);
    }
}
