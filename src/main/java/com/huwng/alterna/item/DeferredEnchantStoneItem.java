package com.huwng.alterna.item;

import com.huwng.alterna.enchantment.ModEnchantments;
import net.minecraft.world.item.Item;

public class DeferredEnchantStoneItem extends EnchantStoneItem {
    public DeferredEnchantStoneItem(Item.Properties properties) {
        super(() -> ModEnchantments.DEFERRED, 1, properties);
    }

    @Override
    public int getAnvilCost() {
        return 10;
    }
}
