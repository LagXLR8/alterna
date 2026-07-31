package com.huwng.alterna;

import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Per-player data that isn't saved anywhere else. We use NeoForge's
 * Data Attachment system so it survives across ticks (it does NOT need to
 * survive death/respawn, so we don't bother making it persistent/serialized).
 */
public class AlternaAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Alterna.MODID);

    // How many blocks the player has fallen since dropping below the Abyss
    // trigger height (see VoidFallHandler.VOID_START_Y). Reset whenever the
    // player is no longer below that height, or once it crosses the threshold
    // and the teleport fires.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Double>> VOID_FALL_DISTANCE =
            ATTACHMENT_TYPES.register("void_fall_distance",
                    () -> AttachmentType.builder(() -> 0.0d)
                            .serialize(Codec.DOUBLE.fieldOf("value"))
                            .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<com.huwng.alterna.gravity.GravityData>> GRAVITY_DATA =
            ATTACHMENT_TYPES.register("gravity_data",
                    () -> AttachmentType.builder(com.huwng.alterna.gravity.GravityData::new)
                            .serialize(com.huwng.alterna.gravity.GravityData.CODEC)
                            .sync(com.huwng.alterna.gravity.GravityData.STREAM_CODEC)
                            .build());


}
