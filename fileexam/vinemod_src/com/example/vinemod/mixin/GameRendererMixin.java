package com.example.vinemod.mixin;

import com.example.vinemod.duck.GameRendererDuck;
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

@Mixin({GameRenderer.class})
public class GameRendererMixin implements GameRendererDuck {
   @Unique
   int zipline$ziplineTilt = 0;
   @Unique
   float zipline$ziplineTiltDirection;
   @Shadow
   @Final
   private Minecraft minecraft;

   @Inject(
      method = {"bobHurt"},
      at = {@At("HEAD")}
   )
   void bobHurt(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci) {
      if (this.zipline$ziplineTilt > 0) {
         float tickDelta = this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
         float progress = ((float)this.zipline$ziplineTilt - tickDelta) / 10.0F;
         float ease = Mth.sin((double)(progress * (float)Math.PI));
         poseStack.mulPose(Axis.YP.rotationDegrees(-this.zipline$ziplineTiltDirection));
         float tiltStrength = (float)((double)ease * (double)5.0F * (Double)this.minecraft.options.damageTiltStrength().get());
         poseStack.mulPose(Axis.ZP.rotationDegrees(tiltStrength));
         poseStack.mulPose(Axis.YP.rotationDegrees(this.zipline$ziplineTiltDirection));
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   void tick(CallbackInfo ci) {
      --this.zipline$ziplineTilt;
   }

   public void zipline$setZiplineTilt(float yaw) {
      this.zipline$ziplineTiltDirection = yaw;
      this.zipline$ziplineTilt = 10;
   }
}
