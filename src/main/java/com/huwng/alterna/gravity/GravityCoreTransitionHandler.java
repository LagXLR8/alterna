package com.huwng.alterna.gravity;

import com.huwng.alterna.Alterna;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Port từ GravityCoreTransitionHandler của Gravity Changer, thích ứng sang
 * GravityApi/GravityCoreTracker của alterna. Đây là phần xử lý chính xác
 * việc "đi vòng cạnh" của GravityCoreBlock: phát hiện lúc entity sắp bước/rơi
 * qua mép khối, xoay vận tốc + hướng nhìn quanh cạnh đó, tìm 1 vị trí không
 * va chạm ở mặt kế tiếp rồi mới đổi hướng trọng lực + dịch chuyển entity sang.
 * <p>
 * Khác biệt so với bản gốc:
 * - GravityDirectionUtil.getOwnGravityDirection/setGravityDirection ->
 * GravityApi
 * (không cần syncGravityDirectionBeforeTeleport bằng
 * ClientboundUpdateAttributesPacket
 * vì GravityData là NeoForge Data Attachment, tự động sync khi setData()).
 * - Không có ModEntityTags.GRAVITY_FIXED (alterna chưa có hệ thống tag này);
 * nếu bạn cần chặn 1 số entity không cho đổi hướng qua core, thêm điều kiện
 * tương ứng ở đầu tryTransition()/tryTransitionOnFall().
 */
public final class GravityCoreTransitionHandler {

    private static final double MIN_EDGE_SPEED = 0.01;
    private static final double EDGE_LOOKAHEAD_MARGIN = 0.05;
    private static final double SUPPORT_OFFSET = 1.0E-3;
    // Số tick "nghỉ" bắt buộc sau mỗi lần đổi mặt trước khi cho phép transition
    // tiếp theo. Vị trí đích luôn được đặt gần sát mép (chỉ cách SUPPORT_OFFSET),
    // nên nếu không có cooldown, ngay tick kế tiếp hệ thống có thể lại tưởng
    // entity "sắp vượt mép" (vì vẫn còn quá gần) và bật transition ngược lại,
    // gây giật liên tục / kẹt tại chỗ vĩnh viễn.
    private static final int TRANSITION_COOLDOWN_TICKS = 4;

    private GravityCoreTransitionHandler() {
    }

