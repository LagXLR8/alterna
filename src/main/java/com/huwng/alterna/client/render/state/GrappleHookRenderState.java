package com.huwng.alterna.client.render.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;

public class GrappleHookRenderState extends EntityRenderState {
    /** From the claw to the owner's hand. */
    public Vec3 lineOriginOffset = Vec3.ZERO;
    public boolean anchored;
}
