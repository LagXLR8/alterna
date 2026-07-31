package com.huwng.alterna.mixin.client;

import com.huwng.alterna.gravity.GravityApi;
import com.huwng.alterna.gravity.GravityData;
import com.huwng.alterna.gravity.RotationUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
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

    @Shadow private Entity entity;
    @Shadow @Final private Quaternionf rotation;
    @Shadow protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "setRotation(FFF)V", at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;", shift = At.Shift.AFTER, remap = false), require = 0)
    private void alterna$onSetRotation(float yRot, float xRot, float roll, CallbackInfo ci) {
        if (this.entity != null) {
            GravityData data = GravityApi.getData(this.entity);
            Direction currentDir = data.getDirection();
            Direction prevDir = data.getPrevDirection();

            if (currentDir == Direction.DOWN && prevDir == Direction.DOWN) return;

            float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
            float progress = data.getAnimationProgress(partialTicks);

            Quaternionf qPrev = RotationUtil.getCameraRotationQuaternion(prevDir);
            Quaternionf qCurr = RotationUtil.getCameraRotationQuaternion(currentDir);
            Quaternionf qSmooth = new Quaternionf(qPrev).slerp(qCurr, progress);

            qSmooth.mul(this.rotation);

            this.rotation.set(qSmooth.x(), qSmooth.y(), qSmooth.z(), qSmooth.w());
        }
    }

    @Redirect(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", ordinal = 0), require = 0)
    private void alterna$redirectAlignWithEntitySetPosition(Camera camera, double x, double y, double z) {
        if (this.entity != null) {
            Direction gravityDirection = GravityApi.getDirection(this.entity);
            if (gravityDirection != Direction.DOWN) {
                float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
                Vec3 eyePosWorld = this.entity.getEyePosition(partialTicks);
                this.setPosition(eyePosWorld.x, eyePosWorld.y, eyePosWorld.z);
                return;
            }
        }
        this.setPosition(x, y, z);
    }
}
