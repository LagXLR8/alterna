package com.huwng.alterna.event;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.enchantment.ModEnchantments;
import com.huwng.alterna.item.EnchantStoneItem;
import com.huwng.alterna.item.LunarTomeItem;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;

import java.util.Optional;

@EventBusSubscriber(modid = Alterna.MODID)
public class AnvilUpdateHandler {

    private static final String HAS_ENCHANT_STONE = "HasEnchantStone";
    private static final String HAS_FIRE_ASPECT = "HasFireAspect";
    private static final String SOULBOUND_OWNER_TAG = "SoulboundOwner";

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack leftItem = event.getLeft();
        ItemStack rightItem = event.getRight();

        if (!leftItem.isEmpty() && !rightItem.isEmpty()) {

            // ===== LUNAR TOME - CLEANSE =====
            if (rightItem.getItem() instanceof LunarTomeItem) {
                handleLunarTomeCleanse(event, leftItem);
                return;
            }

            // ===== ENCHANT STONE =====
            if (rightItem.getItem() instanceof EnchantStoneItem enchantStone) {
                if (!enchantStone.canApplyTo(leftItem)) {
                    return;
                }

                if (event.getPlayer() == null) {
                    return;
                }

                var registryAccess = event.getPlayer().level().registryAccess();
                var enchantmentRegistry = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);

                ResourceKey<Enchantment> stoneEnchantKey = enchantStone.getEnchantmentKey();
                Optional<Holder.Reference<Enchantment>> stoneEnchantHolder = enchantmentRegistry.get(stoneEnchantKey);

                if (stoneEnchantHolder.isEmpty()) {
                    return;
                }

                // Check 1: Already has Enchant Stone
                CustomData customData = leftItem.get(DataComponents.CUSTOM_DATA);
                if (customData != null && customData.copyTag().getBoolean(HAS_ENCHANT_STONE).orElse(false)) {
                    event.setCanceled(true);
                    return;
                }

                // Check 2: Already has this enchantment
                if (leftItem.getEnchantmentLevel(stoneEnchantHolder.get()) > 0) {
                    event.setCanceled(true);
                    return;
                }

                ItemStack output = leftItem.copy();

                // Apply Stone Enchantment
                output.enchant(stoneEnchantHolder.get(), enchantStone.getEnchantmentLevel());

                // Apply Soulbound
                Optional<Holder.Reference<Enchantment>> soulboundHolder = enchantmentRegistry.get(ModEnchantments.SOULBOUND);
                soulboundHolder.ifPresent(holder -> output.enchant(holder, 1));

                // Mark Owner & HasEnchantStone
                CustomData.update(DataComponents.CUSTOM_DATA, output, tag -> {
                    tag.putBoolean(HAS_ENCHANT_STONE, true);
                    if (event.getPlayer() != null) {
                        tag.putString(SOULBOUND_OWNER_TAG, event.getPlayer().getUUID().toString());
                    }
                });

                event.setOutput(output);
                event.setXpCost(enchantStone.getAnvilCost());
                event.setMaterialCost(1);
            }
        }
    }

    private static void handleLunarTomeCleanse(AnvilUpdateEvent event, ItemStack leftItem) {
        if (event.getPlayer() == null) return;

        var registryAccess = event.getPlayer().level().registryAccess();
        var enchantmentRegistry = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);

        CustomData customData = leftItem.get(DataComponents.CUSTOM_DATA);
        boolean hasEnchantStone = customData != null && customData.copyTag().getBoolean(HAS_ENCHANT_STONE).orElse(false);

        Optional<Holder.Reference<Enchantment>> soulboundHolder = enchantmentRegistry.get(ModEnchantments.SOULBOUND);
        boolean hasSoulbound = soulboundHolder.isPresent() && leftItem.getEnchantmentLevel(soulboundHolder.get()) > 0;

        if (!hasSoulbound && !hasEnchantStone) {
            return;
        }

        ItemStack output = leftItem.copy();

        // Clear enchantments added by stones & soulbound
        ItemEnchantments currentEnchants = output.getEnchantments();
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(currentEnchants);

        if (soulboundHolder.isPresent()) {
            mutable.set(soulboundHolder.get(), 0);
        }

        ResourceKey<?>[] stoneKeys = {
            ModEnchantments.CHILLING, ModEnchantments.ELASTIC, ModEnchantments.GLUTTONY,
            ModEnchantments.VAMPIRISM, ModEnchantments.WILD, ModEnchantments.HEROISM,
            ModEnchantments.DEFERRED, ModEnchantments.DETONATION, ModEnchantments.STORMBREAKER
        };

        for (ResourceKey<?> key : stoneKeys) {
            @SuppressWarnings("unchecked")
            Optional<Holder.Reference<Enchantment>> h = enchantmentRegistry.get((ResourceKey<Enchantment>) key);
            h.ifPresent(holder -> mutable.set(holder, 0));
        }

        output.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());

        CustomData.update(DataComponents.CUSTOM_DATA, output, tag -> {
            tag.remove(HAS_ENCHANT_STONE);
            tag.remove(HAS_FIRE_ASPECT);
            tag.remove(SOULBOUND_OWNER_TAG);
        });

        event.setOutput(output);
        event.setXpCost(4);
        event.setMaterialCost(1);
    }

    public static boolean hasEnchantStoneApplied(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.copyTag().getBoolean(HAS_ENCHANT_STONE).orElse(false);
    }
}
