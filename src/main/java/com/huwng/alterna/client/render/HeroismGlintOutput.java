package com.huwng.alterna.client.render;

public interface HeroismGlintOutput {
    void alterna$markCustomGlint(ModGlintType glintType);

    default void alterna$markHeroismGlint() {
        alterna$markCustomGlint(ModGlintType.HEROISM);
    }
}
