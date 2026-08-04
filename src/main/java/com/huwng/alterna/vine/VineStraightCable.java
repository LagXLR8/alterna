package com.huwng.alterna.vine;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A curved vine cable between two points with natural gravity sag.
 * Ported from Zipline mod (MIT) — credit: Evan, Tomate0613.
 */
public record VineStraightCable(Vec3 start, Vec3 end, Vec3 direction, double length) implements VineCable {
    public VineStraightCable(Vec3 start, Vec3 end) {
        this(start, end, end.subtract(start).normalize(), start.distanceTo(end));
    }

    @Override
    public double getProgress(Vec3 playerPos) {
        Vec3 playerToStart = playerPos.subtract(this.start);
        double t = playerToStart.dot(this.direction) / this.length;
        t = Mth.clamp(t, 0.0, 1.0);
        return t;
    }

    @Override
    public Vec3 getPoint(double progress) {
        Vec3 linearPoint = this.start.add(this.direction.scale(progress * this.length));
        double sagAmount = Math.min(3.0, this.length * 0.08);
        double sagY = 4.0 * progress * (1.0 - progress) * sagAmount;
        return new Vec3(linearPoint.x, linearPoint.y - sagY, linearPoint.z);
    }

    @Override
    public Vec3 getClosestPoint(Vec3 pos) {
        double t = this.getProgress(pos);
        return this.getPoint(t);
    }

    @Override
    public Vec3 direction(double progress) {
        Vec3 p1 = getPoint(Math.max(0.0, progress - 0.01));
        Vec3 p2 = getPoint(Math.min(1.0, progress + 0.01));
        return p2.subtract(p1).normalize();
    }
}
