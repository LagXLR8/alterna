package com.huwng.alterna.item;

import com.huwng.alterna.enchantment.ModEnchantments;
import net.minecraft.world.item.Item;

public class StormbreakerEnchantStoneItem extends EnchantStoneItem {
    public StormbreakerEnchantStoneItem(Item.Properties properties) {
        super(() -> ModEnchantments.STORMBREAKER, 1, properties);
    }

    @Override
    public int getAnvilCost() {
        return 10;
    }
}
