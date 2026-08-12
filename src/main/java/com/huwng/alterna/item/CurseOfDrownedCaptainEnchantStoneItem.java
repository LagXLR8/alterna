package com.huwng.alterna.item;

import com.huwng.alterna.enchantment.ModEnchantments;
import net.minecraft.world.item.Item;

public class CurseOfDrownedCaptainEnchantStoneItem extends EnchantStoneItem {
    public CurseOfDrownedCaptainEnchantStoneItem(Item.Properties properties) {
        super(() -> ModEnchantments.CURSE_OF_DROWNED_CAPTAIN, 1, properties);
    }
}
