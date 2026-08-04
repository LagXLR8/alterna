package com.evandev.zipline.mixin;

import com.evandev.zipline.registry.ZiplineTags;
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

@Mixin({ItemInHandRenderer.class})
public abstract class ItemInHandRendererMixin {
   @Shadow
   public abstract void renderItem(LivingEntity var1, ItemStack var2, ItemDisplayContext var3, PoseStack var4, SubmitNodeCollector var5, int var6);

   @Shadow
   protected abstract void renderPlayerArm(PoseStack var1, SubmitNodeCollector var2, int var3, float var4, float var5, HumanoidArm var6);

   @Inject(
      method = {"renderArmWithItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   void renderArmWithItem(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
      if (itemStack.is(ZiplineTags.ATTACHMENT) && player.isUsingItem()) {
         poseStack.pushPose();
         boolean bl = hand == InteractionHand.MAIN_HAND;
         HumanoidArm humanoidArm = bl ? player.getMainArm() : player.getMainArm().getOpposite();
         boolean bl2 = humanoidArm == HumanoidArm.RIGHT;
         int q = bl2 ? 1 : -1;
         this.zipline$shake(itemStack, player, frameInterp, poseStack);
         poseStack.translate((double)0.0F, -0.4, 0.2);
         poseStack.mulPose(Axis.XP.rotationDegrees(10.0F));
         poseStack.mulPose(Axis.YN.rotationDegrees((float)q * -10.0F));
         poseStack.mulPose(Axis.ZP.rotationDegrees((float)q * 70.0F));
         this.renderPlayerArm(poseStack, submitNodeCollector, lightCoords, 0.0F, 0.0F, humanoidArm);
         poseStack.popPose();
         poseStack.pushPose();
         this.zipline$shake(itemStack, player, frameInterp, poseStack);
         poseStack.translate((double)q * 0.4, 0.3, -0.8);
         poseStack.mulPose(Axis.ZP.rotationDegrees((float)q * 15.0F));
         poseStack.mulPose(Axis.XP.rotationDegrees(15.0F));
         this.renderItem(player, itemStack, bl2 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND, poseStack, submitNodeCollector, lightCoords);
         poseStack.popPose();
         ci.cancel();
      }
   }

   @Unique
   void zipline$shake(ItemStack itemStack, AbstractClientPlayer abstractClientPlayer, float frameInterp, PoseStack poseStack) {
      float useFactor = (float)itemStack.getUseDuration(abstractClientPlayer) - ((float)abstractClientPlayer.getUseItemRemainingTicks() - frameInterp + 1.0F);
      float m = Mth.sin((double)((useFactor - 0.1F) * 1.3F));
      float q = Mth.sin((double)((useFactor * 0.3F - 0.4F) * 1.3F));
      float influence = Mth.clamp(useFactor * 0.1F - 0.1F, 0.0F, 1.0F);
      float o = m * influence;
      float l = q * influence;
      poseStack.translate(l * 0.003F, o * 0.001F, o * 0.001F);
   }
}
