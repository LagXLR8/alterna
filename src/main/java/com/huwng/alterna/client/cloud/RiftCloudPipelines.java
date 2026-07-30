package com.huwng.alterna.client.cloud;

import com.huwng.alterna.Alterna;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

/**
 * Pipeline for the Better Clouds-style rift cloud field. Puff data rides in
 * a texel buffer ("CloudPuffs", RGBA8, two texels per puff) rather than a
 * UBO, because a UBO's guaranteed minimum size (16KB) caps out around ~900
 * vec4s while the BC look needs thousands of puffs - same reason vanilla's
 * own cloud renderer feeds "CloudFaces" through a texel buffer.
 *
 * Fired on the mod event bus (RegisterRenderPipelinesEvent implements
 * IModBusEvent); the plain {@code @EventBusSubscriber} annotation routes it
 * there automatically based on the event type.
 */
@EventBusSubscriber(modid = Alterna.MODID, value = Dist.CLIENT)
public class RiftCloudPipelines {

    public static RenderPipeline RIFT_CLOUDS;
    public static RenderPipeline RIFT_FOG;
    public static RenderPipeline RIFT_MIST_PARTICLE;

    @SubscribeEvent
    public static void onRegisterPipelines(RegisterRenderPipelinesEvent event) {
        // Mist particle pipeline: vanilla's TRANSLUCENT_PARTICLE with the
        // depth WRITE turned off. The vanilla pipeline writes depth, which
        // made overlapping mist quads hard-clip each other instead of
        // blending, and stamped the depth buffer so the cloud pass (which
        // depth-tests at AfterLevel) went invisible behind every puff of
        // mist.
        RIFT_MIST_PARTICLE = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(Alterna.MODID, "pipeline/rift_mist_particle"))
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                .build();
        event.registerPipeline(RIFT_MIST_PARTICLE);
        // Fullscreen height-fog pass (see rift_fog.fsh): samples the main
        // depth buffer, so its render pass runs color-only with NO depth
        // attachment and the pipeline itself has no depth/stencil state -
        // same shape as vanilla's post-processing pipelines.
        RIFT_FOG = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET, RenderPipelines.GLOBALS_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(Alterna.MODID, "pipeline/rift_fog"))
                .withVertexShader(Identifier.fromNamespaceAndPath(Alterna.MODID, "core/rift_fog"))
                .withFragmentShader(Identifier.fromNamespaceAndPath(Alterna.MODID, "core/rift_fog"))
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.TRIANGLES)
                .withUniform("RiftFogInfo", UniformType.UNIFORM_BUFFER)
                .withSampler("DepthSampler")
                .withCull(false)
                .build();
        event.registerPipeline(RIFT_FOG);
        RIFT_CLOUDS = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(Alterna.MODID, "pipeline/rift_clouds"))
                .withVertexShader(Identifier.fromNamespaceAndPath(Alterna.MODID, "core/rift_clouds"))
                .withFragmentShader(Identifier.fromNamespaceAndPath(Alterna.MODID, "core/rift_clouds"))
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withVertexFormat(DefaultVertexFormat.EMPTY, VertexFormat.Mode.QUADS)
                .withUniform("CloudInfo", UniformType.UNIFORM_BUFFER)
                .withUniform("CloudPuffs", UniformType.TEXEL_BUFFER, TextureFormat.RGBA8)
                // Depth test keeps clouds behind terrain, but no depth
                // write: thousands of translucent puffs must blend into
                // each other, not z-clip. Backface culling stays at the
                // default (on) - the puffs are boxes using vanilla's cloud
                // vertex winding, and skipping their back faces halves the
                // overdraw.
                .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
                .build();

        event.registerPipeline(RIFT_CLOUDS);
    }
}
