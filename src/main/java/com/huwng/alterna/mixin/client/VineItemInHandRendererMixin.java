package com.huwng.alterna.mixin.client;

import com.huwng.alterna.vine.VineTags;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * First-person hand/item rendering when sliding on a vine.
 * Ported from Zipline mod (MIT) — credit: Evan, Tomate0613.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class VineItemInHandRendererMixin {
    @Shadow
    public abstract void renderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                                    PoseStack poseStack, SubmitNodeCollector collector, int lightCoords);

    @Shadow
    protected abstract void renderPlayerArm(PoseStack poseStack, SubmitNodeCollector collector,
                                            int lightCoords, float equipProgress, float swingProgress, HumanoidArm arm);

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void vine$renderArmWithItem(
            AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand,
            float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci
    ) {
        if (itemStack.is(VineTags.VINE_ATTACHMENT) && player.isUsingItem()) {
            poseStack.pushPose();
            boolean isMainHand = hand == InteractionHand.MAIN_HAND;
            HumanoidArm humanoidArm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            boolean isRight = humanoidArm == HumanoidArm.RIGHT;
            int side = isRight ? 1 : -1;

            // Render the arm reaching up
            vine$shake(itemStack, player, frameInterp, poseStack);
            poseStack.translate(0.0, -0.4, 0.2);
            poseStack.mulPose(Axis.XP.rotationDegrees(10.0F));
            poseStack.mulPose(Axis.YN.rotationDegrees((float) side * -10.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) side * 70.0F));
            this.renderPlayerArm(poseStack, submitNodeCollector, lightCoords, 0.0F, 0.0F, humanoidArm);
            poseStack.popPose();

            // Render the held item
            poseStack.pushPose();
            vine$shake(itemStack, player, frameInterp, poseStack);
            poseStack.translate((double) side * 0.4, 0.3, -0.8);
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) side * 15.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(15.0F));
            this.renderItem(player, itemStack,
                    isRight ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                    poseStack, submitNodeCollector, lightCoords);
            poseStack.popPose();

            ci.cancel();
        }
    }

    @Unique
    private void vine$shake(ItemStack itemStack, AbstractClientPlayer player, float frameInterp, PoseStack poseStack) {
        float useFactor = (float) itemStack.getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - frameInterp + 1.0F);
        float m = Mth.sin((double) ((useFactor - 0.1F) * 1.3F));
        float q = Mth.sin((double) ((useFactor * 0.3F - 0.4F) * 1.3F));
        float influence = Mth.clamp(useFactor * 0.1F - 0.1F, 0.0F, 1.0F);
        float o = m * influence;
        float l = q * influence;
        poseStack.translate(l * 0.003F, o * 0.001F, o * 0.001F);
    }
}
