package com.huwng.alterna.item;

import com.huwng.alterna.enchantment.ModEnchantments;
import net.minecraft.world.item.Item;

public class CurseOfTheNoLifeKingEnchantStoneItem extends EnchantStoneItem {
    public CurseOfTheNoLifeKingEnchantStoneItem(Item.Properties properties) {
        super(() -> ModEnchantments.CURSE_OF_THE_NO_LIFE_KING, 1, properties);
    }
}