    /**
     * Gọi mỗi tick (LivingEntity#tick) để bắt trường hợp rơi/nhảy gần mép, không
     * chỉ lúc đứng yên trên core.
     */
    public static void tryTransitionOnFall(Level level, LivingEntity entity) {
        if (!(level instanceof ServerLevel serverLevel))
            return;
        if (!(entity instanceof GravityCoreTracker tracker))
            return;
        if (!entity.isAlive() || entity.isPassenger() || entity.isNoGravity() || entity.noPhysics)
            return;

        BlockPos corePos = tracker.alterna$getLastGravityCore();
        if (corePos == null)
            return;

        if (entity.tickCount - tracker.alterna$getLastTransitionTick() < TRANSITION_COOLDOWN_TICKS) {
            return;
        }

        if (entity.onGround()) {
            if (!isSupportedByOrNearCore(entity, corePos)) {
                tracker.alterna$clearGravityCore();
            }
            return;
        }

        // Core chỉ còn "hợp lệ" trong 1 tick sau khi rời mặt đất, để tránh
        // áp dụng transition dựa trên 1 core đã rời xa từ lâu.
        if (entity.tickCount - tracker.alterna$getLastGravityCoreTick() > 1) {
            tracker.alterna$clearGravityCore();
            return;
        }

        Direction currentGravity = GravityApi.getDirection(entity);
        Vec3 positionMovement = entity.position().subtract(oldPosition(entity));
        Vec3 movement = getTransitionMovement(entity, currentGravity);
        Vec3 localMovement = RotationUtil.vecWorldToPlayer(movement, currentGravity);
        Vec3 localPositionMovement = RotationUtil.vecWorldToPlayer(positionMovement, currentGravity);
        // QUAN TRỌNG: KHÔNG bắt buộc phải đang "rơi thật sự" (localMovement.y < 0)
        // mới xử lý — nếu bắt buộc, tick người chơi ĐI BỘ NGANG tới đúng góc
        // (chưa rơi, chỉ là onGround() chập chờn về false do cách vanilla giải
        // va chạm ở rìa AABB) sẽ bị bỏ qua, và tryTransition (đường chính, yêu
        // cầu onGround()==true) cũng bỏ qua đúng tick đó — lọt lưới cả 2 hàm,
        // gây kẹt vĩnh viễn ở góc. Chỉ cần chặn trường hợp đang nhảy LÊN rõ
        // ràng (localMovement.y dương đáng kể) để tránh bắn nhầm lúc jump.
        if (localMovement.y > 1.0E-5 || localPositionMovement.y > 1.0E-5) {
            return;
        }

        Direction edgeDirection = findFallTransitionEdge(entity.position(), oldPosition(entity), movement, corePos,
                currentGravity);
        if (edgeDirection == null) {
            tracker.alterna$clearGravityCore();
            return;
        }

        Direction targetGravity = edgeDirection.getOpposite();
        EntityDimensions dimensions = entity.getDimensions(entity.getPose());
        Vec3 faceCenter = getFaceCenterPosition(dimensions, corePos, edgeDirection, currentGravity);
        Vec3 targetPosition = findCollisionFreeTargetPosition(serverLevel, entity, faceCenter, currentGravity,
                targetGravity);
        if (targetPosition == null) {
            tracker.alterna$clearGravityCore();
            return;
        }

        applyTransition(level, entity, movement, currentGravity, edgeDirection, targetGravity, targetPosition);
        tracker.alterna$clearGravityCore();
    }

    /** Gọi từ GravityCoreBlock#stepOn mỗi khi entity đang đứng trên core. */
    public static void tryTransition(Level level, BlockPos corePos, LivingEntity entity) {
        if (!(level instanceof ServerLevel serverLevel))
            return;
        if (!(entity instanceof GravityCoreTracker tracker))
            return;
        if (entity.tickCount - tracker.alterna$getLastTransitionTick() < TRANSITION_COOLDOWN_TICKS)
            return;
        if (!entity.isAlive() || entity.isPassenger() || entity.isNoGravity() || entity.noPhysics)
            return;

        boolean debug = entity instanceof net.minecraft.world.entity.player.Player && entity.tickCount % 20 == 0;

        if (!entity.onGround() || !isSupportedByOrNearCore(entity, corePos)) {
            if (debug) {
                Alterna.LOGGER.info(
                        "[gravitycore-debug] tryTransition BAIL: onGround={} isSupportedByOrNearCore={} dir={}",
                        entity.onGround(), isSupportedByOrNearCore(entity, corePos), GravityApi.getDirection(entity));
            }
            return;
        }

        Direction currentGravity = GravityApi.getDirection(entity);
        Vec3 movement = getTransitionMovement(entity, currentGravity);
        Vec3 localMovement = RotationUtil.vecWorldToPlayer(movement, currentGravity);
        Vec3 localPositionMovement = RotationUtil.vecWorldToPlayer(entity.position().subtract(oldPosition(entity)),
                currentGravity);
        if (localMovement.y > 1.0E-5 || localPositionMovement.y > 1.0E-5) {
            if (debug) {
                Alterna.LOGGER.info(
                        "[gravitycore-debug] tryTransition BAIL: localMovement.y={} localPositionMovement.y={}",
                        localMovement.y, localPositionMovement.y);
            }
            return;
        }

        Direction edgeDirection = findTransitionEdge(entity.position(), movement, corePos, currentGravity);
        if (edgeDirection == null) {
            if (debug) {
                Alterna.LOGGER.info(
                        "[gravitycore-debug] tryTransition BAIL: no edge found. dir={} movement={} localMovement={} pos={} corePos={}",
                        currentGravity, movement, localMovement, entity.position(), corePos);
            }
            return;
        }

        Direction targetGravity = edgeDirection.getOpposite();
        EntityDimensions dimensions = entity.getDimensions(entity.getPose());
        Vec3 faceCenter = getFaceCenterPosition(dimensions, corePos, edgeDirection, currentGravity);
        Vec3 targetPosition = findCollisionFreeTargetPosition(serverLevel, entity, faceCenter, currentGravity,
                targetGravity);
        if (targetPosition == null) {
            if (debug) {
                Alterna.LOGGER.info(
                        "[gravitycore-debug] tryTransition BAIL: no collision-free target. edge={} targetGravity={}",
                        edgeDirection, targetGravity);
            }
            return;
        }

        Alterna.LOGGER.info("[gravitycore-debug] tryTransition FIRE: {} -> {} via edge {}", currentGravity,
                targetGravity, edgeDirection);
        applyTransition(level, entity, movement, currentGravity, edgeDirection, targetGravity, targetPosition);
    }

