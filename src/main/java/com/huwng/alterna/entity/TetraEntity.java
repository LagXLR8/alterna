package com.huwng.alterna.entity;

import com.huwng.alterna.block.ModBlocks;
import com.huwng.alterna.block.TetraNestBlock;
import com.huwng.alterna.item.ModItems;
import com.huwng.alterna.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Tetra - a four-winged flier of the Lichen Savanna.
 *
 * Flight model: the Tetra is either FLYING or GROUNDED (or clinging to a cliff), never
 * "both". It takes off with a jump and a burst of flaps, flies with momentum (it banks
 * through turns, pitches into climbs and dives, and glides between wing beats), and
 * only lands when it deliberately chose a landing spot. After landing it stays down
 * for a while before it is allowed to take off again, so it never flickers between
 * walking and flying near the ground.
 *
 * - Flies in flocks; every so often lands and clings to a vertical cliff face to
 *   look around, then takes off again.
 * - Nests on top of cliff ledges and lays eggs (feed two adults raw fish to breed:
 *   one of them flies off and lays an egg in a nest, building one if needed).
 * - Hunts fish: dives on fish swimming in ponds, and snaps up dropped fish.
 * - Never attacks anything on its own... unless you touch its nest or eggs, then the
 *   whole colony dives at you.
 * - Babies can be tamed with raw fish; a baby hatched from a nest you tended imprints
 *   on you and comes out tame. Tame Tetras follow you and help you fight.
 */
