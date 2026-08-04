package com.huwng.alterna.vine;

import com.huwng.alterna.Alterna;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Tag definitions for the vine system.
 */
public class VineTags {
    /** Items that can be used to attach to and slide along vines (e.g. stick). */
    public static final TagKey<Item> VINE_ATTACHMENT = TagKey.create(
            Registries.ITEM, Alterna.id("vine_attachment"));

    /** Blocks that form vine cables (e.g. leaves). */
    public static final TagKey<Block> VINE_CABLE = TagKey.create(
            Registries.BLOCK, Alterna.id("vine_cable"));
}
