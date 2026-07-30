package com.huwng.alterna.client.cloud;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalInt;

/**
 * Localized atmosphere for the rift: a height-fog volume filling the space
 * below {@link #FOG_TOP_Y}, drawn as one fullscreen pass that reads the
 * main depth buffer (see rift_fog.fsh for the ray/slab math). This is NOT
 * the vanilla camera fog - the haze sits at a fixed place in the world,
 * and only inside the rift, because everywhere else below that height is
 * solid rock the view rays never get through.
 *
 * The pass structure copies vanilla's PostPass: color-only render pass (no
 * depth attachment, since the depth texture is being sampled), fullscreen
 * triangle via draw(0, 3), depth bound through the shared sampler cache.
 */
public class RiftFogRenderer {

    /** World Y the haze surface sits at. */
    private static final float FOG_TOP_Y = -50.0F;
    /** Below this the fog is fully saturated - hides the drop into the void. */
    private static final float FOG_BOTTOM_Y = -135.0F;
    /** Beer-Lambert extinction per block of fog crossed. */
    private static final float DENSITY = 0.035F;
    /** Pale mist, slightly cool. Alpha is the overall strength cap. */
    private static final float RED = 0.62F, GREEN = 0.67F, BLUE = 0.74F, ALPHA = 0.85F;

    private static final int UBO_SIZE = new Std140SizeCalculator().putVec4().putVec4().get();

    private final MappableRingBuffer fogInfo = new MappableRingBuffer(
            () -> "Rift Fog Info", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, UBO_SIZE);

    public void render(Vec3 cameraPos, Matrix4fc modelViewMatrix) {
        RenderPipeline pipeline = RiftCloudPipelines.RIFT_FOG;
        if (pipeline == null) {
            return;
        }

        this.fogInfo.rotate();
        try (GpuBuffer.MappedView view = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.fogInfo.currentBuffer(), false, true)) {
            Std140Builder.intoBuffer(view.data())
                    .putVec4(RED, GREEN, BLUE, ALPHA)
                    .putVec4((float) cameraPos.y, FOG_TOP_Y, FOG_BOTTOM_Y, DENSITY);
        }

        // The shader inverts ProjMat * ModelViewMat to unproject depth, so
        // DynamicTransforms must carry the level's real view matrix - same
        // reasoning as the cloud pass.
        var dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(new Matrix4f(modelViewMatrix), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());

        Minecraft mc = Minecraft.getInstance();
        RenderTarget mainTarget = mc.getMainRenderTarget();
        GpuSampler depthSampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);

        // Color-only pass: the depth texture can't be an attachment while
        // it's being sampled.
        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "Rift Fog", mainTarget.getColorTextureView(), OptionalInt.empty())) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setUniform("RiftFogInfo", this.fogInfo.currentBuffer());
            renderPass.bindTexture("DepthSampler", mainTarget.getDepthTextureView(), depthSampler);
            renderPass.draw(0, 3);
        }
    }
}
