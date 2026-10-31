package com.huwng.alterna.client.render;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.client.ModModelLayers;
import com.huwng.alterna.client.model.CragbackModel;
import com.huwng.alterna.client.render.state.CragbackRenderState;
import com.huwng.alterna.entity.CragbackEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.resources.Identifier;

public class CragbackRenderer extends AgeableMobRenderer<CragbackEntity, CragbackRenderState, CragbackModel> {
    private static final Identifier TEXTURE = Alterna.id("textures/entity/cragback/cragback.png");
    private static final Identifier BABY_TEXTURE = Alterna.id("textures/entity/cragback/cragback_baby.png");

    public CragbackRenderer(EntityRendererProvider.Context context) {
        super(context, new CragbackModel(context.bakeLayer(ModModelLayers.CRAGBACK)),
                new CragbackModel(context.bakeLayer(ModModelLayers.CRAGBACK_BABY), true), 1.3F);
    }

    @Override
    public Identifier getTextureLocation(CragbackRenderState state) {
        return state.isBaby ? BABY_TEXTURE : TEXTURE;
    }

    @Override
    public CragbackRenderState createRenderState() {
        return new CragbackRenderState();
    }

    @Override
    public void extractRenderState(CragbackEntity entity, CragbackRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.eatAnimation = entity.getEatAnimation(partialTicks);
        state.drinkAnimation = entity.getDrinkAnimation(partialTicks);
        state.charging = entity.isCharging();
        float[] att = new float[5];
        state.legsActive = entity.extractLegs(partialTicks, state.x, state.y, state.z, state.bodyRot, state.scale,
                state.footX, state.footY, state.footZ, att);
        state.bodyPitch = att[0];
        state.bodyRoll = att[1];
        state.bodyDrop = att[2];
        state.stepBob = att[3];
        state.stepSway = att[4];
    }
}
