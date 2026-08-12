package com.huwng.alterna.item;

import com.huwng.alterna.enchantment.ModEnchantments;
import net.minecraft.world.item.Item;

public class HeroismEnchantStoneItem extends EnchantStoneItem {
    public HeroismEnchantStoneItem(Item.Properties properties) {
        super(() -> ModEnchantments.HEROISM, 1, properties);
    }

    @Override
    public int getAnvilCost() {
        return 10;
    }
}
