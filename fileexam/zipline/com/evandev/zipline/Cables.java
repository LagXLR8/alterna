package com.evandev.zipline;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class Cables {
   private static final List<CableProvider> providers = new ArrayList();

   public static @Nullable Cable getClosestCable(Level level, Vec3 offsetPlayerPos, double radius) {
      double nearestDist = radius * radius;
      Cable nearestCable = null;

      for(CableProvider provider : providers) {
         Cable cable = provider.getNearestCable(level, offsetPlayerPos, nearestDist);
         if (cable != null) {
            Vec3 closestPoint = cable.getClosestPoint(offsetPlayerPos);
            double distance = closestPoint.distanceToSqr(offsetPlayerPos);
            if (distance < nearestDist) {
               nearestCable = cable;
               nearestDist = distance;
            }
         }
      }

      assert nearestCable == null || nearestCable.isValid();

      if (nearestCable != null && !nearestCable.isValid()) {
         return null;
      } else {
         return nearestCable;
      }
   }

   public static void registerProvider(CableProvider provider) {
      providers.add(provider);
   }

   @FunctionalInterface
   public interface CableProvider {
      Cable getNearestCable(Level var1, Vec3 var2, double var3);
   }
}
