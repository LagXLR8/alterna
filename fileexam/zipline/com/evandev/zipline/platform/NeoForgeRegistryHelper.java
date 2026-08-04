package com.evandev.zipline.platform;

import com.evandev.zipline.platform.services.IRegistryHelper;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

public class NeoForgeRegistryHelper implements IRegistryHelper {
   public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("zipline");
   public static final DeferredRegister<SoundEvent> SOUNDS;

   public <T extends Item> Supplier<T> registerItem(String name, Supplier<T> factory) {
      return ITEMS.register(name, factory);
   }

   public Supplier<SoundEvent> registerSound(String name, Supplier<SoundEvent> factory) {
      return SOUNDS.register(name, factory);
   }

   static {
      SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, "zipline");
   }
}
