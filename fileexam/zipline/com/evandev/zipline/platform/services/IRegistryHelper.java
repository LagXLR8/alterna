package com.evandev.zipline.platform.services;

import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;

public interface IRegistryHelper {
   <T extends Item> Supplier<T> registerItem(String var1, Supplier<T> var2);

   Supplier<SoundEvent> registerSound(String var1, Supplier<SoundEvent> var2);
}
