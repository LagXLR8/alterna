package com.huwng.alterna.effect;

import com.huwng.alterna.Alterna;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class BottomFreezeMobEffect extends MobEffect {

    public BottomFreezeMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x00FFFF); // Cyan
        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            Alterna.id("effect.bottom_freeze"),
            -1.0D,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0, 1, 0));

        if (entity.tickCount % 2 == 0) { // Every 2 ticks
            for (int i = 0; i < 3; i++) {
                double x = entity.getX() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
                double y = entity.getY() + 0.1;
                double z = entity.getZ() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();

                level.sendParticles(
                    ParticleTypes.SNOWFLAKE,
                    x, y, z,
                    2, 0.1, 0, 0.1, 0
                );
            }
        }
        return true;
    }
}
