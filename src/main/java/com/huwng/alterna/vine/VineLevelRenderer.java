package com.huwng.alterna.vine;

import com.huwng.alterna.Alterna;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.Collection;

/**
 * 3D Renderer for Vine cables between connected points using moss_block texture.
 * Renders curved 3D vine lines matching the player's curved sliding path.
 */
@EventBusSubscriber(modid = Alterna.MODID, value = Dist.CLIENT)
public class VineLevelRenderer {

    private static final Identifier MOSS_TEXTURE = Identifier.parse("minecraft:textures/block/moss_block.png");
    private static final int SUBDIVISIONS = 24;

    @SubscribeEvent
    public static void onAfterLevel(RenderLevelStageEvent.AfterLevel event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        Collection<VineConnection> connections = VineClientCache.getConnections();
        if (connections.isEmpty()) {
            return;
        }

        Vec3 camPos = mc.gameRenderer.getMainCamera().position();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType renderType = RenderTypes.entityCutout(MOSS_TEXTURE);
        VertexConsumer consumer = buffers.getBuffer(renderType);

        // Combine camera view matrix with world translation
        Matrix4f pose = new Matrix4f(event.getModelViewMatrix());
        pose.translate((float) -camPos.x, (float) -camPos.y, (float) -camPos.z);

        for (VineConnection connection : connections) {
            renderCurvedVineLine(pose, consumer, connection);
        }

        buffers.endBatch(renderType);
    }

    private static void renderCurvedVineLine(Matrix4f pose, VertexConsumer consumer, VineConnection connection) {
        VineStraightCable cable = new VineStraightCable(connection.startVec(), connection.endVec());

        Vec3 prevPoint = cable.getPoint(0.0);
        float vAccum = 0.0f;

        for (int i = 1; i <= SUBDIVISIONS; i++) {
            double progress = (double) i / SUBDIVISIONS;
            Vec3 currPoint = cable.getPoint(progress);

            double segLength = currPoint.distanceTo(prevPoint);
            float nextV = vAccum + (float) (segLength * 1.5);

            renderSegment(pose, consumer, prevPoint, currPoint, vAccum, nextV);

            prevPoint = currPoint;
            vAccum = nextV;
        }
    }

    private static void renderSegment(Matrix4f pose, VertexConsumer consumer, Vec3 start, Vec3 end, float vStart, float vEnd) {
        Vec3 dir = end.subtract(start);
        double length = dir.length();
        if (length < 0.0001) return;

        Vec3 normDir = dir.normalize();

        // Calculate perpendicular vectors for thickness (0.15 blocks thick)
        Vec3 up = new Vec3(0, 1, 0);
        if (Math.abs(normDir.y) > 0.9) {
            up = new Vec3(1, 0, 0);
        }
        Vec3 right = normDir.cross(up).normalize().scale(0.15);
        Vec3 top = normDir.cross(right).normalize().scale(0.15);

        // 4 Quad faces around the line segment
        // Top Face
        addQuad(pose, consumer,
                start.add(top).add(right), start.add(top).subtract(right),
                end.add(top).subtract(right), end.add(top).add(right),
                0, vStart, 1, vEnd, 0, 1, 0);

        // Bottom Face
        addQuad(pose, consumer,
                end.subtract(top).add(right), end.subtract(top).subtract(right),
                start.subtract(top).subtract(right), start.subtract(top).add(right),
                0, vStart, 1, vEnd, 0, -1, 0);

        // Right Face
        addQuad(pose, consumer,
                start.add(right).add(top), start.add(right).subtract(top),
                end.add(right).subtract(top), end.add(right).add(top),
                0, vStart, 1, vEnd, 1, 0, 0);

        // Left Face
        addQuad(pose, consumer,
                end.subtract(right).add(top), end.subtract(right).subtract(top),
                start.subtract(right).subtract(top), start.subtract(right).add(top),
                0, vStart, 1, vEnd, -1, 0, 0);
    }

    private static void addQuad(Matrix4f pose, VertexConsumer consumer,
                                Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4,
                                float u1, float v1, float u2, float v2,
                                float nx, float ny, float nz) {
        int packedLight = 0xF000F0; // Full brightness outdoors
        consumer.addVertex(pose, (float) p1.x, (float) p1.y, (float) p1.z)
                .setColor(255, 255, 255, 255)
                .setUv(u1, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(nx, ny, nz);

        consumer.addVertex(pose, (float) p2.x, (float) p2.y, (float) p2.z)
                .setColor(255, 255, 255, 255)
                .setUv(u2, v1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(nx, ny, nz);

        consumer.addVertex(pose, (float) p3.x, (float) p3.y, (float) p3.z)
                .setColor(255, 255, 255, 255)
                .setUv(u2, v2)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(nx, ny, nz);

        consumer.addVertex(pose, (float) p4.x, (float) p4.y, (float) p4.z)
                .setColor(255, 255, 255, 255)
                .setUv(u1, v2)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(nx, ny, nz);
    }
}
