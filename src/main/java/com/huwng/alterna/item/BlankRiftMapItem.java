package com.huwng.alterna.item;

import com.huwng.alterna.worldgen.GiantCrackParams;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class BlankRiftMapItem extends Item {
    public BlankRiftMapItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        BlockPos playerPos = player.blockPosition();

        // Find nearest rift crack within search radius
        BlockPos nearestCrack = GiantCrackParams.findNearestCrack(serverLevel, playerPos, 16);
        if (nearestCrack == null) {
            player.sendSystemMessage(Component.translatable("item.alterna.blank_rift_map.no_rift"));
            return InteractionResult.FAIL;
        }

        // Create explorer map pointing to the rift
        ItemStack mapStack = MapItem.create(serverLevel, nearestCrack.getX(), nearestCrack.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(serverLevel, mapStack);
        MapItemSavedData.addTargetDecoration(mapStack, nearestCrack, "+", MapDecorationTypes.TARGET_X);
        mapStack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.alterna.rift_explorer_map"));

        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);

        ItemStack resultStack = ItemUtils.createFilledResult(heldStack, player, mapStack);
        return InteractionResult.SUCCESS.heldItemTransformedTo(resultStack);
    }
}
