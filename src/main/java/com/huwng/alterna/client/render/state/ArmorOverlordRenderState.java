package com.huwng.alterna.client.render.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class ArmorOverlordRenderState extends LivingEntityRenderState {
    // ---- procedural legs (filled from OverlordLegs): planted feet in model space (pixels)
    public boolean legsActive;
    public final float[] footX = new float[4], footY = new float[4], footZ = new float[4];
    /** Body nose-up pitch / left-up roll (radians), drop into dips (px, + = down), stride bob (px), sway (rad). */
    public float bodyPitch, bodyRoll, bodyDrop, stepBob, stepSway;
    // ---- hook arms: where each blade reaches (model px) and how far the reach is blended in
    public final float[] armX = new float[2], armY = new float[2], armZ = new float[2];
    public final float[] armReach = new float[2];
    /** 0 = blade folded back like a mantis, 1 = blade straightened out to strike. */
    public final float[] bladeOpen = new float[2];
    /** Brain glow 0..1 (brighter while hunting). */
    public float brainGlow;
    /** 0..1 crouch before a stomp. */
    public float stomp;
}
