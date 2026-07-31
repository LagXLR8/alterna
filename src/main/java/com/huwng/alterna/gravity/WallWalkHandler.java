package com.huwng.alterna.gravity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class WallWalkHandler {

    /**
     * Tự động kiểm tra và chuyển hướng trọng lực cho phép người chơi đi mượt mà liên tục trên cả 6 mặt khối block.
     */
    public static void tickWallWalk(LivingEntity entity) {
        if (entity == null || entity.level() == null || entity.isSpectator()) return;

        Level level = entity.level();
        Direction currentDir = GravityApi.getDirection(entity);
        Vec3 vel = entity.getDeltaMovement();

        // 1. Kiểm tra va chạm góc trong (Inner Corner): Đi húc vào bức tường trước mặt
        if (entity.horizontalCollision) {
            Vec3 lookVec = entity.getLookAngle();
            for (Direction dir : Direction.values()) {
                if (dir == currentDir || dir == currentDir.getOpposite()) continue;

                BlockPos checkPos = entity.blockPosition().relative(dir);
                BlockState state = level.getBlockState(checkPos);
                if (state.isFaceSturdy(level, checkPos, dir.getOpposite())) {
                    double dot = lookVec.x * dir.getStepX() + lookVec.y * dir.getStepY() + lookVec.z * dir.getStepZ();
                    if (dot > 0.1) {
                        GravityApi.setDirection(entity, dir);
                        return;
                    }
                }
            }
        }

        // 2. Kiểm tra đi qua mép ranh giới khối góc ngoài (Outer Corner / 360 Wrap):
        // CHỈ chuyển hướng nếu người chơi thực sự đang di chuyển về phía bề mặt candidateDir
        if (!entity.onGround() && vel.lengthSqr() > 1.0E-4) {
            Vec3 movementDir = vel.normalize();
            for (Direction candidateDir : Direction.values()) {
                if (candidateDir == currentDir || candidateDir == currentDir.getOpposite()) continue;

                // Kiểm tra Gate hướng di chuyển: người chơi phải thực sự di chuyển về phía candidateDir (dot > 0.1)
                double dot = movementDir.x * candidateDir.getStepX() + movementDir.y * candidateDir.getStepY() + movementDir.z * candidateDir.getStepZ();
                if (dot <= 0.1) continue;

                BlockPos feetPos = BlockPos.containing(entity.position().add(candidateDir.getUnitVec3().scale(-0.5)));
                BlockState state = level.getBlockState(feetPos);
                if (state.isFaceSturdy(level, feetPos, candidateDir.getOpposite())) {
                    GravityApi.setDirection(entity, candidateDir);
                    return;
                }
            }
        }
    }
}
