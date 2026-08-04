package com.huwng.alterna.vine;

import com.huwng.alterna.vine.duck.VinePlayerDuck;
import java.util.Collection;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Core logic for vine sliding — handles attach, tick, detach, momentum.
 * Ported from Zipline mod (MIT) — credit: Evan, Tomate0613.
 */
public class VineLogic {

    private static final double ATTACH_THRESHOLD_PADDING = 1.01;

    public static void inventoryTick(LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            VinePlayerDuck duck = (VinePlayerDuck) player;
            if (duck.vine$isActuallyUsing() && !player.isUsingItem()) {
                interruptUsing(player, duck);
            }
        }
    }

    public static void tick(Level level, LivingEntity livingEntity, ItemStack stack) {
        if (livingEntity instanceof Player player) {
            if (!level.isClientSide()) {
                // Server-side: no durability consumption for stick/empty hand
            } else if (player.isLocalPlayer()) {
                VinePlayerDuck duck = (VinePlayerDuck) player;
                if (!duck.vine$isActuallyUsing()) {
                    attemptAttach(player, duck);
                } else {
                    vineTick(player, duck, stack);
                }
            }
        }
    }

    private static void attemptAttach(Player player, VinePlayerDuck duck) {
        if (!player.onGround()) {
            Vec3 playerPos = player.position();
            Vec3 offsetPlayerPos = playerPos.add(0.0, VineConfig.get().hangOffset, 0.0);
            VineCable cable = VineCables.getClosestCable(player.level(), offsetPlayerPos, VineConfig.get().snapRadius);
            if (cable != null && cable.isValid()) {
                Vec3 closestPoint = cable.getClosestPoint(offsetPlayerPos);
                Vec3 playerAttachPos = closestPoint.add(0.0, -VineConfig.get().hangOffset, 0.0);
                if (closestPoint.y > playerPos.y + ATTACH_THRESHOLD_PADDING * VineConfig.get().hangOffset
                        && !isInvalidPosition(player, playerAttachPos.subtract(playerPos))) {
                    enable(player, duck, cable, offsetPlayerPos);
                }
            }
        }
    }

    private static void enable(Player player, VinePlayerDuck duck, VineCable cable, Vec3 offsetPlayerPos) {
        duck.vine$setActuallyUsing(true);
        duck.vine$setCable(cable);
        double initialSpeed = Math.min(player.getDeltaMovement().length(), 0.5);
        duck.vine$setSpeed(initialSpeed);
        double progress = cable.getProgress(offsetPlayerPos);
        duck.vine$setProgress(progress);
        int dirFactor = player.getLookAngle().dot(cable.direction(progress)) >= 0.0 ? 1 : -1;
        duck.vine$setDirectionFactor(dirFactor);

        double futureT = progress + (double) dirFactor * 0.1 / cable.length();
        Vec3 delta = cable.getPoint(futureT).subtract(offsetPlayerPos);
        float rawYaw = (float) (Mth.atan2(delta.z, delta.x) * (180.0 / Math.PI) - (double) player.getYRot());
        float clampedYaw = Mth.clamp(Mth.wrapDegrees(rawYaw), -15.0F, 15.0F) * 0.3F;
        VineClient.vineTilt(clampedYaw);

        player.playSound(VineSoundEvents.VINE_ATTACH.get(), 0.6F, 1.0F);
    }

    private static void vineTick(Player player, VinePlayerDuck duck, ItemStack stack) {
        if (player.onGround()) {
            interruptUsing(player, duck);
            return;
        }
        if (stack.isEmpty()) {
            interruptUsing(player, duck);
            return;
        }

        VineCable cable = duck.vine$getCable();
        if (cable == null || !cable.isValid()) {
            interruptUsing(player, duck);
            return;
        }

        double oldProgress = duck.vine$getProgress();
        double velocity = duck.vine$getSpeed() * (double) duck.vine$getDirectionFactor();

        if (VineConfig.get().realisticPhysics) {
            double deltaT = 0.1 / Math.max(1.0, cable.length());
            double tForward = Math.min(1.0, oldProgress + deltaT);
            double tBackward = Math.max(0.0, oldProgress - deltaT);
            Vec3 pForward = cable.getPoint(tForward);
            Vec3 pBackward = cable.getPoint(tBackward);
            Vec3 tangent = pForward.subtract(pBackward).normalize();
            double gravityAccel = 0.04;
            double acceleration = -gravityAccel * tangent.y;
            velocity += acceleration;
            velocity *= 0.98;
            if (Math.abs(velocity) < 0.01 && Math.abs(tangent.y) < 0.1) {
                velocity = 0.0;
            }
        } else {
            int intendedDir = player.getLookAngle().dot(cable.direction(oldProgress)) >= 0.0 ? 1 : -1;
            velocity = Mth.lerp(0.05, velocity, 1.6 * (double) intendedDir);
        }

        duck.vine$setSpeed(Math.abs(velocity));
        duck.vine$setDirectionFactor(velocity >= 0.0 ? 1 : -1);

        double moveDelta = velocity * VineConfig.get().speedMultiplier / cable.length();
        double newProgress = oldProgress + moveDelta;
        newProgress = Mth.clamp(newProgress, 0.0, 1.0);
        duck.vine$setProgress(newProgress);

        Vec3 newPosition = cable.getPoint(newProgress);
        Vec3 newOffsetPosition = new Vec3(newPosition.x, newPosition.y - VineConfig.get().hangOffset, newPosition.z);
        Vec3 oldPosition = cable.getPoint(oldProgress);
        Vec3 lastDir = newPosition.subtract(oldPosition);
        duck.vine$setLastDir(lastDir);

        if (isInvalidPosition(player, lastDir)) {
            duck.vine$setSpeed(0.0);
            duck.vine$setProgress(oldProgress);
            newProgress = oldProgress;
            newOffsetPosition = new Vec3(oldPosition.x, oldPosition.y - VineConfig.get().hangOffset, oldPosition.z);
        }

        player.setPos(newOffsetPosition);
        player.setDeltaMovement(0.0, 0.0, 0.0);
        player.fallDistance = 0.0;
        player.playSound(VineSoundEvents.VINE_USE.get(), 1.0F, 0.3F + (float) duck.vine$getSpeed());

        if (newProgress >= 1.0 || newProgress <= 0.0) {
            handleCableSwitch(player, duck, cable, velocity >= 0.0 ? 1 : -1, lastDir);
        }
    }

    private static void handleCableSwitch(Player player, VinePlayerDuck duck, VineCable currentCable, int dirFactor, Vec3 lastDir) {
        Collection<VineCable> nextCables = currentCable.getNext(dirFactor == 1);
        Vec3 playerDir = player.getLookAngle();
        Vec3 movementDir = lastDir.normalize();
        double highestDotProduct = -1.0;
        VineCable nextCable = null;

        for (VineCable next : nextCables) {
            if (!currentCable.equals(next)) {
                double startAlignment = next.direction(0.0).dot(movementDir);
                double endAlignment = next.direction(1.0).dot(movementDir.scale(-1.0));
                double bestAlignment = Math.max(startAlignment, endAlignment);
                if (bestAlignment > VineConfig.get().maxTurnAngle) {
                    double lookDotProduct = next.direction(0.0).dot(playerDir);
                    if (lookDotProduct > highestDotProduct) {
                        highestDotProduct = lookDotProduct;
                        nextCable = next;
                    }
                }
            }
        }

        if (nextCable == null) {
            interruptUsing(player, duck);
        } else {
            Vec3 exitPos = currentCable.getPoint(dirFactor == 1 ? 1.0 : 0.0);
            double distToStart = exitPos.distanceToSqr(nextCable.getPoint(0.0));
            double distToEnd = exitPos.distanceToSqr(nextCable.getPoint(1.0));
            boolean startAtBeginning = distToStart <= distToEnd;
            duck.vine$setCable(nextCable);
            duck.vine$setProgress(startAtBeginning ? 0.0 : 1.0);
            int newDir = nextCable.direction(startAtBeginning ? 0.0 : 1.0).dot(movementDir) >= 0.0 ? 1 : -1;
            duck.vine$setDirectionFactor(newDir);
        }
    }

    private static void interruptUsing(Player player, VinePlayerDuck duck) {
        disable(duck);
        player.stopUsingItem();
        applyExitMomentum(player, duck);
        player.playSound(VineSoundEvents.VINE_INTERRUPT.get(), 0.5F, 1.0F);
    }

    public static void disable(VinePlayerDuck duck) {
        duck.vine$setCable(null);
        duck.vine$setActuallyUsing(false);
        duck.vine$setSpeed(0.0);
    }

    public static void release(Player player, ItemStack stack) {
        VinePlayerDuck duck = (VinePlayerDuck) player;
        player.getCooldowns().addCooldown(stack.getItem().getDefaultInstance(), VineConfig.get().releaseCooldown);
        if (duck.vine$isActuallyUsing()) {
            if (!player.isShiftKeyDown()) {
                double jumpY = 0.5 * VineConfig.get().exitJumpMultiplier;
                player.addDeltaMovement(new Vec3(0.0, jumpY, 0.0));
            }
            applyExitMomentum(player, duck);
            disable(duck);
        }
    }

    private static void applyExitMomentum(LivingEntity livingEntity, VinePlayerDuck duck) {
        Vec3 lastDir = duck.vine$getLastDir();
        if (lastDir != null) {
            livingEntity.addDeltaMovement(lastDir.scale(0.5));
        }
        livingEntity.addDeltaMovement(livingEntity.getLookAngle().with(Axis.Y, 0.0).scale(0.5));
    }

    private static boolean isInvalidPosition(Player player, Vec3 deltaPos) {
        AABB collisionBox = player.getBoundingBox().move(deltaPos);
        for (VoxelShape shape : player.level().getBlockCollisions(player, collisionBox)) {
            if (!shape.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
