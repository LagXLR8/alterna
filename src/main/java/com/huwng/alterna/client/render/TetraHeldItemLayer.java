package com.huwng.alterna.client.render;

import com.huwng.alterna.client.model.TetraModel;
import com.huwng.alterna.client.render.state.TetraRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Renders whatever the Tetra carries (berries) as a real item in its beak, like the vanilla fox. */
public class TetraHeldItemLayer extends RenderLayer<TetraRenderState, TetraModel> {
    public TetraHeldItemLayer(RenderLayerParent<TetraRenderState, TetraModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, TetraRenderState state, float yRot, float xRot) {
        ItemStackRenderState item = state.heldItem;
        if (item.isEmpty()) return;
        poseStack.pushPose();
        this.getParentModel().translateToBeak(poseStack);
        // lay the item flat between the beak halves, pointing forward
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        float scale = this.getParentModel().heldItemScale();
        poseStack.scale(scale, scale, scale);
        item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
    }
}
