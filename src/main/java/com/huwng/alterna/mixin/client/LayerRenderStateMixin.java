package com.huwng.alterna.mixin.client;

import com.huwng.alterna.client.render.HeroismGlintContext;
import com.huwng.alterna.client.render.HeroismGlintHolder;
import com.huwng.alterna.client.render.ModGlintType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackRenderState.LayerRenderState.class)
public class LayerRenderStateMixin implements HeroismGlintHolder {

    @Unique
    private ModGlintType alterna$glintType = ModGlintType.NONE;

    @Override
    public ModGlintType alterna$getGlintType() {
        return this.alterna$glintType;
    }

    @Override
    public void alterna$setGlintType(ModGlintType glintType) {
        this.alterna$glintType = glintType != null ? glintType : ModGlintType.NONE;
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void alterna$onClear(CallbackInfo ci) {
        this.alterna$glintType = ModGlintType.NONE;
    }

    @Inject(method = "submit", at = @At("HEAD"))
    private void alterna$onSubmitHead(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, int outlineColor, CallbackInfo ci) {
        HeroismGlintContext.setGlintType(this.alterna$glintType);
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void alterna$onSubmitReturn(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, int outlineColor, CallbackInfo ci) {
        HeroismGlintContext.setGlintType(ModGlintType.NONE);
    }
}
