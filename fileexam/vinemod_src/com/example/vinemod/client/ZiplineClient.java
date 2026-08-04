package com.example.vinemod.client;

import com.example.vinemod.duck.GameRendererDuck;
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
