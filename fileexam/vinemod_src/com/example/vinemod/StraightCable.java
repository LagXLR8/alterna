package com.example.vinemod;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public record StraightCable(Vec3 start, Vec3 end, Vec3 direction, double length) implements Cable {
   public StraightCable(Vec3 start, Vec3 end) {
      this(start, end, end.subtract(start).normalize(), start.distanceTo(end));
   }

   public double getProgress(Vec3 playerPos) {
      Vec3 playerToStart = playerPos.subtract(this.start);
      double t = playerToStart.dot(this.direction) / this.length;
      t = Mth.clamp(t, (double)0.0F, (double)1.0F);
      return t;
   }

   public Vec3 getPoint(double progress) {
      return this.start.add(this.direction.scale(progress * this.length));
   }

   public Vec3 getClosestPoint(Vec3 pos) {
      double t = this.getProgress(pos);
      return this.getPoint(t);
   }

   public Vec3 direction(double progress) {
      return this.direction;
   }
}