    private static void applyTransition(Level level, LivingEntity entity, Vec3 movement, Direction currentGravity,
            Direction edgeDirection, Direction targetGravity, Vec3 targetPosition) {
        Vec3 targetMovement = rotateAroundEdge(movement, currentGravity, edgeDirection);
        Vec3 targetLook = rotateAroundEdge(entity.getViewVector(1.0F), currentGravity, edgeDirection);
        Vec2 targetRotation = RotationUtil.vecToRot(RotationUtil.vecWorldToPlayer(targetLook, targetGravity));
        float targetBodyRotation = rotateLocalYaw(entity.yBodyRot, currentGravity, edgeDirection, targetGravity);
        float targetHeadRotation = rotateLocalYaw(entity.getYHeadRot(), currentGravity, edgeDirection, targetGravity);

        GravityApi.setDirection(entity, targetGravity);
        level.playSound((Entity) null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.METAL_FALL,
                entity.getSoundSource(), 1.0F, 0.5F);
        moveEntity(entity, targetPosition, targetMovement, targetRotation);
        entity.setYBodyRot(targetBodyRotation);
        entity.setYHeadRot(targetHeadRotation);
        entity.setOnGround(true);
        entity.resetFallDistance();

        // Đồng bộ lại "vị trí cũ" theo đúng vị trí vừa teleport tới, để tick kế
        // tiếp không tính nhầm cú teleport (nhảy vọt qua cạnh) thành 1 cú di
        // chuyển khổng lồ rồi lại kích hoạt transition ngược lại ngay lập tức.
        entity.xOld = targetPosition.x;
        entity.yOld = targetPosition.y;
        entity.zOld = targetPosition.z;
        entity.xo = targetPosition.x;
        entity.yo = targetPosition.y;
        entity.zo = targetPosition.z;

        if (entity instanceof GravityCoreTracker tracker) {
            tracker.alterna$setLastTransitionTick(entity.tickCount);
        }
    }

    private static Vec3 getTransitionMovement(LivingEntity entity, Direction gravityDirection) {
        Vec3 collisionMovement = ((GravityCoreTracker) entity).alterna$getLastMoveDelta();
        Vec3 positionMovement = entity.position().subtract(oldPosition(entity));
        Vec3 localCollisionMovement = RotationUtil.vecWorldToPlayer(collisionMovement, gravityDirection);
        Vec3 localPositionMovement = RotationUtil.vecWorldToPlayer(positionMovement, gravityDirection);
        return localPositionMovement.horizontalDistanceSqr() <= localCollisionMovement.horizontalDistanceSqr()
                ? collisionMovement
                : new Vec3(positionMovement.x, collisionMovement.y, positionMovement.z);
    }

