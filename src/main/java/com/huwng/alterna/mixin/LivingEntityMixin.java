package com.huwng.alterna.mixin;

import com.huwng.alterna.gravity.GravityApi;
import com.huwng.alterna.gravity.GravityCoreTransitionHandler;
import com.huwng.alterna.gravity.GravityData;
import com.huwng.alterna.gravity.RotationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    protected abstract double getEffectiveGravity();

    @Shadow
    protected abstract float getJumpPower();

    /**
     * getInputVector nằm ở Entity, không phải LivingEntity — @Shadow xuyên
     * qua 2 class không hoạt động ổn định (đã crash 1 lần vì lý do này), nên
     * viết thẳng lại công thức vanilla chuẩn (ổn định qua nhiều bản MC, không
     * đổi) tại đây thay vì phụ thuộc shadow.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void alterna$tickGravityAnimation(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        GravityData data = GravityApi.getData(self);
        data.tickAnimation();

        // Tự động khôi phục trọng lực về DOWN ngay khi người chơi không còn dẫm/chạm vào GravityPlatingBlock
        if (!self.level().isClientSide() && self instanceof net.minecraft.world.entity.player.Player player && data.getDirection() != Direction.DOWN) {
            if (!alterna$isSteppingOnGravityPlating(player)) {
                GravityApi.setDirection(player, Direction.DOWN);
            }
        }
    }

    @Unique
    private boolean alterna$isSteppingOnGravityPlating(net.minecraft.world.entity.player.Player player) {
        // Kiểm tra xem bounding box của người chơi (mở rộng nhẹ 0.1m) có va chạm với khối GravityPlatingBlock nào không
        net.minecraft.world.phys.AABB touchBox = player.getBoundingBox().inflate(0.1);
        BlockPos minPos = BlockPos.containing(touchBox.minX, touchBox.minY, touchBox.minZ);
        BlockPos maxPos = BlockPos.containing(touchBox.maxX, touchBox.maxY, touchBox.maxZ);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
            for (int y = minPos.getY(); y <= maxPos.getY(); y++) {
                for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
                    mutable.set(x, y, z);
                    if (player.level().getBlockState(mutable).getBlock() instanceof com.huwng.alterna.block.GravityPlatingBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void alterna$tryGravityCoreEdgeTransitionOnFall(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        GravityCoreTransitionHandler.tryTransitionOnFall(self.level(), self);
    }

    @Inject(method = "checkFallDamage", at = @At("HEAD"), cancellable = true, require = 0)
    private void alterna$checkFallDamageLocal(double y, boolean onGround, BlockState state, BlockPos pos, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        Direction direction = GravityApi.getDirection(self);
        if (direction == Direction.DOWN) {
            return;
        }

        ci.cancel();

        Vec3 deltaWorld = self.getDeltaMovement();
        Vec3 deltaLocal = RotationUtil.vecWorldToPlayer(deltaWorld, direction);
        double localY = deltaLocal.y;

        if (!self.isInWater() && localY < 0.0) {
            self.fallDistance -= (float) localY;
        }

        if (onGround) {
            if (self.fallDistance > 0.0) {
                state.getBlock().fallOn(self.level(), state, pos, self, self.fallDistance);
            }
            self.resetFallDistance();
        }
    }

    @Inject(method = "travelInAir", at = @At("RETURN"), require = 1)
    private void alterna$applyDirectionalGravityInAir(Vec3 input, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        Direction direction = GravityApi.getDirection(self);
        if (direction == Direction.DOWN) {
            return;
        }

        double effectiveGravity = this.getEffectiveGravity();
        Vec3 currentVel = self.getDeltaMovement();

        Vec3 gravityVecWorld = RotationUtil.vecPlayerToWorld(new Vec3(0.0, -effectiveGravity, 0.0), direction);

        Vec3 correctedVel = new Vec3(
                currentVel.x + gravityVecWorld.x,
                currentVel.y + (self.isNoGravity() ? 0.0 : effectiveGravity) + gravityVecWorld.y,
                currentVel.z + gravityVecWorld.z);
        self.setDeltaMovement(correctedVel);
    }

    @Inject(method = "travel", at = @At("RETURN"), require = 1)
    private void alterna$applyDirectionalFrictionAndFluid(Vec3 input, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        Direction direction = GravityApi.getDirection(self);
        if (direction == Direction.DOWN) {
            return;
        }

        if (self.isInWater()) {
            // Đẩy nhẹ theo hướng "lên" (buoyancy) trong không gian trọng lực cục bộ
            Vec3 currentWorldVel = self.getDeltaMovement();
            Vec3 localVel = RotationUtil.vecWorldToPlayer(currentWorldVel, direction);
            // Giữ cho chuyển động trong nước mượt mà theo hướng trọng lực
            Vec3 buoyancyWorld = RotationUtil.vecPlayerToWorld(new Vec3(0.0, 0.015, 0.0), direction);
            self.setDeltaMovement(currentWorldVel.add(buoyancyWorld));
        } else if (self.onGround()) {
            Vec3 currentWorldVel = self.getDeltaMovement();
            Vec3 localVel = RotationUtil.vecWorldToPlayer(currentWorldVel, direction);
            Vec3 localVelFriction = new Vec3(localVel.x * 0.6, localVel.y, localVel.z * 0.6);
            self.setDeltaMovement(RotationUtil.vecPlayerToWorld(localVelFriction, direction));
        }
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true, require = 1)
    private void alterna$applyDirectionalJumpHead(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        Direction direction = GravityApi.getDirection(self);
        if (direction == Direction.DOWN) {
            return;
        }

        ci.cancel();

        float jumpPower = this.getJumpPower();
        if (jumpPower <= 1.0E-5F) {
            return;
        }
        double effectiveJumpPower = Math.max((double) jumpPower + 0.05, 0.45);

        Vec3 initialWorldVel = self.getDeltaMovement();
        Vec3 localVel = RotationUtil.vecWorldToPlayer(initialWorldVel, direction);

        double sprintX = 0.0;
        double sprintZ = 0.0;
        if (self.isSprinting()) {
            float f = self.getYRot() * (float) (Math.PI / 180.0);
            sprintX = (double) (-Mth.sin(f)) * 0.2;
            sprintZ = (double) Mth.cos(f) * 0.2;
        }

        Vec3 newLocalVel = new Vec3(localVel.x + sprintX, effectiveJumpPower, localVel.z + sprintZ);

        self.setDeltaMovement(RotationUtil.vecPlayerToWorld(newLocalVel, direction));
        self.setOnGround(false);
        ((Entity) (Object) self).needsSync = true;
        net.neoforged.neoforge.common.CommonHooks.onLivingJump(self);
    }
}
