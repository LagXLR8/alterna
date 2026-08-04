package com.huwng.alterna.mixin.client;

import com.huwng.alterna.vine.VineTags;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Poses the player's arm straight up when sliding on a vine.
 * Ported from Zipline mod (MIT) — credit: Evan, Tomate0613.
 */
@Mixin(HumanoidModel.class)
public class VineHumanoidModelMixin<T extends HumanoidRenderState> {
    @Shadow
    @Final
    public ModelPart leftArm;
    @Shadow
    @Final
    public ModelPart rightArm;

    @Inject(
            method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At("TAIL")
    )
    private void vine$poseArm(T state, CallbackInfo ci) {
        if (state.isUsingItem) {
            InteractionHand hand = state.useItemHand;
            HumanoidArm mainArm = state.mainArm;
            HumanoidArm usingArm = hand == InteractionHand.MAIN_HAND ? mainArm : mainArm.getOpposite();
            ItemStack useItem = state.getUseItemStackForArm(usingArm);
            if (useItem.is(VineTags.VINE_ATTACHMENT)) {
                vine$positionArm(vine$getArmModel(usingArm));
            }
        }
    }

    @Unique
    private ModelPart vine$getArmModel(HumanoidArm arm) {
        return arm == HumanoidArm.RIGHT ? this.rightArm : this.leftArm;
    }

    @Unique
    private void vine$positionArm(ModelPart arm) {
        arm.xRot = -(float) Math.PI; // Arm pointing straight up
        arm.zRot = 0.0F;
        arm.y = 5.0F;
        arm.z = 0.0F;
    }
}
