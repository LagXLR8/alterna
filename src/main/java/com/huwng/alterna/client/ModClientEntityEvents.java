package com.huwng.alterna.client;

import com.huwng.alterna.client.model.CragbackModel;
import com.huwng.alterna.client.model.TetraModel;
import com.huwng.alterna.client.render.CragbackRenderer;
import com.huwng.alterna.client.render.TetraRenderer;
import com.huwng.alterna.entity.ModEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class ModClientEntityEvents {

    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CLIMBING_ZOMBIE.get(), ClimbingZombieRenderer::new);
        event.registerEntityRenderer(ModEntities.CRAGBACK.get(), CragbackRenderer::new);
        event.registerEntityRenderer(ModEntities.TETRA.get(), TetraRenderer::new);
        event.registerEntityRenderer(ModEntities.ARMOR_OVERLORD.get(), com.huwng.alterna.client.render.ArmorOverlordRenderer::new);
        event.registerEntityRenderer(ModEntities.GRAPPLE_HOOK.get(), com.huwng.alterna.client.render.GrappleHookRenderer::new);
    }

    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModelLayers.CRAGBACK, CragbackModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.TETRA, TetraModel::createBodyLayer);
        event.registerLayerDefinition(ModModelLayers.CRAGBACK_BABY, CragbackModel::createBabyLayer);
        event.registerLayerDefinition(ModModelLayers.TETRA_BABY, TetraModel::createBabyLayer);
        event.registerLayerDefinition(ModModelLayers.ARMOR_OVERLORD, com.huwng.alterna.client.model.ArmorOverlordModel::createBodyLayer);
    }
}
