package com.huwng.alterna;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * ResourceKeys for the "Abyss" dimension living below bedrock.
 * The actual dimension is data-driven, see:
 *  - src/main/resources/data/alterna/dimension_type/abyss.json
 *  - src/main/resources/data/alterna/dimension/abyss.json
 */
public class AlternaDimensions {

    public static final ResourceKey<Level> ABYSS = ResourceKey.create(
            Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(Alterna.MODID, "abyss"));

    public static final ResourceKey<DimensionType> ABYSS_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            Identifier.fromNamespaceAndPath(Alterna.MODID, "abyss"));

    // Where players land after falling ~1000 blocks. Chosen to sit safely above
    // the flat floor defined in data/alterna/dimension/abyss.json (floor is at
    // min_y .. min_y+9). Tweak freely.
    public static final int ABYSS_SPAWN_X = 0;
    public static final int ABYSS_SPAWN_Y = -1900;
    public static final int ABYSS_SPAWN_Z = 0;
}
