package com.huwng.alterna.mixin.client;

import com.huwng.alterna.client.render.HeroismGlintContext;
import com.huwng.alterna.client.render.HeroismGlintHolder;
import com.huwng.alterna.client.render.ModGlintType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin {

    @Shadow
    @Final
    private List<SubmitNodeStorage.ItemSubmit> itemSubmits;

    @Inject(method = "submitItem", at = @At("RETURN"))
    private void alterna$onSubmitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType, CallbackInfo ci) {
        ModGlintType activeGlint = HeroismGlintContext.getGlintType();
        if (activeGlint != ModGlintType.NONE && !this.itemSubmits.isEmpty()) {
            SubmitNodeStorage.ItemSubmit lastSubmit = this.itemSubmits.get(this.itemSubmits.size() - 1);
            ((HeroismGlintHolder) (Object) lastSubmit).alterna$setGlintType(activeGlint);
        }
    }
}
