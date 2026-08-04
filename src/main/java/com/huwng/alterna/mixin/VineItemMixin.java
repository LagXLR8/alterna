package com.huwng.alterna.mixin;

import com.huwng.alterna.vine.VineCable;
import com.huwng.alterna.vine.VineCables;
import com.huwng.alterna.vine.VineConfig;
import com.huwng.alterna.vine.VineLogic;
import com.huwng.alterna.vine.VineTags;
import com.huwng.alterna.vine.duck.VinePlayerDuck;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks into Item methods to enable vine sliding when holding a vine_attachment item.
 * Ported from Zipline mod (MIT) — credit: Evan, Tomate0613.
 */
@Mixin(value = Item.class, priority = 500)
public class VineItemMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void vine$use(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(VineTags.VINE_ATTACHMENT)) {
            Vec3 offset = player.position().add(0.0, VineConfig.get().hangOffset, 0.0);
            VineCable cable = VineCables.getClosestCable(level, offset, VineConfig.get().clickReach);
            if (cable != null || VineConfig.get().useAnywhere) {
                if (level.isClientSide() && player.isLocalPlayer()) {
                    VineLogic.disable((VinePlayerDuck) player);
                }
                player.startUsingItem(hand);
                cir.setReturnValue(InteractionResult.CONSUME);
            }
        }
    }

    @Inject(method = "onUseTick", at = @At("HEAD"))
    private void vine$onUseTick(Level level, LivingEntity livingEntity, ItemStack itemStack, int ticksRemaining, CallbackInfo ci) {
        if (itemStack.is(VineTags.VINE_ATTACHMENT)) {
            VineLogic.tick(level, livingEntity, itemStack);
        }
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"))
    private void vine$releaseUsing(ItemStack itemStack, Level level, LivingEntity entity, int remainingTime, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player player) {
            if (itemStack.is(VineTags.VINE_ATTACHMENT)) {
                VineLogic.release(player, itemStack);
            }
        }
    }

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void vine$getUseDuration(ItemStack itemStack, LivingEntity user, CallbackInfoReturnable<Integer> cir) {
        if (itemStack.is(VineTags.VINE_ATTACHMENT)) {
            cir.setReturnValue(72000);
        }
    }

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void vine$getUseAnimation(ItemStack itemStack, CallbackInfoReturnable<ItemUseAnimation> cir) {
        if (itemStack.is(VineTags.VINE_ATTACHMENT)) {
            cir.setReturnValue(ItemUseAnimation.NONE);
        }
    }

    @Inject(method = "inventoryTick", at = @At("HEAD"))
    private void vine$inventoryTick(ItemStack itemStack, ServerLevel level, Entity owner, EquipmentSlot slot, CallbackInfo ci) {
        if (itemStack.is(VineTags.VINE_ATTACHMENT) && owner instanceof LivingEntity living) {
            VineLogic.inventoryTick(living);
        }
    }
}
