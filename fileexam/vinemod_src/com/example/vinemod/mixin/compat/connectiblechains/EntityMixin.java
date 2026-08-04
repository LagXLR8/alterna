package com.example.vinemod.mixin.compat.connectiblechains;

import com.example.vinemod.registry.ZiplineTags;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({Entity.class})
public class EntityMixin {
   @WrapMethod(
      method = {"canCollideWith"}
   )
   boolean canCollideWith(Entity entity, Operation<Boolean> original) {
      Boolean defaultValue = (Boolean)original.call(new Object[]{entity});
      if (!defaultValue) {
         return false;
      } else if (this instanceof Player) {
         Player player = (Player)this;
         if (!entity.getClass().getSimpleName().equals("ChainCollisionEntity")) {
            return defaultValue;
         } else {
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            return !mainHand.is(ZiplineTags.ATTACHMENT) && !offHand.is(ZiplineTags.ATTACHMENT);
         }
      } else {
         return defaultValue;
      }
   }
}
