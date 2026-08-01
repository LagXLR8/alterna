package com.huwng.alterna.event;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.AlternaAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// @EventBusSubscriber(modid = Alterna.MODID) // Temporarily disabled
public class VoidFallHandler {

    // Y level in the Overworld below which the player is considered to be
    // "under bedrock". Whatever lets them pass through bedrock in your
    // worldgen/blocks should place them below this height.
    public static final int VOID_START_Y = -64;

    // How many blocks the player must fall below VOID_START_Y before being
    // pulled into the Abyss.
    public static final double FALL_DISTANCE_REQUIRED = 1000.0D;

    /**
     * Cancel ALL incoming damage on the player while they're below
     * VOID_START_Y in the Overworld - not just void damage specifically.
     *
     * Earlier version tried to match the exact "in_void" damage type by
     * ResourceKey, which silently never matched (likely an ID/lookup
     * mismatch we couldn't debug without a log showing the actual damage
     * type). Since this is meant to be a blanket "safe falling zone"
     * anyway, dropping the type check and just cancelling anything while
     * below the threshold sidesteps that problem entirely - and because
     * this is a genuine event cancellation (not a heal-after-the-fact
     * patch), the hurt sound / red flash / camera jolt never fire in the
     * first place, since vanilla never gets to apply the damage at all.
     */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().dimension() != Level.OVERWORLD) {
            return;
        }
        if (player.getY() < VOID_START_Y) {
            event.setCanceled(true);
        }
    }

    /**
     * Safety net kept in case the event above ever fails to catch something
     * (e.g. a damage path that bypasses events entirely) - forces health
     * back to max every tick while below the threshold so the player can
     * never actually die there, even if the hurt animation/sound still
     * played that tick. With the fix above this should rarely if ever
     * trigger in practice.
     */
    private static void forceFullHealth(ServerPlayer player) {
        if (player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.level().dimension() != Level.OVERWORLD) {
            return;
        }

        boolean belowThreshold = player.getY() < VOID_START_Y;

        if (belowThreshold) {
            forceFullHealth(player);
        }

        if (!belowThreshold) {
            // Back above the threshold (e.g. climbed back up) - reset the counter.
            if (player.getData(AlternaAttachments.VOID_FALL_DISTANCE) != 0.0D) {
                player.setData(AlternaAttachments.VOID_FALL_DISTANCE, 0.0D);
            }
            return;
        }

        double fellThisTick = player.yo - player.getY();
        if (fellThisTick <= 0) {
            // Moving sideways/upward this tick - don't count it.
            return;
        }

        double total = player.getData(AlternaAttachments.VOID_FALL_DISTANCE) + fellThisTick;

        if (total >= FALL_DISTANCE_REQUIRED) {
            player.setData(AlternaAttachments.VOID_FALL_DISTANCE, 0.0D);
            AlternaTeleporter.sendToAbyss(player);
        } else {
            player.setData(AlternaAttachments.VOID_FALL_DISTANCE, total);
        }
    }
}
