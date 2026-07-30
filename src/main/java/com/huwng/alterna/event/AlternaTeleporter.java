package com.huwng.alterna.event;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.AlternaDimensions;
import com.huwng.alterna.network.StartVoidTeleportPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Set;

public class AlternaTeleporter {

    private AlternaTeleporter() {
    }

    public static void sendToAbyss(ServerPlayer player) {
        // player.getServer() / player.server both failed to compile on 26.1
        // (likely renamed again along with the rest of the encapsulation
        // pass). ServerLifecycleHooks is a NeoForge-maintained utility that
        // has been stable across many versions specifically so mods don't
        // have to chase vanilla's internal renames for this.
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ServerLevel abyss = server.getLevel(AlternaDimensions.ABYSS);
        if (abyss == null) {
            // ResourceKey#location was renamed to #identifier in 26.1.
            Alterna.LOGGER.warn("Alterna: dimension {} is not loaded, cannot teleport {}",
                    AlternaDimensions.ABYSS.identifier(), player.getName().getString());
            return;
        }

        // Fired first so the client can already be covering the screen with
        // VoidFadeOverlay by the time the vanilla respawn/dimension-change
        // packet arrives a tick or two later.
        PacketDistributor.sendToPlayer(player, new StartVoidTeleportPayload());

        double x = AlternaDimensions.ABYSS_SPAWN_X + 0.5;
        double y = AlternaDimensions.ABYSS_SPAWN_Y;
        double z = AlternaDimensions.ABYSS_SPAWN_Z + 0.5;

        // Cross-dimension teleport. Empty relative-movement set = absolute
        // coordinates. Keep the player's current look direction.
        //
        // NOTE: your compiler reported this overload now takes an 8th
        // trailing `boolean` param that didn't exist in 1.21.x
        // (teleportTo(ServerLevel, double, double, double, Set<Relative>, float, float, boolean)).
        // I don't have a confirmed answer for what that flag controls in
        // 26.1 (candidates: whether to keep the camera entity, whether to
        // avoid suffocation checks, etc). `false` is a safe-ish default -
        // hover over the parameter in your IDE (or check the 26.1 source)
        // and adjust if the teleport behaves oddly (e.g. player is fine but
        // camera detaches, or spawn point checks are skipped).
        player.teleportTo(abyss, x, y, z, Set.<Relative>of(), player.getYRot(), player.getXRot(), false);

        // Prevent fall damage from the 1000-block drop from applying once
        // they land in the Abyss, but keep their downward momentum so the
        // "falling through" feeling continues uninterrupted into the new world.
        player.resetFallDistance();
    }
}