    private static void moveEntity(LivingEntity entity, Vec3 position, Vec3 movement, Vec2 rotation) {
        if (entity instanceof ServerPlayer player) {
            player.connection.teleport(new PositionMoveRotation(position, movement, rotation.x, rotation.y), Set.of());
        } else {
            entity.snapTo(position, rotation.x, rotation.y);
            entity.setDeltaMovement(movement);
        }
    }

    static Direction findTransitionEdge(Vec3 position, Vec3 movement, BlockPos corePos, Direction gravityDirection) {
        Direction closestEdge = null;
        double closestArrivalTime = Double.POSITIVE_INFINITY;

        for (Direction edgeDirection : Direction.values()) {
            if (edgeDirection.getAxis() == gravityDirection.getAxis())
                continue;
            double speedTowardEdge = component(movement, edgeDirection.getAxis())
                    * edgeDirection.getAxisDirection().getStep();
            double boundary = boundary(corePos, edgeDirection);
            double distance = (boundary - component(position, edgeDirection.getAxis()))
                    * edgeDirection.getAxisDirection().getStep();

            if ((speedTowardEdge > MIN_EDGE_SPEED && distance <= speedTowardEdge + EDGE_LOOKAHEAD_MARGIN) ||
                    (speedTowardEdge > 0.001 && distance <= EDGE_LOOKAHEAD_MARGIN + 0.05)) {
                double arrivalTime = speedTowardEdge > MIN_EDGE_SPEED ? Math.max(0.0, distance) / speedTowardEdge
                        : distance;
                if (arrivalTime < closestArrivalTime) {
                    closestArrivalTime = arrivalTime;
                    closestEdge = edgeDirection;
                }
            }
        }
        return closestEdge;
    }

    static Vec3 rotateAroundEdge(Vec3 vector, Direction gravityDirection, Direction edgeDirection) {
        Vec3 gravity = directionVector(gravityDirection);
        Vec3 edge = directionVector(edgeDirection);
        Vec3 targetGravity = edge.reverse();
        double edgeComponent = vector.dot(edge);
        double gravityComponent = vector.dot(gravity);
        Vec3 parallelToEdge = vector.subtract(edge.scale(edgeComponent)).subtract(gravity.scale(gravityComponent));
        return parallelToEdge.add(gravity.scale(edgeComponent)).add(targetGravity.scale(gravityComponent));
    }

    static Vec3 getFaceCenterPosition(EntityDimensions dimensions, BlockPos corePos, Direction edgeDirection,
            Direction currentGravity) {
        Vec3 center = new Vec3(corePos.getX() + 0.5, corePos.getY() + 0.5, corePos.getZ() + 0.5);
        Vec3 position = center.add(directionVector(currentGravity.getOpposite()).scale(dimensions.width() * 0.5));
        return withComponent(position, edgeDirection.getAxis(),
                boundary(corePos, edgeDirection) + edgeDirection.getAxisDirection().getStep() * SUPPORT_OFFSET);
    }

    private static Vec3 findCollisionFreeTargetPosition(ServerLevel level, LivingEntity entity, Vec3 position,
            Direction currentGravity, Direction targetGravity) {
        EntityDimensions dimensions = entity.getDimensions(entity.getPose());
        Vec3 awayFromSupport = directionVector(currentGravity.getOpposite());
        double maximumShift = dimensions.width() * 0.5 + SUPPORT_OFFSET;
        int steps = Math.max(1, (int) Math.ceil(maximumShift / 0.05));

        for (int step = 0; step <= steps; ++step) {
            double shift = step == steps ? maximumShift : step * 0.05;
            Vec3 candidate = position.add(awayFromSupport.scale(shift));
            AABB candidateBox = RotationUtil.makeBoxFromDimensions(dimensions, targetGravity, candidate);
            if (level.noCollision(entity, candidateBox.deflate(1.0E-7))) {
                return candidate;
            }
        }
        return null;
    }

