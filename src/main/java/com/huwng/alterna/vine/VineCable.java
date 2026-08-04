package com.huwng.alterna.vine;

import java.util.Collection;
import java.util.Collections;
import net.minecraft.world.phys.Vec3;

/**
 * Represents a cable/vine that players can slide along.
 * Ported from Zipline mod (MIT) — credit: Evan, Tomate0613.
 */
public interface VineCable {
    double getProgress(Vec3 playerPos);

    Vec3 getPoint(double progress);

    Vec3 getClosestPoint(Vec3 pos);

    Vec3 direction(double progress);

    double length();

    default Collection<VineCable> getNext(boolean forward) {
        return Collections.emptyList();
    }

    default boolean isValid() {
        return true;
    }
}
