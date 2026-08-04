package com.example.vinemod.registry;

import com.example.vinemod.VineMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ZiplineTags {
   public static final TagKey<Item> ATTACHMENT;

   static {
      ATTACHMENT = TagKey.create(Registries.ITEM, VineMod.id("attachment"));
   }
}
