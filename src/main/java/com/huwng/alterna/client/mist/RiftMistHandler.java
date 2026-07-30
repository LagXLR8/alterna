package com.huwng.alterna.client.mist;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.AlternaParticles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/**
 * Spawner for the rift haze, modeled on Atmospherics' AmbientMistEmitter:
 * every tick a few spawn attempts pick random spots in a shell around the
 * player, and any spot that is open air inside the mist band becomes a
 * mist particle. The band starts at {@link #MIST_TOP_Y} and runs
 * downward - and since everything outside the rift below that height is
 * solid rock, the air check alone confines the haze to the rift interior.
 */
@EventBusSubscriber(modid = Alterna.MODID, value = Dist.CLIENT)
public class RiftMistHandler {

    /** Temporarily off on request - flip to true to bring the haze back. */
    private static final boolean MIST_ENABLED = false;

    /** Haze exists from here downward. */
    private static final double MIST_TOP_Y = -20.0;
    /** Lower cap so the band tracks the player instead of filling the abyss. */
    private static final double MIST_BOTTOM_Y = -160.0;

    /** Player must be within reach of the band for the emitter to run. */
    private static final double MIN_ACTIVE_Y = -200.0;
    private static final double MAX_ACTIVE_Y = 40.0;

    private static final int ATTEMPTS_PER_TICK = 3;
    private static final double SPAWN_RADIUS = 28.0;
    private static final double MIN_SPAWN_DISTANCE = 6.0;

    private static final RandomSource RANDOM = RandomSource.create();

    @SubscribeEvent
    public static void onRegisterProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(AlternaParticles.RIFT_MIST.get(), RiftMistParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!MIST_ENABLED) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null || level.dimension() != Level.OVERWORLD || mc.isPaused()) {
            return;
        }
        double py = player.getY();
        if (py < MIN_ACTIVE_Y || py > MAX_ACTIVE_Y) {
            return;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < ATTEMPTS_PER_TICK; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double dist = MIN_SPAWN_DISTANCE + RANDOM.nextDouble() * (SPAWN_RADIUS - MIN_SPAWN_DISTANCE);
            double x = player.getX() + Math.cos(angle) * dist;
            double z = player.getZ() + Math.sin(angle) * dist;

            // Random y around the player, clamped into the mist band.
            double y = py + (RANDOM.nextDouble() - 0.5) * 36.0;
            y = Mth.clamp(y, MIST_BOTTOM_Y, MIST_TOP_Y - 1.0);

            pos.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
            if (!level.getBlockState(pos).isAir()) {
                continue;
            }

            level.addParticle(AlternaParticles.RIFT_MIST.get(), x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
