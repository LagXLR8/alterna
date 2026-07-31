package com.huwng.alterna.mixin.client;

import com.huwng.alterna.gravity.GravityApi;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true, require = 0)
    private void alterna$preventWrongPushOutOfBlocks(double x, double z, CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        Direction direction = GravityApi.getDirection(self);
        if (direction != Direction.DOWN) {
            ci.cancel();
        }
    }
}
