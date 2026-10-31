package com.huwng.alterna.item;

import com.huwng.alterna.entity.GrappleHookEntity;
import com.huwng.alterna.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Overlord Grapple: hold right-click to fire the Overlord's hooked claw. Once it bites into a
 * block, keep holding to be reeled in; near the claw you hang there (no falling) until you let
 * go. Letting go releases the claw. Falling while reeled in does no damage.
 */
public class GrappleItem extends Item {
    public GrappleItem(Properties properties) {
        super(properties);
    }

    public static boolean isGrappling(Player p) {
        return p.isUsingItem() && p.getUseItem().getItem() instanceof GrappleItem;
    }

    public static @Nullable GrappleHookEntity findHook(Level level, Player p) {
        for (GrappleHookEntity h : level.getEntitiesOfClass(GrappleHookEntity.class,
                p.getBoundingBox().inflate(GrappleHookEntity.MAX_RANGE + 8), h -> h.isAlive() && h.getOwner() == p)) {
            return h;
        }
        return null;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            GrappleHookEntity old = findHook(level, player);
            if (old != null) old.discard();
            level.addFreshEntity(new GrappleHookEntity(level, player));
            stack.hurtAndBreak(1, player, hand);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.OVERLORD_HOOK.get(), SoundSource.PLAYERS, 0.7F, 1.4F);
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        if (!(entity instanceof Player p)) return;
        GrappleHookEntity hook = findHook(level, p);
        if (hook == null || !hook.isAnchored()) return;
        p.resetFallDistance();
        // the player moves itself: only the client of that player steers the motion
        if (!level.isClientSide()) return;
        Vec3 to = hook.position().subtract(p.position().add(0.0, p.getBbHeight() * 0.6, 0.0));
        double d = to.length();
        Vec3 v = p.getDeltaMovement();
        if (d < 1.4) {
            // reached the claw: hang there, gravity cancelled
            p.setDeltaMovement(v.x * 0.5, 0.08 + to.y * 0.1, v.z * 0.5);
        } else {
            double speed = Math.min(1.25, 0.4 + d * 0.06);
            Vec3 nv = v.scale(0.75).add(to.scale(0.42 * speed / d));
            if (nv.length() > speed) nv = nv.normalize().scale(speed);
            p.setDeltaMovement(nv.x, nv.y + 0.06, nv.z);
        }
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        if (entity instanceof Player p) {
            if (!p.level().isClientSide()) {
                GrappleHookEntity hook = findHook(p.level(), p);
                if (hook != null) hook.discard();
            }
            p.getCooldowns().addCooldown(stack, 8);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.alterna.overlord_grapple.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
