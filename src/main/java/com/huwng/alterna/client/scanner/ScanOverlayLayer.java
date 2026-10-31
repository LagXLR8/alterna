package com.huwng.alterna.client.scanner;

import com.huwng.alterna.Alterna;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Holographic scan skin drawn over the creature being scanned (like Subnautica's scanner):
 * the creature's own model is drawn again with a glowing, scrolling scan-line texture.
 * It brightens as the scan progresses and flashes when it completes.
 */
public class ScanOverlayLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {
    static final Identifier TEXTURE = Alterna.id("textures/misc/scan_overlay.png");

    public ScanOverlayLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, S state, float yRot, float xRot) {
        float strength = ScannerClient.overlayStrengthFor(state);
        if (strength <= 0.0F) return;
        float t = state.ageInTicks;
        int c = (int) (Mth.clamp(strength, 0.0F, 1.0F) * 255);
        int color = ARGB.color(255, (int) (c * 0.35F), (int) (c * 0.9F), c);
        collector.order(1).submitModel(this.getParentModel(), state, poseStack,
                RenderTypes.energySwirl(TEXTURE, t * 0.003F % 1.0F, -t * 0.02F % 1.0F),
                lightCoords, OverlayTexture.NO_OVERLAY, color, null, state.outlineColor, null);
    }

    /** True when this render state is the scanned entity (matched by type and interpolated position). */
    static boolean matches(LivingEntityRenderState state, Entity e, float partial) {
        if (state.entityType != e.getType()) return false;
        double x = Mth.lerp(partial, e.xOld, e.getX()), y = Mth.lerp(partial, e.yOld, e.getY()), z = Mth.lerp(partial, e.zOld, e.getZ());
        return Math.abs(state.x - x) < 0.05 && Math.abs(state.y - y) < 0.05 && Math.abs(state.z - z) < 0.05;
    }

    static float partial() {
        return Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }
}
