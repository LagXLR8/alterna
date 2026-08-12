package com.huwng.alterna.item;

import com.huwng.alterna.enchantment.ModEnchantments;
import net.minecraft.world.item.Item;

public class VampirismEnchantStoneItem extends EnchantStoneItem {
    public VampirismEnchantStoneItem(Item.Properties properties) {
        super(() -> ModEnchantments.VAMPIRISM, 1, properties);
    }
}
