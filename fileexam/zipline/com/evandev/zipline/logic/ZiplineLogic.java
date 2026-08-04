package com.evandev.zipline.logic;

import com.evandev.zipline.Cable;
import com.evandev.zipline.Cables;
import com.evandev.zipline.client.ZiplineClient;
import com.evandev.zipline.config.ModConfig;
import com.evandev.zipline.duck.ZiplinePlayerDuck;
import com.evandev.zipline.registry.ZiplineSoundEvents;
import java.util.Collection;
import net.minecraft.core.Direction.Axis;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ZiplineLogic {
   private static final double ATTACH_THRESHOLD_PADDING = 1.01;

   public static void inventoryTick(LivingEntity livingEntity) {
      if (livingEntity instanceof Player player) {
         ZiplinePlayerDuck duck = (ZiplinePlayerDuck)player;
         if (duck.zipline$isActuallyUsing() && !player.isUsingItem()) {
            interruptUsing(player, duck);
         }

      }
   }

   public static void tick(Level level, LivingEntity livingEntity, ItemStack stack) {
      if (livingEntity instanceof Player player) {
         if (!level.isClientSide()) {
            if (ModConfig.get().consumeDurability && player.tickCount % 40 == 0) {
               Vec3 offsetPlayerPos = player.position().add((double)0.0F, ModConfig.get().hangOffset, (double)0.0F);
               Cable cable = Cables.getClosestCable(level, offsetPlayerPos, ModConfig.get().snapRadius);
               if (cable != null) {
                  Vec3 closestPoint = cable.getClosestPoint(offsetPlayerPos);
                  if (closestPoint.distanceToSqr(offsetPlayerPos) < (double)0.25F) {
                     EquipmentSlot slot = player.getOffhandItem() == stack ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
                     stack.hurtAndBreak(1, player, slot);
                  }
               }
            }

         } else if (player.isLocalPlayer()) {
            ZiplinePlayerDuck duck = (ZiplinePlayerDuck)player;
            if (!duck.zipline$isActuallyUsing()) {
               attemptAttach(player, duck);
            } else {
               ziplineTick(player, duck, stack);
            }

         }
      }
   }

   private static void attemptAttach(Player player, ZiplinePlayerDuck duck) {
      if (!player.onGround()) {
         Vec3 playerPos = player.position();
         Vec3 offsetPlayerPos = playerPos.add((double)0.0F, ModConfig.get().hangOffset, (double)0.0F);
         Cable cable = Cables.getClosestCable(player.level(), offsetPlayerPos, ModConfig.get().snapRadius);
         if (cable != null && cable.isValid()) {
            Vec3 closestPoint = cable.getClosestPoint(offsetPlayerPos);
            Vec3 playerAttachPos = closestPoint.add((double)0.0F, -ModConfig.get().hangOffset, (double)0.0F);
            if (closestPoint.y > playerPos.y + 1.01 * ModConfig.get().hangOffset && !isInvalidPosition(player, playerAttachPos.subtract(playerPos))) {
               enable(player, duck, cable, offsetPlayerPos);
            }

         }
      }
   }

   private static void enable(Player player, ZiplinePlayerDuck duck, Cable cable, Vec3 offsetPlayerPos) {
      duck.zipline$setActuallyUsing(true);
      duck.zipline$setCable(cable);
      double initialSpeed = Math.min(player.getDeltaMovement().length(), (double)0.5F);
      duck.zipline$setSpeed(initialSpeed);
      double progress = cable.getProgress(offsetPlayerPos);
      duck.zipline$setProgress(progress);
      int dirFactor = player.getLookAngle().dot(cable.direction(progress)) >= (double)0.0F ? 1 : -1;
      duck.zipline$setDirectionFactor(dirFactor);
      double futureT = progress + (double)dirFactor * 0.1 / cable.length();
      Vec3 delta = cable.getPoint(futureT).subtract(offsetPlayerPos);
      float rawYaw = (float)(Mth.atan2(delta.z, delta.x) * (double)(180F / (float)Math.PI) - (double)player.getYRot());
      float clampedYaw = Mth.clamp(Mth.wrapDegrees(rawYaw), -15.0F, 15.0F) * 0.3F;
      ZiplineClient.ziplineTilt(clampedYaw);
      player.playSound((SoundEvent)ZiplineSoundEvents.ZIPLINE_ATTACH.get(), 0.6F, 1.0F);
   }

   private static void ziplineTick(Player player, ZiplinePlayerDuck duck, ItemStack stack) {
      if (player.onGround()) {
         interruptUsing(player, duck);
      } else if (stack.isEmpty()) {
         interruptUsing(player, duck);
      } else {
         Cable cable = duck.zipline$getCable();
         if (cable != null && cable.isValid()) {
            double oldProgress = duck.zipline$getProgress();
            double velocity = duck.zipline$getSpeed() * (double)duck.zipline$getDirectionFactor();
            if (ModConfig.get().realisticPhysics) {
               double deltaT = 0.1 / Math.max((double)1.0F, cable.length());
               double tForward = Math.min((double)1.0F, oldProgress + deltaT);
               double tBackward = Math.max((double)0.0F, oldProgress - deltaT);
               Vec3 pForward = cable.getPoint(tForward);
               Vec3 pBackward = cable.getPoint(tBackward);
               Vec3 tangent = pForward.subtract(pBackward).normalize();
               double gravityAccel = 0.04;
               double acceleration = -gravityAccel * tangent.y;
               velocity += acceleration;
               velocity *= 0.98;
               if (Math.abs(velocity) < 0.01 && Math.abs(tangent.y) < 0.1) {
                  velocity = (double)0.0F;
               }
            } else {
               int intendedDir = player.getLookAngle().dot(cable.direction(oldProgress)) >= (double)0.0F ? 1 : -1;
               velocity = Mth.lerp(0.05, velocity, 1.6 * (double)intendedDir);
            }

            duck.zipline$setSpeed(Math.abs(velocity));
            duck.zipline$setDirectionFactor(velocity >= (double)0.0F ? 1 : -1);
            double moveDelta = velocity * ModConfig.get().speedMultiplier / cable.length();
            double newProgress = oldProgress + moveDelta;
            newProgress = Mth.clamp(newProgress, (double)0.0F, (double)1.0F);
            duck.zipline$setProgress(newProgress);
            Vec3 newPosition = cable.getPoint(newProgress);
            Vec3 newOffsetPosition = new Vec3(newPosition.x, newPosition.y - ModConfig.get().hangOffset, newPosition.z);
            Vec3 oldPosition = cable.getPoint(oldProgress);
            Vec3 lastDir = newPosition.subtract(oldPosition);
            duck.zipline$setLastDir(lastDir);
            if (isInvalidPosition(player, lastDir)) {
               duck.zipline$setSpeed((double)0.0F);
               duck.zipline$setProgress(oldProgress);
               newProgress = oldProgress;
               newOffsetPosition = new Vec3(oldPosition.x, oldPosition.y - ModConfig.get().hangOffset, oldPosition.z);
            }

            player.setPos(newOffsetPosition);
            player.setDeltaMovement((double)0.0F, (double)0.0F, (double)0.0F);
            player.fallDistance = (double)0.0F;
            player.playSound((SoundEvent)ZiplineSoundEvents.ZIPLINE_USE.get(), 1.0F, 0.3F + (float)duck.zipline$getSpeed());
            if (newProgress >= (double)1.0F || newProgress <= (double)0.0F) {
               handleCableSwitch(player, duck, cable, velocity >= (double)0.0F ? 1 : -1, lastDir);
            }

         } else {
            interruptUsing(player, duck);
         }
      }
   }

   private static void handleCableSwitch(Player player, ZiplinePlayerDuck duck, Cable currentCable, int dirFactor, Vec3 lastDir) {
      Collection<Cable> nextCables = currentCable.getNext(dirFactor == 1);
      Vec3 playerDir = player.getLookAngle();
      Vec3 movementDir = lastDir.normalize();
      double highestDotProduct = (double)-1.0F;
      Cable nextCable = null;

      for(Cable next : nextCables) {
         if (!currentCable.equals(next)) {
            double startAlignment = next.direction((double)0.0F).dot(movementDir);
            double endAlignment = next.direction((double)1.0F).dot(movementDir.scale((double)-1.0F));
            double bestAlignment = Math.max(startAlignment, endAlignment);
            if (bestAlignment > ModConfig.get().maxTurnAngle) {
               double lookDotProduct = next.direction((double)0.0F).dot(playerDir);
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
         Vec3 exitPos = currentCable.getPoint(dirFactor == 1 ? (double)1.0F : (double)0.0F);
         double distToStart = exitPos.distanceToSqr(nextCable.getPoint((double)0.0F));
         double distToEnd = exitPos.distanceToSqr(nextCable.getPoint((double)1.0F));
         boolean startAtBeginning = distToStart <= distToEnd;
         duck.zipline$setCable(nextCable);
         duck.zipline$setProgress(startAtBeginning ? (double)0.0F : (double)1.0F);
         int newDir = nextCable.direction(startAtBeginning ? (double)0.0F : (double)1.0F).dot(movementDir) >= (double)0.0F ? 1 : -1;
         duck.zipline$setDirectionFactor(newDir);
      }
   }

   private static void interruptUsing(Player player, ZiplinePlayerDuck duck) {
      disable(duck);
      player.stopUsingItem();
      applyExitMomentum(player, duck);
      player.playSound((SoundEvent)ZiplineSoundEvents.ZIPLINE_INTERRUPT.get(), 0.5F, 1.0F);
   }

   public static void disable(ZiplinePlayerDuck duck) {
      duck.zipline$setCable((Cable)null);
      duck.zipline$setActuallyUsing(false);
      duck.zipline$setSpeed((double)0.0F);
   }

   public static void release(Player player, ItemStack stack) {
      ZiplinePlayerDuck duck = (ZiplinePlayerDuck)player;
      player.getCooldowns().addCooldown(stack.getItem().getDefaultInstance(), ModConfig.get().releaseCooldown);
      if (duck.zipline$isActuallyUsing()) {
         if (!player.isShiftKeyDown()) {
            double jumpY = (double)0.5F * ModConfig.get().exitJumpMultiplier;
            player.addDeltaMovement(new Vec3((double)0.0F, jumpY, (double)0.0F));
         }

         applyExitMomentum(player, duck);
         disable(duck);
      }

   }

   private static void applyExitMomentum(LivingEntity livingEntity, ZiplinePlayerDuck duck) {
      Vec3 lastDir = duck.zipline$getLastDir();
      if (lastDir != null) {
         livingEntity.addDeltaMovement(lastDir.scale((double)0.5F));
      }

      livingEntity.addDeltaMovement(livingEntity.getLookAngle().with(Axis.Y, (double)0.0F).scale((double)0.5F));
   }

   private static boolean isInvalidPosition(Player player, Vec3 deltaPos) {
      AABB collisionBox = player.getBoundingBox().move(deltaPos);

      for(VoxelShape shape : player.level().getBlockCollisions(player, collisionBox)) {
         if (!shape.isEmpty()) {
            return true;
         }
      }

      return false;
   }
}
