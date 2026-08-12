package com.huwng.alterna.event;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.enchantment.ModEnchantments;
import com.huwng.alterna.network.BloodlustMissPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;

@EventBusSubscriber(modid = Alterna.MODID)
public class CurseEnchantmentEvents {

    private static final String REJECTION_COOLDOWN_TAG = "RejectionCooldown";
    private static final String NO_LIFE_KING_DIM_TICK_TAG = "NoLifeKingDimTick";
    private static final String PENDING_NETHER_RESPAWN_TAG = "PendingNetherRespawn";
    private static final Map<UUID, Integer> NO_LIFE_KING_HITS = new HashMap<>();

    // Map to delay nether teleportation slightly after respawn to avoid terrain loading lock
    private static final Map<UUID, Integer> PENDING_TELEPORT_TICKS = new HashMap<>();

    // ==========================================
    // 1. RIGHT CLICK / LEFT CLICK HANDLERS
    // ==========================================
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        var reg = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var rejectionHolder = reg.get(ModEnchantments.CURSE_OF_REJECTION);
        if (rejectionHolder.isPresent() && stack.getEnchantmentLevel(rejectionHolder.get()) > 0) {
            player.startUsingItem(event.getHand());
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static float lastClientAttackStrength = 1.0f;

    // LeftClickEmpty = player swung at air (miss)
    // LeftClickEmpty fires CLIENT-SIDE only in integrated server.
    // Send packet to server so it can apply damage there.
    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) return;
        if (player.isCreative() || player.isSpectator()) return;

        ItemStack weapon = player.getMainHandItem();
        if (weapon.isEmpty()) return;

        // Use last recorded attack charge before swing animation reset it
        if (lastClientAttackStrength < 0.8f) return;

