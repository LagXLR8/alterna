package com.huwng.alterna.worldgen;

import com.huwng.alterna.Alterna;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers Alterna's custom Feature types. Remember to call
 * FEATURES.register(modEventBus) from the Alterna constructor.
 */
public class AlternaFeatures {

        public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE,
                        Alterna.MODID);

        public static final DeferredHolder<Feature<?>, GiantCrackFeature> GIANT_CRACK = FEATURES.register("giant_crack",
                        () -> new GiantCrackFeature(NoneFeatureConfiguration.CODEC));

        // Must run at a LATER decoration step than GIANT_CRACK - see
        // GobletTreeFeature's class doc.
        public static final DeferredHolder<Feature<?>, GobletTreeFeature> GOBLET_TREE = FEATURES.register("goblet_tree",
                        () -> new GobletTreeFeature(NoneFeatureConfiguration.CODEC));

        public static final DeferredHolder<Feature<?>, KapokTreeFeature> KAPOK_TREE = FEATURES.register("kapok_tree",
                        () -> new KapokTreeFeature(KapokTreeConfiguration.CODEC));
}