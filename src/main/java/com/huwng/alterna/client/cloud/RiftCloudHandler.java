package com.huwng.alterna.client.cloud;

import com.huwng.alterna.Alterna;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Activates {@link RiftCloudRenderer} only when the camera is both near the
 * giant crack's below-bedrock depth range AND actually next to an open
 * (carved) column at cloud height - so this never scans terrain or issues a
 * draw call anywhere else in the world. There's no client-side way to ask
 * GiantCrackParams "is there a crack here" directly (crack shape is a pure
 * function of the world seed, which worldgen code never exposes to
 * rendering, and isn't safely available client-side in multiplayer), so
 * proximity is inferred from real block data instead - which also means
 * this automatically matches the crack's true carved shape, branches
 * included, with no risk of drifting out of sync with GiantCrackParams.
 */
@EventBusSubscriber(modid = Alterna.MODID, value = Dist.CLIENT)
public class RiftCloudHandler {

    /** Base Y of the main blanket - the rift's bedrock mouth. */
    public static final float RIFT_CLOUD_Y = -64.0F;
    private static final int RIFT_CLOUD_COLOR = 0xE6E8ECEE;

    /** Base Y of each layer, matching {@code layers} below by index. */
    private static final float[] LAYER_Y = { RIFT_CLOUD_Y, -20.0F, 30.0F };

    // Active band. The lower bound keeps the scan from running while deep
    // in the abyss below; the upper bound sits above any surface terrain,
    // because the whole point of the layer is being seen from the rift's
    // mouth looking down - the old +20 cap made clouds vanish the moment
    // the camera climbed ~80 blocks above the layer.
    private static final double MIN_ACTIVE_Y = -140.0;
    private static final double MAX_ACTIVE_Y = 340.0;

    // Lazily created on first actual render call, NOT as an eager static
    // field: this class gets classloaded (and its static initializers run)
    // while FML is still scanning for @SubscribeEvent methods, well before
    // GameRenderer/RenderSystem's GpuDevice exists. RiftCloudRenderer's
    // constructor allocates GPU buffers via RenderSystem.getDevice(), so
    // constructing it eagerly here crashes mod loading with
    // ExceptionInInitializerError before the game window even opens.
    /** Temporarily off - see the FOG_ENABLED block in onAfterLevel. */
    private static final boolean FOG_ENABLED = false;

    private static RiftCloudRenderer[] layers;
    private static RiftFogRenderer fog;

    private static RiftCloudRenderer[] createLayers() {
        return new RiftCloudRenderer[] {
                // -64: the dense blanket sealing the bedrock mouth - tiny
                // gaps, wall-hugging, stationary.
                new RiftCloudRenderer(new RiftCloudGenerator(0.14, 0.35, true, 0), 0.0F, 0.6F),
                // -55: sparse, well-separated cloud groups drifting +X.
                // The high gap threshold keeps only coverage peaks, so
                // groups are islands with real space between them.
                new RiftCloudRenderer(new RiftCloudGenerator(0.56, 0.5, false, 1), 0.02F, 0.5F),
                // -40: even sparser groups, drifting the other way a bit
                // faster - the counter-motion between the two upper layers
                // is what makes the stack read as alive.
                new RiftCloudRenderer(new RiftCloudGenerator(0.62, 0.5, false, 2), -0.03F, 0.5F),
        };
    }

    // AfterLevel, NOT AfterSky: at AfterSky the terrain hasn't been drawn
    // into the depth buffer yet, so the puffs pass the depth test
    // everywhere and then every block draws over them - clouds looked like
    // they were behind all terrain. At AfterLevel the main target's depth
    // buffer is complete, so the LEQUAL depth test in the pipeline clips
    // puffs against real geometry correctly.
    @SubscribeEvent
    public static void onAfterLevel(RenderLevelStageEvent.AfterLevel event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null || level.dimension() != Level.OVERWORLD) {
            return;
        }

        Vec3 camPos = mc.gameRenderer.getMainCamera().position();
        if (camPos.y < MIN_ACTIVE_Y || camPos.y > MAX_ACTIVE_Y) {
            return;
        }
        if (!nearOpenColumn(level, camPos)) {
            return;
        }

        // Height-fog pass disabled for now (it wasn't the haze/mist effect
        // the user was after). Flip to true to bring it back - renderer,
        // pipeline and shaders are all still wired up.
        if (FOG_ENABLED) {
            if (fog == null) {
                fog = new RiftFogRenderer();
            }
            fog.render(camPos, event.getModelViewMatrix());
        }

        if (mc.options.getCloudStatus() == CloudStatus.OFF) {
            return;
        }

        if (layers == null) {
            layers = createLayers();
        }

        long gameTime = level.getGameTime();
        float partialTicks = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        // The event's matrix, NOT RenderSystem.getModelViewMatrix(): by the
        // time AfterLevel fires, the global model-view has been reset, and
        // rendering with it makes the whole field stick to the camera
        // (positions get interpreted as view-space instead of world-space).
        // Bottom-up draw order: lower layers are usually the farther ones
        // when looking down into the rift, so this keeps translucent
        // blending roughly back-to-front between layers.
        for (int i = 0; i < layers.length; i++) {
            layers[i].render(level, camPos, LAYER_Y[i], gameTime, partialTicks, RIFT_CLOUD_COLOR, event.getModelViewMatrix());
        }
    }

    /**
     * Sparse sample grid around the camera at cloud height - widened to
     * roughly match {@link RiftCloudRenderer}'s ~240-block radius, so the
     * cloud patch is already active by the time its edge would otherwise
     * become visible, instead of only lighting up once the player is
     * standing right on top of the opening.
     */
    private static boolean nearOpenColumn(ClientLevel level, Vec3 camPos) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int baseX = Mth.floor(camPos.x);
        int baseZ = Mth.floor(camPos.z);
        int y = Mth.floor(RIFT_CLOUD_Y);
        int openFound = 0;
        for (int dx = -220; dx <= 220; dx += 44) {
            for (int dz = -220; dz <= 220; dz += 44) {
                pos.set(baseX + dx, y, baseZ + dz);
                if (level.getBlockState(pos).isAir()) {
                    openFound++;
                    if (openFound >= 2) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
