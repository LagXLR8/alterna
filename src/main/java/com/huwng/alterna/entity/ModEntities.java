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

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownStormbreakerEntity>> THROWN_STORMBREAKER =
            ENTITY_TYPES.register("thrown_stormbreaker",
                    () -> EntityType.Builder.<ThrownStormbreakerEntity>of(ThrownStormbreakerEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Alterna.MODID, "thrown_stormbreaker"))));

    /** Lichen Savanna grazer - a heavily armoured, peaceful herd animal. */
    public static final DeferredHolder<EntityType<?>, EntityType<CragbackEntity>> CRAGBACK =
            ENTITY_TYPES.register("cragback",
                    () -> EntityType.Builder.<CragbackEntity>of(CragbackEntity::new, MobCategory.CREATURE)
                            .sized(2.4F, 2.35F)
                            .eyeHeight(1.9F)
                            .clientTrackingRange(10)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Alterna.MODID, "cragback"))));

    /** Four-winged flock flier that nests on the cliffs above the Lichen Savanna. */
    public static final DeferredHolder<EntityType<?>, EntityType<TetraEntity>> TETRA =
            ENTITY_TYPES.register("tetra",
                    () -> EntityType.Builder.<TetraEntity>of(TetraEntity::new, MobCategory.CREATURE)
                            .sized(1.1F, 0.9F)
                            .eyeHeight(0.6F)
                            .clientTrackingRange(12)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Alterna.MODID, "tetra"))));

    /** Apex hunter of the Rootshroom Forest: a five-block-tall capped spider with hooked arms. */
    public static final DeferredHolder<EntityType<?>, EntityType<ArmorOverlordEntity>> ARMOR_OVERLORD =
            ENTITY_TYPES.register("armor_overlord",
                    () -> EntityType.Builder.<ArmorOverlordEntity>of(ArmorOverlordEntity::new, MobCategory.MONSTER)
                            .sized(1.4F, 4.4F)
                            .eyeHeight(2.4F)
                            .clientTrackingRange(12)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Alterna.MODID, "armor_overlord"))));

    /** The claw fired by the Overlord Grapple. */
    public static final DeferredHolder<EntityType<?>, EntityType<GrappleHookEntity>> GRAPPLE_HOOK =
            ENTITY_TYPES.register("grapple_hook",
                    () -> EntityType.Builder.<GrappleHookEntity>of(GrappleHookEntity::new, MobCategory.MISC)
                            .sized(0.3F, 0.3F)
                            .noSave()
                            .clientTrackingRange(6)
                            .updateInterval(1)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Alterna.MODID, "grapple_hook"))));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
