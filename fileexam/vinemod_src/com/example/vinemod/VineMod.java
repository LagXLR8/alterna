package com.example.vinemod;

import com.example.vinemod.compat.connectiblechains.ConnectibleChainsCompat;
import com.example.vinemod.platform.Services;
import com.example.vinemod.registry.ZiplineSoundEvents;
import net.minecraft.resources.Identifier;

public class VineMod {
   public static final String MOD_ID = "vinemod";

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("vinemod", path);
   }

   public static void init() {
      ZiplineSoundEvents.register();
      if (Services.PLATFORM.isModLoaded("connectiblechains")) {
         ConnectibleChainsCompat.register();
      }

   }
}
