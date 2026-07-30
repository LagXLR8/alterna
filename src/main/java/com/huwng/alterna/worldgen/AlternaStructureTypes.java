package com.huwng.alterna.worldgen;

import com.huwng.alterna.Alterna;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers Alterna's custom Structure types. Remember to call
 * STRUCTURE_TYPES.register(modEventBus) from the Alterna constructor.
 */
public class AlternaStructureTypes {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Alterna.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<HugeStoneSpikeStructure>> HUGE_STONE_SPIKE =
            STRUCTURE_TYPES.register("huge_stone_spike", () -> () -> HugeStoneSpikeStructure.CODEC);
}
