package com.huwng.alterna.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class ThrownStormbreakerEntity extends AbstractArrow implements ItemSupplier {

    private static final EntityDataAccessor<ItemStack> ID_ITEM = SynchedEntityData.defineId(ThrownStormbreakerEntity.class, EntityDataSerializers.ITEM_STACK);
    private boolean dealtDamage = false;
    private int returnTickCount = 0;

    public ThrownStormbreakerEntity(EntityType<? extends ThrownStormbreakerEntity> type, Level level) {
        super(type, level);
    }

    public ThrownStormbreakerEntity(Level level, LivingEntity owner, ItemStack weaponItem) {
        super(ModEntities.THROWN_STORMBREAKER.get(), owner, level, weaponItem, weaponItem);
        this.setItem(weaponItem.copy());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ID_ITEM, new ItemStack(Items.DIAMOND_SWORD));
    }

    @Override
    public ItemStack getItem() {
        return this.entityData.get(ID_ITEM);
    }

    public void setItem(ItemStack stack) {
        this.entityData.set(ID_ITEM, stack.copyWithCount(1));
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return this.getItem();
    }

    @Override
    public void tick() {
        if (this.inGroundTime > 4) {
            this.dealtDamage = true;
        }

        Entity owner = this.getOwner();
        if ((this.dealtDamage || this.isNoPhysics()) && owner != null && owner.isAlive()) {
            if (owner instanceof Player player) {
                this.setNoPhysics(true);
                Vec3 vec = player.getEyePosition().subtract(this.position());
                this.setPosRaw(this.getX(), this.getY() + vec.y * 0.015 * 3, this.getZ());
                double accel = 0.05 * 3;
                this.setDeltaMovement(this.getDeltaMovement().scale(0.95).add(vec.normalize().scale(accel)));

                if (this.returnTickCount == 0) {
                    this.playSound(SoundEvents.TRIDENT_RETURN, 10.0F, 1.0F);
                }
                this.returnTickCount++;

                if (this.position().distanceTo(player.getEyePosition()) < player.getBbWidth() + 1.2) {
                    if (!player.getInventory().add(this.getItem())) {
                        player.drop(this.getItem(), false);
                    }
                    this.playSound(SoundEvents.TRIDENT_RETURN, 1.0F, 1.0F);
                    this.discard();
                    return;
                }
            }
        }

        super.tick();
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        if (this.dealtDamage) return;
        Entity target = hitResult.getEntity();
        Entity owner = this.getOwner();

        float damage = 8.0F;
        DamageSource damageSource = this.damageSources().trident(this, owner != null ? owner : this);

        this.dealtDamage = true;

        if (this.level() instanceof ServerLevel serverLevel) {
            target.hurtServer(serverLevel, damageSource, damage);

            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
            if (lightning != null) {
                lightning.setPos(target.getX(), target.getY(), target.getZ());
                if (owner instanceof ServerPlayer serverPlayer) {
                    lightning.setCause(serverPlayer);
                }
                serverLevel.addFreshEntity(lightning);
            }
        }

        this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01D, -0.1D, -0.01D));
    }
}
