package com.huwng.alterna.entity;

import com.huwng.alterna.Alterna;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Alterna.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ClimbingZombieEntity>> CLIMBING_ZOMBIE =
            ENTITY_TYPES.register("climbing_zombie",
                    () -> EntityType.Builder.<ClimbingZombieEntity>of(ClimbingZombieEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(8)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Alterna.MODID, "climbing_zombie"))));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
