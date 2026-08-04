package com.huwng.alterna.vine;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Registry and lookup for vine cable providers.
 * Ported from Zipline mod (MIT) — credit: Evan, Tomate0613.
 */
public class VineCables {
    private static final List<CableProvider> providers = new ArrayList<>();

    public static @Nullable VineCable getClosestCable(Level level, Vec3 offsetPlayerPos, double radius) {
        double nearestDist = radius * radius;
        VineCable nearestCable = null;

        for (CableProvider provider : providers) {
            VineCable cable = provider.getNearestCable(level, offsetPlayerPos, nearestDist);
            if (cable != null) {
                Vec3 closestPoint = cable.getClosestPoint(offsetPlayerPos);
                double distance = closestPoint.distanceToSqr(offsetPlayerPos);
                if (distance < nearestDist) {
                    nearestCable = cable;
                    nearestDist = distance;
                }
            }
        }

        if (nearestCable != null && !nearestCable.isValid()) {
            return null;
        }

        return nearestCable;
    }

    public static void registerProvider(CableProvider provider) {
        providers.add(provider);
    }

    @FunctionalInterface
    public interface CableProvider {
        @Nullable VineCable getNearestCable(Level level, Vec3 pos, double maxDistSqr);
    }
}
