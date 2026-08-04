package com.huwng.alterna.vine;

import com.huwng.alterna.Alterna;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Sound events for the vine system.
 * Uses leaf/grass/wood vanilla sounds to match the natural vine theme.
 */
public class VineSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Alterna.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> VINE_ATTACH =
            SOUND_EVENTS.register("vine_attach", () ->
                    SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Alterna.MODID, "vine_attach")));

    public static final DeferredHolder<SoundEvent, SoundEvent> VINE_USE =
            SOUND_EVENTS.register("vine_use", () ->
                    SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Alterna.MODID, "vine_use")));

    public static final DeferredHolder<SoundEvent, SoundEvent> VINE_INTERRUPT =
            SOUND_EVENTS.register("vine_interrupt", () ->
                    SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Alterna.MODID, "vine_interrupt")));
}
