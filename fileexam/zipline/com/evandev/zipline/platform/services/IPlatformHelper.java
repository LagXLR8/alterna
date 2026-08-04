package com.evandev.zipline.platform.services;

import java.nio.file.Path;

public interface IPlatformHelper {
   String getPlatformName();

   boolean isModLoaded(String var1);

   boolean isDevelopmentEnvironment();

   default String getEnvironmentName() {
      return this.isDevelopmentEnvironment() ? "development" : "production";
   }

   Path getConfigDirectory();

   boolean isPhysicalClient();
}
