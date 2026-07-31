package com.huwng.alterna.mixin.client;

import com.huwng.alterna.gravity.GravityApi;
import com.huwng.alterna.gravity.RotationUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(method = "submit", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V", ordinal = 0, shift = At.Shift.AFTER), require = 0)
    private void alterna$onSubmitTranslate(EntityRenderState renderState, CameraRenderState camera, double x, double y, double z, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo ci) {
        if (renderState instanceof AvatarRenderState) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                Direction gravityDirection = GravityApi.getDirection(player);
                if (gravityDirection != Direction.DOWN) {
                    Quaternionf rot = new Quaternionf(RotationUtil.getWorldRotationQuaternion(gravityDirection)).conjugate();
                    poseStack.mulPose(rot);
                }
            }
        }
    }
}
