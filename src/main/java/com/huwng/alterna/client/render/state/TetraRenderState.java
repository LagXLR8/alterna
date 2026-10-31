package com.huwng.alterna.client.render.state;

import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;

public class TetraRenderState extends HoldingEntityRenderState {
    public boolean flying;
    public boolean perched;
    public boolean diving;
    public boolean tame;
    /** Yaw (degrees) of the cliff face it clings to while perched. */
    public float perchYaw;
    /** Banking angle (degrees, positive = right wing down). */
    public float roll;
    /** Nose-down angle of the flight path (degrees). */
    public float flightPitch;
    /** 0 = gliding with wings spread, 1 = full wing beats. */
    public float flap = 1.0F;
    /** Wing-beat phase (radians), integrated per tick. */
    public float flapPhase;
    /** Procedural extras: tail swing (rad, lags turns), tail lift (rad), ground slope pitch (rad, nose up). */
    public float tailSwing, tailLift, groundPitch;
}
