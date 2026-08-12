package com.huwng.alterna.event;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.effect.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Alterna.MODID)
public class InsanityEvents {

    private static final String INSANITY_LEVEL_TAG = "InsanityLevel";
    private static final String INSANITY_DECAY_TICK_TAG = "InsanityDecayTick";

    public static int getInsanityLevel(Player player) {
        int nbtLevel = player.getPersistentData().getInt(INSANITY_LEVEL_TAG).orElse(0);
        MobEffectInstance effect = player.getEffect(ModMobEffects.INSANITY);
        int effectLevel = effect != null ? effect.getAmplifier() + 1 : 0;
        return Math.max(nbtLevel, effectLevel);
    }

    public static void setInsanityLevel(Player player, int level) {
        level = Math.clamp(level, 0, 20);
        player.getPersistentData().putInt(INSANITY_LEVEL_TAG, level);

        if (level > 0) {
            // Use a long duration so the effect stays; we manage it ourselves each tick
            player.addEffect(new MobEffectInstance(ModMobEffects.INSANITY, 72000, level - 1, false, false, true));
        } else {
            player.removeEffect(ModMobEffects.INSANITY);
        }
    }

    public static void addInsanity(Player player, int amount) {
        int current = getInsanityLevel(player);
        setInsanityLevel(player, current + amount);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        int insanityLevel = getInsanityLevel(player);
        if (insanityLevel <= 0) {
            player.getPersistentData().putInt(INSANITY_DECAY_TICK_TAG, 0);
            // Clean up stale effect if any
            if (player.hasEffect(ModMobEffects.INSANITY)) {
                player.removeEffect(ModMobEffects.INSANITY);
            }
            return;
        }

        // 1. Max Level 20: Instantly die ("Nổ đầu") — only if NOT in creative/spectator
        if (insanityLevel >= 20 && !player.isCreative() && !player.isSpectator()) {
            if (player.level() instanceof ServerLevel serverLevel) {
                player.kill(serverLevel);
            } else {
                player.hurt(player.damageSources().genericKill(), 100000.0f);
            }
            setInsanityLevel(player, 0);
            return;
        }

        // 2. Debuffs based on level (not in creative/spectator)
        if (!player.isCreative() && !player.isSpectator()) {
            if (insanityLevel >= 10 && insanityLevel <= 15) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, false, false));
            } else if (insanityLevel >= 16) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1, false, false));
            }

            if (insanityLevel > 18) {
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0, false, false));
            }
        }

        // Ensure MobEffect is present and synced to client with correct level
        MobEffectInstance current = player.getEffect(ModMobEffects.INSANITY);
        if (current == null || current.getAmplifier() != insanityLevel - 1) {
            player.addEffect(new MobEffectInstance(ModMobEffects.INSANITY, 72000, insanityLevel - 1, false, false, true));
        }

        // 3. Decay timing (10s near heat vs 2 minutes natural)
        boolean nearHeatSource = isNearLitCampfireOrFurnace(player);
        int targetTicks = nearHeatSource ? 200 : 2400; // 10s near heat vs 2min natural

        int decayTick = player.getPersistentData().getInt(INSANITY_DECAY_TICK_TAG).orElse(0) + 1;
        if (decayTick >= targetTicks) {
            decayTick = 0;
            setInsanityLevel(player, insanityLevel - 1);
        }
        player.getPersistentData().putInt(INSANITY_DECAY_TICK_TAG, decayTick);
    }

    public static boolean isNearLitCampfireOrFurnace(Player player) {
        BlockPos playerPos = player.blockPosition();
        int range = 5;

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    if (playerPos.distSqr(checkPos) <= range * range) {
                        BlockState state = player.level().getBlockState(checkPos);

                        if ((state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE))
                            && state.hasProperty(CampfireBlock.LIT) && state.getValue(CampfireBlock.LIT)) {
                            return true;
                        }

                        if ((state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.SMOKER))
                            && state.hasProperty(AbstractFurnaceBlock.LIT) && state.getValue(AbstractFurnaceBlock.LIT)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
