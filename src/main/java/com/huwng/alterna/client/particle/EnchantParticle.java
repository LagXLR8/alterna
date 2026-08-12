package com.huwng.alterna.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
public class EnchantParticle extends SingleQuadParticle {

    public static class EnchantParticleProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public EnchantParticleProvider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, RandomSource random) {
            return new EnchantParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
        }
    }

    private final float rotationSpeed;

    protected EnchantParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
        super(world, x, y, z, vx, vy, vz, spriteSet.get(world.getRandom()));
        this.quadSize = 0.05f + world.getRandom().nextFloat() * 0.1f;
        this.lifetime = 60;
        this.gravity = 0f;
        this.hasPhysics = false;

        // Purple color
        this.rCol = 139f / 255f;
        this.gCol = 92f / 255f;
        this.bCol = 246f / 255f;
        this.alpha = 0.6f;

        this.rotationSpeed = (world.getRandom().nextFloat() - 0.5f) * 0.1f;
        this.oRoll = world.getRandom().nextFloat() * (float) Math.PI * 2f;
        this.roll = this.oRoll;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        this.oRoll = this.roll;

        double wobbleX = Math.sin(this.age * 0.1) * 0.01;
        double wobbleY = Math.cos(this.age * 0.15) * 0.01;
        double wobbleZ = Math.sin(this.age * 0.12) * 0.01;

        this.x += wobbleX;
        this.y += wobbleY;
        this.z += wobbleZ;

        this.roll += this.rotationSpeed;

        if (this.age > this.lifetime - 10) {
            this.alpha = 0.6f * (this.lifetime - this.age) / 10f;
        }
    }
}
