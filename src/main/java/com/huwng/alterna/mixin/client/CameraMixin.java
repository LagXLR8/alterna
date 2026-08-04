package com.huwng.alterna.mixin.client;

import com.huwng.alterna.gravity.GravityApi;
import com.huwng.alterna.gravity.GravityData;
import com.huwng.alterna.gravity.RotationUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    private Entity entity;
    @Shadow
    @Final
    private Quaternionf rotation;

    @Shadow
    private float eyeHeightOld;
    @Shadow
    private float eyeHeight;

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "setRotation(FFF)V", at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;", shift = At.Shift.AFTER, remap = false), require = 0)
    private void alterna$onSetRotation(float yRot, float xRot, float roll, CallbackInfo ci) {
        if (this.entity != null) {
            GravityData data = GravityApi.getData(this.entity);
            Direction currentDir = data.getDirection();
            Direction prevDir = data.getPrevDirection();

            if (currentDir == Direction.DOWN && prevDir == Direction.DOWN) {
                return;
            }

            float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
            float progress = data.getAnimationProgress(partialTicks);
            float smoothProgress = progress * progress * (3.0f - 2.0f * progress);

            // Use startGravityQuaternion when set (by LocalPlayerMixin) so the animation
            // starts at the exact pre-change camera state — eliminates visual jumps.
            Quaternionf startQuat = data.getStartGravityQuaternion();
            if (startQuat == null) startQuat = RotationUtil.getWorldRotationQuaternion(prevDir);
            Quaternionf qCurr = RotationUtil.getWorldRotationQuaternion(currentDir);
            Quaternionf gravityRotation = new Quaternionf(startQuat).slerp(qCurr, smoothProgress);

            Quaternionf rot = new Quaternionf(gravityRotation).conjugate();
            rot.mul(this.rotation);
            this.rotation.set(rot.x(), rot.y(), rot.z(), rot.w());
        }
    }

    @Redirect(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", ordinal = 0), require = 0)
    private void alterna$redirectAlignWithEntitySetPosition(Camera camera, double x, double y, double z) {
        if (this.entity != null) {
            GravityData data = GravityApi.getData(this.entity);
            Direction currentDir = data.getDirection();
            Direction prevDir = data.getPrevDirection();

            if (currentDir != Direction.DOWN || prevDir != Direction.DOWN) {
                float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
                float progress = data.getAnimationProgress(partialTicks);
                float smoothProgress = progress * progress * (3.0f - 2.0f * progress);

                Quaternionf startQuat = data.getStartGravityQuaternion();
                if (startQuat == null) startQuat = RotationUtil.getWorldRotationQuaternion(prevDir);
                Quaternionf qCurr = RotationUtil.getWorldRotationQuaternion(currentDir);
                Quaternionf gravityRotation = new Quaternionf(startQuat).slerp(qCurr, smoothProgress);

                double entityX = Mth.lerp((double) partialTicks, this.entity.xo, this.entity.getX());
                double entityY = Mth.lerp((double) partialTicks, this.entity.yo, this.entity.getY());
                double entityZ = Mth.lerp((double) partialTicks, this.entity.zo, this.entity.getZ());
                double currentEyeHeight = (double) Mth.lerp(partialTicks, this.eyeHeightOld, this.eyeHeight);

                Quaternionf gravityRotForEntity = new Quaternionf(gravityRotation).conjugate();
                org.joml.Vector3f eyeOffset = new org.joml.Vector3f(0.0f, (float) currentEyeHeight, 0.0f).rotate(gravityRotForEntity);

                this.setPosition(entityX + eyeOffset.x(), entityY + eyeOffset.y(), entityZ + eyeOffset.z());
                return;
            }
        }
        this.setPosition(x, y, z);
    }
}
