package com.huwng.alterna.event;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.enchantment.ModEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Alterna.MODID)
public class SoulboundEventHandler {

    private static final String SOULBOUND_ITEMS_TAG = "SoulboundItems";
    private static final String SOULBOUND_PENALTY_TAG = "SoulboundPenalty";
    private static final String SOULBOUND_DEATH_MARKER = "SoulboundDiedWithItems";
    private static final String SOULBOUND_OWNER_TAG = "SoulboundOwner";

    // BLOCK ITEM TOSS
    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        ItemStack stack = event.getEntity().getItem();
        Player player = event.getPlayer();

        if (player != null && hasSoulbound(stack, player.level())) {
            event.setCanceled(true);
            player.getInventory().add(stack);
            player.sendSystemMessage(
                Component.translatable("message.alterna.soulbound.cannot_drop")
                    .withStyle(ChatFormatting.RED)
            );
        }
    }

    // BLOCK CONTAINER STORAGE (EXCLUDING ANVIL OUTPUT)
    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Anvil handles its own output and item return on close
        if (event.getContainer() instanceof AnvilMenu) {
            return;
        }

        if (event.getContainer() != null && event.getContainer() != player.inventoryMenu) {
            for (Slot slot : event.getContainer().slots) {
                if (slot.container != player.getInventory()) {
                    ItemStack stack = slot.getItem();
                    if (!stack.isEmpty() && hasSoulbound(stack, player.level())) {
                        if (isOwnedBy(stack, player)) {
                            ItemStack returned = stack.copy();
                            slot.set(ItemStack.EMPTY);

                            if (!player.getInventory().add(returned)) {
                                player.drop(returned, false);
                            }

                            player.sendSystemMessage(
                                Component.translatable("message.alterna.soulbound.returned")
                                    .withStyle(ChatFormatting.YELLOW)
                            );
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CompoundTag playerData = player.getPersistentData();
        if (playerData.getBoolean(SOULBOUND_PENALTY_TAG).orElse(false)) {
            float maxHealth = player.getMaxHealth();
            float newHealth = maxHealth * 0.1f;
            player.setHealth(newHealth);
            playerData.remove(SOULBOUND_PENALTY_TAG);
        }

        AbstractContainerMenu container = player.containerMenu;
        if (container != null && container != player.inventoryMenu && !(container instanceof AnvilMenu)) {
            for (Slot slot : container.slots) {
                if (slot.container != player.getInventory()) {
                    ItemStack stack = slot.getItem();
                    if (!stack.isEmpty() && hasSoulbound(stack, player.level())) {
                        if (!isOwnedBy(stack, player)) {
                            player.containerMenu.sendAllDataToRemote();
                        }
                    }
                }
            }
        }
    }

    // SAVE SOULBOUND ON DEATH
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Object keepInventoryObj = player.level().getGameRules().get(GameRules.KEEP_INVENTORY);
        boolean keepInventory = Boolean.TRUE.equals(keepInventoryObj);

        List<ItemStack> soulboundItems = new ArrayList<>();

        if (!keepInventory) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && hasSoulbound(stack, player.level())) {
                    soulboundItems.add(stack.copy());
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }

            if (!soulboundItems.isEmpty()) {
                saveSoulboundItems(player, soulboundItems);
            }
        }

        boolean hasSoulboundInInventory = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && hasSoulbound(stack, player.level())) {
                hasSoulboundInInventory = true;
                break;
            }
        }

        if (hasSoulboundInInventory || !soulboundItems.isEmpty()) {
            player.getPersistentData().putBoolean(SOULBOUND_DEATH_MARKER, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        CompoundTag playerData = player.getPersistentData();

        if (playerData.getBoolean(SOULBOUND_DEATH_MARKER).orElse(false)) {
            playerData.putBoolean(SOULBOUND_PENALTY_TAG, true);
            playerData.remove(SOULBOUND_DEATH_MARKER);
        }

        List<ItemStack> soulboundItems = loadSoulboundItems(player);
        if (!soulboundItems.isEmpty()) {
            for (ItemStack stack : soulboundItems) {
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false);
                }
            }
            clearSoulboundItems(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer newPlayer) {
            ServerPlayer oldPlayer = (ServerPlayer) event.getOriginal();

            CompoundTag oldData = oldPlayer.getPersistentData();
            CompoundTag newData = newPlayer.getPersistentData();

            if (oldData.contains(SOULBOUND_ITEMS_TAG)) {
                Tag tag = oldData.get(SOULBOUND_ITEMS_TAG);
                if (tag != null) {
                    newData.put(SOULBOUND_ITEMS_TAG, tag);
                }
            }

            if (oldData.getBoolean(SOULBOUND_DEATH_MARKER).orElse(false)) {
                newData.putBoolean(SOULBOUND_DEATH_MARKER, true);
            }
        }
    }

    // HELPERS
    public static boolean hasSoulbound(ItemStack stack, Level level) {
        if (stack.isEmpty()) return false;
        var reg = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ModEnchantments.SOULBOUND);
        return reg.isPresent() && stack.getEnchantmentLevel(reg.get()) > 0;
    }

    public static void setOwner(ItemStack stack, Player player) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(SOULBOUND_OWNER_TAG, player.getUUID().toString()));
    }

    private static boolean isOwnedBy(ItemStack stack, Player player) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.copyTag().contains(SOULBOUND_OWNER_TAG)) {
            setOwner(stack, player);
            return true;
        }

        String ownerUUID = customData.copyTag().getString(SOULBOUND_OWNER_TAG).orElse("");
        return ownerUUID.equals(player.getUUID().toString());
    }

    private static void saveSoulboundItems(ServerPlayer player, List<ItemStack> items) {
        CompoundTag playerData = player.getPersistentData();
        ListTag itemsList = new ListTag();
        playerData.put(SOULBOUND_ITEMS_TAG, itemsList);
    }

    private static List<ItemStack> loadSoulboundItems(ServerPlayer player) {
        return new ArrayList<>();
    }

    private static void clearSoulboundItems(ServerPlayer player) {
        CompoundTag playerData = player.getPersistentData();
        playerData.remove(SOULBOUND_ITEMS_TAG);
    }
}
