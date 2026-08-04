package com.huwng.alterna.vine;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.item.ModItems;
import com.huwng.alterna.vine.network.VineSyncConnectionsPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event handler for connecting and breaking 3D Vine lines between two blocks using Vine Linker (VINE_ROPE).
 */
@EventBusSubscriber(modid = Alterna.MODID)
public class VineInteractionHandler {

    private static final Map<UUID, BlockPos> FIRST_POINTS = new HashMap<>();

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        ItemStack stack = event.getItemStack();

        // Must be holding the Vine Linker item to connect vine lines
        if (!stack.is(ModItems.VINE_ROPE.get())) {
            return;
        }

        if (!level.isClientSide()) {
            BlockPos clickedPos = event.getPos();
            UUID playerId = player.getUUID();

            if (!FIRST_POINTS.containsKey(playerId)) {
                FIRST_POINTS.put(playerId, clickedPos);
                player.sendSystemMessage(
                        Component.literal("§a[Vine] §fĐã chọn điểm 1: (" + clickedPos.toShortString() + "). Hãy chọn điểm 2.")
                );
                level.playSound(null, clickedPos, SoundEvents.VINE_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
            } else {
                BlockPos posA = FIRST_POINTS.remove(playerId);
                if (posA.equals(clickedPos)) {
                    player.sendSystemMessage(Component.literal("§c[Vine] §fHủy chọn điểm."));
                    return;
                }

                double distance = Math.sqrt(posA.distSqr(clickedPos));
                if (distance > 48.0) {
                    player.sendSystemMessage(Component.literal("§c[Vine] §fKhoảng cách quá xa (Tối đa 48 blocks)!"));
                    return;
                }

                VineSavedData data = VineSavedData.get((ServerLevel) level);
                VineConnection connection = new VineConnection(posA, clickedPos);
                data.addConnection(connection);

                // Sync to all players in dimension
                PacketDistributor.sendToPlayersInDimension((ServerLevel) level, VineSyncConnectionsPayload.fromConnections(data.getConnections()));

                level.playSound(null, clickedPos, SoundEvents.VINE_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
                player.sendSystemMessage(
                        Component.literal("§a[Vine] §fĐã nối dây Vine thành công giữa " + posA.toShortString() + " và " + clickedPos.toShortString() + "!")
                );
            }
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            BlockPos pos = event.getPos();
            VineSavedData data = VineSavedData.get(serverLevel);
            if (data.removeConnectionsAt(pos)) {
                PacketDistributor.sendToPlayersInDimension(serverLevel, VineSyncConnectionsPayload.fromConnections(data.getConnections()));
                event.getEntity().sendSystemMessage(Component.literal("§c[Vine] §fĐã tháo dây Vine tại " + pos.toShortString()));
            }
        }
    }

    public static void syncAllToPlayer(ServerPlayer player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            VineSavedData data = VineSavedData.get(serverLevel);
            PacketDistributor.sendToPlayer(player, VineSyncConnectionsPayload.fromConnections(data.getConnections()));
        }
    }
}
