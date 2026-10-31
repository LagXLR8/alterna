package com.huwng.alterna.client.render;

import com.huwng.alterna.Alterna;
import com.huwng.alterna.client.render.state.GrappleHookRenderState;
import com.huwng.alterna.entity.GrappleHookEntity;
import com.huwng.alterna.item.GrappleItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** The grapple claw (a billboard sprite) and the line back to the owner's hand - taut once it has bitten. */
public class GrappleHookRenderer extends EntityRenderer<GrappleHookEntity, GrappleHookRenderState> {
    private static final Identifier TEXTURE = Alterna.id("textures/entity/grapple_hook.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutoutCull(TEXTURE);
    private static final int ROPE_COLOR = 0xFF3A2C24;

    public GrappleHookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(GrappleHookEntity entity, Frustum culler, double camX, double camY, double camZ) {
        return entity.getPlayerOwner() != null;
    }

    @Override
    public void submit(GrappleHookRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.pushPose();
        poseStack.scale(0.6F, 0.6F, 0.6F);
        poseStack.mulPose(camera.orientation);
        collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            vertex(buffer, pose, state.lightCoords, 0.0F, 0, 0, 1);
            vertex(buffer, pose, state.lightCoords, 1.0F, 0, 1, 1);
            vertex(buffer, pose, state.lightCoords, 1.0F, 1, 1, 0);
            vertex(buffer, pose, state.lightCoords, 0.0F, 1, 0, 0);
        });
        poseStack.popPose();
        float xa = (float) state.lineOriginOffset.x;
        float ya = (float) state.lineOriginOffset.y;
        float za = (float) state.lineOriginOffset.z;
        float sag = state.anchored ? 0.0F : 0.6F;
        float width = Minecraft.getInstance().gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth * 1.5F;
        collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
            for (int i = 0; i < 16; i++) {
                float a0 = i / 16.0F, a1 = (i + 1) / 16.0F;
                ropeVertex(xa, ya, za, sag, buffer, pose, a0, a1, width);
                ropeVertex(xa, ya, za, sag, buffer, pose, a1, a0, width);
            }
        });
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static void vertex(VertexConsumer builder, PoseStack.Pose pose, int light, float x, int y, int u, int v) {
        builder.addVertex(pose, x - 0.5F, y - 0.5F, 0.0F).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void ropeVertex(float xa, float ya, float za, float sag, VertexConsumer buffer, PoseStack.Pose pose, float a, float next, float width) {
        float x = xa * a, y = ya * a - sag * a * (1.0F - a), z = za * a;
        float nx = xa * next - x, ny = ya * next - sag * next * (1.0F - next) - y, nz = za * next - z;
        float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-5F) len = 1.0F;
        buffer.addVertex(pose, x, y, z).setColor(ROPE_COLOR).setNormal(pose, nx / len, ny / len, nz / len).setLineWidth(width);
    }

    private Vec3 handPos(Player owner, float partial) {
        boolean main = owner.getMainHandItem().getItem() instanceof GrappleItem;
        HumanoidArm arm = main ? owner.getMainArm() : owner.getMainArm().getOpposite();
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
        if (this.entityRenderDispatcher.options.getCameraType().isFirstPerson() && owner == Minecraft.getInstance().player) {
            float fov = this.entityRenderDispatcher.options.fov().get().intValue();
            Vec3 view = this.entityRenderDispatcher.camera.getNearPlane(fov).getPointOnPlane(invert * 0.525F, -0.1F).scale(960.0 / fov);
            return owner.getEyePosition(partial).add(view);
        }
        float yRot = Mth.lerp(partial, owner.yBodyRotO, owner.yBodyRot) * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yRot), cos = Mth.cos(yRot);
        double right = invert * 0.35, forward = 0.8;
        return owner.getEyePosition(partial).add(-cos * right - sin * forward, (owner.isCrouching() ? -0.1875 : 0.0) - 0.45, -sin * right + cos * forward);
    }

    @Override
    public GrappleHookRenderState createRenderState() {
        return new GrappleHookRenderState();
    }

    @Override
    public void extractRenderState(GrappleHookEntity entity, GrappleHookRenderState state, float partial) {
        super.extractRenderState(entity, state, partial);
        state.anchored = entity.isAnchored();
        Player owner = entity.getPlayerOwner();
        state.lineOriginOffset = owner == null ? Vec3.ZERO : this.handPos(owner, partial).subtract(entity.getPosition(partial));
    }

    @Override
    protected boolean affectedByCulling(GrappleHookEntity entity) {
        return false;
    }
}
