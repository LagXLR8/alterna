package com.huwng.alterna.client;

import com.huwng.alterna.Alterna;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * "Blindness, but you can see further" while the player is below
 * DARK_ZONE_Y in the Overworld (the shaft under bedrock). Unlike vanilla
 * Blindness - whose visibility radius is short and not configurable - this
 * pulls fog in to a radius WE choose, so nearby things (torches you place,
 * terrain right around you) stay visible while anything past that fades to
 * black.
 *
 * Purely a client-side rendering effect (fog distance + color). It does not
 * touch the real lighting engine, so block light from torches/glowstone
 * placed down there still works normally within the visible radius.
 */
@EventBusSubscriber(modid = Alterna.MODID, value = Dist.CLIENT)
public class VoidVisionHandler {

    public static final int DARK_ZONE_Y = -55;

    // How far the player can see before it fades to black. Vanilla
    // Blindness is roughly a couple of blocks; tune these up/down to taste.
    private static final float FOG_START = 4000F;
    private static final float FOG_END = 5000.0F;

    private static boolean inDarkZone() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return false;
        }
        if (player.level().dimension() != Level.OVERWORLD) {
            return false;
        }
        return player.getY() < DARK_ZONE_Y;
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!inDarkZone()) {
            return;
        }
        event.setNearPlaneDistance(FOG_START);
        event.setFarPlaneDistance(FOG_END);
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!inDarkZone()) {
            return;
        }
        // Fade to black instead of the default sky-blue fog color, so it
        // actually reads as darkness rather than a blue haze.
        event.setRed(0.0F);
        event.setGreen(0.0F);
        event.setBlue(0.0F);
    }
}
