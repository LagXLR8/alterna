package com.huwng.alterna.vine;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Cable provider that looks up active 3D vine connections in the level.
 */
public class VineCableProvider implements VineCables.CableProvider {

    @Override
    public @Nullable VineCable getNearestCable(Level level, Vec3 pos, double maxDistSqr) {
        Collection<VineConnection> connections;
        if (level instanceof ServerLevel serverLevel) {
            connections = VineSavedData.get(serverLevel).getConnections();
        } else {
            connections = VineClientCache.getConnections();
        }

        VineCable nearestCable = null;
        double nearestDistSqr = maxDistSqr;

        for (VineConnection connection : connections) {
            VineStraightCable cable = new VineStraightCable(connection.startVec(), connection.endVec());
            Vec3 closestPoint = cable.getClosestPoint(pos);
            double distSqr = closestPoint.distanceToSqr(pos);
            if (distSqr < nearestDistSqr) {
                nearestDistSqr = distSqr;
                nearestCable = cable;
            }
        }

        return nearestCable;
    }
}
