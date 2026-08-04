package com.example.vinemod.registry;

import com.example.vinemod.VineMod;
import com.example.vinemod.platform.Services;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;

public class ZiplineSoundEvents {
   public static final Supplier<SoundEvent> ZIPLINE_ATTACH = register("zipline_attach");
   public static final Supplier<SoundEvent> ZIPLINE_INTERRUPT = register("zipline_interrupt");
   public static final Supplier<SoundEvent> ZIPLINE_USE = register("zipline_use");

   private static Supplier<SoundEvent> register(String path) {
      return Services.REGISTRY.registerSound(path, () -> SoundEvent.createVariableRangeEvent(VineMod.id(path)));
   }

   public static void register() {
   }
}
