package com.example.vinemod.mixin;

import com.example.vinemod.registry.ZiplineTags;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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

@Mixin({ServerGamePacketListenerImpl.class})
public class ServerGamePacketListenerImplMixin {
   @Shadow
   public ServerPlayer player;

   @Inject(
      method = {"getMaximumFlyingTicks"},
      at = {@At("HEAD")},
      cancellable = true
   )
   void getMaximumFlyingTicks(Entity entity, CallbackInfoReturnable<Integer> cir) {
      if (entity instanceof Player playerEntity) {
         if (this.zipline$isUsingZipline(playerEntity)) {
            cir.setReturnValue(Integer.MAX_VALUE);
         }

      }
   }

   @WrapOperation(
      method = {"handleMovePlayer"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/server/level/ServerPlayer;isInPostImpulseGraceTime()Z"
)}
   )
   boolean bypassMovedWrongly(ServerPlayer instance, Operation<Boolean> original) {
      return this.zipline$isUsingZipline(instance) ? true : (Boolean)original.call(new Object[]{instance});
   }

   @WrapOperation(
      method = {"handleMovePlayer"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;shouldCheckPlayerMovement(Z)Z"
)}
   )
   boolean bypassSpeedCheck(ServerGamePacketListenerImpl instance, boolean isFallFlying, Operation<Boolean> original) {
      return this.zipline$isUsingZipline(this.player) ? false : (Boolean)original.call(new Object[]{instance, isFallFlying});
   }

   @Unique
   boolean zipline$isUsingZipline(Player p) {
      return p.getUseItem().is(ZiplineTags.ATTACHMENT);
   }
}
