package com.huwng.alterna.client.cloud;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * GPU half of the Better Clouds-style rift cloud field; the puff set
 * itself comes from {@link RiftCloudGenerator}. Draw-call plumbing
 * (MappableRingBuffer triple-buffering, sequential quad index buffer,
 * clouds render target) mirrors vanilla's CloudRenderer; the pipeline and
 * shaders are our own ({@link RiftCloudPipelines#RIFT_CLOUDS}).
 *
 * Drift/camera motion strategy: puff positions are stored in "cloud space"
 * (world + accumulated travel), quantized relative to a patch origin
 * chosen at build time. Every frame the CPU recomputes, in doubles, the
 * camera-relative position of that origin (folding in both camera movement
 * and cloud travel since the build) and hands it to the shader as one
 * vec3 - so between rebuilds the field moves perfectly smoothly, and a
 * rebuild only refreshes WHICH puffs exist, never visibly teleports them.
 */
public class RiftCloudRenderer implements AutoCloseable {

    private static final int CLOUD_INFO_SIZE = new Std140SizeCalculator()
            .putVec4() // CloudColor
            .putVec4() // Geometry
            .putVec4() // Origin + wave time
            .putVec4() // Params
            .get();
    private static final int PUFF_BUFFER_SIZE = RiftCloudGenerator.MAX_PUFFS * RiftCloudGenerator.BYTES_PER_PUFF;

    private static final float EDGE_FADE_START = RiftCloudGenerator.PATCH_RADIUS * 0.70F;

    /** Rebuild when the camera strays this far from the build position... */
    private static final double REBUILD_CAMERA_DISTANCE = 8.0;
    /** ...or the field has drifted this far past the build snapshot. */
    private static final double REBUILD_DRIFT_DISTANCE = 4.0;

    // One renderer instance = one cloud layer.
    private final RiftCloudGenerator generator;
    /** Blocks per tick along +X (negative = -X); 0 = layer stands still. */
    private final float travelSpeed;
    /** Alpha of a single puff; full density comes from overlap stacking. */
    private final float puffOpacity;

    private final MappableRingBuffer cloudInfo = new MappableRingBuffer(
            () -> "Rift Cloud Info", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, CLOUD_INFO_SIZE);
    private final MappableRingBuffer puffBuffer = new MappableRingBuffer(
            () -> "Rift Cloud Puffs", GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER | GpuBuffer.USAGE_MAP_WRITE, PUFF_BUFFER_SIZE);

    // Private Fog UBO with FogCloudsEnd pushed to ~infinity. The shared Fog
    // uniform is built from the player's render-distance option, which
    // would fade the far half of a 240-block patch to nothing regardless
    // of what we draw (the original "can't render far" bug).
    private final MappableRingBuffer farFog;

    private boolean needsRebuild = true;
    private double buildCamX;
    private double buildCamZ;
    private double buildDrift;
    private int puffCount = 0;

    public RiftCloudRenderer(RiftCloudGenerator generator, float travelSpeed, float puffOpacity) {
        this.generator = generator;
        this.travelSpeed = travelSpeed;
        this.puffOpacity = puffOpacity;
        this.farFog = new MappableRingBuffer(
                () -> "Rift Cloud Far Fog", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE, FogRenderer.FOG_UBO_SIZE);
        try (GpuBuffer.MappedView view = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.farFog.currentBuffer(), false, true)) {
            // Field order must match fog.glsl's Fog block: vec4 FogColor,
            // then EnvironmentalStart/End, RenderDistanceStart/End,
            // SkyEnd, CloudsEnd.
            Std140Builder.intoBuffer(view.data())
                    .putVec4(0.0F, 0.0F, 0.0F, 0.0F)
                    .putFloat(1.0E7F)
                    .putFloat(1.0E7F)
                    .putFloat(1.0E7F)
                    .putFloat(1.0E7F)
                    .putFloat(1.0E7F)
                    .putFloat(1.0E7F);
        }
    }

    public void render(ClientLevel level, Vec3 cameraPos, float cloudY, long gameTime, float partialTicks, int argbColor,
                       Matrix4fc modelViewMatrix) {
        RenderPipeline pipeline = RiftCloudPipelines.RIFT_CLOUDS;
        if (pipeline == null) {
            return;
        }

        double driftTotal = (gameTime + partialTicks) * (double) this.travelSpeed;

        if (this.needsRebuild
                || Math.abs(cameraPos.x - this.buildCamX) > REBUILD_CAMERA_DISTANCE
                || Math.abs(cameraPos.z - this.buildCamZ) > REBUILD_CAMERA_DISTANCE
                || Math.abs(driftTotal - this.buildDrift) > REBUILD_DRIFT_DISTANCE) {
            this.needsRebuild = false;
            this.buildCamX = cameraPos.x;
            this.buildCamZ = cameraPos.z;
            this.buildDrift = driftTotal;
            this.puffBuffer.rotate();

            try (GpuBuffer.MappedView view = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.puffBuffer.currentBuffer(), false, true)) {
                this.puffCount = this.generator.build(level, view.data(), cameraPos.x, cameraPos.z, cloudY, driftTotal);
            }
        }

        if (this.puffCount == 0) {
            return;
        }

        // Camera-relative patch origin, folding in all drift since the
        // build - computed in doubles so nothing snaps or jitters.
        float originRelX = (float) (this.generator.originCsX() - driftTotal - cameraPos.x);
        float originRelY = (float) (cloudY - cameraPos.y);
        float originRelZ = (float) (this.generator.originCsZ() - cameraPos.z);
        float waveTime = (float) (gameTime % 24000L) + partialTicks;

        this.cloudInfo.rotate();
        try (GpuBuffer.MappedView view = RenderSystem.getDevice().createCommandEncoder().mapBuffer(this.cloudInfo.currentBuffer(), false, true)) {
            Std140Builder.intoBuffer(view.data())
                    .putVec4(ARGB.vector4fFromARGB32(argbColor))
                    .putVec4(RiftCloudGenerator.PATCH_SIZE, RiftCloudGenerator.SIZE_XZ,
                            RiftCloudGenerator.SIZE_Y, RiftCloudGenerator.LIFT_RANGE)
                    .putVec4(originRelX, originRelY, originRelZ, waveTime)
                    .putVec4(RiftCloudGenerator.SCALE_FALLOFF_MIN, RiftCloudGenerator.PATCH_RADIUS,
                            EDGE_FADE_START, this.puffOpacity);
        }

        var dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(new Matrix4f(modelViewMatrix), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());

        Minecraft mc = Minecraft.getInstance();
        // Always the main target: this runs at RenderLevelStageEvent
        // .AfterLevel, when the vanilla clouds target has already been
        // composited into the main one. Drawing there instead would go
        // nowhere - and the main target is also the only one whose depth
        // buffer holds the finished terrain, which is what clips puffs
        // correctly against blocks.
        RenderTarget mainTarget = mc.getMainRenderTarget();
        // 6 quad faces per puff box (24 verts -> 36 indices).
        int quadCount = 6 * this.puffCount;
        RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer indexBuffer = indices.getBuffer(6 * quadCount);

        GpuTextureView colorTexture = mainTarget.getColorTextureView();
        GpuTextureView depthTexture = mainTarget.getDepthTextureView();

        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "Rift Clouds", colorTexture, OptionalInt.empty(), depthTexture, OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setUniform("Fog", this.farFog.currentBuffer());
            renderPass.setIndexBuffer(indexBuffer, indices.type());
            renderPass.setUniform("CloudInfo", this.cloudInfo.currentBuffer());
            renderPass.setUniform("CloudPuffs", this.puffBuffer.currentBuffer());
            renderPass.drawIndexed(0, 0, 6 * quadCount, 1);
        }
    }

    public void markForRebuild() {
        this.needsRebuild = true;
    }

    @Override
    public void close() {
        this.cloudInfo.close();
        this.puffBuffer.close();
        this.farFog.close();
    }
}
