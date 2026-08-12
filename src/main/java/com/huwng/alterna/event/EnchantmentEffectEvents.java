package com.huwng.alterna.event;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.effect.ModMobEffects;
import com.huwng.alterna.enchantment.ModEnchantments;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = Alterna.MODID)
public class EnchantmentEffectEvents {

    // CHILLING
    private static final Map<UUID, Integer> CHILLING_HITS = new HashMap<>();

    // ELASTIC
    private static final String ELASTIC_HIT_COUNT_TAG = "ElasticHitCount";
    private static final String LAST_ELASTIC_ATTACKER_TAG = "LastElasticAttacker";
    private static final String TETHER_SOURCE_UUID_TAG = "TetherSourceUUID";
    private static final double PULL_STRENGTH = 0.3;
    private static final int TETHER_DURATION = 40;

    // VAMPIRISM
    private static final Map<UUID, Long> VAMPIRISM_COOLDOWNS = new HashMap<>();
    private static final long VAMPIRISM_HIT_COOLDOWN = 10;

    // HEROISM
    private static final Map<UUID, Long> HEROISM_COOLDOWNS = new HashMap<>();
    private static final long HEROISM_REFLECT_COOLDOWN = 100; // 5 seconds (100 ticks)

    // WILD
    private static final Map<UUID, Integer> WILD_HITS = new HashMap<>();
    private static final int WILD_REQUIRED_HITS = 3;
    private static final double WILD_DAMAGE_MULTIPLIER = 0.4;
    private static final double WILD_AOE_RADIUS = 3.0;

    // DETONATION
    private static final Map<UUID, Integer> DETONATION_HITS = new HashMap<>();

    // DEFERRED
    private static final ThreadLocal<Boolean> IS_DEFERRED_TICK = ThreadLocal.withInitial(() -> false);
    private static final java.util.List<DeferredDamageTask> DEFERRED_TASKS = new java.util.ArrayList<>();

    public static class DeferredDamageTask {
        final UUID targetUUID;
        final net.minecraft.world.level.Level level;
        final float portionPerTick;
        int remainingInstallments = 10;
        int ticksUntilNextInstallment = 5;

