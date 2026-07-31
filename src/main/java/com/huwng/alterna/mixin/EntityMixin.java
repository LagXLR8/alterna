package com.huwng.alterna.mixin;

import com.huwng.alterna.gravity.GravityApi;
import com.huwng.alterna.gravity.RotationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public double xo;
    @Shadow public double yo;
    @Shadow public double zo;

    @Inject(method = "move", at = @At("RETURN"))
    private void alterna$fixOnGroundAndVelocityInMove(MoverType moverType, Vec3 deltaWorld, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        Direction direction = GravityApi.getDirection(self);
        if (direction == Direction.DOWN) {
            return;
        }

        Vec3 deltaLocal = RotationUtil.vecWorldToPlayer(deltaWorld, direction);
        Vec3 movementWorld = self.position().subtract(self.xOld, self.yOld, self.zOld);
        Vec3 movementLocal = RotationUtil.vecWorldToPlayer(movementWorld, direction);

        boolean localXCollision = !Mth.equal(deltaLocal.x, movementLocal.x);
        boolean localZCollision = !Mth.equal(deltaLocal.z, movementLocal.z);
        boolean localHorizontalCollision = localXCollision || localZCollision;

        boolean localVerticalCollisionBelow = deltaLocal.y < -1.0E-5 && movementLocal.y > deltaLocal.y + 1.0E-5;

        self.setOnGroundWithMovement(localVerticalCollisionBelow, localHorizontalCollision, movementWorld);
    }

    @Inject(method = "makeBoundingBox(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/AABB;", at = @At("RETURN"), cancellable = true, require = 0)
    private void alterna$makeBoundingBox(Vec3 position, CallbackInfoReturnable<AABB> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Projectile) return;
        Direction gravityDirection = GravityApi.getDirection(self);
        if (gravityDirection == Direction.DOWN) return;

        AABB box = cir.getReturnValue().move(position.reverse());
        if (gravityDirection.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            box = box.move(0.0, -1.0E-6, 0.0);
        }
        cir.setReturnValue(RotationUtil.boxPlayerToWorld(box, gravityDirection).move(position));
    }

    @Inject(method = "calculateViewVector", at = @At("RETURN"), cancellable = true, require = 0)
    private void alterna$calculateViewVector(CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        Direction gravityDirection = GravityApi.getDirection(self);
        if (gravityDirection == Direction.DOWN) return;
        cir.setReturnValue(RotationUtil.vecPlayerToWorld(cir.getReturnValue(), gravityDirection));
    }

    @Inject(method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true, require = 0)
    private void alterna$getEyePositionHead(CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        Direction gravityDirection = GravityApi.getDirection(self);
        if (gravityDirection == Direction.DOWN) return;
        cir.setReturnValue(RotationUtil.vecPlayerToWorld(0.0, self.getEyeHeight(), 0.0, gravityDirection).add(self.position()));
    }

    @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true, require = 0)
    private void alterna$getEyePositionPartial(float tickDelta, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        Direction gravityDirection = GravityApi.getDirection(self);
        if (gravityDirection == Direction.DOWN) return;
        Vec3 eyeOffsetWorld = RotationUtil.vecPlayerToWorld(0.0, self.getEyeHeight(), 0.0, gravityDirection);
        double x = Mth.lerp((double) tickDelta, self.xo, self.getX()) + eyeOffsetWorld.x;
        double y = Mth.lerp((double) tickDelta, self.yo, self.getY()) + eyeOffsetWorld.y;
        double z = Mth.lerp((double) tickDelta, self.zo, self.getZ()) + eyeOffsetWorld.z;
        cir.setReturnValue(new Vec3(x, y, z));
    }

    @Shadow protected static Vec3 getInputVector(Vec3 input, float speed, float yRot) { throw new AssertionError(); }

    @Inject(method = "moveRelative", at = @At("HEAD"), cancellable = true, require = 0)
    private void alterna$moveRelativeDirectional(float speed, Vec3 input, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        Direction gravityDirection = GravityApi.getDirection(self);
        if (gravityDirection == Direction.DOWN) {
            return;
        }

        ci.cancel();
        Vec3 localDelta = getInputVector(input, speed, self.getYRot());
        Vec3 worldDelta = RotationUtil.vecPlayerToWorld(localDelta, gravityDirection);
        self.setDeltaMovement(self.getDeltaMovement().add(worldDelta));
    }

    @Inject(method = "getBlockPosBelowThatAffectsMyMovement", at = @At("HEAD"), cancellable = true, require = 0)
    private void alterna$getBlockPosBelowThatAffectsMyMovement(CallbackInfoReturnable<BlockPos> cir) {
        Entity self = (Entity) (Object) this;
        Direction gravityDirection = GravityApi.getDirection(self);
        if (gravityDirection == Direction.DOWN) return;
        cir.setReturnValue(BlockPos.containing(self.position().add(gravityDirection.getUnitVec3().scale(0.5000001))));
    }

    @Inject(method = "getOnPosLegacy", at = @At("HEAD"), cancellable = true, require = 0)
    private void alterna$getOnPosLegacy(CallbackInfoReturnable<BlockPos> cir) {
        Entity self = (Entity) (Object) this;
        Direction gravityDirection = GravityApi.getDirection(self);
        if (gravityDirection == Direction.DOWN) return;
        cir.setReturnValue(BlockPos.containing(RotationUtil.vecPlayerToWorld(0.0, -0.2, 0.0, gravityDirection).add(self.position())));
    }
}
