package com.huwng.alterna.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record KapokTreeConfiguration(
        BlockStateProvider trunkProvider,
        BlockStateProvider foliageProvider,
        BlockStateProvider branchProvider,
        int minimumSize,
        int sizeVariation
) implements FeatureConfiguration {

    public static final Codec<KapokTreeConfiguration> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockStateProvider.CODEC.fieldOf("trunk_provider").forGetter(KapokTreeConfiguration::trunkProvider),
                    BlockStateProvider.CODEC.fieldOf("foliage_provider").forGetter(KapokTreeConfiguration::foliageProvider),
                    BlockStateProvider.CODEC.fieldOf("branch_provider").forGetter(KapokTreeConfiguration::branchProvider),
                    Codec.INT.fieldOf("minimum_size").forGetter(KapokTreeConfiguration::minimumSize),
                    Codec.INT.fieldOf("size_variation").forGetter(KapokTreeConfiguration::sizeVariation)
            ).apply(instance, KapokTreeConfiguration::new)
    );
}
