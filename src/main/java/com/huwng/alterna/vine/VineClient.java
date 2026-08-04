package com.huwng.alterna.vine;

import com.huwng.alterna.vine.duck.VineGameRendererDuck;
import net.minecraft.client.Minecraft;

/**
 * Client-side helper for the vine system.
 * Ported from Zipline mod (MIT) — credit: Evan, Tomate0613.
 */
public class VineClient {
    public static void init() {
        // No-op for now; could register client-side keybinds or overlays.
    }

    public static void vineTilt(float yaw) {
        ((VineGameRendererDuck) Minecraft.getInstance().gameRenderer).vine$setVineTilt(yaw);
    }
}
