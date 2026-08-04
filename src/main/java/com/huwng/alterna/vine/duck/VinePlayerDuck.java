package com.huwng.alterna.vine.duck;

import com.huwng.alterna.vine.VineCable;
import net.minecraft.world.phys.Vec3;

/**
 * Duck interface injected into Player via mixin to store vine-sliding state.
 * Ported from Zipline mod (MIT) — credit: Evan, Tomate0613.
 */
public interface VinePlayerDuck {
    VineCable vine$getCable();
    void vine$setCable(VineCable cable);

    double vine$getSpeed();
    void vine$setSpeed(double speed);

    double vine$getProgress();
    void vine$setProgress(double progress);

    int vine$getDirectionFactor();
    void vine$setDirectionFactor(int factor);

    Vec3 vine$getLastDir();
    void vine$setLastDir(Vec3 dir);

    boolean vine$isActuallyUsing();
    void vine$setActuallyUsing(boolean using);
}
