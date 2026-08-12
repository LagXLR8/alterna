package com.huwng.alterna.item;

import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class EnchantStoneItem extends Item {
    private final Supplier<ResourceKey<Enchantment>> enchantment;
    private final int enchantmentLevel;
    private final int anvilCost;
    private final Predicate<ItemStack> applicableItems;

    public EnchantStoneItem(Supplier<ResourceKey<Enchantment>> enchantment,
                            int enchantmentLevel,
                            int anvilCost,
                            Predicate<ItemStack> applicableItems,
                            Item.Properties properties) {
        super(properties.stacksTo(1));
        this.enchantment = enchantment;
        this.enchantmentLevel = enchantmentLevel;
        this.anvilCost = anvilCost;
        this.applicableItems = applicableItems;
    }

    public EnchantStoneItem(Supplier<ResourceKey<Enchantment>> enchantment, int anvilCost, Item.Properties properties) {
        this(enchantment, 1, anvilCost, EnchantStoneItem::isWeapon, properties.rarity(Rarity.EPIC));
    }

    public ResourceKey<Enchantment> getEnchantmentKey() {
        return enchantment.get();
    }

    public int getEnchantmentLevel() {
        return enchantmentLevel;
    }

    public int getAnvilCost() {
        return anvilCost;
    }

    public boolean canApplyTo(ItemStack stack) {
        return applicableItems.test(stack);
    }

    // PREDICATES USING ITEM TAGS
    public static boolean isWeapon(ItemStack stack) {
        return stack.is(ItemTags.SWORDS) ||
               stack.is(ItemTags.AXES) ||
               stack.is(ItemTags.SPEARS) ||
               stack.is(ItemTags.MELEE_WEAPON_ENCHANTABLE) ||
               stack.is(ItemTags.WEAPON_ENCHANTABLE) ||
               stack.getItem() instanceof TridentItem;
    }

    public static boolean isArmor(ItemStack stack) {
        return stack.is(ItemTags.ARMOR_ENCHANTABLE);
    }

    public static boolean isTool(ItemStack stack) {
        return stack.is(ItemTags.MINING_ENCHANTABLE);
    }

    public static boolean isBow(ItemStack stack) {
        return stack.is(ItemTags.BOW_ENCHANTABLE);
    }

    public static boolean isCrossbow(ItemStack stack) {
        return stack.is(ItemTags.CROSSBOW_ENCHANTABLE);
    }

    public static boolean isRangedWeapon(ItemStack stack) {
        return isBow(stack) || isCrossbow(stack);
    }

    public static boolean isHelmet(ItemStack stack) {
        return stack.is(ItemTags.HEAD_ARMOR);
    }

    public static boolean isChestplate(ItemStack stack) {
        return stack.is(ItemTags.CHEST_ARMOR);
    }

    public static boolean isLeggings(ItemStack stack) {
        return stack.is(ItemTags.LEG_ARMOR);
    }

    public static boolean isBoots(ItemStack stack) {
        return stack.is(ItemTags.FOOT_ARMOR);
    }
}