public class TetraEntity extends TamableAnimal implements FlyingAnimal {
    private static final EntityDataAccessor<Boolean> DATA_PERCHED = SynchedEntityData.defineId(TetraEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_PERCH_YAW = SynchedEntityData.defineId(TetraEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_DIVING = SynchedEntityData.defineId(TetraEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FLYING = SynchedEntityData.defineId(TetraEntity.class, EntityDataSerializers.BOOLEAN);
    public static final int BERRY_NONE = 0, BERRY_SWEET = 1, BERRY_WHITE_CURRANT = 2;

    /** Air drag per tick while flying: high = long glides and wide, swooping turns. */
    static final double AIR_DRAG = 0.86;

    @Nullable BlockPos homeNest;
    boolean hasEgg;
    int fishCooldown;
    int perchCooldown;
    int angerTicks;
    @Nullable Vec3 perchAnchor;

    // ---- flight state (server)
    /** Ticks since take-off. */
    int timeFlying;
    /** Ticks it still wants to stay on the ground after landing. */
    int restTime;
    /** Where it is coming down to land, or null when just flying around. */
    @Nullable Vec3 landTarget;
    /** Set by the move control when it had to escape from being stuck; goals pick a new route. */
    boolean flightStuck;
    int forageCooldown;
    private int heldBerryTicks;
    int hopCooldown;
    /** Current leader of its flock (may be itself). */
    @Nullable TetraEntity flockLeader;
    private final PathNavigation groundNavigation;
    private final PathNavigation flightNavigation;

    // ---- animation (client)
    private float roll, roll0;
    private float pitch, pitch0;
    private float flap = 1.0F, flap0 = 1.0F;
    private float flapPhase, flapPhase0;
    // procedural extras (client): tail/neck swing that lags behind turns, ground slope pitch
    private float tailSwing, tailSwing0, tailSwingVel, tailLift, tailLift0, groundPitch, groundPitch0;

    public TetraEntity(EntityType<? extends TetraEntity> type, Level level) {
        super(type, level);
        this.moveControl = new TetraGoals.TetraMoveControl(this);
        this.lookControl = new TetraLookControl(this);
        this.groundNavigation = this.navigation;
        FlyingPathNavigation flight = new FlyingPathNavigation(this, level);
        flight.setCanOpenDoors(false);
        flight.setCanFloat(true);
        this.flightNavigation = flight;
        this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, -1.0F);
        this.setPathfindingMalus(PathType.FIRE, -1.0F);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 16.0F);
        this.fishCooldown = this.random.nextInt(1200);
        this.perchCooldown = 200 + this.random.nextInt(600);
        this.restTime = 100 + this.random.nextInt(200);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                // thrust per tick; with AIR_DRAG this cruises at ~8 blocks/s and dives at ~16
                .add(Attributes.FLYING_SPEED, 0.065)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    public static boolean checkTetraSpawnRules(EntityType<TetraEntity> type, LevelAccessor level, EntitySpawnReason reason,
            BlockPos pos, RandomSource random) {
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.below()).is(ModBlocks.LICHEN_TURF.get());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PERCHED, false);
        builder.define(DATA_PERCH_YAW, 0.0F);
        builder.define(DATA_DIVING, false);
        builder.define(DATA_FLYING, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        GroundPathNavigation nav = new GroundPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new TetraGoals.EscapeWaterGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new TetraGoals.SwoopAttackGoal(this));
        this.goalSelector.addGoal(3, new TetraGoals.LayEggGoal(this));
        this.goalSelector.addGoal(4, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(5, new TetraGoals.FollowOwnerFlightGoal(this));
        this.goalSelector.addGoal(6, new TetraGoals.HuntFishGoal(this));
        this.goalSelector.addGoal(6, new TetraGoals.ForageBerryGoal(this));
        this.goalSelector.addGoal(7, new TetraGoals.PerchOnCliffGoal(this));
        this.goalSelector.addGoal(7, new TetraGoals.LandGoal(this));
        this.goalSelector.addGoal(8, new TetraGoals.FlockFlightGoal(this));
        this.goalSelector.addGoal(8, new TetraGoals.TakeOffGoal(this));
        this.goalSelector.addGoal(9, new TetraGoals.GroundStrollGoal(this));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(11, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    // ------------------------------------------------------------------ state
    public boolean isPerched() {
        return this.entityData.get(DATA_PERCHED);
    }

    public float getPerchYaw() {
        return this.entityData.get(DATA_PERCH_YAW);
    }

    public boolean isDiving() {
        return this.entityData.get(DATA_DIVING);
    }

    void setDiving(boolean diving) {
        this.entityData.set(DATA_DIVING, diving);
    }

    @Override
    public boolean isFlying() {
        return this.entityData.get(DATA_FLYING);
    }

    private void setFlyingState(boolean flying) {
        if (flying == this.isFlying()) return;
        this.entityData.set(DATA_FLYING, flying);
        PathNavigation old = this.navigation;
        old.stop();
        this.navigation = flying ? this.flightNavigation : this.groundNavigation;
        this.timeFlying = 0;
        this.landTarget = null;
        this.setNoGravity(flying);
    }

    boolean canTakeOff() {
        return !this.isPerched() && !this.isOrderedToSit() && !this.isPassenger() && !this.isLeashed();
    }

    /** Jump into the air with a burst of wing beats. */
    void takeOff() {
        if (this.isFlying() || !this.canTakeOff()) return;
        this.setFlyingState(true);
        Vec3 fwd = Vec3.directionFromRotation(0.0F, this.getYRot());
        this.setDeltaMovement(this.getDeltaMovement().add(fwd.x * 0.15, 0.42, fwd.z * 0.15));
        this.playSound(ModSounds.TETRA_FLAP.get(), 0.7F, 1.4F + this.random.nextFloat() * 0.2F);
    }

    /** Touch down; it will then stay on the ground for {@code rest} ticks. */
    void land(int rest) {
        this.setFlyingState(false);
        this.setDiving(false);
        this.restTime = rest;
        Vec3 v = this.getDeltaMovement();
        this.setDeltaMovement(v.x * 0.3, Math.min(0.0, v.y), v.z * 0.3);
    }

    public void setHomeNest(@Nullable BlockPos pos) {
        this.homeNest = pos;
    }

    /** Cling to the cliff face at {@code wall} (the solid block) from the air block {@code at}. */
    void perch(BlockPos at, Direction towardWall) {
        this.setFlyingState(false);
        this.setNoGravity(true);
        // close enough that the belly of the upright body rests on the rock
        // the hitbox (1.1 wide) must stay fully out of the rock, so the entity sits slightly
        // back from the wall; the renderer slides the model the rest of the way onto the face
        Vec3 anchor = Vec3.atCenterOf(at).add(towardWall.getStepX() * -0.06, -0.3, towardWall.getStepZ() * -0.06);
        this.perchAnchor = anchor;
        this.setPos(anchor);
        this.setDeltaMovement(Vec3.ZERO);
        this.getNavigation().stop();
        float yaw = towardWall.toYRot();
        this.entityData.set(DATA_PERCH_YAW, yaw);
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.entityData.set(DATA_PERCHED, true);
    }

    void unperch() {
        if (!this.isPerched()) return;
        this.entityData.set(DATA_PERCHED, false);
        this.perchAnchor = null;
        this.perchCooldown = 600 + this.random.nextInt(900);
        // kick off the wall backwards, spin around and fly away
        float yaw = this.getPerchYaw() + 180.0F;
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setFlyingState(true);
        Vec3 out = Vec3.directionFromRotation(0.0F, yaw).scale(0.5);
        this.setDeltaMovement(out.x, 0.3, out.z);
        this.playSound(ModSounds.TETRA_FLAP.get(), 0.6F, 1.5F);
    }

    // ------------------------------------------------------------------ ticking
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.tickFlightAnimation();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.isPerched()) {
            this.setDeltaMovement(Vec3.ZERO);
            if (this.perchAnchor != null) this.setPos(this.perchAnchor);
            float yaw = this.getPerchYaw();
            this.setYBodyRot(yaw);
            this.setYRot(yaw);
            this.setNoGravity(true);
        } else {
            this.setNoGravity(this.isFlying() && !this.isInWater());
        }
        if (!this.level().isClientSide() && this.tickCount % 10 == 0) {
            this.snapUpDroppedFish();
        }
        if (!this.level().isClientSide()) this.tickGroundHop();
        if (this.level().isClientSide() && this.isFlying() && this.flap > 0.6F && this.tickCount % 12 == 0 && this.random.nextInt(3) == 0) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), ModSounds.TETRA_FLAP.get(), this.getSoundSource(),
                    0.3F, 1.5F + this.random.nextFloat() * 0.3F, false);
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.fishCooldown > 0) this.fishCooldown--;
        if (this.perchCooldown > 0) this.perchCooldown--;
        if (this.forageCooldown > 0) this.forageCooldown--;
        this.tickHeldBerry(level);
        if (this.isFlying()) {
            this.timeFlying++;
            // only come down when it actually decided to land; brushing the ground does nothing
            if (this.timeFlying > 10 && this.onGround()) {
                if (this.landTarget != null || this.timeFlying > 1200 && this.random.nextInt(40) == 0) {
                    this.land(300 + this.random.nextInt(500));
                }
            }
        } else if (!this.isPerched() && this.restTime > 0) {
            this.restTime--;
        }
        if (this.angerTicks > 0 && --this.angerTicks == 0 && this.getTarget() instanceof Player) {
            this.setTarget(null);
        }
        LivingEntity target = this.getTarget();
        if (target != null && (!target.isAlive() || this.distanceToSqr(target) > 48 * 48)) {
            this.setTarget(null);
        }
    }

    /**
     * Walking into a ledge too tall to step up: hop up it with a couple of wing beats
     * (about 2 blocks) instead of standing there.
     */
    private void tickGroundHop() {
        if (this.hopCooldown > 0) {
            this.hopCooldown--;
            return;
        }
        if (this.isFlying() || this.isPerched() || !this.onGround() || !this.horizontalCollision || this.isOrderedToSit()) return;
        if (this.getNavigation().isDone() && !this.getMoveControl().hasWanted()) return;
        Vec3 fwd = Vec3.directionFromRotation(0.0F, this.getYRot());
        this.setDeltaMovement(fwd.x * 0.18, 0.62, fwd.z * 0.18);
        this.hopCooldown = 15;
        this.playSound(ModSounds.TETRA_FLAP.get(), 0.4F, 1.6F);
    }

    /** Flight with momentum: thrust along the heading, then air drag. */
    @Override
    public void travel(Vec3 input) {
        if (!this.isFlying() || this.isInWater() || this.isInLava()) {
            super.travel(input);
            return;
        }
        this.moveRelative(this.getSpeed(), input);
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(AIR_DRAG));
        this.calculateEntityAnimation(false);
    }

    private void tickFlightAnimation() {
        this.roll0 = this.roll;
        this.pitch0 = this.pitch;
        this.flap0 = this.flap;
        if (this.flapPhase > 1000.0F) {
            // keep the float precise; shift both by a whole number of beats
            float wrap = Mth.TWO_PI * 150.0F;
            this.flapPhase -= wrap;
            this.flapPhase0 -= wrap;
        }
        this.flapPhase0 = this.flapPhase;
        this.flapPhase += 0.30F + 0.20F * this.flap;
        this.tickSecondaryMotion();
        if (!this.isFlying()) {
            this.roll *= 0.6F;
            this.pitch *= 0.6F;
            this.flap = 1.0F;
            return;
        }
        // bank into turns
        float dYaw = Mth.wrapDegrees(this.getYRot() - this.yRotO);
        float targetRoll = Mth.clamp(dYaw * 5.0F, -60.0F, 60.0F);
        this.roll += Mth.clamp(targetRoll - this.roll, -7.0F, 7.0F);
        // nose follows the flight path
        double dx = this.getX() - this.xo, dy = this.getY() - this.yo, dz = this.getZ() - this.zo;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float targetPitch = horiz + Math.abs(dy) < 0.02 ? 0.0F : (float) (-Mth.atan2(dy, horiz) * Mth.RAD_TO_DEG);
        targetPitch = Mth.clamp(targetPitch, -50.0F, 70.0F);
        this.pitch += (targetPitch - this.pitch) * 0.25F;
        // beat the wings when climbing or slow, glide when level and fast, tuck them in a dive
        float targetFlap;
        if (this.isDiving() || dy < -0.25) targetFlap = 0.08F;
        else if (dy > 0.03 || horiz < 0.2) targetFlap = 1.0F;
        else targetFlap = Mth.sin((this.tickCount + this.getId() * 37) * 0.045F) > 0.25F ? 0.22F : 0.85F;
        this.flap += (targetFlap - this.flap) * 0.12F;
    }

    /**
     * Small procedural touches: the tail is a damped spring that swings out opposite to a turn
     * and lifts/droops with climbing/sinking, and on the ground the body follows the slope
     * under its front and hind wings.
     */
    private void tickSecondaryMotion() {
        this.tailSwing0 = this.tailSwing;
        this.tailLift0 = this.tailLift;
        this.groundPitch0 = this.groundPitch;
        float dYaw = Mth.wrapDegrees(this.yBodyRot - this.yBodyRotO) * Mth.DEG_TO_RAD;
        // spring toward "trail behind the turn", with damping
        float target = Mth.clamp(-dYaw * 3.0F, -0.7F, 0.7F);
        this.tailSwingVel += (target - this.tailSwing) * 0.25F;
        this.tailSwingVel *= 0.7F;
        this.tailSwing += this.tailSwingVel;
        double vy = this.getY() - this.yo;
        this.tailLift += ((float) Mth.clamp(-vy * 2.5, -0.5, 0.5) - this.tailLift) * 0.2F;
        float slope = 0.0F;
        if (!this.isFlying() && !this.isPerched() && this.onGround()) {
            Vec3 fwd = Vec3.directionFromRotation(0.0F, this.yBodyRot).scale(0.45);
            double front = CragbackLegs.groundY(this.level(), this.getX() + fwd.x, this.getY() + 0.6, this.getZ() + fwd.z, this.getY());
            double back = CragbackLegs.groundY(this.level(), this.getX() - fwd.x, this.getY() + 0.6, this.getZ() - fwd.z, this.getY());
            slope = (float) Mth.clamp(Math.atan2(front - back, 0.9), -0.6, 0.6);
        }
        this.groundPitch += (slope - this.groundPitch) * 0.3F;
    }

    public float getTailSwing(float partial) {
        return Mth.lerp(partial, this.tailSwing0, this.tailSwing);
    }

    public float getTailLift(float partial) {
        return Mth.lerp(partial, this.tailLift0, this.tailLift);
    }

    /** Nose-up angle (radians) of the ground under it while walking. */
    public float getGroundPitch(float partial) {
        return Mth.lerp(partial, this.groundPitch0, this.groundPitch);
    }

    public float getRoll(float partial) {
        return Mth.lerp(partial, this.roll0, this.roll);
    }

    public float getFlightPitch(float partial) {
        return Mth.lerp(partial, this.pitch0, this.pitch);
    }

    public float getFlapPhase(float partial) {
        return Mth.lerp(partial, this.flapPhase0, this.flapPhase);
    }

    public float getFlap(float partial) {
        return Mth.lerp(partial, this.flap0, this.flap);
    }

    private void snapUpDroppedFish() {
        if (this.fishCooldown > 0 || this.isTame() && this.isOrderedToSit()) return;
        List<ItemEntity> items = this.level().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(1.5),
                e -> e.getItem().is(ItemTags.FISHES));
        if (items.isEmpty()) return;
        ItemEntity item = items.get(0);
        item.getItem().shrink(1);
        if (item.getItem().isEmpty()) item.discard();
        this.eatFish();
    }

    void eatFish() {
        this.heal(6.0F);
        this.fishCooldown = 1600 + this.random.nextInt(1600);
        this.playSound(SoundEvents.GENERIC_EAT.value(), 0.8F, 1.3F);
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 0.5, this.getZ(), 3, 0.3, 0.3, 0.3, 0.0);
        }
        if (this.isBaby()) this.ageUp(120);
    }

    // ------------------------------------------------------------------ berries
    // The berry lives in the main-hand slot: it is synced to clients automatically and the
    // renderer draws the real item model in the beak (like a fox carrying something).
    public int getHeldBerry() {
        ItemStack held = this.getMainHandItem();
        if (held.is(Items.SWEET_BERRIES)) return BERRY_SWEET;
        if (held.is(ModItems.WHITE_CURRANT_BERRIES.get())) return BERRY_WHITE_CURRANT;
        return BERRY_NONE;
    }

    boolean isHoldingBerry() {
        return !this.getMainHandItem().isEmpty();
    }

    void holdBerry(int berry) {
        ItemStack stack = switch (berry) {
            case BERRY_SWEET -> new ItemStack(Items.SWEET_BERRIES);
            case BERRY_WHITE_CURRANT -> new ItemStack(ModItems.WHITE_CURRANT_BERRIES.get());
            default -> ItemStack.EMPTY;
        };
        this.setItemSlot(EquipmentSlot.MAINHAND, stack);
        this.setGuaranteedDrop(EquipmentSlot.MAINHAND);   // dropped if it dies while carrying
        this.heldBerryTicks = 0;
    }

    private ItemStack heldBerryStack() {
        return this.getMainHandItem().copy();
    }

    /** Lets go of the berry: it falls to the ground as an item. */
    void dropBerry() {
        if (!this.isHoldingBerry()) return;
        ItemStack stack = this.heldBerryStack();
        this.holdBerry(BERRY_NONE);
        if (this.level() instanceof ServerLevel server) {
            Vec3 beak = this.position().add(Vec3.directionFromRotation(0.0F, this.getYRot()).scale(0.8)).add(0, 0.4, 0);
            ItemEntity item = new ItemEntity(server, beak.x, beak.y, beak.z, stack);
            item.setDeltaMovement(this.getDeltaMovement().scale(0.5));
            item.setPickUpDelay(20);
            server.addFreshEntity(item);
        }
    }

    /** Carried berries are eaten later once it is settled, or now and then dropped in flight. */
    private void tickHeldBerry(ServerLevel level) {
        if (!this.isHoldingBerry()) return;
        this.heldBerryTicks++;
        boolean settled = !this.isFlying() || this.isPerched();
        if (this.isFlying() && !this.isPerched() && this.heldBerryTicks > 60 && this.random.nextInt(900) == 0) {
            this.dropBerry();
            return;
        }
        if (settled && this.heldBerryTicks > 80 && this.random.nextInt(160) == 0) {
            ItemStack stack = this.heldBerryStack();
            this.holdBerry(BERRY_NONE);
            this.heal(2.0F);
            this.playSound(SoundEvents.GENERIC_EAT.value(), 0.6F, 1.4F);
            Vec3 beak = this.position().add(Vec3.directionFromRotation(0.0F, this.getYRot()).scale(0.7)).add(0, 0.45, 0);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack.getItem()), beak.x, beak.y, beak.z, 6, 0.1, 0.1, 0.1, 0.05);
            if (this.isBaby()) this.ageUp(40);
        }
    }

    /** Thorny berry bushes don't bother it: no slowdown and no prickle damage. */
    @Override
    public void makeStuckInBlock(BlockState state, Vec3 speedMultiplier) {
        if (state.getBlock() instanceof SweetBerryBushBlock) return;
        super.makeStuckInBlock(state, speedMultiplier);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypes.SWEET_BERRY_BUSH)) return false;
        // clinging to a cliff never suffocates it, whatever the collision box does
        if (this.isPerched() && source.is(DamageTypes.IN_WALL)) return false;
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt) {
            this.dropBerry();
            this.unperch();
            this.setOrderedToSit(false);
            this.takeOff();
        }
        return hurt;
    }

    // ------------------------------------------------------------------ territory
    /** Someone messed with a nest: every wild adult Tetra nearby turns on them. */
    public static void alertTetras(ServerLevel level, BlockPos nest, Player player) {
        if (player.isCreative() || player.isSpectator()) return;
        AABB area = new AABB(nest).inflate(32.0, 24.0, 32.0);
        for (TetraEntity tetra : level.getEntitiesOfClass(TetraEntity.class, area, t -> !t.isTame() && !t.isBaby() && t.isAlive())) {
            tetra.unperch();
            tetra.takeOff();
            tetra.setTarget(player);
            tetra.angerTicks = 600 + tetra.random.nextInt(200);
            tetra.playSound(SoundEvents.PHANTOM_SWOOP, 1.0F, 1.6F);
        }
    }

    // ------------------------------------------------------------------ taming & breeding
    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ItemTags.FISHES);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!this.isTame() && this.isBaby() && this.isFood(stack)) {
            if (!this.level().isClientSide()) {
                stack.consume(1, player);
                this.eatFish();
                if (this.random.nextInt(3) == 0) {
                    this.tame(player);
                    this.navigation.stop();
                    this.level().broadcastEntityEvent(this, (byte) 7);
                } else {
                    this.level().broadcastEntityEvent(this, (byte) 6);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (this.isTame() && this.isOwnedBy(player) && !this.isFood(stack)) {
            if (!this.level().isClientSide()) {
                boolean sit = !this.isOrderedToSit();
                this.setOrderedToSit(sit);
                this.setInSittingPose(sit && !this.isFlying());
                this.navigation.stop();
                this.setTarget(null);
            }
            return InteractionResult.SUCCESS;
        }
        if (this.isTame() && this.isFood(stack) && this.getHealth() < this.getMaxHealth()) {
            if (!this.level().isClientSide()) {
                stack.consume(1, player);
                this.eatFish();
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /** Like turtles, breeding gives one parent an egg to lay instead of a live baby. */
    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal partner) {
        this.finalizeSpawnChildFromBreeding(level, partner, null);
        this.hasEgg = true;
        this.resetLove();
        partner.resetLove();
        this.playSound(ModSounds.TETRA_AMBIENT.get(), 1.0F, 0.9F);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.TETRA.get().create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    public boolean canMate(Animal partner) {
        return partner != this && partner instanceof TetraEntity other && this.isInLove() && other.isInLove()
                && !this.hasEgg && !other.hasEgg;
    }

    boolean isUsableNest(BlockPos pos) {
        BlockState state = this.level().getBlockState(pos);
        return state.is(ModBlocks.TETRA_NEST.get()) && state.getValue(TetraNestBlock.EGGS) < 3;
    }

    @Override
    protected boolean canFlyToOwner() {
        return true;
    }

    // ------------------------------------------------------------------ physics
    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean isPushable() {
        return !this.isPerched() && super.isPushable();
    }

    // ------------------------------------------------------------------ save
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("has_egg", this.hasEgg);
        output.putInt("fish_cooldown", this.fishCooldown);
        output.putBoolean("flying", this.isFlying());
        output.putInt("rest_time", this.restTime);
        output.storeNullable("home_nest", BlockPos.CODEC, this.homeNest);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.hasEgg = input.getBooleanOr("has_egg", false);
        this.fishCooldown = input.getIntOr("fish_cooldown", 0);
        this.setFlyingState(input.getBooleanOr("flying", false));
        this.restTime = input.getIntOr("rest_time", 0);
        this.homeNest = input.read("home_nest", BlockPos.CODEC).orElse(null);
    }

    // ------------------------------------------------------------------ sounds
    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSounds.TETRA_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.TETRA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.TETRA_DEATH.get();
    }

    @Override
    public float getVoicePitch() {
        return (this.isBaby() ? 1.35F : 1.0F) + (this.random.nextFloat() - this.random.nextFloat()) * 0.08F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.75F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    /** Lets the head look around freely without fighting the body's flight direction. */
    static class TetraLookControl extends LookControl {
        TetraLookControl(TetraEntity mob) {
            super(mob);
        }

        @Override
        protected void clampHeadRotationToBody() {
            this.mob.yHeadRot = Mth.rotateIfNecessary(this.mob.yHeadRot, this.mob.yBodyRot, 75.0F);
        }
    }
}
