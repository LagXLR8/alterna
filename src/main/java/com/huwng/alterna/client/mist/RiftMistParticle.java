package com.huwng.alterna.client.mist;

import com.huwng.alterna.client.cloud.RiftCloudPipelines;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * One big, slow, soft mist quad - the rift's haze is hundreds of these
 * hanging in the open air below the mouth, modeled on Atmospherics'
 * AmbientMistParticle (CAVE profile: ~8-block quads, alpha ~0.35, 5-9s
 * lifetimes, barely-there drift and roll). Alpha ramps in over the first
 * fifth of the lifetime and back out over the last third, so puffs
 * materialize and dissolve instead of popping.
 *
 * No @OnlyIn: this class lives purely in client-side code paths already
 * (only the client mist handler ever touches it), and NeoForge warns
 * against mods using the runtime member-stripping annotation themselves.
 */
public class RiftMistParticle extends SingleQuadParticle {

    // Custom layer over vanilla's TRANSLUCENT: same atlas, but a pipeline
    // that does NOT write depth (see RiftCloudPipelines.RIFT_MIST_PARTICLE)
    // so mist puffs blend into each other and never occlude the cloud
    // pass's depth test. Lazily created because the pipeline is only
    // registered once RegisterRenderPipelinesEvent has fired.
    private static SingleQuadParticle.Layer mistLayer;

    private static SingleQuadParticle.Layer layer() {
        if (mistLayer == null) {
            mistLayer = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, RiftCloudPipelines.RIFT_MIST_PARTICLE);
        }
        return mistLayer;
    }

    private final float maxAlpha;

    private RiftMistParticle(ClientLevel level, double x, double y, double z, float quadSize, float maxAlpha, int lifetime) {
        super(level, x, y, z, null);
        this.quadSize = quadSize;
        this.maxAlpha = maxAlpha;
        this.lifetime = lifetime;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.xd = (this.random.nextDouble() - 0.5) * 0.004;
        this.yd = (this.random.nextDouble() - 0.5) * 0.002;
        this.zd = (this.random.nextDouble() - 0.5) * 0.004;
        this.roll = this.random.nextFloat() * (float) (Math.PI * 2.0);
        this.oRoll = this.roll;
        this.alpha = 0.0F;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.oRoll = this.roll;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        this.roll += 0.0008F;
        this.move(this.xd, this.yd, this.zd);

        float t = (float) this.age / (float) this.lifetime;
        float fadeIn = Mth.clamp(t / 0.2F, 0.0F, 1.0F);
        float fadeOut = Mth.clamp((1.0F - t) / 0.35F, 0.0F, 1.0F);
        this.alpha = this.maxAlpha * fadeIn * fadeOut;
    }

    @Override
    protected int getLightCoords(float partialTick) {
        // The rift interior is pitch dark; give the mist a block-light
        // floor so it stays faintly visible instead of rendering black.
        int light = super.getLightCoords(partialTick);
        int block = light & 0xFFFF;
        int floor = 8 << 4;
        return Math.max(block, floor) | (light & 0xFFFF0000);
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return layer();
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z,
                                       double xAux, double yAux, double zAux, RandomSource random) {
            float quadSize = 3.5F + random.nextFloat() * 3.0F;
            float maxAlpha = 1F;
            int lifetime = 140 + random.nextInt(100);
            RiftMistParticle particle = new RiftMistParticle(level, x, y, z, quadSize, maxAlpha, lifetime);
            particle.setSprite(this.sprites.get(random));
            return particle;
        }
    }
}
