package com.huwng.alterna.client.render;

import com.huwng.alterna.client.render.state.ThrownStormbreakerRenderState;
import com.huwng.alterna.entity.ThrownStormbreakerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

public class ThrownStormbreakerRenderer extends EntityRenderer<ThrownStormbreakerEntity, ThrownStormbreakerRenderState> {

    private final ItemModelResolver itemModelResolver;

    public ThrownStormbreakerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public ThrownStormbreakerRenderState createRenderState() {
        return new ThrownStormbreakerRenderState();
    }

    @Override
    public void extractRenderState(ThrownStormbreakerEntity entity, ThrownStormbreakerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = entity.getYRot(partialTicks);
        state.xRot = entity.getXRot(partialTicks);
        state.spinTicks = entity.tickCount + partialTicks;
        this.itemModelResolver.updateForNonLiving(state.item, entity.getItem(), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, entity);
    }

    @Override
    public void submit(ThrownStormbreakerRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();

        // Orient toward flight trajectory (vertical standing alignment)
        poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.xRot));

        // Re-orient blade face 90° forward along flight path
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));

        // Smooth end-over-end vertical flip along flight path
        poseStack.mulPose(Axis.ZP.rotationDegrees(state.spinTicks * 22.0F));

        // Scale 3D weapon model
        poseStack.scale(1.2F, 1.2F, 1.2F);

        state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }
}
