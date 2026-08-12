package com.huwng.alterna.item;

import com.huwng.alterna.enchantment.ModEnchantments;
import net.minecraft.world.item.Item;

public class WildEnchantStoneItem extends EnchantStoneItem {
    public WildEnchantStoneItem(Item.Properties properties) {
        super(() -> ModEnchantments.WILD, 1, properties);
    }
}
