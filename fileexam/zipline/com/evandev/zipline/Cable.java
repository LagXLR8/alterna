package com.evandev.zipline;

import java.util.Collection;
import java.util.Collections;
import net.minecraft.world.phys.Vec3;

public interface Cable {
   double getProgress(Vec3 var1);

   Vec3 getPoint(double var1);

   Vec3 getClosestPoint(Vec3 var1);

   Vec3 direction(double var1);

   double length();

   default Collection<Cable> getNext(boolean forward) {
      return Collections.emptyList();
   }

   default boolean isValid() {
      return true;
   }
}
