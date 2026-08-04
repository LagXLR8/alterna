package com.evandev.zipline.client;

import com.evandev.zipline.duck.GameRendererDuck;
import net.minecraft.client.Minecraft;

public class ZiplineClient {
   public static void init() {
      compat();
   }

   public static void compat() {
   }

   public static void ziplineTilt(float yaw) {
      ((GameRendererDuck)Minecraft.getInstance().gameRenderer).zipline$setZiplineTilt(yaw);
   }
}
