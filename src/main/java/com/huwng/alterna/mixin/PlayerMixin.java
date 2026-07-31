package com.huwng.alterna.mixin;

import com.huwng.alterna.gravity.GravityApi;
import com.huwng.alterna.gravity.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "canPlayerFitWithinBlocksAndEntitiesWhen", at = @At("HEAD"), cancellable = true, require = 0)
    private void alterna$canPlayerFitWithinBlocksAndEntitiesWhen(Pose pose, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        Direction gravityDirection = GravityApi.getDirection(self);
        if (gravityDirection == Direction.DOWN) {
            return;
        }

        EntityDimensions dimensions = self.getDimensions(pose);
        float w = dimensions.width() / 2.0F;
        float h = dimensions.height();
        AABB boxAtOrigin = new AABB(-w, 0.0, -w, w, h, w);

        if (gravityDirection.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            boxAtOrigin = boxAtOrigin.move(0.0, -1.0E-6, 0.0);
        }

        AABB rotatedBoxWorld = RotationUtil.boxPlayerToWorld(boxAtOrigin, gravityDirection).move(self.position()).deflate(1.0E-7);
        boolean canFit = self.level().noCollision(self, rotatedBoxWorld);
        cir.setReturnValue(canFit);
    }
}
