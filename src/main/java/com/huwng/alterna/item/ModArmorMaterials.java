package com.huwng.alterna.item;

import com.huwng.alterna.Alterna;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.EnumMap;
import java.util.Map;

/**
 * Cragplate armour - made from the plates Cragbacks shed. A little below diamond in
 * raw defence, but very heavy: high toughness and strong knockback resistance,
 * just like the beast it came from.
 */
public final class ModArmorMaterials {
    public static final TagKey<Item> REPAIRS_CRAGPLATE = TagKey.create(Registries.ITEM, Alterna.id("repairs_cragplate_armor"));
    public static final ResourceKey<EquipmentAsset> CRAGPLATE_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, Alterna.id("cragplate"));

    public static final ArmorMaterial CRAGPLATE = new ArmorMaterial(
            28, defense(3, 6, 7, 3, 9), 10, SoundEvents.ARMOR_EQUIP_NETHERITE, 2.5F, 0.08F,
            REPAIRS_CRAGPLATE, CRAGPLATE_ASSET);

    private static Map<ArmorType, Integer> defense(int boots, int leggings, int chest, int helmet, int body) {
        Map<ArmorType, Integer> m = new EnumMap<>(ArmorType.class);
        m.put(ArmorType.BOOTS, boots);
        m.put(ArmorType.LEGGINGS, leggings);
        m.put(ArmorType.CHESTPLATE, chest);
        m.put(ArmorType.HELMET, helmet);
        m.put(ArmorType.BODY, body);
        return m;
    }

    private ModArmorMaterials() {}
}
