package com.huwng.alterna.item;

import com.huwng.alterna.enchantment.ModEnchantments;
import net.minecraft.world.item.Item;

public class GluttonyEnchantStoneItem extends EnchantStoneItem {
    public GluttonyEnchantStoneItem(Item.Properties properties) {
        super(() -> ModEnchantments.GLUTTONY, 1, properties);
    }
}
