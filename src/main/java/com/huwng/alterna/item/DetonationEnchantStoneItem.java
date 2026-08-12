package com.huwng.alterna.item;

import com.huwng.alterna.enchantment.ModEnchantments;
import net.minecraft.world.item.Item;

public class DetonationEnchantStoneItem extends EnchantStoneItem {
    public DetonationEnchantStoneItem(Item.Properties properties) {
        super(() -> ModEnchantments.DETONATION, 1, properties);
    }

    @Override
    public int getAnvilCost() {
        return 10;
    }
}
