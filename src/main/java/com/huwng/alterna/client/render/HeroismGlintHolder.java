package com.huwng.alterna.client.render;

public interface HeroismGlintHolder {
    ModGlintType alterna$getGlintType();
    void alterna$setGlintType(ModGlintType glintType);

    default boolean alterna$isHeroismGlint() {
        return alterna$getGlintType() != ModGlintType.NONE;
    }

    default void alterna$setHeroismGlint(boolean heroismGlint) {
        if (!heroismGlint) {
            alterna$setGlintType(ModGlintType.NONE);
        }
    }
}