        var reg = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var bloodlustHolder = reg.get(ModEnchantments.CURSE_OF_BLOODLUST);
        if (bloodlustHolder.isPresent() && weapon.getEnchantmentLevel(bloodlustHolder.get()) > 0) {
            var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
            if (connection != null) {
                connection.send(new BloodlustMissPayload());
            }
        }
    }


    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // block interaction handler
    }

    // ==========================================
    // 2. ATTACK ENTITY EVENTS
    // ==========================================
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;

        ItemStack weapon = player.getMainHandItem();
        if (weapon.isEmpty()) return;

        var reg = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        float attackStrength = player.getAttackStrengthScale(0.5f);

        // --- CURSE OF DROWNED CAPTAIN ---
        var drownedHolder = reg.get(ModEnchantments.CURSE_OF_DROWNED_CAPTAIN);
        if (drownedHolder.isPresent() && weapon.getEnchantmentLevel(drownedHolder.get()) > 0 && attackStrength >= 0.7f) {
            float minerals = countMinerals(player);
            // 3 stacks = 192 minerals -> max +12 damage
            float bonusDamage = Math.min(12.0f, (minerals / 192.0f) * 12.0f);
            if (bonusDamage > 0) {
                target.hurt(player.damageSources().playerAttack(player), bonusDamage);
            }
        }

        // --- CURSE OF BLOODLUST --- (+3 damage on hit)
        var bloodlustHolder = reg.get(ModEnchantments.CURSE_OF_BLOODLUST);
        if (bloodlustHolder.isPresent() && weapon.getEnchantmentLevel(bloodlustHolder.get()) > 0 && attackStrength >= 0.7f) {
            target.hurtTime = 0;
            target.invulnerableTime = 0;
            target.hurt(player.damageSources().playerAttack(player), 3.0f);
            target.hurtTime = 0;
            target.invulnerableTime = 0;
        }

        // --- CURSE OF THE NO LIFE KING ---
        var noLifeKingHolder = reg.get(ModEnchantments.CURSE_OF_THE_NO_LIFE_KING);
        if (noLifeKingHolder.isPresent() && weapon.getEnchantmentLevel(noLifeKingHolder.get()) > 0 && attackStrength >= 0.7f) {
            boolean isTargetPlayerOrMannequin = (target instanceof Player) || BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getPath().contains("mannequin");
            if (isTargetPlayerOrMannequin) {
                UUID attackerUUID = player.getUUID();
                int hits = NO_LIFE_KING_HITS.getOrDefault(attackerUUID, 0) + 1;
                if (hits >= 5) {
                    if (target instanceof Player targetPlayer) {
                        InsanityEvents.addInsanity(targetPlayer, 1);
                    }
                    NO_LIFE_KING_HITS.put(attackerUUID, 0);
                } else {
                    NO_LIFE_KING_HITS.put(attackerUUID, hits);
                }
            }
        }
    }

    // ==========================================
    // 3. INCOMING DAMAGE EVENTS
    // ==========================================
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        ItemStack weapon = player.getMainHandItem();
        var reg = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        // --- CURSE OF REJECTION ---
        var rejectionHolder = reg.get(ModEnchantments.CURSE_OF_REJECTION);
        if (rejectionHolder.isPresent() && weapon.getEnchantmentLevel(rejectionHolder.get()) > 0) {
            int cooldown = player.getPersistentData().getInt(REJECTION_COOLDOWN_TAG).orElse(0);
            if ((player.isUsingItem() || player.isBlocking()) && cooldown <= 0) {
                // Set 1.5s (30 ticks) cooldown
                player.getPersistentData().putInt(REJECTION_COOLDOWN_TAG, 30);

                // Knockback attacker far away
                if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                    Vec3 knockbackDir = attacker.position().subtract(player.position()).normalize();
                    attacker.setDeltaMovement(attacker.getDeltaMovement().add(knockbackDir.x * 2.5, 0.4, knockbackDir.z * 2.5));
                    attacker.hurtMarked = true;
                }

                // Player takes 1 damage (half heart) bypassing armor/effects
                player.hurt(player.damageSources().genericKill(), 1.0f);

                // Block/parry incoming damage
                event.setCanceled(true);
                return;
            }
        }

        // --- CURSE OF DROWNED CAPTAIN ---
        var drownedHolder = reg.get(ModEnchantments.CURSE_OF_DROWNED_CAPTAIN);
        if (drownedHolder.isPresent() && weapon.getEnchantmentLevel(drownedHolder.get()) > 0) {
            dropMineralsOnDamage(player);
        }

        // --- CURSE OF THE NO LIFE KING ---
        var noLifeKingHolder = reg.get(ModEnchantments.CURSE_OF_THE_NO_LIFE_KING);
        if (noLifeKingHolder.isPresent() && weapon.getEnchantmentLevel(noLifeKingHolder.get()) > 0) {
            // Receive +10% extra damage from all sources
            event.setAmount(event.getAmount() * 1.10f);
        }
    }

    // ==========================================
    // 4. HEAL EVENT (+30% Instant Heal for No Life King)
    // ==========================================
    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack weapon = player.getMainHandItem();
        if (weapon.isEmpty()) return;

        var reg = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var noLifeKingHolder = reg.get(ModEnchantments.CURSE_OF_THE_NO_LIFE_KING);
        if (noLifeKingHolder.isPresent() && weapon.getEnchantmentLevel(noLifeKingHolder.get()) > 0) {
            // Instant health potion heal bonus +30%
            event.setAmount(event.getAmount() * 1.30f);
        }
    }

    // ==========================================
    // 5. TICK EVENT (Holding Tickers, Cooldowns, Inventory Checks, Pending Teleports)
    // ==========================================
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            if (player.swingTime == 0) {
                lastClientAttackStrength = player.getAttackStrengthScale(0.0f);
            }
            return;
        }

        CompoundTag nbt = player.getPersistentData();

        // --- Rejection Cooldown Ticker ---
        int rejCooldown = nbt.getInt(REJECTION_COOLDOWN_TAG).orElse(0);
        if (rejCooldown > 0) {
            nbt.putInt(REJECTION_COOLDOWN_TAG, rejCooldown - 1);
        }

        // --- Delayed Nether Teleport Process ---
        UUID playerUUID = player.getUUID();
        if (PENDING_TELEPORT_TICKS.containsKey(playerUUID)) {
            int ticksLeft = PENDING_TELEPORT_TICKS.get(playerUUID) - 1;
            if (ticksLeft <= 0) {
                PENDING_TELEPORT_TICKS.remove(playerUUID);
                if (player instanceof ServerPlayer serverPlayer) {
                    MinecraftServer server = serverPlayer.level().getServer();
                    if (server != null) {
                        ServerLevel netherLevel = server.getLevel(Level.NETHER);
                        if (netherLevel != null) {
                            int x = 100 + serverPlayer.getRandom().nextInt(100);
                            int z = 100 + serverPlayer.getRandom().nextInt(100);
                            int safeY = 64;
                            for (int testY = 100; testY >= 35; testY--) {
                                BlockPos pos = new BlockPos(x, testY, z);
                                if (netherLevel.getBlockState(pos).isAir() && netherLevel.getBlockState(pos.above()).isAir() && netherLevel.getBlockState(pos.below()).isSolid()) {
                                    safeY = testY;
                                    break;
                                }
                            }
                            serverPlayer.teleportTo(netherLevel, x + 0.5, safeY, z + 0.5, Set.<Relative>of(), serverPlayer.getYRot(), serverPlayer.getXRot(), false);
                        }
                    }
                }
            } else {
                PENDING_TELEPORT_TICKS.put(playerUUID, ticksLeft);
            }
        }

        ItemStack weapon = player.getMainHandItem();
        var reg = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        // --- CURSE OF THE NO LIFE KING ---
        var noLifeKingHolder = reg.get(ModEnchantments.CURSE_OF_THE_NO_LIFE_KING);
        boolean hasNoLifeKingInInventory = hasEnchantInInventory(player, ModEnchantments.CURSE_OF_THE_NO_LIFE_KING);

        if (hasNoLifeKingInInventory) {
            // Disable Speed and Jump Boost if anywhere in inventory
            player.removeEffect(MobEffects.SPEED);
            player.removeEffect(MobEffects.JUMP_BOOST);
        }

        if (noLifeKingHolder.isPresent() && weapon.getEnchantmentLevel(noLifeKingHolder.get()) > 0) {
            // Regeneration I — add only when effect is missing or about to expire (< 40 ticks left)
            var regenEffect = player.getEffect(MobEffects.REGENERATION);
            if (regenEffect == null || regenEffect.getDuration() < 40) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0, false, false));
            }

            // Immune to Wither, Poison, Weakness
            player.removeEffect(MobEffects.WITHER);
            player.removeEffect(MobEffects.POISON);
            player.removeEffect(MobEffects.WEAKNESS);

            // Non-Overworld Dimension Tick (1 insanity level every 10 seconds / 200 ticks)
            if (player.level().dimension() != Level.OVERWORLD) {
                int dimTicks = nbt.getInt(NO_LIFE_KING_DIM_TICK_TAG).orElse(0) + 1;
                if (dimTicks >= 200) {
                    dimTicks = 0;
                    InsanityEvents.addInsanity(player, 1);
                }
                nbt.putInt(NO_LIFE_KING_DIM_TICK_TAG, dimTicks);
            } else {
                nbt.putInt(NO_LIFE_KING_DIM_TICK_TAG, 0);
            }
        }
    }

    // ==========================================
    // 6. PLAYER DEATH & RESPAWN EVENTS
    // ==========================================
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        ItemStack weapon = player.getMainHandItem();
        if (weapon.isEmpty()) return;

        var reg = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var noLifeKingHolder = reg.get(ModEnchantments.CURSE_OF_THE_NO_LIFE_KING);
        if (noLifeKingHolder.isPresent() && weapon.getEnchantmentLevel(noLifeKingHolder.get()) > 0) {
            if (player.level().dimension() == Level.OVERWORLD) {
                if (player.getRandom().nextFloat() < 0.40f) { // 40% chance
                    player.getPersistentData().putBoolean(PENDING_NETHER_RESPAWN_TAG, true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            boolean pendingRespawn = event.getOriginal().getPersistentData().getBoolean(PENDING_NETHER_RESPAWN_TAG).orElse(false);
            if (pendingRespawn) {
                event.getEntity().getPersistentData().putBoolean(PENDING_NETHER_RESPAWN_TAG, true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        if (player.getPersistentData().getBoolean(PENDING_NETHER_RESPAWN_TAG).orElse(false)) {
            player.getPersistentData().putBoolean(PENDING_NETHER_RESPAWN_TAG, false);
            // Schedule teleport 10 ticks (0.5s) after respawn finishes to prevent loading screen lock
            PENDING_TELEPORT_TICKS.put(player.getUUID(), 10);
        }
    }

    // ==========================================
    // HELPER FUNCTIONS
    // ==========================================
    private static boolean hasEnchantInInventory(Player player, ResourceKey<Enchantment> enchantKey) {
        var reg = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var holder = reg.get(enchantKey);
        if (holder.isEmpty()) return false;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getEnchantmentLevel(holder.get()) > 0) {
                return true;
            }
        }
        return false;
    }

    private static float countMinerals(Player player) {
        float total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.is(Items.GOLD_INGOT) || stack.is(Items.RAW_GOLD) || stack.is(Items.GOLD_ORE) || stack.is(Items.DEEPSLATE_GOLD_ORE) || stack.is(Items.NETHER_GOLD_ORE)) {
                total += stack.getCount();
            } else if (stack.is(Items.GOLD_BLOCK) || stack.is(Items.RAW_GOLD_BLOCK)) {
                total += stack.getCount() * 9;
            } else if (stack.is(Items.GOLD_NUGGET)) {
                total += stack.getCount() / 9.0f;
            } else if (stack.is(Items.EMERALD) || stack.is(Items.EMERALD_ORE) || stack.is(Items.DEEPSLATE_EMERALD_ORE)) {
                total += stack.getCount();
            } else if (stack.is(Items.EMERALD_BLOCK)) {
                total += stack.getCount() * 9;
            } else if (stack.is(Items.DIAMOND) || stack.is(Items.DIAMOND_ORE) || stack.is(Items.DEEPSLATE_DIAMOND_ORE)) {
                total += stack.getCount();
            } else if (stack.is(Items.DIAMOND_BLOCK)) {
                total += stack.getCount() * 9;
            } else if (stack.is(Items.NETHERITE_INGOT) || stack.is(Items.NETHERITE_SCRAP) || stack.is(Items.ANCIENT_DEBRIS)) {
                total += stack.getCount();
            } else if (stack.is(Items.NETHERITE_BLOCK)) {
                total += stack.getCount() * 9;
            }
        }
        return total;
    }

    private static void dropMineralsOnDamage(Player player) {
        int amountToDrop = player.getRandom().nextInt(5) + 1; // 1 to 5
        int dropped = 0;

        List<Integer> mineralSlots = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && isMineralItem(stack)) {
                mineralSlots.add(i);
            }
        }

        if (mineralSlots.isEmpty()) return;
        Collections.shuffle(mineralSlots, new Random());

        for (int slot : mineralSlots) {
            if (dropped >= amountToDrop) break;
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty() || !isMineralItem(stack)) continue;

            int count = Math.min(stack.getCount(), amountToDrop - dropped);
            ItemStack lostParticleStack = stack.copyWithCount(1);
            stack.shrink(count);
            dropped += count;

            if (player.level() instanceof ServerLevel serverLevel) {
                for (int p = 0; p < 6; p++) {
                    double px = player.getX() + (player.getRandom().nextDouble() - 0.5) * 0.8;
                    double py = player.getY() + 1.0 + (player.getRandom().nextDouble() - 0.5) * 0.8;
                    double pz = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 0.8;

                    serverLevel.sendParticles(
                        new ItemParticleOption(ParticleTypes.ITEM, lostParticleStack.getItem()),
                        px, py, pz,
                        1, 0, 0.1, 0, 0.05
                    );
                }
            }
        }
    }

    private static boolean isMineralItem(ItemStack stack) {
        return stack.is(Items.GOLD_INGOT) || stack.is(Items.RAW_GOLD) || stack.is(Items.GOLD_BLOCK) || stack.is(Items.RAW_GOLD_BLOCK) || stack.is(Items.GOLD_NUGGET) || stack.is(Items.GOLD_ORE) || stack.is(Items.DEEPSLATE_GOLD_ORE) || stack.is(Items.NETHER_GOLD_ORE)
            || stack.is(Items.EMERALD) || stack.is(Items.EMERALD_BLOCK) || stack.is(Items.EMERALD_ORE) || stack.is(Items.DEEPSLATE_EMERALD_ORE)
            || stack.is(Items.DIAMOND) || stack.is(Items.DIAMOND_BLOCK) || stack.is(Items.DIAMOND_ORE) || stack.is(Items.DEEPSLATE_DIAMOND_ORE)
            || stack.is(Items.NETHERITE_INGOT) || stack.is(Items.NETHERITE_BLOCK) || stack.is(Items.NETHERITE_SCRAP) || stack.is(Items.ANCIENT_DEBRIS);
    }
}
