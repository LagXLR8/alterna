package com.huwng.alterna.entity;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.block.ModBlocks;
import com.huwng.alterna.item.ModItems;
import com.huwng.alterna.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.entity.PartEntity;
import org.jspecify.annotations.Nullable;

/**
 * Cragback - the big grey grazer of the Lichen Savanna.
 *
 * - Peaceful herd animal: wanders the purple grassland in loose herds, grazes the
 *   lichen grass and regularly walks to the nearest pond to drink.
 * - Extremely tough hide: lots of health and natural armour, nearly immune to
 *   knockback, and takes reduced damage from arrows and weak hits.
 * - Never starts a fight, but lowers its horn and charges whoever hurts it.
 * - Every so often sheds a Crag Plate from its back (like an armadillo scute),
 *   so players can farm armour material without killing it.
 */
public class CragbackEntity extends Animal {
    public static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, Alterna.id("cragback_food"));
    private static final EntityDataAccessor<Boolean> DATA_CHARGING = SynchedEntityData.defineId(CragbackEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDimensions BABY_DIMENSIONS = EntityDimensions.scalable(1.1F, 1.2F).withEyeHeight(0.95F);

    public static final byte EVENT_EAT = 10;
    public static final byte EVENT_DRINK = 11;
    public static final int EAT_TICKS = 40;
    public static final int DRINK_TICKS = 60;

    private int eatAnimationTick;
    private int drinkAnimationTick;
    private int plateTime;
    /** Rises over time; the drink goal resets it. */
    int thirst;

    /** Second hitbox covering the head, which sticks out past the square body hitbox. */
    private final CragbackHeadPart headPart;
    private final PartEntity<?>[] parts;

    public CragbackEntity(EntityType<? extends CragbackEntity> type, Level level) {
        super(type, level);
        this.plateTime = this.pickNextPlateTime();
        this.thirst = this.random.nextInt(2400);
        this.moveControl = new CragbackMoveControl(this);
        this.headPart = new CragbackHeadPart(this, 1.3F, 1.4F);
        this.parts = new PartEntity<?>[]{this.headPart};
        // part ids must directly follow the parent id so the client can match them up
        this.setId(ENTITY_COUNTER.getAndAdd(this.parts.length + 1) + 1);
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (this.parts != null) {
            for (int i = 0; i < this.parts.length; i++) this.parts[i].setId(id + i + 1);
        }
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return this.parts;
    }

    private void tickHeadPart() {
        float scale = this.isBaby() ? 0.5F : 1.0F;
        if (Math.abs(this.headPart.getBbWidth() - 1.3F * scale) > 0.01F) this.headPart.refreshDimensions();
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        // the snout reaches about 1.85 blocks in front of the body centre; lower it while grazing/drinking
        double forward = 1.75 * scale;
        double lift = (this.eatAnimationTick > 0 || this.drinkAnimationTick > 0 ? 0.15 : 0.6) * scale;
        this.headPart.setOldPosAndRot();
        this.headPart.setPos(this.getX() - Mth.sin(yaw) * forward, this.getY() + lift, this.getZ() + Mth.cos(yaw) * forward);
    }

    // ---------------------------------------------------------------- solid back
    /** The Cragback's back is solid: players and mobs can stand on it (like a boat). */
    @Override
    public boolean canBeCollidedWith(net.minecraft.world.entity.@Nullable Entity other) {
        return this.isAlive() && !(other instanceof CragbackHeadPart);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /** Head turns are heavy and slow, too. */
    @Override
    public int getHeadRotSpeed() {
        return 6;
    }

    @Override
    public int getMaxHeadYRot() {
        return 45;
    }

    private double carryX, carryY, carryZ;
    private float carryYaw;
    private boolean carryInit;

    /**
     * Anything standing on the back rides along: it is moved with the Cragback and turned
     * with it. Players are moved on their own client (their movement is client-side),
     * everything else on the server.
     */
    private void carryRiders() {
        double dx = this.getX() - this.carryX, dy = this.getY() - this.carryY, dz = this.getZ() - this.carryZ;
        float dyaw = Mth.wrapDegrees(this.getYRot() - this.carryYaw);
        boolean first = !this.carryInit;
        this.carryInit = true;
        this.carryX = this.getX();
        this.carryY = this.getY();
        this.carryZ = this.getZ();
        this.carryYaw = this.getYRot();
        if (first || dx * dx + dy * dy + dz * dz > 4.0 || (dx == 0 && dy == 0 && dz == 0 && dyaw == 0)) return;
        net.minecraft.world.phys.AABB top = this.getBoundingBox();
        net.minecraft.world.phys.AABB zone = new net.minecraft.world.phys.AABB(top.minX, top.maxY - 0.1, top.minZ, top.maxX, top.maxY + 0.6, top.maxZ);
        double cos = Math.cos(-dyaw * Mth.DEG_TO_RAD), sin = Math.sin(-dyaw * Mth.DEG_TO_RAD);
        for (net.minecraft.world.entity.Entity rider : this.level().getEntities(this, zone,
                e -> !(e instanceof CragbackHeadPart) && !e.isPassenger() && !e.noPhysics && e.getBoundingBox().minY >= top.maxY - 0.15)) {
            boolean clientPlayer = rider instanceof Player && rider.isLocalInstanceAuthoritative();
            if (this.level().isClientSide() ? !clientPlayer : rider instanceof Player) continue;
            // rotate the rider's offset around the Cragback's centre, then translate
            double ox = rider.getX() - (this.getX() - dx), oz = rider.getZ() - (this.getZ() - dz);
            double rx = ox * cos - oz * sin, rz = ox * sin + oz * cos;
            rider.setPos(this.getX() + rx, rider.getY() + Math.max(0.0, dy), this.getZ() + rz);
            rider.setYRot(rider.getYRot() + dyaw);
            rider.setYHeadRot(rider.getYHeadRot() + dyaw);
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.carryRiders();
    }

    /**
     * Vanilla melee reach is measured from the (square) body hitbox, but the Cragback
     * attacks with the horn on its long head - so the head hitbox counts as reach too.
     */
    @Override
    public boolean isWithinMeleeAttackRange(LivingEntity target) {
        return super.isWithinMeleeAttackRange(target)
                || this.headPart.getBoundingBox().inflate(0.9, 0.4, 0.9).intersects(target.getBoundingBox());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.17)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.6)
                .add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    public static boolean checkCragbackSpawnRules(EntityType<CragbackEntity> type, LevelAccessor level, EntitySpawnReason reason,
            BlockPos pos, RandomSource random) {
        // Lichen does not need light, and neither does the Cragback - it lives deep in the crack.
        return level.getBlockState(pos.below()).is(ModBlocks.LICHEN_TURF.get());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHARGING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CragbackChargeGoal(this));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, s -> s.is(FOOD), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(5, new CragbackGoals.DrinkGoal(this));
        this.goalSelector.addGoal(6, new CragbackGoals.GrazeGoal(this));
        this.goalSelector.addGoal(7, new CragbackGoals.HerdGoal(this));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        // Only ever fights back against whoever hurt it - never starts a fight itself.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    /** Melee charge used only while retaliating. */
    static class CragbackChargeGoal extends MeleeAttackGoal {
        private final CragbackEntity cragback;

        CragbackChargeGoal(CragbackEntity mob) {
            super(mob, 1.45, true);
            this.cragback = mob;
        }

        @Override
        public boolean canUse() {
            return !this.cragback.isBaby() && super.canUse();
        }

        @Override
        public void start() {
            super.start();
            this.cragback.setCharging(true);
        }

        @Override
        public void stop() {
            super.stop();
            this.cragback.setCharging(false);
        }
    }

    public boolean isCharging() {
        return this.entityData.get(DATA_CHARGING);
    }

    void setCharging(boolean charging) {
        this.entityData.set(DATA_CHARGING, charging);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.CRAGBACK.get().create(level, EntitySpawnReason.BREEDING);
    }

    // ---------------------------------------------------------------- toughness
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            if (source.is(DamageTypeTags.IS_PROJECTILE)) {
                damage *= 0.4F;              // arrows mostly glance off the hide
            } else if (damage <= 4.0F) {
                damage *= 0.6F;              // weak hits barely scratch it
            }
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        this.thirst++;
        LivingEntity target = this.getTarget();
        if (target != null && (!target.isAlive() || this.distanceToSqr(target) > 24 * 24
                || this.tickCount - this.getLastHurtByMobTimestamp() > 400)) {
            // calms down quickly once the threat is gone
            this.setTarget(null);
        }
        if (this.isAlive() && !this.isBaby() && --this.plateTime <= 0) {
            this.spawnAtLocation(level, ModItems.CRAG_PLATE.get());
            this.playSound(SoundEvents.ARMADILLO_SCUTE_DROP, 1.0F, 0.6F + this.random.nextFloat() * 0.1F);
            this.plateTime = this.pickNextPlateTime();
        }
    }

    private int pickNextPlateTime() {
        return 9000 + this.random.nextInt(9000);
    }

    // ---------------------------------------------------------------- animation
    @Override
    public void aiStep() {
        if (this.level().isClientSide()) {
            this.eatAnimationTick = Math.max(0, this.eatAnimationTick - 1);
            this.drinkAnimationTick = Math.max(0, this.drinkAnimationTick - 1);
        }
        super.aiStep();
        this.tickHeadPart();
        if (this.level().isClientSide()) {
            if (this.legs == null) this.legs = new CragbackLegs();
            this.legs.tick(this);
        }
    }

    /** Procedural leg state (client only, purely visual). */
    private @Nullable CragbackLegs legs;

    /** Fills the leg / body-attitude part of the render state; returns false if there is no data yet. */
    public boolean extractLegs(float partial, double px, double py, double pz, float bodyYawDeg, float modelScale,
            float[] outX, float[] outY, float[] outZ, float[] attitude) {
        if (this.legs == null || !this.legs.ready()) return false;
        float yaw = bodyYawDeg * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);      // forward
        double rx = -Mth.cos(yaw), rz = -Mth.sin(yaw);     // right
        float s = 16.0F / modelScale;
        for (int i = 0; i < 4; i++) {
            net.minecraft.world.phys.Vec3 f = this.legs.foot(i, partial);
            double dx = f.x - px, dy = f.y - py, dz = f.z - pz;
            double lr = dx * rx + dz * rz, lf = dx * fx + dz * fz;
            outX[i] = (float) (-lr * s);              // model +x is the creature's left
            outY[i] = (float) (24.0 - dy * s);        // model y points down, ground at 24
            outZ[i] = (float) (-lf * s);              // model -z is forward
        }
        attitude[0] = Mth.lerp(partial, this.legs.pitchO, this.legs.pitch);
        attitude[1] = Mth.lerp(partial, this.legs.rollO, this.legs.roll);
        attitude[2] = -Mth.lerp(partial, this.legs.dropO, this.legs.drop) * s;   // pixels down
        attitude[3] = Mth.lerp(partial, this.legs.bobO, this.legs.bob) * 16.0F / modelScale * 0.12F;
        attitude[4] = Mth.lerp(partial, this.legs.swayO, this.legs.sway);
        return true;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_EAT) {
            this.eatAnimationTick = EAT_TICKS;
        } else if (id == EVENT_DRINK) {
            this.drinkAnimationTick = DRINK_TICKS;
        } else {
            super.handleEntityEvent(id);
        }
    }

    private static float envelope(int ticks, int total, float partial) {
        if (ticks <= 0) return 0.0F;
        float t = (ticks - partial);
        float fadeIn = Mth.clamp((total - t) / 6.0F, 0.0F, 1.0F);
        float fadeOut = Mth.clamp(t / 6.0F, 0.0F, 1.0F);
        return Math.min(fadeIn, fadeOut);
    }

    public float getEatAnimation(float partial) {
        return envelope(this.eatAnimationTick, EAT_TICKS, partial);
    }

    public float getDrinkAnimation(float partial) {
        return envelope(this.drinkAnimationTick, DRINK_TICKS, partial);
    }

    /** Called by the graze goal once a mouthful of grass has been eaten. */
    void onGrazed() {
        if (this.isBaby()) this.ageUp(60);
        this.heal(1.0F);
    }

    // ---------------------------------------------------------------- misc
    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(pose);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("plate_time", this.plateTime);
        output.putInt("thirst", this.thirst);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.plateTime = input.getIntOr("plate_time", this.pickNextPlateTime());
        this.thirst = input.getIntOr("thirst", 0);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSounds.CRAGBACK_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CRAGBACK_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CRAGBACK_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // footfalls are played per foot by the procedural legs (client side)
    }

    @Override
    public float getVoicePitch() {
        return (this.isBaby() ? 1.5F : 1.0F) + (this.random.nextFloat() - this.random.nextFloat()) * 0.08F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.9F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }
}
