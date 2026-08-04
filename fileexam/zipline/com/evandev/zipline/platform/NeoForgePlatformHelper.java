package com.evandev.zipline.platform;

import com.evandev.zipline.platform.services.IPlatformHelper;
import java.nio.file.Path;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

public class NeoForgePlatformHelper implements IPlatformHelper {
   public String getPlatformName() {
      return "NeoForge";
   }

   public boolean isModLoaded(String modId) {
      return ModList.get().isLoaded(modId);
   }

   public boolean isDevelopmentEnvironment() {
      return !FMLLoader.getCurrent().isProduction();
   }

   public Path getConfigDirectory() {
      return FMLPaths.CONFIGDIR.get();
   }

   public boolean isPhysicalClient() {
      return FMLLoader.getCurrent().getDist() == Dist.CLIENT;
   }
}
