package com.huwng.alterna.mixin;

import com.huwng.alterna.vine.VineCable;
import com.huwng.alterna.vine.duck.VinePlayerDuck;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Injects vine-sliding state fields into Player via duck interface.
 * Ported from Zipline mod (MIT) — credit: Evan, Tomate0613.
 */
@Mixin(Player.class)
public class VinePlayerMixin implements VinePlayerDuck {
    @Unique
    private VineCable vine$cable;
    @Unique
    private double vine$speed;
    @Unique
    private double vine$progress;
    @Unique
    private int vine$directionFactor;
    @Unique
    private Vec3 vine$lastDir;
    @Unique
    private boolean vine$actuallyUsing;

    @Override
    public VineCable vine$getCable() {
        return this.vine$cable;
    }

    @Override
    public void vine$setCable(VineCable cable) {
        this.vine$cable = cable;
    }

    @Override
    public double vine$getSpeed() {
        return this.vine$speed;
    }

    @Override
    public void vine$setSpeed(double speed) {
        this.vine$speed = speed;
    }

    @Override
    public double vine$getProgress() {
        return this.vine$progress;
    }

    @Override
    public void vine$setProgress(double progress) {
        this.vine$progress = progress;
    }

    @Override
    public int vine$getDirectionFactor() {
        return this.vine$directionFactor;
    }

    @Override
    public void vine$setDirectionFactor(int factor) {
        this.vine$directionFactor = factor;
    }

    @Override
    public Vec3 vine$getLastDir() {
        return this.vine$lastDir;
    }

    @Override
    public void vine$setLastDir(Vec3 dir) {
        this.vine$lastDir = dir;
    }

    @Override
    public boolean vine$isActuallyUsing() {
        return this.vine$actuallyUsing;
    }

    @Override
    public void vine$setActuallyUsing(boolean using) {
        this.vine$actuallyUsing = using;
    }
}
