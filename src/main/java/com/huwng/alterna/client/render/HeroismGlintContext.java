package com.huwng.alterna.client.render;

public class HeroismGlintContext {
    private static final ThreadLocal<ModGlintType> ACTIVE_GLINT = ThreadLocal.withInitial(() -> ModGlintType.NONE);

    public static void setGlintType(ModGlintType type) {
        ACTIVE_GLINT.set(type != null ? type : ModGlintType.NONE);
    }

    public static ModGlintType getGlintType() {
        return ACTIVE_GLINT.get();
    }

    public static boolean isHeroism() {
        return ACTIVE_GLINT.get() != ModGlintType.NONE;
    }

    public static void setHeroism(boolean heroism) {
        if (!heroism) {
            ACTIVE_GLINT.set(ModGlintType.NONE);
        }
    }
}
