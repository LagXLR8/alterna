package com.huwng.alterna.client.render;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.enchantment.ModEnchantments;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public enum ModGlintType {
    NONE(null, null),
    HEROISM("heroism", ModEnchantments.HEROISM),
    CHILLING("chilling", ModEnchantments.CHILLING),
    ELASTIC("elastic", ModEnchantments.ELASTIC),
    GLUTTONY("gluttony", ModEnchantments.GLUTTONY),
    VAMPIRISM("vampirism", ModEnchantments.VAMPIRISM),
    WILD("wild", ModEnchantments.WILD),
    DEFERRED("deferred", ModEnchantments.DEFERRED),
    DETONATION("detonation", ModEnchantments.DETONATION),
    STORMBREAKER("stormbreaker", ModEnchantments.STORMBREAKER),
    CURSE_OF_DROWNED_CAPTAIN("curse_of_drowned", ModEnchantments.CURSE_OF_DROWNED_CAPTAIN),
    CURSE_OF_REJECTION("curse_of_rejection", ModEnchantments.CURSE_OF_REJECTION),
    CURSE_OF_BLOODLUST("curse_of_bloodlust", ModEnchantments.CURSE_OF_BLOODLUST),
    CURSE_OF_THE_NO_LIFE_KING("curse_of_the_no_life_king", ModEnchantments.CURSE_OF_THE_NO_LIFE_KING);

    private final String name;
    private final ResourceKey<Enchantment> enchantmentKey;
    private RenderType glint;
    private RenderType glintTranslucent;
    private RenderType entityGlint;

    ModGlintType(String name, ResourceKey<Enchantment> enchantmentKey) {
        this.name = name;
        this.enchantmentKey = enchantmentKey;
    }

    public static void init() {
        for (ModGlintType type : values()) {
            if (type == NONE) continue;
            Identifier texture = Alterna.id("textures/misc/" + type.name + "_glint.png");

            type.glint = RenderType.create(
                "alterna_" + type.name + "_glint",
                RenderSetup.builder(RenderPipelines.GLINT)
                    .withTexture("Sampler0", texture)
                    .setTextureTransform(TextureTransform.GLINT_TEXTURING)
                    .createRenderSetup()
            );

            type.glintTranslucent = RenderType.create(
                "alterna_" + type.name + "_glint_translucent",
                RenderSetup.builder(RenderPipelines.GLINT)
                    .withTexture("Sampler0", texture)
                    .setTextureTransform(TextureTransform.GLINT_TEXTURING)
                    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .createRenderSetup()
            );

            type.entityGlint = RenderType.create(
                "alterna_" + type.name + "_entity_glint",
                RenderSetup.builder(RenderPipelines.GLINT)
                    .withTexture("Sampler0", texture)
                    .setTextureTransform(TextureTransform.ENTITY_GLINT_TEXTURING)
                    .createRenderSetup()
            );
        }
    }

    public RenderType getGlint() {
        return glint;
    }

    public RenderType getGlintTranslucent() {
        return glintTranslucent;
    }

    public RenderType getEntityGlint() {
        return entityGlint;
    }

    public static ModGlintType fromStack(ItemStack stack) {
        if (stack.isEmpty() || !com.huwng.alterna.Config.ENABLE_ENCHANTMENT_GLINT.get()) return NONE;

        if (stack.getItem() instanceof com.huwng.alterna.item.EnchantStoneItem stone) {
            ResourceKey<Enchantment> key = stone.getEnchantmentKey();
            for (ModGlintType type : values()) {
                if (type != NONE && type.enchantmentKey.equals(key)) {
                    return type;
                }
            }
        }

        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) return NONE;

        for (Holder<Enchantment> holder : enchantments.keySet()) {
            for (ModGlintType type : values()) {
                if (type != NONE && holder.is(type.enchantmentKey)) {
                    return type;
                }
            }
        }

        return NONE;
    }

}
