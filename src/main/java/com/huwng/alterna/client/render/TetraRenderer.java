package com.huwng.alterna.client.render;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.client.ModModelLayers;
import com.huwng.alterna.client.model.TetraModel;
import com.huwng.alterna.client.render.state.TetraRenderState;
import com.huwng.alterna.entity.TetraEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.resources.Identifier;

public class TetraRenderer extends AgeableMobRenderer<TetraEntity, TetraRenderState, TetraModel> {
    private static final Identifier TEXTURE = Alterna.id("textures/entity/tetra/tetra.png");
    private static final Identifier BABY_TEXTURE = Alterna.id("textures/entity/tetra/tetra_baby.png");

    public TetraRenderer(EntityRendererProvider.Context context) {
        super(context, new TetraModel(context.bakeLayer(ModModelLayers.TETRA)),
                new TetraModel(context.bakeLayer(ModModelLayers.TETRA_BABY), true), 0.6F);
        this.addLayer(new TetraHeldItemLayer(this));
    }

    @Override
    public Identifier getTextureLocation(TetraRenderState state) {
        return state.isBaby ? BABY_TEXTURE : TEXTURE;
    }

    @Override
    public TetraRenderState createRenderState() {
        return new TetraRenderState();
    }

    @Override
    public void extractRenderState(TetraEntity entity, TetraRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.perched = entity.isPerched();
        state.flying = entity.isFlying();
        state.diving = entity.isDiving();
        state.tame = entity.isTame();
        state.perchYaw = entity.getPerchYaw();
        state.roll = entity.getRoll(partialTicks);
        state.flightPitch = entity.getFlightPitch(partialTicks);
        state.flap = entity.getFlap(partialTicks);
        state.flapPhase = entity.getFlapPhase(partialTicks);
        state.tailSwing = entity.getTailSwing(partialTicks);
        state.tailLift = entity.getTailLift(partialTicks);
        state.groundPitch = entity.getGroundPitch(partialTicks);
        HoldingEntityRenderState.extractHoldingEntityRenderState(entity, state, this.itemModelResolver);
    }

    @Override
    protected void setupRotations(TetraRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        if (state.perched) {
            // face the cliff; the model itself stands the body up against the rock. The entity
            // (and its hitbox) sits a little back from the wall, so slide the model onto the face.
            super.setupRotations(state, poseStack, state.perchYaw, entityScale);
            poseStack.translate(0.0F, 0.0F, -0.3F);
            return;
        }
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        if (!state.flying && Math.abs(state.groundPitch) > 0.01F) {
            // walking up/down a slope: tilt with the ground under its wing-feet
            poseStack.translate(0.0F, 0.3F, 0.0F);
            poseStack.mulPose(Axis.XP.rotation(state.groundPitch));
            poseStack.translate(0.0F, -0.3F, 0.0F);
        }
        if (state.flying) {
            // pitch with the flight path and bank into turns, pivoting around the body's centre
            float pivot = 0.45F * entityScale;
            poseStack.translate(0.0F, pivot, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-state.flightPitch));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-state.roll));
            poseStack.translate(0.0F, -pivot, 0.0F);
        }
    }

    @Override
    protected void scale(TetraRenderState state, PoseStack poseStack) {
        poseStack.scale(1.15F, 1.15F, 1.15F);
    }
}
