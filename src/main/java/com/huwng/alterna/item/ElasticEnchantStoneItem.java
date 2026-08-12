package com.huwng.alterna.item;

import com.huwng.alterna.enchantment.ModEnchantments;
import net.minecraft.world.item.Item;

public class ElasticEnchantStoneItem extends EnchantStoneItem {
    public ElasticEnchantStoneItem(Item.Properties properties) {
        super(() -> ModEnchantments.ELASTIC, 1, properties);
    }
}
