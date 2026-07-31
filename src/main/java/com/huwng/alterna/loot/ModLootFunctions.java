package com.huwng.alterna.loot;

import com.huwng.alterna.Alterna;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModLootFunctions {
    public static final DeferredRegister<MapCodec<? extends LootItemFunction>> LOOT_FUNCTIONS =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, Alterna.MODID);

    public static final DeferredHolder<MapCodec<? extends LootItemFunction>, MapCodec<GiantCrackMapFunction>> GIANT_CRACK_MAP =
            LOOT_FUNCTIONS.register("giant_crack_map", () -> GiantCrackMapFunction.MAP_CODEC);

    public static void register(IEventBus eventBus) {
        LOOT_FUNCTIONS.register(eventBus);
    }
}
