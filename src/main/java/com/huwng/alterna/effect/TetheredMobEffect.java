package com.huwng.alterna.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

public class TetheredMobEffect extends MobEffect {
    public TetheredMobEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF33CC); // Pink purple
    }

    @Override
    public void removeAttributeModifiers(AttributeMap attributeMap) {
        super.removeAttributeModifiers(attributeMap);
    }

    public void onEffectEnded(LivingEntity entity) {
        if (!entity.level().isClientSide()) {
            entity.refreshDimensions();
        }
    }
}
