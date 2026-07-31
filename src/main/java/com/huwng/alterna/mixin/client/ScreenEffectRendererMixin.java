package com.huwng.alterna.mixin.client;

import com.huwng.alterna.gravity.GravityApi;
import com.huwng.alterna.gravity.RotationUtil;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    @Inject(method = "getOverlayBlock", at = @At("HEAD"), cancellable = true, require = 0)
    private static void alterna$getOverlayBlock(Player player, CallbackInfoReturnable<Pair<BlockState, BlockPos>> cir) {
        Direction gravityDirection = GravityApi.getDirection(player);
        if (gravityDirection == Direction.DOWN) {
            return;
        }

        cir.cancel();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        Vec3 eyePos = player.getEyePosition();
        Vec3 multipliers = RotationUtil.vecPlayerToWorld(player.getBbWidth() * 0.8, 0.1, player.getBbWidth() * 0.8, gravityDirection);

        for (int i = 0; i < 8; ++i) {
            double d = eyePos.x + (((i % 2) - 0.5) * multipliers.x());
            double e = eyePos.y + ((((i >> 1) % 2) - 0.5) * multipliers.y());
            double f = eyePos.z + ((((i >> 2) % 2) - 0.5) * multipliers.z());
            mutable.set(d, e, f);
            BlockState blockState = player.level().getBlockState(mutable);
            if (blockState.getRenderShape() != RenderShape.INVISIBLE && blockState.isViewBlocking(player.level(), mutable)) {
                cir.setReturnValue(Pair.of(blockState, mutable.immutable()));
                return;
            }
        }
        cir.setReturnValue(null);
    }
}
