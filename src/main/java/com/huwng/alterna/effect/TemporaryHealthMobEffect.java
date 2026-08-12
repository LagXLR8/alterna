package com.huwng.alterna.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

public class TemporaryHealthMobEffect extends MobEffect {

    public TemporaryHealthMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFFFF); // White
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        super.onEffectStarted(entity, amplifier);
        float desiredAbsorption = (amplifier + 1) * 2.0F; // +2 absorption health (1 heart) per level
        if (entity.getAbsorptionAmount() < desiredAbsorption) {
            entity.setAbsorptionAmount(desiredAbsorption);
        }
    }

    @Override
    public void removeAttributeModifiers(AttributeMap attributeMap) {
        super.removeAttributeModifiers(attributeMap);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        float desiredAbsorption = (amplifier + 1) * 2.0F;
        if (entity.getAbsorptionAmount() < desiredAbsorption) {
            entity.setAbsorptionAmount(desiredAbsorption);
        }

        // Slowly decay absorption every 3 seconds (60 ticks)
        if (entity.tickCount % 60 == 0) {
            float currentAbsorption = entity.getAbsorptionAmount();
            if (currentAbsorption > 0) {
                entity.setAbsorptionAmount(Math.max(0, currentAbsorption - 2.0F));
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        return true;
    }
}
