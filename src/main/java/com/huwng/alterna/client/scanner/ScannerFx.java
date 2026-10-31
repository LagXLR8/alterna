package com.huwng.alterna.client.scanner;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * World-space visuals of the Bio-Scanner:
 *  - a glowing beam from the scanner to the target;
 *  - for plants, a holographic skin laid over every block of the plant (the whole tree for
 *    the big ones), using the same scrolling scan-line texture as the creature skin
 *    (creatures get theirs from ScanOverlayLayer, drawn over their own model);
 *  - on completion the skin flashes bright and fades.
 *
 * Note: at this render stage the camera rotation is already on the render system's model-view
 * stack, so geometry is given in camera-relative world coordinates (no extra view matrix).
 */
public final class ScannerFx {
    private ScannerFx() {}

    public static void onRenderLevel(RenderLevelStageEvent.AfterTranslucentParticles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (mc.level == null || player == null) return;
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float time = mc.level.getGameTime() + partial;
        boolean scanning = ScannerClient.isScanning() && ScannerClient.fxPoint != null;
        float flash = ScannerClient.flashFade(time);
        if (!scanning && (flash <= 0.0F || ScannerClient.flashShell == null)) return;

        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        Matrix4f pose = new Matrix4f().translate((float) -cam.x, (float) -cam.y, (float) -cam.z);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        // ---- holographic skin over the plant
        RenderType skin = RenderTypes.energySwirl(ScanOverlayLayer.TEXTURE, time * 0.003F % 1.0F, -time * 0.02F % 1.0F);
        boolean drewSkin = false;
        if (scanning && ScannerClient.plantShell != null && ScannerClient.hasEntry()) {
            float s = ScannerClient.skinStrength(ScannerClient.progressFraction(partial), time);
            shell(buffers.getBuffer(skin), pose, ScannerClient.plantShell, s);
            drewSkin = true;
        }
        if (flash > 0.0F && ScannerClient.flashShell != null) {
            shell(buffers.getBuffer(skin), pose, ScannerClient.flashShell, flash);
            drewSkin = true;
        }
        if (drewSkin) buffers.endBatch(skin);

        // ---- beam
        if (scanning) {
            RenderType quadsType = RenderTypes.debugQuads();
            Vec3 to = ScannerClient.fxPoint;
            if (ScannerClient.fxEntity != null) {
                var e = ScannerClient.fxEntity;
                to = e.getPosition(partial).add(0, e.getBbHeight() * 0.5, 0);
            }
            beam(buffers.getBuffer(quadsType), pose, cam, handPos(player, partial), to, time, ScannerClient.hasEntry());
            buffers.endBatch(quadsType);
        }
    }

    // ------------------------------------------------------------------ plant skin
    private static void shell(VertexConsumer c, Matrix4f m, PlantShell shell, float strength) {
        int v = (int) (Mth.clamp(strength, 0.0F, 1.0F) * 255);
        int r = (int) (v * 0.35F), g = (int) (v * 0.9F), b = v;
        for (float[] q : shell.quads) {
            float nx = q[12], ny = q[13], nz = q[14];
            for (int i = 0; i < 4; i++) {
                float x = q[i * 3], y = q[i * 3 + 1], z = q[i * 3 + 2];
                // texture coordinates from world position, so the scan lines run continuously
                // across neighbouring blocks (4 blocks per texture repeat)
                float u = ny != 0 ? x * 0.25F : (x + z) * 0.25F;
                float w = ny != 0 ? z * 0.25F : y * 0.25F;
                c.addVertex(m, x, y, z).setColor(r, g, b, 255).setUv(u, w).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(0xF000F0).setNormal(nx, ny, nz);
            }
        }
    }

    // ------------------------------------------------------------------ beam
    /** Roughly where the scanner sits in the player's hand. */
    private static Vec3 handPos(LocalPlayer player, float partial) {
        Vec3 eye = player.getEyePosition(partial);
        Vec3 look = player.getViewVector(partial);
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = right.cross(look).normalize();
        boolean rightHand = (player.getUsedItemHand() == InteractionHand.MAIN_HAND) == (player.getMainArm() == HumanoidArm.RIGHT);
        return eye.add(look.scale(0.5)).add(right.scale(rightHand ? 0.26 : -0.26)).add(up.scale(-0.2));
    }

    private static void beam(VertexConsumer c, Matrix4f m, Vec3 cam, Vec3 from, Vec3 to, float time, boolean known) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 0.1) return;
        Vec3 dir = d.scale(1.0 / len);
        int segments = Math.max(8, (int) (len * 5));
        float strength = known ? 1.0F : 0.4F;
        for (int i = 0; i < segments; i++) {
            double t0 = i / (double) segments, t1 = (i + 1) / (double) segments;
            Vec3 p0 = from.add(d.scale(t0)), p1 = from.add(d.scale(t1));
            Vec3 side = dir.cross(p0.subtract(cam)).normalize();   // ribbon faces the camera
            float pulse = 0.6F + 0.4F * Mth.sin((float) (t0 * len * 4.0 - time * 0.8));
            float fade = (float) Mth.clamp(t0 * len / 0.5, 0.0, 1.0);
            ribbon(c, m, p0, p1, side, 0.012, strength * fade * (0.7F + 0.3F * pulse));
            ribbon(c, m, p0, p1, side, 0.05, strength * fade * 0.18F * pulse);
        }
    }

    private static void ribbon(VertexConsumer c, Matrix4f m, Vec3 p0, Vec3 p1, Vec3 side, double halfWidth, float alpha) {
        Vec3 s = side.scale(halfWidth);
        int a = (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255);
        vertex(c, m, p0.add(s), a);
        vertex(c, m, p0.subtract(s), a);
        vertex(c, m, p1.subtract(s), a);
        vertex(c, m, p1.add(s), a);
    }

    private static void vertex(VertexConsumer c, Matrix4f m, Vec3 p, int alpha) {
        c.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(80, 225, 255, alpha);
    }
}
