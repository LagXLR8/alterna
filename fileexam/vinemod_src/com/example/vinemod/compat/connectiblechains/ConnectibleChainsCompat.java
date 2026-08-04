package com.example.vinemod.compat.connectiblechains;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.entity.ChainKnotEntity;
import com.evandev.connectiblechains.entity.Chainable;
import com.example.vinemod.Cable;
import com.example.vinemod.Cables;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ConnectibleChainsCompat {
   public static void register() {
      Cables.registerProvider((level, offsetPlayerPos, squaredRadius) -> {
         int radius = CommonClass.runtimeConfig.getMaxChainRange() + 1;
         AABB aabb = new AABB(offsetPlayerPos.subtract((double)radius, (double)radius, (double)radius), offsetPlayerPos.add((double)radius, (double)radius, (double)radius));
         List<ChainKnotEntity> knots = level.getEntitiesOfClass(ChainKnotEntity.class, aabb, (a) -> true);
         double nearestDist = squaredRadius;
         Cable nearestCable = null;

         for(ChainKnotEntity knot : knots) {
            for(Chainable.ChainData chainData : knot.getChainDataSet()) {
               Entity holder = knot.getChainHolder(chainData);
               if (holder != null) {
                  ChainCable cable = ChainCable.from(knot, holder, (double)chainData.getSlack());
                  Vec3 closestPoint = cable.getClosestPoint(offsetPlayerPos);
                  double distance = closestPoint.distanceToSqr(offsetPlayerPos);
                  if (distance < nearestDist) {
                     nearestDist = distance;
                     nearestCable = cable;
                  }
               }
            }
         }

         return nearestCable;
      });
   }
}
