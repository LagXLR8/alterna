package com.huwng.alterna.mixin.client;

import com.huwng.alterna.vine.duck.VineGameRendererDuck;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Camera tilt effect when the player attaches to a vine.
 * Ported from Zipline mod (MIT) — credit: Evan, Tomate0613.
 */
@Mixin(GameRenderer.class)
public class VineGameRendererMixin implements VineGameRendererDuck {
    @Unique
    private int vine$tiltTicks = 0;
    @Unique
    private float vine$tiltDirection;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "bobHurt", at = @At("HEAD"))
    private void vine$bobHurt(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
        if (this.vine$tiltTicks > 0) {
            float tickDelta = this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            float progress = ((float) this.vine$tiltTicks - tickDelta) / 10.0F;
            float ease = Mth.sin((double) (progress * (float) Math.PI));
            poseStack.mulPose(Axis.YP.rotationDegrees(-this.vine$tiltDirection));
            float tiltStrength = (float) ((double) ease * 5.0 * (Double) this.minecraft.options.damageTiltStrength().get());
            poseStack.mulPose(Axis.ZP.rotationDegrees(tiltStrength));
            poseStack.mulPose(Axis.YP.rotationDegrees(this.vine$tiltDirection));
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void vine$tick(CallbackInfo ci) {
        --this.vine$tiltTicks;
    }

    @Override
    public void vine$setVineTilt(float yaw) {
        this.vine$tiltDirection = yaw;
        this.vine$tiltTicks = 10;
    }
}
