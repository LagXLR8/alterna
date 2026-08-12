package com.huwng.alterna.item;

import com.huwng.alterna.enchantment.ModEnchantments;
import net.minecraft.world.item.Item;

public class CurseOfBloodlustEnchantStoneItem extends EnchantStoneItem {
    public CurseOfBloodlustEnchantStoneItem(Item.Properties properties) {
        super(() -> ModEnchantments.CURSE_OF_BLOODLUST, 1, properties);
    }
}
