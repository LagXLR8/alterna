package com.huwng.alterna.mixin;

import com.huwng.alterna.client.VoidFadeOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses the vanilla dimension-change loading screen while our own
 * VoidFadeOverlay is covering the screen, so the player never sees it.
 *
 * We tried referencing the specific vanilla screen class by name
 * (ReceivingLevelScreen), but 26.1's client rendering/screen package was
 * restructured enough that we can't reliably guess its new name or package
 * without your decompiled sources. Instead, this takes a more robust
 * approach: while VoidFadeOverlay is active, cancel ANY attempt to open a
 * non-null screen at all. In the ~1.5 second window our fade covers, nothing
 * legitimate should normally be trying to open a screen (no player input is
 * happening mid-teleport), so this is safe in practice and doesn't depend on
 * any class name that might change again next version.
 *
 * If you notice a real screen (e.g. a GUI another mod tried to open) getting
 * eaten during that window, you can narrow this back down to a specific
 * screen type once you've identified the correct class name in your IDE.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void alterna$suppressScreenDuringVoidFade(Screen screen, CallbackInfo ci) {
        if (VoidFadeOverlay.active && screen != null) {
            ci.cancel();
        }
    }
}
