package com.huwng.alterna.effect;

import com.huwng.alterna.Alterna;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Alterna.MODID);

    public static final DeferredHolder<MobEffect, ChillMobEffect> CHILL =
            MOB_EFFECTS.register("chill", ChillMobEffect::new);

    public static final DeferredHolder<MobEffect, BottomFreezeMobEffect> BOTTOM_FREEZE =
            MOB_EFFECTS.register("bottom_freeze", BottomFreezeMobEffect::new);

    public static final DeferredHolder<MobEffect, TetheredMobEffect> TETHERED =
            MOB_EFFECTS.register("tethered", TetheredMobEffect::new);

    public static final DeferredHolder<MobEffect, TemporaryHealthMobEffect> TEMPORARY_HEALTH =
            MOB_EFFECTS.register("temporary_health", TemporaryHealthMobEffect::new);

    public static final DeferredHolder<MobEffect, InsanityMobEffect> INSANITY =
            MOB_EFFECTS.register("insanity", InsanityMobEffect::new);

    public static void register(IEventBus eventBus) {

        MOB_EFFECTS.register(eventBus);
    }
}
