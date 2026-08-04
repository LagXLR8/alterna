package com.evandev.zipline.duck;

import com.evandev.zipline.Cable;
import net.minecraft.world.phys.Vec3;

public interface ZiplinePlayerDuck {
   Cable zipline$getCable();

   void zipline$setCable(Cable var1);

   double zipline$getSpeed();

   void zipline$setSpeed(double var1);

   double zipline$getProgress();

   void zipline$setProgress(double var1);

   int zipline$getDirectionFactor();

   void zipline$setDirectionFactor(int var1);

   Vec3 zipline$getLastDir();

   void zipline$setLastDir(Vec3 var1);

   boolean zipline$isActuallyUsing();

   void zipline$setActuallyUsing(boolean var1);
}