    private static Direction findFallTransitionEdge(Vec3 position, Vec3 previousPosition, Vec3 movement,
            BlockPos corePos, Direction gravityDirection) {
        Direction edge = findTransitionEdge(position, movement, corePos, gravityDirection);
        if (edge != null)
            return edge;

        Vec3 positionMovement = position.subtract(previousPosition);
        double closestArrivalTime = Double.POSITIVE_INFINITY;
        double greatestOutsideDistance = 0.01;
        boolean hasOutsideEdge = false;
        Direction closestEdge = null;

        for (Direction candidate : Direction.values()) {
            if (candidate.getAxis() == gravityDirection.getAxis())
                continue;
            double speed = component(positionMovement, candidate.getAxis()) * candidate.getAxisDirection().getStep();
            double distance = (boundary(corePos, candidate) - component(position, candidate.getAxis()))
                    * candidate.getAxisDirection().getStep();
            double outsideDistance = -distance;
            if (outsideDistance > greatestOutsideDistance) {
                greatestOutsideDistance = outsideDistance;
                hasOutsideEdge = true;
                closestEdge = candidate;
                closestArrivalTime = Double.POSITIVE_INFINITY;
            }
            if ((!hasOutsideEdge || outsideDistance > 0.0) && speed > MIN_EDGE_SPEED
                    && distance <= EDGE_LOOKAHEAD_MARGIN) {
                double arrivalTime = Math.max(0.0, distance) / speed;
                if (arrivalTime < closestArrivalTime) {
                    closestArrivalTime = arrivalTime;
                    closestEdge = candidate;
                }
            }
        }
        return closestEdge;
    }

    private static float rotateLocalYaw(float yaw, Direction currentGravity, Direction edgeDirection,
            Direction targetGravity) {
        Vec3 worldFacing = RotationUtil.vecPlayerToWorld(RotationUtil.rotToVec(yaw, 0.0F), currentGravity);
        Vec3 targetWorldFacing = rotateAroundEdge(worldFacing, currentGravity, edgeDirection);
        return RotationUtil.vecToRot(RotationUtil.vecWorldToPlayer(targetWorldFacing, targetGravity)).x;
    }

    /**
     * Bản gravity_changer gốc dùng entity.oldPosition() (Vec3), nhưng API đó
     * không tồn tại trong phiên bản Minecraft mà alterna đang target — EntityMixin
     * của alterna tự đọc trực tiếp xOld/yOld/zOld, nên ta làm tương tự ở đây.
     */
    private static Vec3 oldPosition(Entity entity) {
        return new Vec3(entity.xOld, entity.yOld, entity.zOld);
    }

    private static Vec3 directionVector(Direction direction) {
        return new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    private static double boundary(BlockPos pos, Direction direction) {
        double minimum = switch (direction.getAxis()) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
        return direction.getAxisDirection() == AxisDirection.POSITIVE ? minimum + 1.0 : minimum;
    }

    private static double component(Vec3 vector, Direction.Axis axis) {
        return switch (axis) {
            case X -> vector.x;
            case Y -> vector.y;
            case Z -> vector.z;
        };
    }

    private static Vec3 withComponent(Vec3 vector, Direction.Axis axis, double value) {
        return switch (axis) {
            case X -> new Vec3(value, vector.y, vector.z);
            case Y -> new Vec3(vector.x, value, vector.z);
            case Z -> new Vec3(vector.x, vector.y, value);
        };
    }

    private static boolean isSupportedByOrNearCore(LivingEntity entity, BlockPos corePos) {
        if (entity.isSupportedBy(corePos)) {
            return true;
        }
        // Cho phép ở các góc/mép: kiểm tra xem hitbox của entity (mở rộng 0.15) có chạm
        // vào corePos không
        AABB box = entity.getBoundingBox().inflate(0.15);
        return box.intersects(new AABB(corePos));
    }
}