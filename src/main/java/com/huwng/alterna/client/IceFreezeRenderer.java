package com.huwng.alterna.client;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.effect.ModMobEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Matrix4f;

public class IceFreezeRenderer {

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

        if (matchedEntity == null) return;

        if (matchedEntity.hasEffect(ModMobEffects.BOTTOM_FREEZE)) {
            MobEffectInstance freezeEffect = matchedEntity.getEffect(ModMobEffects.BOTTOM_FREEZE);
            if (freezeEffect != null && freezeEffect.getDuration() > 0) {
                PoseStack poseStack = event.getPoseStack();
                MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

                poseStack.pushPose();
                poseStack.translate(0, 0.01, 0);

                TextureAtlasSprite iceTexture = mc.getModelManager()
                        .getBlockStateModelSet()
                        .getParticleMaterial(Blocks.ICE.defaultBlockState())
                        .sprite();

                VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.translucentMovingBlock());

                float width = state.boundingBoxWidth;
                float height = state.boundingBoxHeight * 0.4F;

                float alpha = Math.min(1.0F, freezeEffect.getDuration() / 10.0F);
                if (alpha > 0.7F) alpha = 0.7F;

                renderIceCube(poseStack, buffer, width, height, iceTexture, alpha);

                poseStack.popPose();
                bufferSource.endBatch(RenderTypes.translucentMovingBlock());
            }
        }
    }

    private static void renderIceCube(PoseStack poseStack, VertexConsumer buffer, float width, float height,
                                       TextureAtlasSprite texture, float alpha) {
        Matrix4f matrix = poseStack.last().pose();

        float halfWidth = width / 2.0F;
        float r = 0.8F, g = 0.9F, b = 1.0F;

        float minU = texture.getU0();
        float maxU = texture.getU1();
        float minV = texture.getV0();
        float maxV = texture.getV1();

        // Bottom face (Y-)
        addVertex(buffer, matrix, -halfWidth, 0, -halfWidth, minU, minV, 0, -1, 0, r, g, b, alpha);
        addVertex(buffer, matrix, halfWidth, 0, -halfWidth, maxU, minV, 0, -1, 0, r, g, b, alpha);
        addVertex(buffer, matrix, halfWidth, 0, halfWidth, maxU, maxV, 0, -1, 0, r, g, b, alpha);
        addVertex(buffer, matrix, -halfWidth, 0, halfWidth, minU, maxV, 0, -1, 0, r, g, b, alpha);

        // Top face (Y+)
        addVertex(buffer, matrix, -halfWidth, height, halfWidth, minU, maxV, 0, 1, 0, r, g, b, alpha);
        addVertex(buffer, matrix, halfWidth, height, halfWidth, maxU, maxV, 0, 1, 0, r, g, b, alpha);
        addVertex(buffer, matrix, halfWidth, height, -halfWidth, maxU, minV, 0, 1, 0, r, g, b, alpha);
        addVertex(buffer, matrix, -halfWidth, height, -halfWidth, minU, minV, 0, 1, 0, r, g, b, alpha);

        // North face (Z-)
        addVertex(buffer, matrix, -halfWidth, 0, -halfWidth, minU, maxV, 0, 0, -1, r, g, b, alpha);
        addVertex(buffer, matrix, -halfWidth, height, -halfWidth, minU, minV, 0, 0, -1, r, g, b, alpha);
        addVertex(buffer, matrix, halfWidth, height, -halfWidth, maxU, minV, 0, 0, -1, r, g, b, alpha);
        addVertex(buffer, matrix, halfWidth, 0, -halfWidth, maxU, maxV, 0, 0, -1, r, g, b, alpha);

        // South face (Z+)
        addVertex(buffer, matrix, halfWidth, 0, halfWidth, maxU, maxV, 0, 0, 1, r, g, b, alpha);
        addVertex(buffer, matrix, halfWidth, height, halfWidth, maxU, minV, 0, 0, 1, r, g, b, alpha);
        addVertex(buffer, matrix, -halfWidth, height, halfWidth, minU, minV, 0, 0, 1, r, g, b, alpha);
        addVertex(buffer, matrix, -halfWidth, 0, halfWidth, minU, maxV, 0, 0, 1, r, g, b, alpha);

        // East face (X+)
        addVertex(buffer, matrix, halfWidth, 0, -halfWidth, minU, maxV, 1, 0, 0, r, g, b, alpha);
        addVertex(buffer, matrix, halfWidth, height, -halfWidth, minU, minV, 1, 0, 0, r, g, b, alpha);
        addVertex(buffer, matrix, halfWidth, height, halfWidth, maxU, minV, 1, 0, 0, r, g, b, alpha);
        addVertex(buffer, matrix, halfWidth, 0, halfWidth, maxU, maxV, 1, 0, 0, r, g, b, alpha);

        // West face (X-)
        addVertex(buffer, matrix, -halfWidth, 0, halfWidth, maxU, maxV, -1, 0, 0, r, g, b, alpha);
        addVertex(buffer, matrix, -halfWidth, height, halfWidth, maxU, minV, -1, 0, 0, r, g, b, alpha);
        addVertex(buffer, matrix, -halfWidth, height, -halfWidth, minU, minV, -1, 0, 0, r, g, b, alpha);
        addVertex(buffer, matrix, -halfWidth, 0, -halfWidth, minU, maxV, -1, 0, 0, r, g, b, alpha);
    }

    private static void addVertex(VertexConsumer buffer, Matrix4f matrix,
                                   float x, float y, float z, float u, float v,
                                   float nx, float ny, float nz,
                                   float r, float g, float b, float a) {
        buffer.addVertex(matrix, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setUv2(240, 240)
                .setNormal(nx, ny, nz);
    }
}
