package com.example.vinemod.client;

import com.example.vinemod.config.ClothConfigIntegration;
import com.example.vinemod.platform.Services;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class ClientConfigSetup {
   public static void register(ModContainer container) {
      if (Services.PLATFORM.isModLoaded("cloth_config")) {
         container.registerExtensionPoint(IConfigScreenFactory.class, (IConfigScreenFactory)(c, parent) -> ClothConfigIntegration.createScreen(parent));
      }

   }
}
