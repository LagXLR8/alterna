package com.huwng.alterna.mixin.client;

import com.huwng.alterna.client.render.HeroismGlintHolder;
import com.huwng.alterna.client.render.HeroismGlintOutput;
import com.huwng.alterna.client.render.ModGlintType;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemStackRenderState.class)
public class ItemStackRenderStateMixin implements HeroismGlintOutput {

    @Shadow
    private int activeLayerCount;

    @Shadow
    private ItemStackRenderState.LayerRenderState[] layers;

    @Shadow
    private boolean animated;

    @Override
    public void alterna$markCustomGlint(ModGlintType glintType) {
        if (glintType == ModGlintType.NONE) return;
        this.animated = true;
        for (int i = 0; i < this.activeLayerCount; i++) {
            this.layers[i].setFoilType(ItemStackRenderState.FoilType.STANDARD);
            ((HeroismGlintHolder) this.layers[i]).alterna$setGlintType(glintType);
        }
    }
}
