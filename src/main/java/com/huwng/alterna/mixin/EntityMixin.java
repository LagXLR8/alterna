package com.huwng.alterna.mixin;

import com.huwng.alterna.gravity.GravityApi;
import com.huwng.alterna.gravity.GravityCoreTracker;
import com.huwng.alterna.gravity.RotationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import it.unimi.dsi.fastutil.doubles.DoubleListIterator;
import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatSet;

import java.util.List;
import java.util.Optional;

@Mixin(Entity.class)
public abstract class EntityMixin implements GravityCoreTracker {

    @Shadow public double xo;
    @Shadow public double yo;
    @Shadow public double zo;
    @Shadow public Optional<BlockPos> mainSupportingBlockPos;
    @Shadow protected boolean onGroundNoBlocks;

    @Shadow public abstract AABB getBoundingBox();
    @Shadow public abstract Level level();

    @Unique
    private Vec3 alterna$lastMoveDelta = Vec3.ZERO;
    @Unique
    private BlockPos alterna$lastGravityCore = null;
    @Unique
    private int alterna$lastGravityCoreTick = Integer.MIN_VALUE;
    // LƯU Ý: KHÔNG dùng Integer.MIN_VALUE ở đây. Cooldown check dùng phép so
    // sánh "hiệu < N" (khác với alterna$lastGravityCoreTick dùng "hiệu > N"),
    // nên entity.tickCount - Integer.MIN_VALUE sẽ TRÀN SỐ (int overflow) và
    // lật thành 1 số âm rất lớn -> "hiệu < N" luôn đúng ngay từ đầu -> cooldown
    // coi như vừa transition xong mãi mãi -> chặn đứng mọi lần đổi hướng vĩnh
    // viễn. Dùng 1 số âm vừa đủ, không đủ lớn để tràn số trong suốt quá trình chơi.
    @Unique
    private int alterna$lastTransitionTick = -1_000_000;

    @Override
    public Vec3 alterna$getLastMoveDelta() {
        return alterna$lastMoveDelta;
    }

    @Override
    public void alterna$rememberGravityCore(BlockPos pos, int tick) {
        this.alterna$lastGravityCore = pos;
        this.alterna$lastGravityCoreTick = tick;
    }

    @Override
    public BlockPos alterna$getLastGravityCore() {
        return alterna$lastGravityCore;
    }

    @Override
    public int alterna$getLastGravityCoreTick() {
        return alterna$lastGravityCoreTick;
    }

    @Override
    public void alterna$clearGravityCore() {
        this.alterna$lastGravityCore = null;
        this.alterna$lastGravityCoreTick = Integer.MIN_VALUE;
    }

    @Override
    public void alterna$setLastTransitionTick(int tick) {
        this.alterna$lastTransitionTick = tick;
    }

    @Override
    public int alterna$getLastTransitionTick() {
        return alterna$lastTransitionTick;
    }

    @Inject(method = "move", at = @At("RETURN"))
    private void alterna$fixOnGroundAndVelocityInMove(MoverType moverType, Vec3 deltaWorld, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        // Ghi lại delta "dự định" của tick này TRƯỚC khi xét early-return theo
        // hướng trọng lực hiện tại, vì GravityCoreTransitionHandler cần giá trị
        // này để tính hướng đi vòng cạnh kể cả khi entity đang ở trọng lực
        // bình thường (DOWN) lúc mới tiến vào vùng ảnh hưởng của core.
        this.alterna$lastMoveDelta = deltaWorld;

        Direction direction = GravityApi.getDirection(self);
        if (direction == Direction.DOWN) {
            return;
        }

        Vec3 deltaLocal = RotationUtil.vecWorldToPlayer(deltaWorld, direction);
        Vec3 movementWorld = self.position().subtract(self.xOld, self.yOld, self.zOld);
        Vec3 movementLocal = RotationUtil.vecWorldToPlayer(movementWorld, direction);

        boolean localXCollision = !Mth.equal(deltaLocal.x, movementLocal.x);
        boolean localZCollision = !Mth.equal(deltaLocal.z, movementLocal.z);
        boolean localHorizontalCollision = localXCollision || localZCollision;

        boolean localVerticalCollisionBelow = deltaLocal.y < -1.0E-5 && movementLocal.y > deltaLocal.y + 1.0E-5;

        self.setOnGroundWithMovement(localVerticalCollisionBelow, localHorizontalCollision, movementWorld);
    }

