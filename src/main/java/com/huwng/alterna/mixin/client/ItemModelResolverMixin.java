package com.huwng.alterna.mixin.client;

import com.huwng.alterna.client.render.HeroismGlintOutput;
import com.huwng.alterna.client.render.ModGlintType;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelResolver.class)
public class ItemModelResolverMixin {

    @Inject(method = "updateForTopItem", at = @At("RETURN"))
    private void alterna$onUpdateForTopItem(
        ItemStackRenderState output,
        ItemStack item,
        ItemDisplayContext displayContext,
        @Nullable Level level,
        @Nullable ItemOwner owner,
        int seed,
        CallbackInfo ci
    ) {
        ModGlintType glintType = ModGlintType.fromStack(item);
        if (glintType != ModGlintType.NONE) {
            ((HeroismGlintOutput) output).alterna$markCustomGlint(glintType);
        }
    }
}
