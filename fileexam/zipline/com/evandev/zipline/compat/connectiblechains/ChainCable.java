package com.evandev.zipline.compat.connectiblechains;

import com.evandev.connectiblechains.util.MathHelper;
import com.evandev.zipline.Cable;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record ChainCable(Vec3 from, Vec3 to, Vec3 delta, Vec3 direction, double length, double slack) implements Cable {
   public static ChainCable from(Entity from, Entity to, double slack) {
      Vec3 fromPos = from.position();
      Vec3 toPos = to.position();
      if (fromPos.y > toPos.y) {
         Vec3 swap = toPos;
         toPos = fromPos;
         fromPos = swap;
      }

      Vec3 delta = toPos.subtract(fromPos);
      Vec3 direction = delta.normalize();
      double length = delta.length();
      return new ChainCable(fromPos, toPos, delta, direction, length, slack);
   }

   public double getProgress(Vec3 playerPos) {
      Vec3 playerToStart = playerPos.subtract(this.from);
      double t = playerToStart.dot(this.direction) / this.length;
      t = Mth.clamp(t, (double)0.0F, (double)1.0F);
      return t;
   }

   public Vec3 getPoint(double progress) {
      double distanceXZ = (double)((float)Math.sqrt(Math.fma(this.delta.x(), this.delta.x(), this.delta.z() * this.delta.z())));
      double wrongDistanceFactor = this.length / distanceXZ;
      double a = progress * distanceXZ;
      double y = MathHelper.drip2(a * wrongDistanceFactor, this.length, this.delta.y(), this.slack) + (double)0.4F;
      double x = progress * this.delta.x();
      double z = progress * this.delta.z();
      return this.from.add(new Vec3(x, y, z));
   }

   public Vec3 getClosestPoint(Vec3 pos) {
      double t = this.getProgress(pos);
      return this.getPoint(t);
   }

   public Vec3 direction(double progress) {
      return this.direction;
   }
}
