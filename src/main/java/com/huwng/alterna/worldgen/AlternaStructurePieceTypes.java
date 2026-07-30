package com.huwng.alterna.worldgen;

import com.huwng.alterna.Alterna;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers Alterna's custom StructurePiece types. Remember to call
 * STRUCTURE_PIECE_TYPES.register(modEventBus) from the Alterna constructor.
 */
public class AlternaStructurePieceTypes {

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, Alterna.MODID);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> HUGE_STONE_SPIKE =
            STRUCTURE_PIECE_TYPES.register("huge_stone_spike", () -> HugeStoneSpikePiece::new);
}
