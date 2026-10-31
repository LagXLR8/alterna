package com.huwng.alterna.entity;

import com.huwng.alterna.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Armor Overlord - the apex hunter of the Rootshroom Forest.
 *
 * A five-block-tall spider-thing: a heavy mushroom-cap carapace carried on four huge arching
 * legs, a glowing brain cluster tucked under the brim and two long hooked arms.
 *
 * - Hunts anything that walks: players, animals, zombies.
 * - Sees only in a cone in front of its brain (about 110 degrees wide). Sneaking shrinks the
 *   distance it can see you from; standing among its legs it always feels you. Get out of
 *   its sight for a few seconds and it loses you, walks to where it last saw you, looks
 *   around - and gives up.
 * - It turns slowly, so circling behind it works.
 * - The cap is armour: hits on it (or anything from above, arrows included) are mostly
 *   absorbed. The brain under the brim takes double damage, and hitting it before it has
 *   noticed you is even better.
 * - Attacks: a long hook that drags prey in, a double slash with both blades, and a stomp
 *   when something is underneath it.
 */
public class ArmorOverlordEntity extends Monster {
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(ArmorOverlordEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HUNTING = SynchedEntityData.defineId(ArmorOverlordEntity.class, EntityDataSerializers.BOOLEAN);

    public static final byte EVENT_HOOK_WINDUP = 40, EVENT_HOOK_STRIKE = 41, EVENT_SLASH = 42, EVENT_STOMP = 43, EVENT_ROAR = 44;
    static final int HOOK_WINDUP = 12, HOOK_TIME = 26, SLASH_TIME = 22, SLASH_HIT_1 = 9, SLASH_HIT_2 = 15, STOMP_TIME = 26, STOMP_HIT = 12, ROAR_TIME = 24;

    /** Vision: half-angle of the view cone, normal / sneaking sight range, and the "feel" radius. */
    /** Render / hitbox scale of the model (the model itself is authored at 1.0). */
    public static final float SCALE = 1.2F;
    static final float VIEW_HALF_ANGLE = 55.0F;
    static final double SIGHT = 24.0, SIGHT_SNEAKING = 7.0, FEEL = 3.0;
    /** How far (degrees) the body may twist past the way its planted claws hold it. */
    static final float TWIST = 12.0F;

    private final OverlordPart capPart, brainPart, bodyPart;
    private final OverlordPart[] legParts = new OverlordPart[4];
    private final PartEntity<?>[] parts;
    private final ServerBossEvent bossEvent = new ServerBossEvent(UUID.randomUUID(), this.getDisplayName(),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    // ---- hunting memory (server)
    int lastSeenTick = -1000;
    @Nullable Vec3 lastKnownPos;
    @Nullable Vec3 searchPos;
    int searchTime;
    int roarTime;
    int hookCooldown, slashCooldown, stompCooldown;
    /** Attack in progress: 0 none, 1 hook, 2 slash, 3 stomp. */
    int attack, attackTick;

    // ---- client animation
    int clHookWind, clHookStrike, clSlash, clStomp, clRoar;
    float glow, glowO;
    private @Nullable OverlordLegs legs;

    public ArmorOverlordEntity(EntityType<? extends ArmorOverlordEntity> type, Level level) {
        super(type, level);
        this.moveControl = new OverlordMoveControl(this);
        this.xpReward = 40;
        this.capPart = new OverlordPart(this, OverlordPart.Zone.CAP, 3.3F * SCALE, 1.0F * SCALE);
        this.brainPart = new OverlordPart(this, OverlordPart.Zone.BRAIN, 1.25F * SCALE, 0.7F * SCALE);
        this.bodyPart = new OverlordPart(this, OverlordPart.Zone.BODY, 1.25F * SCALE, 1.6F * SCALE);
        for (int i = 0; i < 4; i++) this.legParts[i] = new OverlordPart(this, OverlordPart.Zone.LEG, 0.85F * SCALE, 2.3F * SCALE);
        this.parts = new PartEntity<?>[]{this.capPart, this.brainPart, this.bodyPart,
                this.legParts[0], this.legParts[1], this.legParts[2], this.legParts[3]};
        // it walks straight through foliage, breaking it
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.LEAVES, 0.0F);
        // part ids must directly follow the parent id so the client can match them up
        this.setId(ENTITY_COUNTER.getAndAdd(this.parts.length + 1) + 1);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 150.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_HUNTING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new OverlordGoals.HuntGoal(this));
        this.goalSelector.addGoal(2, new OverlordGoals.SearchGoal(this));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.55, 100));
        this.goalSelector.addGoal(5, new OverlordGoals.ListenGoal(this));
        this.targetSelector.addGoal(1, new OverlordGoals.SpotPreyGoal(this));
    }

    // ---------------------------------------------------------------- multipart
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

    /**
     * The hitboxes follow the creature itself, not the (invisible, unhittable) main box: the
     * body, cap and brain ride at the height the legs are holding the body at, and each leg
     * has its own box standing on its planted claw.
     */
    private void tickParts() {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
        double drop = this.legs != null && this.legs.ready() ? this.legs.drop : 0.0;
        double x = this.getX(), y = this.getY() + drop, z = this.getZ();
        this.capPart.setOldPosAndRot();
        this.capPart.setPos(x, y + 2.62 * SCALE, z);
        // underbody + hanging abdomen, a little behind the brain
        this.bodyPart.setOldPosAndRot();
        this.bodyPart.setPos(x - fx * 0.3 * SCALE, y + 1.05 * SCALE, z - fz * 0.3 * SCALE);
        // the brain hangs under the front of the brim
        this.brainPart.setOldPosAndRot();
        this.brainPart.setPos(x + fx * 0.72 * SCALE, y + 1.62 * SCALE, z + fz * 0.72 * SCALE);
        double rx = -Mth.cos(yaw), rz = -Mth.sin(yaw);
        for (int i = 0; i < 4; i++) {
            OverlordPart leg = this.legParts[i];
            leg.setOldPosAndRot();
            if (this.legs != null && this.legs.ready()) {
                Vec3 f = this.legs.foot(i);
                // the lower shin stands on the claw, leaning a little in toward the body
                leg.setPos(Mth.lerp(0.08, f.x, x), f.y, Mth.lerp(0.08, f.z, z));
            } else {
                double r = (i % 2 == 0 ? 2.45 : -2.45) * SCALE, fw = (i < 2 ? 2.65 : -2.35) * SCALE;
                leg.setPos(x + rx * r + fx * fw, this.getY(), z + rz * r + fz * fw);
            }
        }
    }

    // ---------------------------------------------------------------- body: legs carry it
    /** The main box only handles terrain; every hit lands on one of the part hitboxes instead. */
    @Override
    public boolean isPickable() {
        return false;
    }

    /** Only the legs shove things aside: you can walk in under its body. */
    @Override
    protected void pushEntities() {
        for (OverlordPart leg : this.legParts) {
            for (Entity e : this.level().getEntities(this, leg.getBoundingBox(), net.minecraft.world.entity.EntitySelector.pushableBy(this))) {
                double dx = e.getX() - leg.getX(), dz = e.getZ() - leg.getZ();
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d < 1.0E-3) {
                    dx = 1.0;
                    dz = 0.0;
                    d = 1.0;
                }
                e.push(dx / d * 0.12, 0.0, dz / d * 0.12);
            }
        }
    }

    /** The body always faces where the creature faces; turning is limited by the legs (see aiStep). */
    @Override
    protected void tickHeadTurn(float yBodyRotT) {
        this.yBodyRot = this.getYRot();
        float d = Mth.wrapDegrees(this.yHeadRot - this.yBodyRot);
        float max = this.getMaxHeadYRot();
        if (Math.abs(d) > max) this.yHeadRot = this.yBodyRot + Math.signum(d) * max;
    }

    /** Leaves in the way of its cap, body or legs are smashed. */
    private void trampleLeaves(ServerLevel level) {
        if (!net.neoforged.neoforge.event.EventHooks.canEntityGrief(level, this)) return;
        int broken = 0;
        AABB[] boxes = {this.capPart.getBoundingBox().inflate(0.15), this.bodyPart.getBoundingBox().inflate(0.15),
                this.getBoundingBox().inflate(0.2), this.legParts[0].getBoundingBox(), this.legParts[1].getBoundingBox(),
                this.legParts[2].getBoundingBox(), this.legParts[3].getBoundingBox()};
        for (AABB bb : boxes) {
            for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(bb.minX), Mth.floor(bb.minY), Mth.floor(bb.minZ),
                    Mth.floor(bb.maxX), Mth.floor(bb.maxY), Mth.floor(bb.maxZ))) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock || state.is(net.minecraft.tags.BlockTags.LEAVES)) {
                    if (level.destroyBlock(pos, true, this) && ++broken >= 16) return;
                }
            }
        }
    }

    // ---------------------------------------------------------------- vision
    /** Something it would eat. */
    boolean isPrey(LivingEntity e) {
        if (e == this || !e.isAlive() || e instanceof ArmorOverlordEntity) return false;
        if (e instanceof Player p) return !p.isCreative() && !p.isSpectator();
        if (e instanceof FlyingAnimal f && f.isFlying()) return false;
        return e instanceof Animal || e instanceof ClimbingZombieEntity;
    }

    /** Can it see (or feel) this creature right now? */
    boolean canSense(LivingEntity e) {
        double d2 = this.distanceToSqr(e);
        if (d2 < FEEL * FEEL) return true;                       // brushing against its legs
        double range = e.isDiscrete() ? SIGHT_SNEAKING : SIGHT;
        if (e.isInvisible()) range *= 0.25;
        if (d2 > range * range) return false;
        double dx = e.getX() - this.getX(), dz = e.getZ() - this.getZ();
        float yawTo = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        if (Math.abs(Mth.wrapDegrees(yawTo - this.yHeadRot)) > VIEW_HALF_ANGLE) return false;
        return this.hasLineOfSight(e);
    }

    /** Angle (degrees, 0..180) between where its body faces and the given point. */
    float angleTo(Vec3 p) {
        float yawTo = (float) (Mth.atan2(p.z - this.getZ(), p.x - this.getX()) * Mth.RAD_TO_DEG) - 90.0F;
        return Math.abs(Mth.wrapDegrees(yawTo - this.yBodyRot));
    }

    void spotted(LivingEntity target) {
        this.setTarget(target);
        this.lastSeenTick = this.tickCount;
        this.lastKnownPos = target.position();
        this.searchPos = null;
        this.searchTime = 0;
    }

    void roar() {
        this.roarTime = ROAR_TIME;
        this.level().broadcastEntityEvent(this, EVENT_ROAR);
        this.playSound(ModSounds.OVERLORD_ROAR.get(), 2.5F, 0.9F + this.random.nextFloat() * 0.15F);
    }

    /** Lost track of its prey: walk to where it was last seen and look around for a while. */
    private void loseTarget(boolean search) {
        this.setTarget(null);
        this.attack = 0;
        if (search && this.lastKnownPos != null) {
            this.searchPos = this.lastKnownPos;
            this.searchTime = 140 + this.random.nextInt(60);
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        if (!this.level().isClientSide()) {
            this.entityData.set(DATA_TARGET, target == null ? -1 : target.getId());
            this.entityData.set(DATA_HUNTING, target != null);
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.hookCooldown > 0) this.hookCooldown--;
        if (this.slashCooldown > 0) this.slashCooldown--;
        if (this.stompCooldown > 0) this.stompCooldown--;
        if (this.roarTime > 0) this.roarTime--;
        // standing still: swing round toward where it is looking (the legs limit how fast)
        if (this.getNavigation().isDone() && this.isAlive()) {
            float diff = Mth.wrapDegrees(this.yHeadRot - this.getYRot());
            float rate = this.getTarget() != null ? 5.0F : 3.0F;
            if (Math.abs(diff) > 4.0F) this.setYRot(this.getYRot() + Mth.clamp(diff, -rate, rate));
        }

        LivingEntity target = this.getTarget();
        if (target != null) {
            if (!target.isAlive() || !this.isPrey(target) || this.distanceToSqr(target) > 48 * 48) {
                this.loseTarget(false);
            } else if (this.canSense(target)) {
                this.lastSeenTick = this.tickCount;
                this.lastKnownPos = target.position();
            } else if (this.tickCount - this.lastSeenTick > (target.isDiscrete() ? 40 : 70)) {
                this.loseTarget(true);
                this.playSound(ModSounds.OVERLORD_AMBIENT.get(), 1.4F, 0.7F);
            }
        }

        // boss bar for whoever it is hunting
        target = this.getTarget();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        for (ServerPlayer p : java.util.List.copyOf(this.bossEvent.getPlayers())) {
            if (p != target || !p.isAlive()) this.bossEvent.removePlayer(p);
        }
        if (target instanceof ServerPlayer sp && !this.bossEvent.getPlayers().contains(sp)) this.bossEvent.addPlayer(sp);
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity entity, DamageSource source) {
        // a meal: it heals and calms down for a moment
        this.heal(entity instanceof Player ? 20.0F : 10.0F);
        this.playSound(ModSounds.OVERLORD_AMBIENT.get(), 1.5F, 0.6F);
        return super.killedEntity(level, entity, source);
    }

    // ---------------------------------------------------------------- damage zones
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return this.hurtZone(level, source, damage, OverlordPart.Zone.BODY);
    }

    boolean hurtZone(ServerLevel level, DamageSource source, float damage, OverlordPart.Zone zone) {
        if (this.isInvulnerableTo(level, source)) return false;
        Entity attacker = source.getEntity();
        boolean unaware = attacker != null && (this.getTarget() != attacker || this.tickCount - this.lastSeenTick > 30);
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            Vec3 from = source.getSourcePosition();
            boolean fromAbove = from != null && from.y > this.getY() + 3.0 * SCALE;
            float mul = switch (zone) {
                case CAP -> 0.15F;
                case BRAIN -> 2.0F;
                case BODY -> fromAbove ? 0.15F : 1.0F;
                case LEG -> 0.6F;                         // thick chitin
            };
            if (mul < 1.0F) {
                this.playSound(ModSounds.OVERLORD_DEFLECT.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
                Vec3 p = from != null ? from : this.position().add(0, 3.2 * SCALE, 0);
                level.sendParticles(ParticleTypes.CRIT, Mth.lerp(0.7, p.x, this.getX()), Math.max(p.y, this.getY() + 2.8 * SCALE),
                        Mth.lerp(0.7, p.z, this.getZ()), 6, 0.2, 0.1, 0.2, 0.25);
            } else if (zone == OverlordPart.Zone.BRAIN) {
                this.playSound(SoundEvents.SLIME_SQUISH, 1.2F, 0.6F);
                level.sendParticles(ParticleTypes.GLOW, this.brainPart.getX(), this.brainPart.getY() + 0.35, this.brainPart.getZ(),
                        8, 0.35, 0.25, 0.35, 0.05);
            }
            // a blow it never saw coming
            if (unaware && !source.is(DamageTypeTags.IS_PROJECTILE) && mul >= 1.0F) mul *= 1.5F;
            damage *= mul;
        }
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && attacker instanceof LivingEntity living && this.isPrey(living) && this.isAlive()) {
            // it knows where that came from - for now
            boolean wasHunting = this.getTarget() != null;
            this.spotted(living);
            if (!wasHunting) this.roar();
        }
        return hurt;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CACTUS) || source.is(DamageTypes.SWEET_BERRY_BUSH)
                || source.is(DamageTypes.FALL) || super.isInvulnerableTo(level, source);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    // ---------------------------------------------------------------- attacks (called by HuntGoal)
    void hitWithHook(ServerLevel level, LivingEntity target) {
        this.playSound(ModSounds.OVERLORD_HOOK.get(), 2.0F, 0.9F + this.random.nextFloat() * 0.2F);
        if (this.distanceTo(target) > 9.5 || this.angleTo(target.position()) > 50.0F || !this.hasLineOfSight(target)) return;
        if (target.hurtServer(level, this.damageSources().mobAttack(this), 5.0F)) {
            // drag it in toward the blades
            Vec3 front = this.position().add(Vec3.directionFromRotation(0.0F, this.yBodyRot).scale(1.9));
            Vec3 pull = front.subtract(target.position()).multiply(1, 0, 1);
            double d = pull.length();
            if (d > 0.01) {
                Vec3 v = pull.scale(Math.min(1.5, 0.25 + d * 0.22) / d).add(0, 0.38, 0);
                target.setDeltaMovement(v);
                target.hurtMarked = true;
            }
        }
    }

    void hitWithBlade(ServerLevel level, LivingEntity target) {
        this.playSound(ModSounds.OVERLORD_HOOK.get(), 1.2F, 1.3F + this.random.nextFloat() * 0.2F);
        if (this.distanceTo(target) > 5.4 || this.angleTo(target.position()) > 75.0F) return;
        float dmg = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (target.hurtServer(level, this.damageSources().mobAttack(this), dmg)) {
            Vec3 away = target.position().subtract(this.position()).multiply(1, 0, 1).normalize();
            target.knockback(0.5, -away.x, -away.z);
        }
    }

    void stompNow(ServerLevel level) {
        this.playSound(ModSounds.OVERLORD_STOMP.get(), 2.5F, 0.9F + this.random.nextFloat() * 0.1F);
        BlockState ground = level.getBlockState(this.blockPosition().below());
        if (!ground.isAir()) {
            for (int k = 0; k < 24; k++) {
                double a = k / 24.0 * Mth.TWO_PI;
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), this.getX() + Math.cos(a) * 2.6, this.getY() + 0.1,
                        this.getZ() + Math.sin(a) * 2.6, 3, 0.3, 0.05, 0.3, 0.15);
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.3, this.getZ(), 1, 0, 0, 0, 0);
        AABB area = this.getBoundingBox().inflate(3.0, 0.5, 3.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != this && e.isAlive())) {
            if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            double d = Math.sqrt(e.distanceToSqr(this.getX(), e.getY(), this.getZ()));
            if (d > 3.9 || e.getY() > this.getY() + 1.8) continue;
            if (e.hurtServer(level, this.damageSources().mobAttack(this), 7.0F)) {
                Vec3 away = e.position().subtract(this.position()).multiply(1, 0, 1);
                if (away.lengthSqr() < 1.0E-4) away = new Vec3(1, 0, 0);
                away = away.normalize();
                e.setDeltaMovement(e.getDeltaMovement().add(away.x * 1.3, 0.45, away.z * 1.3));
                e.hurtMarked = true;
            }
        }
    }

    // ---------------------------------------------------------------- client animation
    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_HOOK_WINDUP -> this.clHookWind = HOOK_WINDUP;
            case EVENT_HOOK_STRIKE -> {
                this.clHookWind = 0;
                this.clHookStrike = HOOK_TIME - HOOK_WINDUP;
            }
            case EVENT_SLASH -> this.clSlash = SLASH_TIME;
            case EVENT_STOMP -> this.clStomp = STOMP_TIME;
            case EVENT_ROAR -> this.clRoar = ROAR_TIME;
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.legs == null) this.legs = new OverlordLegs();
        this.legs.tick(this);
        if (!this.level().isClientSide()) {
            // the body can only twist a little past where its planted claws hold it:
            // to turn further it has to walk its legs round
            float support = this.legs.supportYaw(this.position(), this.getYRot());
            float twist = Mth.wrapDegrees(this.getYRot() - support);
            float delta = Mth.wrapDegrees(this.getYRot() - this.yRotO);
            // only stops it twisting FURTHER; if the claws are already ahead it simply follows them
            if (Math.abs(twist) > TWIST && Math.signum(twist) == Math.signum(delta)) {
                float limited = support + Math.signum(twist) * TWIST;
                if (Math.signum(Mth.wrapDegrees(limited - this.yRotO)) != Math.signum(delta)) limited = this.yRotO;
                this.setYRot(limited);
            }
            this.yBodyRot = this.getYRot();
        }
        this.tickParts();
        if (this.level() instanceof ServerLevel server && this.isAlive() && this.tickCount % 2 == 0) this.trampleLeaves(server);
        if (this.level().isClientSide()) {
            if (this.clHookWind > 0) this.clHookWind--;
            if (this.clHookStrike > 0) this.clHookStrike--;
            if (this.clSlash > 0) this.clSlash--;
            if (this.clStomp > 0) this.clStomp--;
            if (this.clRoar > 0) this.clRoar--;
            this.glowO = this.glow;
            float want = this.entityData.get(DATA_HUNTING) ? 1.0F : 0.35F;
            if (this.clRoar > 0 || this.clHookWind > 0) want = 1.0F;
            this.glow += (want - this.glow) * 0.08F;
            if (this.clRoar > 4 && this.tickCount % 2 == 0) {
                Vec3 b = this.brainPart.position().add(0, 0.35, 0);
                this.level().addParticle(ParticleTypes.GLOW, b.x + (this.random.nextDouble() - 0.5), b.y, b.z + (this.random.nextDouble() - 0.5),
                        0, 0.03, 0);
            }
        }
    }

    @Override
    protected void tickDeath() {
        // collapses slowly onto its folded legs instead of flipping over
        this.deathTime++;
        if (this.deathTime >= 40 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }

    int targetId() {
        return this.entityData.get(DATA_TARGET);
    }

    boolean isHunting() {
        return this.entityData.get(DATA_HUNTING);
    }

    @Nullable OverlordLegs legs() {
        return this.legs;
    }

    Vec3 brainPos() {
        return this.brainPart.position();
    }

    // ---------------------------------------------------------------- misc
    @Override
    public int getMaxHeadYRot() {
        return 20;
    }

    @Override
    public int getHeadRotSpeed() {
        return this.getTarget() != null ? 6 : 4;
    }

    @Override
    public int getMaxHeadXRot() {
        return 25;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        this.bossEvent.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSounds.OVERLORD_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.OVERLORD_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.OVERLORD_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // every claw plays its own footfall (client side, OverlordLegs)
    }

    @Override
    protected float getSoundVolume() {
        return 1.6F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }
}
