package com.huwng.alterna;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Particle types for the rift's atmosphere effects. */
public class AlternaParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Alterna.MODID);

    // overrideLimiter = true: the mist is the whole effect down there, it
    // must not get culled by the global particle budget.
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RIFT_MIST =
            PARTICLE_TYPES.register("rift_mist", () -> new SimpleParticleType(true));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ENCHANT =
            PARTICLE_TYPES.register("enchant", () -> new SimpleParticleType(false));
}
