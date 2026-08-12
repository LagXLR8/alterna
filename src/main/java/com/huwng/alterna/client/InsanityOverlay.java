package com.huwng.alterna.client;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.effect.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = Alterna.MODID, value = Dist.CLIENT)
public class InsanityOverlay {
    private static final Identifier INSANITY_TEXTURE = Alterna.id("textures/gui/sanity.png");
    private static final Identifier VIGNETTE_TEXTURE = Alterna.id("textures/gui/vignette.png");

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen != null) return;

        Player player = mc.player;

        // Read insanity level from synced MobEffect
        var effect = player.getEffect(ModMobEffects.INSANITY);
        if (effect == null) return;

        int insanityLevel = effect.getAmplifier() + 1; // 0-indexed → level 1 starts at amp 0

        if (insanityLevel <= 0) return;

        float levelRatio = Math.min(1.0f, insanityLevel / 20.0f);

        // Sanity overlay: starts visible at level 1, gets opaque toward level 20
        // Minimum 20% opacity, maximum 90%
        float overlayAlpha = 0.20f + levelRatio * 0.70f;

        // Vignette: starts at level 8, goes to 70% at level 20
        float vignetteAlpha = insanityLevel >= 8 ? Math.min(0.70f, (insanityLevel - 8) / 12.0f * 0.70f) : 0.0f;

        GuiGraphicsExtractor gfx = event.getGuiGraphics();
        int w = gfx.guiWidth();
        int h = gfx.guiHeight();

        // Draw sanity texture overlay
        if (overlayAlpha > 0f) {
            gfx.blit(RenderPipelines.GUI_TEXTURED, INSANITY_TEXTURE, 0, 0, 0.0F, 0.0F, w, h, w, h);
        }

        // Draw vignette overlay
        if (vignetteAlpha > 0f) {
            gfx.blit(RenderPipelines.VIGNETTE, VIGNETTE_TEXTURE, 0, 0, 0.0F, 0.0F, w, h, w, h);
        }
    }
}
