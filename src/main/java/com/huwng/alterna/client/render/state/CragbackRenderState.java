package com.huwng.alterna.client.render.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class CragbackRenderState extends LivingEntityRenderState {
    /** 0..1 head-down grazing amount. */
    public float eatAnimation;
    /** 0..1 head-down drinking amount. */
    public float drinkAnimation;
    /** true while angrily charging whoever hurt it. */
    public boolean charging;

    // ---- procedural legs (filled from CragbackLegs)
    public boolean legsActive;
    /** Planted foot positions in model space (pixels): right-front, left-front, right-hind, left-hind. */
    public final float[] footX = new float[4], footY = new float[4], footZ = new float[4];
    /** Body nose-up pitch and left-side-up roll (radians) from the ground under the feet. */
    public float bodyPitch, bodyRoll;
    /** Body lowered into dips (pixels, positive = down), rise with each step, sideways sway. */
    public float bodyDrop, stepBob, stepSway;
}
