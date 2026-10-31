package com.huwng.alterna.client.render;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.client.ModModelLayers;
import com.huwng.alterna.client.model.ArmorOverlordModel;
import com.huwng.alterna.client.render.state.ArmorOverlordRenderState;
import com.huwng.alterna.entity.ArmorOverlordEntity;
import com.huwng.alterna.entity.OverlordAnimator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

public class ArmorOverlordRenderer extends MobRenderer<ArmorOverlordEntity, ArmorOverlordRenderState, ArmorOverlordModel> {
    private static final Identifier TEXTURE = Alterna.id("textures/entity/armor_overlord/armor_overlord.png");
    private static final Identifier GLOW = Alterna.id("textures/entity/armor_overlord/armor_overlord_glow.png");

    public ArmorOverlordRenderer(EntityRendererProvider.Context context) {
        super(context, new ArmorOverlordModel(context.bakeLayer(ModModelLayers.ARMOR_OVERLORD)), 2.6F);
        this.addLayer(new BrainGlowLayer(this));
    }

    @Override
    public Identifier getTextureLocation(ArmorOverlordRenderState state) {
        return TEXTURE;
    }

    @Override
    public ArmorOverlordRenderState createRenderState() {
        return new ArmorOverlordRenderState();
    }

    @Override
    public void extractRenderState(ArmorOverlordEntity entity, ArmorOverlordRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        OverlordAnimator.extract(entity, state, partialTicks);
    }

    /** The legs reach ~3.5 blocks out and the knees tower above the cap. */
    @Override
    protected AABB getBoundingBoxForCulling(ArmorOverlordEntity entity) {
        return entity.getBoundingBox().inflate(5.5, 2.5, 5.5);
    }

    @Override
    protected void scale(ArmorOverlordRenderState state, PoseStack poseStack) {
        float s = ArmorOverlordEntity.SCALE;
        poseStack.scale(s, s, s);
    }

    /** It collapses onto its legs (see the model) instead of flipping over. */
    @Override
    protected float getFlipDegrees() {
        return 0.0F;
    }

    /** The brain cluster glows in the dark, brighter while it hunts. */
    static class BrainGlowLayer extends RenderLayer<ArmorOverlordRenderState, ArmorOverlordModel> {
        private static final RenderType TYPE = RenderTypes.eyes(GLOW);

        BrainGlowLayer(RenderLayerParent<ArmorOverlordRenderState, ArmorOverlordModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, ArmorOverlordRenderState state, float yRot, float xRot) {
            float g = Mth.clamp(state.brainGlow * (0.85F + 0.15F * Mth.sin(state.ageInTicks * 0.2F)), 0.0F, 1.0F);
            if (g <= 0.01F) return;
            int c = (int) (g * 255.0F);
            collector.order(1).submitModel(this.getParentModel(), state, poseStack, TYPE, lightCoords, OverlayTexture.NO_OVERLAY,
                    ARGB.color(255, c, c, c), null, state.outlineColor, null);
        }
    }
}
