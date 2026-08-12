package com.huwng.alterna.mixin.client;

import com.huwng.alterna.client.render.HeroismGlintHolder;
import com.huwng.alterna.client.render.ModGlintType;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SubmitNodeStorage.ItemSubmit.class)
public class ItemSubmitMixin implements HeroismGlintHolder {

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
}
