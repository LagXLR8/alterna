package com.huwng.alterna.gravity;

import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class RotationUtil {

    private static final Quaternionf[] WORLD_ROTATION_QUATERNIONS = new Quaternionf[6];
    private static final Quaternionf[] ENTITY_ROTATION_QUATERNIONS = new Quaternionf[6];
    private static final Direction[][] DIR_WORLD_TO_PLAYER = new Direction[6][];
    private static final Direction[][] DIR_PLAYER_TO_WORLD = new Direction[6][];

    public static Direction dirWorldToPlayer(Direction direction, Direction gravityDirection) {
        return DIR_WORLD_TO_PLAYER[gravityDirection.get3DDataValue()][direction.get3DDataValue()];
    }

    public static Direction dirPlayerToWorld(Direction direction, Direction gravityDirection) {
        return DIR_PLAYER_TO_WORLD[gravityDirection.get3DDataValue()][direction.get3DDataValue()];
    }

    static {
        for (Direction gravityDirection : Direction.values()) {
            DIR_WORLD_TO_PLAYER[gravityDirection.get3DDataValue()] = new Direction[6];
            for (Direction direction : Direction.values()) {
                Vec3 v = vecWorldToPlayer(direction.getUnitVec3(), gravityDirection);
                DIR_WORLD_TO_PLAYER[gravityDirection.get3DDataValue()][direction.get3DDataValue()] =
                        Direction.getNearest((int) Math.round(v.x), (int) Math.round(v.y), (int) Math.round(v.z), direction);
            }
        }
        for (Direction gravityDirection : Direction.values()) {
            DIR_PLAYER_TO_WORLD[gravityDirection.get3DDataValue()] = new Direction[6];
            for (Direction direction : Direction.values()) {
                Vec3 v = vecPlayerToWorld(direction.getUnitVec3(), gravityDirection);
                DIR_PLAYER_TO_WORLD[gravityDirection.get3DDataValue()][direction.get3DDataValue()] =
                        Direction.getNearest((int) Math.round(v.x), (int) Math.round(v.y), (int) Math.round(v.z), direction);
            }
        }
    }

    static {
        WORLD_ROTATION_QUATERNIONS[0] = new Quaternionf();
        WORLD_ROTATION_QUATERNIONS[1] = Axis.ZP.rotationDegrees(-180.0f);
        WORLD_ROTATION_QUATERNIONS[2] = Axis.XP.rotationDegrees(-90.0f);
        WORLD_ROTATION_QUATERNIONS[3] = Axis.XP.rotationDegrees(-90.0f);
        WORLD_ROTATION_QUATERNIONS[3].mul(Axis.YP.rotationDegrees(-180.0f));
        WORLD_ROTATION_QUATERNIONS[4] = Axis.XP.rotationDegrees(-90.0f);
        WORLD_ROTATION_QUATERNIONS[4].mul(Axis.YP.rotationDegrees(-90.0f));
        WORLD_ROTATION_QUATERNIONS[5] = Axis.XP.rotationDegrees(-90.0f);
        WORLD_ROTATION_QUATERNIONS[5].mul(Axis.YP.rotationDegrees(-270.0f));

        for (int i = 0; i < 6; ++i) {
            ENTITY_ROTATION_QUATERNIONS[i] = new Quaternionf().set(WORLD_ROTATION_QUATERNIONS[i]).conjugate();
        }
    }

    public static Vec3 vecWorldToPlayer(double x, double y, double z, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN -> new Vec3(x, y, z);
            case UP -> new Vec3(-x, -y, z);
            case NORTH -> new Vec3(x, z, -y);
            case SOUTH -> new Vec3(-x, -z, -y);
            case WEST -> new Vec3(-z, x, -y);
            case EAST -> new Vec3(z, -x, -y);
        };
    }

    public static Vec3 vecWorldToPlayer(Vec3 vec, Direction gravityDirection) {
        return vecWorldToPlayer(vec.x, vec.y, vec.z, gravityDirection);
    }

    public static Vec3 vecPlayerToWorld(double x, double y, double z, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN -> new Vec3(x, y, z);
            case UP -> new Vec3(-x, -y, z);
            case NORTH -> new Vec3(x, -z, y);
            case SOUTH -> new Vec3(-x, -z, -y);
            case WEST -> new Vec3(y, -z, -x);
            case EAST -> new Vec3(-y, -z, x);
        };
    }

    public static Vec3 vecPlayerToWorld(Vec3 vec, Direction gravityDirection) {
        return vecPlayerToWorld(vec.x, vec.y, vec.z, gravityDirection);
    }

    public static Vec2 rotWorldToPlayer(float yaw, float pitch, Direction gravityDirection) {
        Vec3 vec3d = vecWorldToPlayer(rotToVec(yaw, pitch), gravityDirection);
        return vecToRot(vec3d.x, vec3d.y, vec3d.z);
    }

    public static Vec2 rotPlayerToWorld(float yaw, float pitch, Direction gravityDirection) {
        Vec3 vec3d = vecPlayerToWorld(rotToVec(yaw, pitch), gravityDirection);
        return vecToRot(vec3d.x, vec3d.y, vec3d.z);
    }

    public static Vec3 rotToVec(float yaw, float pitch) {
        double radPitch = (double) pitch * 0.017453292;
        double radNegYaw = (double) (-yaw) * 0.017453292;
        double cosNegYaw = Math.cos(radNegYaw);
        double sinNegYaw = Math.sin(radNegYaw);
        double cosPitch = Math.cos(radPitch);
        double sinPitch = Math.sin(radPitch);
        return new Vec3(sinNegYaw * cosPitch, -sinPitch, cosNegYaw * cosPitch);
    }

    public static Vec2 vecToRot(double x, double y, double z) {
        // LƯU Ý: bản cũ dùng asin/acos rồi chia cho cosPitch -> chia cho 0 / NaN
        // khi pitch tiến gần ±90 độ (nhìn thẳng lên/xuống ngay lúc trọng lực đổi
        // hướng qua GravityCoreBlock). atan2 không cần chia nên ổn định ở mọi góc.
        double horizontalLength = Math.sqrt(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) (-(Math.atan2(y, horizontalLength) * (180.0 / Math.PI)));
        return new Vec2(Mth.wrapDegrees(yaw), pitch);
    }

    public static Vec2 vecToRot(Vec3 vec) {
        return vecToRot(vec.x, vec.y, vec.z);
    }

    public static AABB boxWorldToPlayer(AABB box, Direction gravityDirection) {
        return new AABB(
                vecWorldToPlayer(box.minX, box.minY, box.minZ, gravityDirection),
                vecWorldToPlayer(box.maxX, box.maxY, box.maxZ, gravityDirection)
        );
    }

    public static AABB boxPlayerToWorld(AABB box, Direction gravityDirection) {
        return new AABB(
                vecPlayerToWorld(box.minX, box.minY, box.minZ, gravityDirection),
                vecPlayerToWorld(box.maxX, box.maxY, box.maxZ, gravityDirection)
        );
    }

    public static AABB makeBoxFromDimensions(EntityDimensions dimensions, Direction gravityDirection, Vec3 pos) {
        if (gravityDirection == Direction.DOWN) {
            return dimensions.makeBoundingBox(pos);
        }
        float halfWidth = dimensions.width() / 2.0F;
        AABB localBox = new AABB(-halfWidth, 0.0F, -halfWidth, halfWidth, dimensions.height(), halfWidth);
        return boxPlayerToWorld(localBox, gravityDirection).move(pos);
    }

    public static Quaternionf getWorldRotationQuaternion(Direction gravityDirection) {
        return WORLD_ROTATION_QUATERNIONS[gravityDirection.get3DDataValue()];
    }

    public static Quaternionf getCameraRotationQuaternion(Direction gravityDirection) {
        return ENTITY_ROTATION_QUATERNIONS[gravityDirection.get3DDataValue()];
    }
}
