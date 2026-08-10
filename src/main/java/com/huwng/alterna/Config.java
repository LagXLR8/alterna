package com.huwng.alterna;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder COMMON_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.Builder CLIENT_BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SHOW_DEV_WARNING = COMMON_BUILDER
            .comment("Whether to show the development warning log when entering a world")
            .define("showDevWarning", true);

    public static final ModConfigSpec.BooleanValue ENABLE_ENCHANTMENT_TINT = CLIENT_BUILDER
            .comment("Whether to enable custom item color tinting for Alterna enchantments")
            .define("enableEnchantmentTint", true);

    public static final ModConfigSpec.BooleanValue ENABLE_ENCHANTMENT_GLINT = CLIENT_BUILDER
            .comment("Whether to enable custom glint textures for Alterna enchantments")
            .define("enableEnchantmentGlint", true);

    public static final ModConfigSpec COMMON_SPEC = COMMON_BUILDER.build();
    public static final ModConfigSpec CLIENT_SPEC = CLIENT_BUILDER.build();
    public static final ModConfigSpec SPEC = COMMON_SPEC;
}
