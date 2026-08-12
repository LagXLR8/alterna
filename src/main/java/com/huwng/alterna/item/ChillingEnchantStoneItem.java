package com.huwng.alterna.item;

import com.huwng.alterna.enchantment.ModEnchantments;
import net.minecraft.world.item.Item;

public class ChillingEnchantStoneItem extends EnchantStoneItem {
    public ChillingEnchantStoneItem(Item.Properties properties) {
        super(() -> ModEnchantments.CHILLING, 1, properties);
    }
}
