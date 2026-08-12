package com.huwng.alterna.mixin.client;

import com.huwng.alterna.client.render.HeroismGlintContext;
import com.huwng.alterna.client.render.HeroismGlintHolder;
import com.huwng.alterna.client.render.ModGlintType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFeatureRenderer.class)
public class ItemFeatureRendererMixin {

    @Inject(method = "renderItem", at = @At("HEAD"))
    private void alterna$onRenderItemHead(MultiBufferSource.BufferSource bufferSource,
                                           OutlineBufferSource outlineBufferSource,
                                           SubmitNodeStorage.ItemSubmit submit,
                                           CallbackInfo ci) {
        ModGlintType glintType = ((HeroismGlintHolder) (Object) submit).alterna$getGlintType();
        HeroismGlintContext.setGlintType(glintType);
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void alterna$onRenderItemReturn(MultiBufferSource.BufferSource bufferSource,
                                             OutlineBufferSource outlineBufferSource,
                                             SubmitNodeStorage.ItemSubmit submit,
                                             CallbackInfo ci) {
        HeroismGlintContext.setGlintType(ModGlintType.NONE);
    }

    @Redirect(method = "getFoilRenderType", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;glintTranslucent()Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private static RenderType alterna$glintTranslucent() {
        ModGlintType type = HeroismGlintContext.getGlintType();
        return type != ModGlintType.NONE && type.getGlintTranslucent() != null
            ? type.getGlintTranslucent()
            : RenderTypes.glintTranslucent();
    }

    @Redirect(method = "getFoilRenderType", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;glint()Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private static RenderType alterna$glint() {
        ModGlintType type = HeroismGlintContext.getGlintType();
        return type != ModGlintType.NONE && type.getGlint() != null
            ? type.getGlint()
            : RenderTypes.glint();
    }

    @Redirect(method = "getFoilRenderType", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entityGlint()Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private static RenderType alterna$entityGlint() {
        ModGlintType type = HeroismGlintContext.getGlintType();
        return type != ModGlintType.NONE && type.getEntityGlint() != null
            ? type.getEntityGlint()
            : RenderTypes.entityGlint();
    }
}
