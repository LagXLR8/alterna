package com.huwng.alterna.client;

import com.huwng.alterna.entity.ModEntities;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class ModClientEntityEvents {

    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CLIMBING_ZOMBIE.get(), ClimbingZombieRenderer::new);
    }
}
