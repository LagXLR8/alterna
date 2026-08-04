package com.example.vinemod.mixin;

import com.example.vinemod.Cable;
import com.example.vinemod.duck.ZiplinePlayerDuck;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({Player.class})
public class PlayerMixin implements ZiplinePlayerDuck {
   @Unique
   private Cable zipline$cable;
   @Unique
   private double zipline$speed;
   @Unique
   private double zipline$progress;
   @Unique
   private int zipline$directionFactor;
   @Unique
   private Vec3 zipline$lastDir;
   @Unique
   private boolean zipline$actuallyUsing;

   public Cable zipline$getCable() {
      return this.zipline$cable;
   }

   public void zipline$setCable(Cable cable) {
      this.zipline$cable = cable;
   }

   public double zipline$getSpeed() {
      return this.zipline$speed;
   }

   public void zipline$setSpeed(double speed) {
      this.zipline$speed = speed;
   }

   public double zipline$getProgress() {
      return this.zipline$progress;
   }

   public void zipline$setProgress(double progress) {
      this.zipline$progress = progress;
   }

   public int zipline$getDirectionFactor() {
      return this.zipline$directionFactor;
   }

   public void zipline$setDirectionFactor(int factor) {
      this.zipline$directionFactor = factor;
   }

   public Vec3 zipline$getLastDir() {
      return this.zipline$lastDir;
   }

   public void zipline$setLastDir(Vec3 dir) {
      this.zipline$lastDir = dir;
   }

   public boolean zipline$isActuallyUsing() {
      return this.zipline$actuallyUsing;
   }

   public void zipline$setActuallyUsing(boolean using) {
      this.zipline$actuallyUsing = using;
   }
}
