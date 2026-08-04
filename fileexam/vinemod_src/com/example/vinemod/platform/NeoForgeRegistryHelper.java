package com.example.vinemod.platform;

import com.example.vinemod.platform.services.IRegistryHelper;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

public class NeoForgeRegistryHelper implements IRegistryHelper {
   public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("vinemod");
   public static final DeferredRegister<SoundEvent> SOUNDS;

   public <T extends Item> Supplier<T> registerItem(String name, Supplier<T> factory) {
      return ITEMS.register(name, factory);
   }

   public Supplier<SoundEvent> registerSound(String name, Supplier<SoundEvent> factory) {
      return SOUNDS.register(name, factory);
   }

   static {
      SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, "vinemod");
   }
}
