package com.huwng.alterna.item;

import com.huwng.alterna.enchantment.ModEnchantments;
import net.minecraft.world.item.Item;

public class CurseOfRejectionEnchantStoneItem extends EnchantStoneItem {
    public CurseOfRejectionEnchantStoneItem(Item.Properties properties) {
        super(() -> ModEnchantments.CURSE_OF_REJECTION, 1, properties);
    }
}
