package com.huwng.alterna.loot;

import com.huwng.alterna.worldgen.GiantCrackParams;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public record GiantCrackMapFunction() implements LootItemFunction {
    public static final GiantCrackMapFunction INSTANCE = new GiantCrackMapFunction();
    public static final MapCodec<GiantCrackMapFunction> MAP_CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<? extends LootItemFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public ItemStack apply(ItemStack stack, LootContext context) {
        ServerLevel serverLevel = context.getLevel();
        Vec3 originVec = context.getOptionalParameter(LootContextParams.ORIGIN);
        BlockPos originPos = originVec != null ? BlockPos.containing(originVec) : BlockPos.ZERO;

        BlockPos nearestCrack = GiantCrackParams.findNearestCrack(serverLevel, originPos, 8);
        if (nearestCrack == null) {
            return ItemStack.EMPTY;
        }

        ItemStack mapStack = MapItem.create(serverLevel, nearestCrack.getX(), nearestCrack.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(serverLevel, mapStack);
        MapItemSavedData.addTargetDecoration(mapStack, nearestCrack, "+", MapDecorationTypes.TARGET_X);
        mapStack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.alterna.rift_explorer_map"));

        return mapStack;
    }
}
