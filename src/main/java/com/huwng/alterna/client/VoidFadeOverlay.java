package com.huwng.alterna.client;

import com.huwng.alterna.Alterna;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = Alterna.MODID, value = Dist.CLIENT)
public class VoidFadeOverlay {

    private static final int HOLD_TICKS = 25;
    private static final int FADE_OUT_TICKS = 10;

    private static int ticks = -1;
    public static volatile boolean active = false;

    /**
     * Call this before starting the teleport.
     */
    public static void startFade() {
        active = true;
        ticks = 0;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!active) {
            return;
        }

        ticks++;

        if (ticks > HOLD_TICKS + FADE_OUT_TICKS) {
            active = false;
            ticks = -1;
        }
    }

    private static float getFadeAlpha() {
        if (!active) {
            return 0.0F;
        }

        if (ticks <= HOLD_TICKS) {
            return 1.0F;
        }

        float alpha = 1.0F - (float) (ticks - HOLD_TICKS) / FADE_OUT_TICKS;
        return Math.max(0.0F, Math.min(1.0F, alpha));
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        float alpha = getFadeAlpha();

        if (alpha <= 0.0F) {
            return;
        }

        int argb = ((int) (alpha * 255.0F) << 24);

        GuiGraphicsExtractor gfx = event.getGuiGraphics();
        gfx.fill(0, 0, gfx.guiWidth(), gfx.guiHeight(), argb);
    }
}