        public DeferredDamageTask(UUID targetUUID, net.minecraft.world.level.Level level, float snapshotDamage) {
            this.targetUUID = targetUUID;
            this.level = level;
            this.portionPerTick = snapshotDamage * 0.10F;
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;
        if (player.getAttackStrengthScale(0.5F) < 0.9F) return;

        ItemStack weapon = player.getMainHandItem();

        if (weapon.isEmpty()) return;

        var enchantmentRegistry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        // ===== CHILLING =====
        var chillingHolder = enchantmentRegistry.get(ModEnchantments.CHILLING);
        if (chillingHolder.isPresent() && weapon.getEnchantmentLevel(chillingHolder.get()) > 0) {
            MobEffectInstance chillEffect = new MobEffectInstance(ModMobEffects.CHILL, 40, 0, false, true, true);
            target.addEffect(chillEffect);

            UUID targetId = target.getUUID();
            int count = CHILLING_HITS.getOrDefault(targetId, 0) + 1;
            CHILLING_HITS.put(targetId, count);

            if (count >= 7) {
                MobEffectInstance freezeEffect = new MobEffectInstance(ModMobEffects.BOTTOM_FREEZE, 60, 0, false, true, true);
                target.addEffect(freezeEffect);

                // Broadcast packet to client tracking players so client entity has MobEffectInstance synced!
                if (target.level().getChunkSource() instanceof ServerChunkCache chunkCache) {
                    chunkCache.sendToTrackingPlayersAndSelf(target, new ClientboundUpdateMobEffectPacket(target.getId(), freezeEffect, false));
                }

                CHILLING_HITS.put(targetId, 0);
            }
            if (CHILLING_HITS.size() > 100) CHILLING_HITS.clear();
        }

        // ===== ELASTIC =====
        var elasticHolder = enchantmentRegistry.get(ModEnchantments.ELASTIC);
        if (elasticHolder.isPresent() && weapon.getEnchantmentLevel(elasticHolder.get()) > 0) {
            CompoundTag nbt = target.getPersistentData();
            String lastAttacker = nbt.getString(LAST_ELASTIC_ATTACKER_TAG).orElse("");
            int hitCount = nbt.getInt(ELASTIC_HIT_COUNT_TAG).orElse(0);

            if (!lastAttacker.equals(player.getStringUUID())) {
                hitCount = 0;
                nbt.putString(LAST_ELASTIC_ATTACKER_TAG, player.getStringUUID());
            }

            hitCount++;
            nbt.putInt(ELASTIC_HIT_COUNT_TAG, hitCount);

            if (hitCount >= 2) {
                MobEffectInstance tetheredEffect = new MobEffectInstance(ModMobEffects.TETHERED, TETHER_DURATION, 0, false, true, false);
                target.addEffect(tetheredEffect);

                // Broadcast packet to client tracking players so client entity has MobEffectInstance synced!
                if (target.level().getChunkSource() instanceof ServerChunkCache chunkCache) {
                    chunkCache.sendToTrackingPlayersAndSelf(target, new ClientboundUpdateMobEffectPacket(target.getId(), tetheredEffect, false));
                }

                nbt.putString(TETHER_SOURCE_UUID_TAG, player.getStringUUID());
                nbt.putInt(ELASTIC_HIT_COUNT_TAG, 0);
            }
        }

        // ===== GLUTTONY =====
        var gluttonyHolder = enchantmentRegistry.get(ModEnchantments.GLUTTONY);
        if (gluttonyHolder.isPresent() && weapon.getEnchantmentLevel(gluttonyHolder.get()) > 0) {
            if (target instanceof Player targetPlayer) {
                int currentFood = targetPlayer.getFoodData().getFoodLevel();
                if (currentFood > 0) {
                    targetPlayer.getFoodData().setFoodLevel(Math.max(0, currentFood - 1));
                }
            }

            int attackerFood = player.getFoodData().getFoodLevel();
            if (attackerFood < 20) {
                player.getFoodData().setFoodLevel(Math.min(20, attackerFood + 1));
            } else {
                float currentSat = player.getFoodData().getSaturationLevel();
                player.getFoodData().setSaturation(currentSat + 0.1f);
            }
        }

        // ===== VAMPIRISM =====
        var vampirismHolder = enchantmentRegistry.get(ModEnchantments.VAMPIRISM);
        if (vampirismHolder.isPresent() && weapon.getEnchantmentLevel(vampirismHolder.get()) > 0) {
            long currentTime = player.level().getGameTime();
            UUID playerUUID = player.getUUID();
            boolean canProc = true;

            if (VAMPIRISM_COOLDOWNS.containsKey(playerUUID)) {
                if (currentTime - VAMPIRISM_COOLDOWNS.get(playerUUID) < VAMPIRISM_HIT_COOLDOWN) {
                    canProc = false;
                }
            }

            spawnVampirismParticles(player, target);

            if (canProc) {
                VAMPIRISM_COOLDOWNS.put(playerUUID, currentTime);
                MobEffectInstance currentEffect = player.getEffect(ModMobEffects.TEMPORARY_HEALTH);
                int currentLevel = 0;
                if (currentEffect != null) {
                    currentLevel = currentEffect.getAmplifier();
                }

                int newLevel = Math.min(currentLevel + 1, 4);
                int newDuration = (newLevel + 1) * 100; // 5s to 25s

                player.addEffect(new MobEffectInstance(ModMobEffects.TEMPORARY_HEALTH, newDuration, newLevel, false, true, true));
                player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), (newLevel + 1) * 2.0F));
            }
        }

        // ===== WILD =====
        var wildHolder = enchantmentRegistry.get(ModEnchantments.WILD);
        if (wildHolder.isPresent() && weapon.getEnchantmentLevel(wildHolder.get()) > 0) {
            UUID playerUUID = player.getUUID();
            int hits = WILD_HITS.getOrDefault(playerUUID, 0) + 1;

            if (hits < WILD_REQUIRED_HITS) {
                WILD_HITS.put(playerUUID, hits);
            } else {
                WILD_HITS.put(playerUUID, 0);
                float baseDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                float aoeDamage = baseDamage * (float) WILD_DAMAGE_MULTIPLIER;

                AABB area = new AABB(
                    target.getX() - WILD_AOE_RADIUS, target.getY() - WILD_AOE_RADIUS, target.getZ() - WILD_AOE_RADIUS,
                    target.getX() + WILD_AOE_RADIUS, target.getY() + WILD_AOE_RADIUS, target.getZ() + WILD_AOE_RADIUS
                );

                var nearby = target.level().getEntitiesOfClass(
                    LivingEntity.class, area,
                    e -> e != player && e != target && e.isAlive()
                );

                if (!nearby.isEmpty()) {
                    DamageSource ds = player.damageSources().playerAttack(player);
                    for (LivingEntity e : nearby) {
                        e.hurt(ds, aoeDamage);
                    }
                }
            }
        }

        // ===== DETONATION =====
        var detonationHolder = enchantmentRegistry.get(ModEnchantments.DETONATION);
        if (detonationHolder.isPresent() && weapon.getEnchantmentLevel(detonationHolder.get()) > 0) {
            if (player.getAttackStrengthScale(0.5F) < 0.9F) return;

            UUID targetId = target.getUUID();
            int hits = DETONATION_HITS.getOrDefault(targetId, 0) + 1;

            if (hits < 5) {
                DETONATION_HITS.put(targetId, hits);
            } else {
                DETONATION_HITS.remove(targetId);
                triggerDetonationExplosion(player, target);
            }
        }
    }

    // ===== HEROISM: DAMAGE SCALE BY HP =====
    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        ItemStack weapon = player.getMainHandItem();
        if (weapon.isEmpty()) return;

        var enchantmentRegistry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var heroismHolder = enchantmentRegistry.get(ModEnchantments.HEROISM);

        if (heroismHolder.isPresent() && weapon.getEnchantmentLevel(heroismHolder.get()) > 0) {
            float health = player.getHealth();
            float maxHealth = player.getMaxHealth();
            float healthPercent = maxHealth > 0 ? health / maxHealth : 1.0F;

            float bonusDamage = 0.0F;

            if (healthPercent > 0.75F) {
                // High HP: +2 damage per 5% HP above 75% (+20% at 100% HP)
                int steps = (int) Math.floor((healthPercent - 0.75F + 0.0001F) / 0.05F);
                bonusDamage = steps * 2.0F;
            } else if (healthPercent < 0.25F) {
                // Low HP: +2 damage per 5% HP below 25% (+20% at 0% HP)
                int steps = (int) Math.floor((0.25F - healthPercent + 0.0001F) / 0.05F);
                bonusDamage = steps * 2.0F;
            }

            if (bonusDamage > 0.0F) {
                event.setAmount(event.getAmount() + bonusDamage);
            }
        }

        // ===== DEFERRED: DAMAGE CONVERSION =====
        var deferredHolder = enchantmentRegistry.get(ModEnchantments.DEFERRED);
        if (deferredHolder.isPresent() && weapon.getEnchantmentLevel(deferredHolder.get()) > 0) {
            if (IS_DEFERRED_TICK.get()) return;
            if (player.getAttackStrengthScale(0.5F) < 0.9F) return;

            // Damage formula: 12.0F (Enchantment Bonus) + Weapon/Attack Damage
            float weaponDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float baseDamage = Math.max(weaponDamage, event.getAmount());
            float totalDeferredDamage = 12.0F + baseDamage;

            // Zero out direct hit damage
            event.setAmount(0.0F);

            // Create 10 installments over 2.5 seconds (50 ticks)
            LivingEntity target = event.getEntity();
            DEFERRED_TASKS.add(new DeferredDamageTask(target.getUUID(), target.level(), totalDeferredDamage));
        }
    }

    // ===== HEROISM: STATUS REFLECTION ON CRITICAL HIT =====
    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;

        ItemStack weapon = player.getMainHandItem();
        if (weapon.isEmpty()) return;

        var enchantmentRegistry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var heroismHolder = enchantmentRegistry.get(ModEnchantments.HEROISM);

        if (heroismHolder.isPresent() && weapon.getEnchantmentLevel(heroismHolder.get()) > 0) {
            UUID playerUUID = player.getUUID();
            long currentTime = player.level().getGameTime();
            long lastProc = HEROISM_COOLDOWNS.getOrDefault(playerUUID, 0L);

            if (currentTime - lastProc >= HEROISM_REFLECT_COOLDOWN) {
                boolean reflectedAny = false;

                // 1. Reflect Fire
                if (player.isOnFire() || player.getRemainingFireTicks() > 0) {
                    int fireSeconds = Math.max(3, player.getRemainingFireTicks() / 20);
                    target.igniteForSeconds(fireSeconds);
                    reflectedAny = true;
                }

                // 2. Reflect Harmful Status Effects
                for (MobEffectInstance effect : player.getActiveEffects()) {
                    if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                        MobEffectInstance reflectedEffect = new MobEffectInstance(
                            effect.getEffect(), effect.getDuration(), effect.getAmplifier(),
                            false, true, true
                        );
                        target.addEffect(reflectedEffect);

                        if (target.level().getChunkSource() instanceof ServerChunkCache chunkCache) {
                            chunkCache.sendToTrackingPlayersAndSelf(target,
                                new ClientboundUpdateMobEffectPacket(target.getId(), reflectedEffect, false));
                        }
                        reflectedAny = true;
                    }
                }

                if (reflectedAny) {
                    HEROISM_COOLDOWNS.put(playerUUID, currentTime);

                    // Spawn particles on target
                    if (target.level() instanceof ServerLevel serverLevel) {
                        DustParticleOptions yellowDust = new DustParticleOptions(0xCCFF00, 1.2F);
                        serverLevel.sendParticles(yellowDust, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 20, 0.4, 0.4, 0.4, 0.1);
                        serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
                    }
                }
            }
        }
    }

    // ELASTIC TICK LOGIC
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity.level().isClientSide()) return;

        // ===== DEFERRED TICK PROCESSING =====
        if (!DEFERRED_TASKS.isEmpty() && entity.level() instanceof ServerLevel serverLevel) {
            java.util.Iterator<DeferredDamageTask> iterator = DEFERRED_TASKS.iterator();
            while (iterator.hasNext()) {
                DeferredDamageTask task = iterator.next();
                if (task.targetUUID.equals(entity.getUUID())) {
                    task.ticksUntilNextInstallment--;
                    if (task.ticksUntilNextInstallment <= 0) {
                        task.ticksUntilNextInstallment = 5;
                        task.remainingInstallments--;

                        IS_DEFERRED_TICK.set(true);
                        try {
                            entity.hurtServer(serverLevel, entity.damageSources().generic(), task.portionPerTick);
                        } finally {
                            IS_DEFERRED_TICK.set(false);
                        }

                        serverLevel.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 5, 0.2, 0.3, 0.2, 0.02);
                    }

                    if (task.remainingInstallments <= 0 || !entity.isAlive()) {
                        iterator.remove();
                    }
                }
            }
        }

        CompoundTag nbt = entity.getPersistentData();
        if (!entity.hasEffect(ModMobEffects.TETHERED)) {
            if (nbt.contains(TETHER_SOURCE_UUID_TAG)) {
                nbt.remove(TETHER_SOURCE_UUID_TAG);
            }
            return;
        }

        if (!nbt.contains(TETHER_SOURCE_UUID_TAG)) return;

        MobEffectInstance effect = entity.getEffect(ModMobEffects.TETHERED);
        if (effect != null && effect.getDuration() <= 1) {
            nbt.remove(TETHER_SOURCE_UUID_TAG);
            entity.removeEffect(ModMobEffects.TETHERED);
            if (entity.level() instanceof ServerLevel serverLevel && serverLevel.getChunkSource() instanceof ServerChunkCache chunkCache) {
                chunkCache.sendToTrackingPlayersAndSelf(entity, new net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket(entity.getId(), effect.getEffect()));
            }
            return;
        }

        String sourceUUID = nbt.getString(TETHER_SOURCE_UUID_TAG).orElse("");
        ServerLevel level = (ServerLevel) entity.level();

        Player sourcePlayer = null;
        for (Player p : level.players()) {
            if (p.getStringUUID().equals(sourceUUID)) {
                sourcePlayer = p;
                break;
            }
        }

        if (sourcePlayer == null) {
            var tetheredEffect = entity.getEffect(ModMobEffects.TETHERED);
            entity.removeEffect(ModMobEffects.TETHERED);
            if (tetheredEffect != null && level.getChunkSource() instanceof ServerChunkCache chunkCache) {
                chunkCache.sendToTrackingPlayersAndSelf(entity, new net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket(entity.getId(), tetheredEffect.getEffect()));
            }
            nbt.remove(TETHER_SOURCE_UUID_TAG);
            return;
        }

        Vec3 entityPos = entity.position();
        Vec3 sourcePos = sourcePlayer.position();
        Vec3 direction = sourcePos.subtract(entityPos).normalize();
        double distance = entityPos.distanceTo(sourcePos);

        if (distance > 1.5) {
            double strength = Math.min(PULL_STRENGTH * (distance / 10.0), PULL_STRENGTH * 2);
            Vec3 pullVelocity = direction.scale(strength);
            Vec3 currentVelocity = entity.getDeltaMovement();
            entity.setDeltaMovement(currentVelocity.add(pullVelocity));
            entity.hurtMarked = true;
        }
    }

    private static void spawnVampirismParticles(LivingEntity attacker, LivingEntity target) {
        if (attacker.level() instanceof ServerLevel serverLevel) {
            Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.6, 0);
            DustParticleOptions redDust = new DustParticleOptions(0xFF0000, 1.0f);
            serverLevel.sendParticles(redDust, targetPos.x, targetPos.y, targetPos.z, 15, 0.3, 0.4, 0.3, 0.05);

            Vec3 attackerPos = attacker.position().add(0, attacker.getBbHeight() * 0.6, 0);
            serverLevel.sendParticles(ParticleTypes.SNEEZE, attackerPos.x, attackerPos.y, attackerPos.z, 10, 0.3, 0.4, 0.3, 0.05);
        }
    }

    private static void triggerDetonationExplosion(Player player, LivingEntity target) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        Vec3 pos = target.position().add(0, target.getBbHeight() * 0.5, 0);
        float explosionDamage = 7.0F;
        double radius = 3.0;

        AABB area = new AABB(pos.x - radius, pos.y - radius, pos.z - radius, pos.x + radius, pos.y + radius, pos.z + radius);
        DamageSource damageSource = player.damageSources().playerAttack(player);
        DamageSource trueDamage = player.damageSources().generic();

        for (LivingEntity nearby : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
            if (nearby.isAlive() && nearby != player && !nearby.isAlliedTo(player)) {
                if (nearby.isBlocking()) {
                    nearby.hurt(damageSource, explosionDamage);
                } else {
                    nearby.hurtServer(serverLevel, trueDamage, explosionDamage);
                }
            }
        }

        // Custom Sound Effect
        serverLevel.playSound(null, pos.x, pos.y, pos.z,
            net.minecraft.sounds.SoundEvents.FIRECHARGE_USE,
            net.minecraft.sounds.SoundSource.PLAYERS,
            1.5F, 0.8F);
        serverLevel.playSound(null, pos.x, pos.y, pos.z,
            net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
            net.minecraft.sounds.SoundSource.PLAYERS,
            0.8F, 1.4F);

        // Custom Orange Shockwave & Particle Explosive Burst (No vanilla explosion particle)
        DustParticleOptions orangeDust = new DustParticleOptions(0xFF6600, 1.8F);
        DustParticleOptions darkOrangeDust = new DustParticleOptions(0xFF3300, 1.4F);

        for (int i = 0; i < 36; i++) {
            double angle = Math.toRadians(i * 10);
            double rx = Math.cos(angle) * 1.8;
            double rz = Math.sin(angle) * 1.8;
            serverLevel.sendParticles(orangeDust, pos.x + rx, pos.y, pos.z + rz, 1, rx * 0.1, 0.05, rz * 0.1, 0.05);
            serverLevel.sendParticles(darkOrangeDust, pos.x + rx * 0.5, pos.y, pos.z + rz * 0.5, 1, 0, 0.1, 0, 0.02);
        }

        serverLevel.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 25, 0.4, 0.5, 0.4, 0.15);
        serverLevel.sendParticles(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 12, 0.3, 0.4, 0.3, 0.1);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 10, 0.3, 0.4, 0.3, 0.05);
        serverLevel.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 20, 0.5, 0.5, 0.5, 0.2);
    }

    // ===== STORMBREAKER: RIGHT-CLICK THROW =====
    @SubscribeEvent
    public static void onRightClickItem(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        var enchantmentRegistry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var stormbreakerHolder = enchantmentRegistry.get(ModEnchantments.STORMBREAKER);

        if (stormbreakerHolder.isPresent() && stack.getEnchantmentLevel(stormbreakerHolder.get()) > 0) {
            if (player.getCooldowns().isOnCooldown(stack)) {
                event.setCanceled(true);
                return;
            }

            ItemStack thrownStack = stack.copy();
            player.getCooldowns().addCooldown(stack, 160); // 8 seconds cooldown
            stack.shrink(1);

            com.huwng.alterna.entity.ThrownStormbreakerEntity entity =
                new com.huwng.alterna.entity.ThrownStormbreakerEntity(player.level(), player, thrownStack);
            entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F, 1.0F);
            player.level().addFreshEntity(entity);

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.TRIDENT_THROW,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);

            event.setCanceled(true);
        }
    }
}
