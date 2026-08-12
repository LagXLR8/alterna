package com.huwng.alterna.client;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.effect.ModMobEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Matrix4f;

public class TetheredEffectRenderer {

    private static final Identifier TETHER_TEXTURE = Alterna.id("textures/misc/tether_rope.png");
    private static RenderType TETHER_RENDER_TYPE = null;

    private static RenderType getTetherRenderType() {
        if (TETHER_RENDER_TYPE == null) {
            TETHER_RENDER_TYPE = RenderTypes.entityTranslucentEmissive(TETHER_TEXTURE);
        }
        return TETHER_RENDER_TYPE;
    }

    @SuppressWarnings("rawtypes")
    public static void onRenderLiving(RenderLivingEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        var state = event.getRenderState();
        LivingEntity matchedEntity = null;
        double bestDistSq = 16.0; // 4 blocks squared tolerance

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && living.isAlive()) {
                double lx = Mth.lerp((double) state.partialTick, living.xOld, living.getX());
                double ly = Mth.lerp((double) state.partialTick, living.yOld, living.getY());
                double lz = Mth.lerp((double) state.partialTick, living.zOld, living.getZ());

                double dx = lx - state.x;
                double dy = ly - state.y;
                double dz = lz - state.z;
                double distSq = dx * dx + dy * dy + dz * dz;

                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    matchedEntity = living;
                }
            }
        }

        if (matchedEntity == null || matchedEntity.isDeadOrDying()) return;

        var tetherEffect = matchedEntity.getEffect(ModMobEffects.TETHERED);
        if (tetherEffect != null && tetherEffect.getDuration() > 1) {
            Player sourcePlayer = findNearestPlayer(matchedEntity, mc);
            if (sourcePlayer != null) {
                PoseStack poseStack = event.getPoseStack();
                MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

                poseStack.pushPose();

                renderTorsoBox(state.boundingBoxWidth, state.boundingBoxHeight, poseStack, bufferSource);
                renderTetherLine(matchedEntity, sourcePlayer, poseStack, bufferSource, mc);

                poseStack.popPose();

                bufferSource.endBatch(RenderTypes.lines());
                bufferSource.endBatch(getTetherRenderType());
            }
        }
    }

    private static Player findNearestPlayer(LivingEntity entity, Minecraft mc) {
        Player nearest = null;
        double minDist = 25.0;
        for (Player player : mc.level.players()) {
            double dist = player.distanceTo(entity);
            if (dist < minDist) {
                minDist = dist;
                nearest = player;
            }
        }
        return nearest;
    }

    private static void renderTorsoBox(float entityWidth, float entityHeight, PoseStack poseStack, MultiBufferSource buffer) {
        poseStack.pushPose();

        double boxY = entityHeight * 0.5;
        poseStack.translate(0, boxY, 0);

        double width = entityWidth * 0.7;
        double height = entityHeight * 0.4;

        AABB box = new AABB(-width / 2, -height / 2, -width / 2, width / 2, height / 2, width / 2);
        VertexConsumer consumer = buffer.getBuffer(RenderTypes.lines());

        float r = 1.0F, g = 0.3F, b = 0.8F, a = 0.9F;
        renderBox(poseStack, consumer, box, r, g, b, a);

        double glowExpand = 0.03;
        AABB glowBox = box.inflate(glowExpand);
        VertexConsumer glowConsumer = buffer.getBuffer(RenderTypes.lines());
        renderBox(poseStack, glowConsumer, glowBox, r, g, b, a * 0.5F);

        poseStack.popPose();
    }

    private static void renderBox(PoseStack poseStack, VertexConsumer buffer, AABB box, float r, float g, float b, float a) {
        Matrix4f matrix = poseStack.last().pose();

        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        addLine(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        addLine(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        addLine(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        addLine(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        addLine(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        addLine(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        addLine(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        addLine(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        addLine(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        addLine(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        addLine(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        addLine(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    private static void addLine(VertexConsumer buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setNormal(0, 1, 0).setLineWidth(2.0F);
        buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setNormal(0, 1, 0).setLineWidth(2.0F);
    }

    private static void renderTetherLine(LivingEntity entity, Player sourcePlayer,
                                         PoseStack poseStack, MultiBufferSource buffer,
                                         Minecraft mc) {
        poseStack.pushPose();

        Vec3 entityWorldPos = entity.position();
        Vec3 playerWorldPos = sourcePlayer.position();

        Vec3 start = playerWorldPos.subtract(entityWorldPos).add(0, sourcePlayer.getEyeHeight() * 0.5, 0);
        Vec3 end = new Vec3(0, entity.getBbHeight() * 0.5, 0);

        VertexConsumer consumer = buffer.getBuffer(getTetherRenderType());

        Matrix4f pose = poseStack.last().pose();

        float r = 1.0F, g = 0.3F, b = 0.8F, a = 0.95F;
        float ropeWidth = 0.15F;

        Vec3 camPos = mc.getEntityRenderDispatcher().camera.position();

        int segments = 12;
        for (int i = 0; i < segments; i++) {
            float t1 = (float) i / segments;
            float t2 = (float) (i + 1) / segments;

            Vec3 p1 = start.lerp(end, t1);
            Vec3 p2 = start.lerp(end, t2);

            double time = System.currentTimeMillis() * 0.002;
            double wave1 = Math.sin(t1 * Math.PI * 3 + time) * 0.15;
            double wave2 = Math.sin(t2 * Math.PI * 3 + time) * 0.15;

            p1 = p1.add(Math.cos(time) * wave1, wave1 * 0.5, Math.sin(time) * wave1);
            p2 = p2.add(Math.cos(time) * wave2, wave2 * 0.5, Math.sin(time) * wave2);

            Vec3 p1World = p1.add(entityWorldPos);
            Vec3 p2World = p2.add(entityWorldPos);
            Vec3 segmentCenter = p1World.add(p2World).scale(0.5);

            Vec3 dir = p2.subtract(p1).normalize();
            Vec3 toCamera = camPos.subtract(segmentCenter).normalize();
            Vec3 perpendicular = dir.cross(toCamera).normalize().scale(ropeWidth);

            Vec3 v1 = p1.subtract(perpendicular);
            Vec3 v2 = p1.add(perpendicular);
            Vec3 v3 = p2.add(perpendicular);
            Vec3 v4 = p2.subtract(perpendicular);

            float u1 = 0.0F, u2 = 1.0F;
            float v1Coord = t1, v2Coord = t2;
            int lightLevel = 15728880;

            addVertex(consumer, pose, (float) v1.x, (float) v1.y, (float) v1.z, u1, v1Coord, r, g, b, a, lightLevel);
            addVertex(consumer, pose, (float) v2.x, (float) v2.y, (float) v2.z, u2, v1Coord, r, g, b, a, lightLevel);
            addVertex(consumer, pose, (float) v3.x, (float) v3.y, (float) v3.z, u2, v2Coord, r, g, b, a, lightLevel);
            addVertex(consumer, pose, (float) v4.x, (float) v4.y, (float) v4.z, u1, v1Coord, r, g, b, a, lightLevel);

            Vec3 glowPerpendicular = perpendicular.scale(1.3);
            Vec3 gv1 = p1.subtract(glowPerpendicular);
            Vec3 gv2 = p1.add(glowPerpendicular);
            Vec3 gv3 = p2.add(glowPerpendicular);
            Vec3 gv4 = p2.subtract(glowPerpendicular);

            float glowAlpha = a * 0.4F;
            addVertex(consumer, pose, (float) gv1.x, (float) gv1.y, (float) gv1.z, u1, v1Coord, r, g, b, glowAlpha, lightLevel);
            addVertex(consumer, pose, (float) gv2.x, (float) gv2.y, (float) gv2.z, u2, v1Coord, r, g, b, glowAlpha, lightLevel);
            addVertex(consumer, pose, (float) gv3.x, (float) gv3.y, (float) gv3.z, u2, v2Coord, r, g, b, glowAlpha, lightLevel);
            addVertex(consumer, pose, (float) gv4.x, (float) gv4.y, (float) gv4.z, u1, v1Coord, r, g, b, glowAlpha, lightLevel);
        }

        poseStack.popPose();
    }

    private static void addVertex(VertexConsumer consumer, Matrix4f pose,
                                  float x, float y, float z, float u, float v,
                                  float r, float g, float b, float a, int light) {
        consumer.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setUv2(light & 0xFFFF, light >> 16)
                .setNormal(0.0F, 1.0F, 0.0F);
    }
}
