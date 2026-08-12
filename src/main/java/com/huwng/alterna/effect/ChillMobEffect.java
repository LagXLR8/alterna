package com.huwng.alterna.effect;

import com.huwng.alterna.Alterna;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;

public class ChillMobEffect extends MobEffect {

    public ChillMobEffect() {
        super(MobEffectCategory.HARMFUL, 0xADD8E6); // Light blue
        this.addAttributeModifier(
            Attributes.MOVEMENT_SPEED,
            Alterna.id("effect.chill"),
            -0.5D,
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity.tickCount % 3 == 0) { // Every 3 ticks
            for (int i = 0; i < 2; i++) {
                double x = entity.getX() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
                double y = entity.getY() + level.getRandom().nextDouble() * entity.getBbHeight();
                double z = entity.getZ() + (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();

                level.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
                    x, y, z,
                    1, 0, 0.05, 0, 0.02
                );
            }
        }
        return true;
    }
}
