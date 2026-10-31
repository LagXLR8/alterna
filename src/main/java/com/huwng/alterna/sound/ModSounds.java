package com.huwng.alterna.sound;

import com.huwng.alterna.Alterna;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Custom creature sounds for the Lichen Savanna (files under assets/alterna/sounds/). */
public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Alterna.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> TETRA_AMBIENT = register("entity.tetra.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> TETRA_HURT = register("entity.tetra.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> TETRA_DEATH = register("entity.tetra.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> TETRA_FLAP = register("entity.tetra.flap");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRAGBACK_AMBIENT = register("entity.cragback.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRAGBACK_HURT = register("entity.cragback.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRAGBACK_DEATH = register("entity.cragback.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> OVERLORD_AMBIENT = register("entity.armor_overlord.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> OVERLORD_HURT = register("entity.armor_overlord.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> OVERLORD_DEATH = register("entity.armor_overlord.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> OVERLORD_ROAR = register("entity.armor_overlord.roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> OVERLORD_STEP = register("entity.armor_overlord.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> OVERLORD_HOOK = register("entity.armor_overlord.hook");
    public static final DeferredHolder<SoundEvent, SoundEvent> OVERLORD_STOMP = register("entity.armor_overlord.stomp");
    public static final DeferredHolder<SoundEvent, SoundEvent> OVERLORD_DEFLECT = register("entity.armor_overlord.deflect");

    // ---- scanner
    public static final DeferredHolder<SoundEvent, SoundEvent> SCAN_LOOP = register("item.scanner.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCAN_COMPLETE = register("item.scanner.complete");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCAN_FAIL = register("item.scanner.fail");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCAN_PANEL = register("item.scanner.panel");
    /** Databank narration, one per scan entry (id -> sound). */
    public static final java.util.Map<String, DeferredHolder<SoundEvent, SoundEvent>> SCAN_VOICES = new java.util.HashMap<>();

    static {
        for (com.huwng.alterna.scanner.ScanEntries.Entry e : com.huwng.alterna.scanner.ScanEntries.all()) {
            SCAN_VOICES.put(e.id(), register("scanner.voice." + e.id()));
        }
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(Alterna.id(name)));
    }

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
