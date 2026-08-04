package com.huwng.alterna.gravity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.joml.Quaternionf;

/**
 * Dữ liệu trọng lực gắn vào 1 entity (hướng + cường độ + tiến trình xoay mượt).
 * Mutable: Entity#getData(attachment) trả về chính tham chiếu này.
 */
public class GravityData {

    public static final MapCodec<GravityData> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Direction.CODEC.optionalFieldOf("direction", Direction.DOWN).forGetter(GravityData::getDirection),
            Direction.CODEC.optionalFieldOf("prevDirection", Direction.DOWN).forGetter(GravityData::getPrevDirection),
            Codec.DOUBLE.optionalFieldOf("strength", 1.0).forGetter(GravityData::getStrength)
    ).apply(instance, GravityData::new));

    public static final StreamCodec<ByteBuf, GravityData> STREAM_CODEC = StreamCodec.composite(
            Direction.STREAM_CODEC, GravityData::getDirection,
            Direction.STREAM_CODEC, GravityData::getPrevDirection,
            ByteBufCodecs.DOUBLE, GravityData::getStrength,
            GravityData::new
    );

    private Direction direction;
    private Direction prevDirection;
    private double strength;
    private float animationTicks;
    private static final float MAX_ANIMATION_TICKS = 5.0f;

    /**
     * Client-only: quaternion at which the animation starts (not synced, not persisted).
     * Set by LocalPlayerMixin before yaw/pitch adjustment so the camera view is continuous.
     * If null, CameraMixin falls back to getWorldRotationQuaternion(prevDirection).
     */
    private Quaternionf startGravityQuaternion = null;

    public GravityData() {
        this(Direction.DOWN, Direction.DOWN, 1.0);
    }

    public GravityData(Direction direction, double strength) {
        this(direction, direction, strength);
    }

    public GravityData(Direction direction, Direction prevDirection, double strength) {
        this.direction = direction;
        this.prevDirection = prevDirection != null ? prevDirection : direction;
        this.strength = strength;
        this.animationTicks = (this.direction == this.prevDirection) ? MAX_ANIMATION_TICKS : 0.0f;
    }

    public Direction getDirection() {
        return direction;
    }

    public Direction getPrevDirection() {
        return prevDirection != null ? prevDirection : direction;
    }

    public void setDirection(Direction direction) {
        if (this.direction != direction) {
            this.prevDirection = this.direction;
            this.animationTicks = 0.0f;
            this.startGravityQuaternion = null; // reset; client will set before first render
        }
        this.direction = direction;
    }

    /** Client-only: call before starting animation to ensure smooth continuity. */
    public void setStartGravityQuaternion(Quaternionf q) {
        this.startGravityQuaternion = new Quaternionf(q);
    }

    /** Returns the animation start quaternion, or null if not set (use qPrev fallback). */
    public Quaternionf getStartGravityQuaternion() {
        return startGravityQuaternion;
    }

    public void tickAnimation() {
        if (animationTicks < MAX_ANIMATION_TICKS) {
            animationTicks += 1.0f;
            if (animationTicks >= MAX_ANIMATION_TICKS) {
                animationTicks = MAX_ANIMATION_TICKS;
                prevDirection = direction;
            }
        }
    }

    public float getAnimationProgress(float partialTick) {
        if (prevDirection == direction) return 1.0f;
        float progress = (animationTicks + partialTick) / MAX_ANIMATION_TICKS;
        return Math.min(1.0f, Math.max(0.0f, progress));
    }

    public double getStrength() {
        return strength;
    }

    public void setStrength(double strength) {
        this.strength = strength;
    }

    public boolean isNormal() {
        return direction == Direction.DOWN && prevDirection == Direction.DOWN;
    }
}
