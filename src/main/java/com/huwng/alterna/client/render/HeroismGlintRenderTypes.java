package com.huwng.alterna.client.render;

import com.huwng.alterna.Alterna;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.resources.Identifier;

public class HeroismGlintRenderTypes {

    public static final Identifier HEROISM_GLINT_TEXTURE = Alterna.id("textures/misc/heroism_glint.png");

    public static final RenderType HEROISM_GLINT = RenderType.create(
        "heroism_glint",
        RenderSetup.builder(RenderPipelines.GLINT)
            .withTexture("Sampler0", HEROISM_GLINT_TEXTURE)
            .setTextureTransform(TextureTransform.GLINT_TEXTURING)
            .createRenderSetup()
    );

    public static final RenderType HEROISM_GLINT_DEBUG = RenderType.create(
        "heroism_glint_debug",
        RenderSetup.builder(RenderPipelines.GLINT)
            .withTexture("Sampler0", Identifier.withDefaultNamespace("textures/misc/enchanted_glint_item.png"))
            .setTextureTransform(TextureTransform.GLINT_TEXTURING)
            .createRenderSetup()
    );

    public static final RenderType HEROISM_GLINT_TRANSLUCENT = RenderType.create(
        "heroism_glint_translucent",
        RenderSetup.builder(RenderPipelines.GLINT)
            .withTexture("Sampler0", HEROISM_GLINT_TEXTURE)
            .setTextureTransform(TextureTransform.GLINT_TEXTURING)
            .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
            .createRenderSetup()
    );

    public static final RenderType HEROISM_ENTITY_GLINT = RenderType.create(
        "heroism_entity_glint",
        RenderSetup.builder(RenderPipelines.GLINT)
            .withTexture("Sampler0", HEROISM_GLINT_TEXTURE)
            .setTextureTransform(TextureTransform.ENTITY_GLINT_TEXTURING)
            .createRenderSetup()
    );

    public static final RenderType HEROISM_ARMOR_ENTITY_GLINT = RenderType.create(
        "heroism_armor_entity_glint",
        RenderSetup.builder(RenderPipelines.GLINT)
            .withTexture("Sampler0", HEROISM_GLINT_TEXTURE)
            .setTextureTransform(TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .createRenderSetup()
    );
}
