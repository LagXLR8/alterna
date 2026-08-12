package com.huwng.alterna.client.render.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public class ThrownStormbreakerRenderState extends EntityRenderState {
    public float yRot;
    public float xRot;
    public float spinTicks;
    public final ItemStackRenderState item = new ItemStackRenderState();
}