    @Inject(method = "makeBoundingBox(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/AABB;", at = @At("RETURN"), cancellable = true, require = 0)
    private void alterna$makeBoundingBox(Vec3 position, CallbackInfoReturnable<AABB> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Projectile) return;
        Direction gravityDirection = GravityApi.getDirection(self);
        if (gravityDirection == Direction.DOWN) return;

        AABB box = cir.getReturnValue().move(position.reverse());
        if (gravityDirection.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            box = box.move(0.0, -1.0E-6, 0.0);
        }
        cir.setReturnValue(RotationUtil.boxPlayerToWorld(box, gravityDirection).move(position));
    }

    @Inject(method = "calculateViewVector", at = @At("RETURN"), cancellable = true, require = 0)
    private void alterna$calculateViewVector(CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        Direction gravityDirection = GravityApi.getDirection(self);
        if (gravityDirection == Direction.DOWN) return;
        cir.setReturnValue(RotationUtil.vecPlayerToWorld(cir.getReturnValue(), gravityDirection));
    }

    @Inject(method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true, require = 0)
    private void alterna$getEyePositionHead(CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        Direction gravityDirection = GravityApi.getDirection(self);
        if (gravityDirection == Direction.DOWN) return;
        cir.setReturnValue(RotationUtil.vecPlayerToWorld(0.0, self.getEyeHeight(), 0.0, gravityDirection).add(self.position()));
    }

    @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true, require = 0)
    private void alterna$getEyePositionPartial(float tickDelta, CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        Direction gravityDirection = GravityApi.getDirection(self);
        if (gravityDirection == Direction.DOWN) return;
        Vec3 eyeOffsetWorld = RotationUtil.vecPlayerToWorld(0.0, self.getEyeHeight(), 0.0, gravityDirection);
        double x = Mth.lerp((double) tickDelta, self.xo, self.getX()) + eyeOffsetWorld.x;
        double y = Mth.lerp((double) tickDelta, self.yo, self.getY()) + eyeOffsetWorld.y;
        double z = Mth.lerp((double) tickDelta, self.zo, self.getZ()) + eyeOffsetWorld.z;
        cir.setReturnValue(new Vec3(x, y, z));
    }

    @Shadow protected static Vec3 getInputVector(Vec3 input, float speed, float yRot) { throw new AssertionError(); }

    @Inject(method = "moveRelative", at = @At("HEAD"), cancellable = true, require = 0)
    private void alterna$moveRelativeDirectional(float speed, Vec3 input, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        Direction gravityDirection = GravityApi.getDirection(self);
        if (gravityDirection == Direction.DOWN) {
            return;
        }

        ci.cancel();
        Vec3 localDelta = getInputVector(input, speed, self.getYRot());
        Vec3 worldDelta = RotationUtil.vecPlayerToWorld(localDelta, gravityDirection);
        self.setDeltaMovement(self.getDeltaMovement().add(worldDelta));
    }

    @Inject(method = "getBlockPosBelowThatAffectsMyMovement", at = @At("HEAD"), cancellable = true, require = 0)
    private void alterna$getBlockPosBelowThatAffectsMyMovement(CallbackInfoReturnable<BlockPos> cir) {
        Entity self = (Entity) (Object) this;
        Direction gravityDirection = GravityApi.getDirection(self);
        if (gravityDirection == Direction.DOWN) return;
        cir.setReturnValue(BlockPos.containing(self.position().add(gravityDirection.getUnitVec3().scale(0.5000001))));
    }

    @Inject(method = "getOnPosLegacy", at = @At("HEAD"), cancellable = true, require = 0)
    private void alterna$getOnPosLegacy(CallbackInfoReturnable<BlockPos> cir) {
        Entity self = (Entity) (Object) this;
        Direction gravityDirection = GravityApi.getDirection(self);
        if (gravityDirection == Direction.DOWN) return;
        cir.setReturnValue(BlockPos.containing(RotationUtil.vecPlayerToWorld(0.0, -0.2, 0.0, gravityDirection).add(self.position())));
    }

    /**
     * Vanilla checkSupportingBlock() luôn dò "nền" bằng cách quét xuống world
     * -Y, bất kể trọng lực hiện tại của entity đang hướng đâu. Kết quả là khi
     * gravity != DOWN (đứng trên mặt bên/mặt trần của GravityCoreBlock), vanilla
     * không tìm thấy khối đỡ thật sự (vì "dưới chân" thật lúc này là 1 hướng
     * ngang/lên), coi entity như đang lơ lửng, rồi các cơ chế khác dựa vào
     * mainSupportingBlockPos (đẩy lùi/step-up/snap ground...) hoạt động sai —
     * đây là nguyên nhân gốc của hiện tượng "chỉ hoạt động ở mặt up, các mặt
     * khác bị đẩy lung tung" đã gặp. Port từ gravitychanger$checkDirectionalSupportingBlock.
     */
    @Inject(method = "checkSupportingBlock", at = @At("HEAD"), cancellable = true, require = 0)
    private void alterna$checkDirectionalSupportingBlock(boolean onGround, Vec3 movement, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        Direction gravityDirection = GravityApi.getDirection(self);
        if (gravityDirection == Direction.DOWN) return;

        ci.cancel();
        if (onGround) {
            AABB testArea = alterna$getSupportArea(this.getBoundingBox(), gravityDirection);
            Optional<BlockPos> supportingBlock = this.level().findSupportingBlock(self, testArea);
            if (supportingBlock.isEmpty() && !this.onGroundNoBlocks) {
                if (movement != null) {
                    Vec3 localMovement = RotationUtil.vecWorldToPlayer(movement, gravityDirection);
                    Vec3 horizontalWorldMovement = RotationUtil.vecPlayerToWorld(localMovement.x, 0.0, localMovement.z, gravityDirection);
                    supportingBlock = this.level().findSupportingBlock(self, testArea.move(horizontalWorldMovement.reverse()));
                    this.mainSupportingBlockPos = supportingBlock;
                }
            } else {
                this.mainSupportingBlockPos = supportingBlock;
            }
            this.onGroundNoBlocks = supportingBlock.isEmpty();
        } else {
            this.onGroundNoBlocks = false;
            if (this.mainSupportingBlockPos.isPresent()) {
                this.mainSupportingBlockPos = Optional.empty();
            }
        }
    }

    @Unique
    private static AABB alterna$getSupportArea(AABB boundingBox, Direction gravityDirection) {
        double epsilon = 0.001;
        return switch (gravityDirection) {
            case DOWN -> new AABB(boundingBox.minX, boundingBox.minY - epsilon, boundingBox.minZ, boundingBox.maxX, boundingBox.minY, boundingBox.maxZ);
            case UP -> new AABB(boundingBox.minX, boundingBox.maxY, boundingBox.minZ, boundingBox.maxX, boundingBox.maxY + epsilon, boundingBox.maxZ);
            case NORTH -> new AABB(boundingBox.minX, boundingBox.minY, boundingBox.minZ - epsilon, boundingBox.maxX, boundingBox.maxY, boundingBox.minZ);
            case SOUTH -> new AABB(boundingBox.minX, boundingBox.minY, boundingBox.maxZ, boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ + epsilon);
            case WEST -> new AABB(boundingBox.minX - epsilon, boundingBox.minY, boundingBox.minZ, boundingBox.minX, boundingBox.maxY, boundingBox.maxZ);
            case EAST -> new AABB(boundingBox.maxX, boundingBox.minY, boundingBox.minZ, boundingBox.maxX + epsilon, boundingBox.maxY, boundingBox.maxZ);
        };
    }

    // ======================================================================
    // Port từ gravitychanger (bản NeoForge, MC 1.21.1) — xử lý va chạm chi
    // tiết BÊN TRONG collide()/collideBoundingBox()/isInWall() theo không
    // gian cục bộ đã xoay theo trọng lực. Đây là phần còn thiếu khiến vật lý
    // ở góc/cạnh GravityCoreBlock không ổn định (giật liên tục): vanilla giải
    // quyết va chạm theo thứ tự trục world X/Y/Z cố định, nhưng khi trọng lực
    // không phải DOWN, thứ tự "trục nào là chiều rơi chính" đã đổi — cần giải
    // theo đúng thứ tự cục bộ (trục trọng lực trước, rồi trục ngang lớn hơn,
    // rồi trục ngang còn lại) mới ra kết quả ổn định tại các góc.
    // ======================================================================

    @Shadow
    private static Vec3 collideWithShapes(Vec3 movement, AABB entityBoundingBox, List<VoxelShape> collisions) {
        throw new AssertionError();
    }

    @ModifyVariable(
            method = "collide",
            at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/level/Level;getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;", ordinal = 0),
            ordinal = 0, argsOnly = true, require = 0
    )
    private Vec3 alterna$collide_toPlayerSpace(Vec3 movement) {
        Direction gravityDirection = GravityApi.getDirection((Entity) (Object) this);
        return gravityDirection == Direction.DOWN ? movement : RotationUtil.vecWorldToPlayer(movement, gravityDirection);
    }

    @Inject(method = "collide", at = @At("RETURN"), cancellable = true, require = 0)
    private void alterna$collide_toWorldSpace(CallbackInfoReturnable<Vec3> cir) {
        Direction gravityDirection = GravityApi.getDirection((Entity) (Object) this);
        if (gravityDirection != Direction.DOWN) {
            cir.setReturnValue(RotationUtil.vecPlayerToWorld(cir.getReturnValue(), gravityDirection));
        }
    }

    @ModifyArgs(method = "collide", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;expandTowards(DDD)Lnet/minecraft/world/phys/AABB;"), require = 0)
    private void alterna$collide_expandTowards(Args args) {
        Direction gravityDirection = GravityApi.getDirection((Entity) (Object) this);
        Vec3 rotated = RotationUtil.vecPlayerToWorld(new Vec3((Double) args.get(0), (Double) args.get(1), (Double) args.get(2)), gravityDirection);
        args.set(0, rotated.x);
        args.set(1, rotated.y);
        args.set(2, rotated.z);
    }

    @ModifyArgs(method = "collide", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;move(DDD)Lnet/minecraft/world/phys/AABB;"), require = 0)
    private void alterna$collide_moveBox(Args args) {
        Direction gravityDirection = GravityApi.getDirection((Entity) (Object) this);
        Vec3 rotated = RotationUtil.vecPlayerToWorld(new Vec3((Double) args.get(0), (Double) args.get(1), (Double) args.get(2)), gravityDirection);
        args.set(0, rotated.x);
        args.set(1, rotated.y);
        args.set(2, rotated.z);
    }

    @Redirect(method = "collide", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collideWithShapes(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;", ordinal = 0), require = 0)
    private Vec3 alterna$collide_resolvePerAxis(Vec3 movement, AABB entityBoundingBox, List<VoxelShape> collisions) {
        return alterna$redirection(movement, entityBoundingBox, collisions, (Entity) (Object) this);
    }

    @Redirect(method = "collideBoundingBox", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collideWithShapes(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;", ordinal = 0), require = 0)
    private static Vec3 alterna$collideBoundingBox_resolvePerAxis(Vec3 movement, AABB entityBoundingBox, List<VoxelShape> collisions, Entity entity) {
        return alterna$redirection(movement, entityBoundingBox, collisions, entity);
    }

    @ModifyVariable(method = "collideBoundingBox", at = @At("HEAD"), ordinal = 0, argsOnly = true, require = 0)
    private static Vec3 alterna$collideBoundingBox_toWorldSpace(Vec3 movement, Entity entity) {
        if (entity == null) return movement;
        Direction gravityDirection = GravityApi.getDirection(entity);
        return gravityDirection == Direction.DOWN ? movement : RotationUtil.vecPlayerToWorld(movement, gravityDirection);
    }

    /**
     * Thay thế Entity.collideWithShapes: giải va chạm theo thứ tự trục CỤC BỘ
     * (trục trọng lực trước — Y cục bộ — rồi tới trục ngang có biên độ lớn hơn,
     * rồi trục ngang còn lại), thay vì thứ tự trục world cố định của vanilla.
     * movement/entityBoundingBox vào/ra đều ở world-space (giữ nguyên hợp đồng
     * với hàm gốc collideWithShapes để chỗ gọi không cần đổi gì thêm).
     */
    @Unique
    private static Vec3 alterna$redirection(Vec3 movement, AABB entityBoundingBox, List<VoxelShape> collisions, Entity entity) {
        Direction gravityDirection = entity == null ? Direction.DOWN : GravityApi.getDirection(entity);
        if (entity == null || gravityDirection == Direction.DOWN) {
            return collideWithShapes(movement, entityBoundingBox, collisions);
        }

        Vec3 local = RotationUtil.vecWorldToPlayer(movement, gravityDirection);
        double localX = local.x;
        double localY = local.y;
        double localZ = local.z;
        Direction axisX = RotationUtil.dirPlayerToWorld(Direction.EAST, gravityDirection);
        Direction axisY = RotationUtil.dirPlayerToWorld(Direction.UP, gravityDirection);
        Direction axisZ = RotationUtil.dirPlayerToWorld(Direction.SOUTH, gravityDirection);

        if (localY != 0.0) {
            localY = Shapes.collide(axisY.getAxis(), entityBoundingBox, collisions, localY * axisY.getAxisDirection().getStep()) * axisY.getAxisDirection().getStep();
            if (localY != 0.0) {
                entityBoundingBox = entityBoundingBox.move(RotationUtil.vecPlayerToWorld(0.0, localY, 0.0, gravityDirection));
            }
        }

        boolean zLargerThanX = Math.abs(localX) < Math.abs(localZ);
        if (zLargerThanX && localZ != 0.0) {
            localZ = Shapes.collide(axisZ.getAxis(), entityBoundingBox, collisions, localZ * axisZ.getAxisDirection().getStep()) * axisZ.getAxisDirection().getStep();
            if (localZ != 0.0) {
                entityBoundingBox = entityBoundingBox.move(RotationUtil.vecPlayerToWorld(0.0, 0.0, localZ, gravityDirection));
            }
        }

        if (localX != 0.0) {
            localX = Shapes.collide(axisX.getAxis(), entityBoundingBox, collisions, localX * axisX.getAxisDirection().getStep()) * axisX.getAxisDirection().getStep();
            if (!zLargerThanX && localX != 0.0) {
                entityBoundingBox = entityBoundingBox.move(RotationUtil.vecPlayerToWorld(localX, 0.0, 0.0, gravityDirection));
            }
        }

        if (!zLargerThanX && localZ != 0.0) {
            localZ = Shapes.collide(axisZ.getAxis(), entityBoundingBox, collisions, localZ * axisZ.getAxisDirection().getStep()) * axisZ.getAxisDirection().getStep();
        }

        return new Vec3(localX, localY, localZ);
    }

    @Redirect(method = "collide", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collectCandidateStepUpHeights(Lnet/minecraft/world/phys/AABB;Ljava/util/List;FF)[F", ordinal = 0), require = 0)
    private float[] alterna$collide_stepUpHeights(AABB boxSnappedToGround, List<VoxelShape> allCollisions, float stepHeight, float distToGround) {
        Direction gravityDirection = GravityApi.getDirection((Entity) (Object) this);
        FloatSet floatSet = new FloatArraySet(4);
        double relativeBottom = alterna$getRelativeBottom(boxSnappedToGround, gravityDirection);

        if (gravityDirection.getAxisDirection() == AxisDirection.NEGATIVE) {
            for (VoxelShape voxelShape : allCollisions) {
                DoubleListIterator it = voxelShape.getCoords(gravityDirection.getAxis()).iterator();
                while (it.hasNext()) {
                    double collisionPoint = it.nextDouble();
                    float verticalDist = (float) (collisionPoint - relativeBottom);
                    if (verticalDist >= 0.0F && verticalDist != distToGround) {
                        if (verticalDist > stepHeight) break;
                        floatSet.add(verticalDist);
                    }
                }
            }
        } else {
            for (VoxelShape voxelShape : allCollisions) {
                for (double collisionPoint : voxelShape.getCoords(gravityDirection.getAxis()).reversed()) {
                    float verticalDist = -((float) (collisionPoint - relativeBottom));
                    if (verticalDist >= 0.0F && verticalDist != distToGround) {
                        if (verticalDist > stepHeight) break;
                        floatSet.add(verticalDist);
                    }
                }
            }
        }

        float[] result = floatSet.toFloatArray();
        FloatArrays.unstableSort(result);
        return result;
    }

    @Unique
    private static double alterna$getRelativeBottom(AABB box, Direction gravityDirection) {
        return switch (gravityDirection) {
            case DOWN -> box.minY;
            case UP -> box.maxY;
            case NORTH -> box.minZ;
            case SOUTH -> box.maxZ;
            case WEST -> box.minX;
            case EAST -> box.maxX;
        };
    }

    @ModifyArgs(method = "isInWall", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;ofSize(Lnet/minecraft/world/phys/Vec3;DDD)Lnet/minecraft/world/phys/AABB;", ordinal = 0), require = 0)
    private void alterna$isInWall_toWorldSpace(Args args) {
        Direction gravityDirection = GravityApi.getDirection((Entity) (Object) this);
        Vec3 rotated = RotationUtil.vecPlayerToWorld(new Vec3((Double) args.get(1), (Double) args.get(2), (Double) args.get(3)), gravityDirection);
        args.set(1, rotated.x);
        args.set(2, rotated.y);
        args.set(3, rotated.z);
    }
}
