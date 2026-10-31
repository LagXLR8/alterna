package com.huwng.alterna.entity;

import com.huwng.alterna.item.GrappleItem;
import com.huwng.alterna.sound.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The claw of the Overlord Grapple. Flies straight out, bites into the first block it hits and
 * holds there while its owner keeps right-click held (see {@link GrappleItem}, which does the
 * pulling). Lets go as soon as the owner stops using the grapple, or if the line gets too long.
 */
public class GrappleHookEntity extends Projectile {
    private static final EntityDataAccessor<Boolean> DATA_ANCHORED = SynchedEntityData.defineId(GrappleHookEntity.class, EntityDataSerializers.BOOLEAN);
    public static final double MAX_RANGE = 36.0;

    public GrappleHookEntity(EntityType<? extends GrappleHookEntity> type, Level level) {
        super(type, level);
    }

    public GrappleHookEntity(Level level, Player owner) {
        this(ModEntities.GRAPPLE_HOOK.get(), level);
        this.setOwner(owner);
        Vec3 eye = owner.getEyePosition();
        this.setPos(eye.x, eye.y - 0.1, eye.z);
        this.shootFromRotation(owner, owner.getXRot(), owner.getYRot(), 0.0F, 2.8F, 0.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ANCHORED, false);
    }

    public boolean isAnchored() {
        return this.entityData.get(DATA_ANCHORED);
    }

    public @Nullable Player getPlayerOwner() {
        return this.getOwner() instanceof Player p ? p : null;
    }

    @Override
    public void tick() {
        super.tick();
        Player owner = this.getPlayerOwner();
        if (!this.level().isClientSide()) {
            if (owner == null || !owner.isAlive() || owner.level() != this.level()
                    || this.distanceToSqr(owner) > (MAX_RANGE + 6) * (MAX_RANGE + 6) || !GrappleItem.isGrappling(owner)) {
                this.discard();
                return;
            }
        }
        if (this.isAnchored()) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (!this.level().isClientSide() && (this.tickCount > 30 || owner != null && this.distanceToSqr(owner) > MAX_RANGE * MAX_RANGE)) {
            this.discard();          // missed
            return;
        }
        Vec3 motion = this.getDeltaMovement();
        Vec3 from = this.position();
        HitResult hit = this.level().clip(new ClipContext(from, from.add(motion), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult bhr) {
            Vec3 at = bhr.getLocation().subtract(motion.normalize().scale(0.08));
            this.setPos(at.x, at.y, at.z);
            this.setDeltaMovement(Vec3.ZERO);
            if (!this.level().isClientSide()) {
                this.entityData.set(DATA_ANCHORED, true);
                this.level().playSound(null, at.x, at.y, at.z, ModSounds.OVERLORD_DEFLECT.get(), SoundSource.PLAYERS, 0.8F, 1.5F);
            }
        } else {
            this.setPos(from.add(motion));
            this.setDeltaMovement(motion.scale(0.99).add(0.0, -0.025, 0.0));
            this.updateRotation();
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
