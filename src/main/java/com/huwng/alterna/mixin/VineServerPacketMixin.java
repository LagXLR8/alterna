package com.huwng.alterna.mixin;

import com.huwng.alterna.vine.VineTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bypasses server-side anti-cheat checks when a player is using a vine.
 * Without this, the server would kick the player for "moving too fast"
 * or "moved wrongly" because the vine system teleports them each tick.
 *
 * Replaces the original WrapOperation-based approach (which required MixinExtras)
 * with standard @Inject into the methods themselves.
 *
 * Ported from Zipline mod (MIT) — credit: Evan, Tomate0613.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class VineServerPacketMixin {

    @Shadow
    public ServerPlayer player;

    /**
     * When a player is using a vine attachment, allow unlimited flying ticks
     * so the server doesn't kick them for being airborne too long.
     */
    @Inject(method = "getMaximumFlyingTicks", at = @At("HEAD"), cancellable = true)
    private void vine$getMaximumFlyingTicks(Entity entity, CallbackInfoReturnable<Integer> cir) {
        if (entity instanceof Player playerEntity) {
            if (vine$isUsingVine(playerEntity)) {
                cir.setReturnValue(Integer.MAX_VALUE);
            }
        }
    }

    /**
     * When a player is using a vine, skip movement speed checks
     * by returning false from shouldCheckPlayerMovement.
     */
    @Inject(method = "shouldCheckPlayerMovement", at = @At("HEAD"), cancellable = true)
    private void vine$shouldCheckPlayerMovement(boolean isFallFlying, CallbackInfoReturnable<Boolean> cir) {
        if (vine$isUsingVine(this.player)) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private boolean vine$isUsingVine(Player p) {
        return p.getUseItem().is(VineTags.VINE_ATTACHMENT);
    }
}
