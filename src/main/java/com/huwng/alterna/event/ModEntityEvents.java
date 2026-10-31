package com.huwng.alterna.event;

import com.huwng.alterna.entity.ClimbingZombieEntity;
import com.huwng.alterna.entity.ModEntities;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

public class ModEntityEvents {

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.CLIMBING_ZOMBIE.get(), ClimbingZombieEntity.createAttributes().build());
        event.put(ModEntities.CRAGBACK.get(), com.huwng.alterna.entity.CragbackEntity.createAttributes().build());
        event.put(ModEntities.TETRA.get(), com.huwng.alterna.entity.TetraEntity.createAttributes().build());
        event.put(ModEntities.ARMOR_OVERLORD.get(), com.huwng.alterna.entity.ArmorOverlordEntity.createAttributes().build());
    }

    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(ModEntities.CLIMBING_ZOMBIE.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.CRAGBACK.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.huwng.alterna.entity.CragbackEntity::checkCragbackSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.TETRA.get(),
                SpawnPlacementTypes.NO_RESTRICTIONS,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                com.huwng.alterna.entity.TetraEntity::checkTetraSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
