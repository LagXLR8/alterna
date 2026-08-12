package com.huwng.alterna.mixin;

import com.huwng.alterna.enchantment.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Item.class, priority = 600)
public class RejectionItemMixin {

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void rejection$getUseDuration(ItemStack itemStack, LivingEntity user, CallbackInfoReturnable<Integer> cir) {
        if (hasRejection(itemStack, user)) {
            cir.setReturnValue(72000);
        }
    }

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void rejection$getUseAnimation(ItemStack itemStack, CallbackInfoReturnable<ItemUseAnimation> cir) {
        if (hasRejection(itemStack, null)) {
            cir.setReturnValue(ItemUseAnimation.BLOCK);
        }
    }

    private boolean hasRejection(ItemStack stack, LivingEntity entity) {
        if (stack == null || stack.isEmpty()) return false;
        var level = entity != null ? entity.level() : null;
        var regAccess = level != null ? level.registryAccess() : null;
        if (regAccess == null) {
            try {
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc != null && mc.level != null) regAccess = mc.level.registryAccess();
            } catch (Throwable ignored) {}
        }
        if (regAccess == null) return false;
        var reg = regAccess.lookupOrThrow(Registries.ENCHANTMENT);
        var holder = reg.get(ModEnchantments.CURSE_OF_REJECTION);
        return holder.isPresent() && stack.getEnchantmentLevel(holder.get()) > 0;
    }
}
