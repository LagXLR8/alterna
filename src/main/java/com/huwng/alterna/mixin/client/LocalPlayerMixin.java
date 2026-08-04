package com.huwng.alterna.mixin.client;

import com.huwng.alterna.gravity.GravityApi;
import com.huwng.alterna.gravity.GravityData;
import com.huwng.alterna.gravity.RotationUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-only mixin for LocalPlayer.
 *
 * Two responsibilities:
 * 1. Prevent wrong "push out of blocks" in non-DOWN gravity.
 * 2. When gravity direction changes, compute startGravityQuaternion (so the camera
 *    transition is visually continuous) and then adjust yaw/pitch (so the final camera
 *    orientation looks natural for the new gravity surface).
 *
 * The startGravityQuaternion ensures progress=0 of the CameraMixin slerp matches
 * exactly what the camera was showing before the direction change — no visual jump.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    /** Track the last known gravity direction to detect changes after server sync. */
    @Unique
    private Direction alterna$lastKnownDirection = Direction.DOWN;

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true, require = 0)
    private void alterna$preventWrongPushOutOfBlocks(double x, double z, CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        if (GravityApi.getDirection(self) != Direction.DOWN) {
            ci.cancel();
        }
    }

    /**
     * Detect gravity direction change on the client tick and:
     *  1. Compute startGravityQuaternion BEFORE adjusting yRot — ensures visual continuity.
     *  2. Adjust yaw/pitch so the final orientation is natural for the new gravity surface.
     */
    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void alterna$onTick(CallbackInfo ci) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        GravityData data = GravityApi.getData(self);
        Direction current = data.getDirection();
        Direction previous = alterna$lastKnownDirection;

        if (current == previous) {
            return;
        }

        // --- Step 1: compute the startGravityQuaternion BEFORE touching yRot ---
        // We want: startQuat.conj * vanilla(oldYRot) = camera view before change
        // i.e. startQuat = vanilla(oldYRot) * prevGravityRot  (since camera = qGrav.conj * vanilla)
        //
        // More precisely: at progress=0, CameraMixin does: qStart.conj * vanilla(newYRot)
        // We want this to equal: qPrev.conj * vanilla(oldYRot)  (the camera before change)
        // => qStart.conj * vanilla(newYRot) = qPrev.conj * vanilla(oldYRot)
        // => qStart = vanilla(newYRot) * vanilla(oldYRot).conj * qPrev
        // We'll compute this AFTER adjusting yRot so we can use the new values.

        float oldYRot = self.getYRot();
        float oldXRot = self.getXRot();
        Quaternionf qPrev = RotationUtil.getWorldRotationQuaternion(previous);

        // --- Step 2: adjust yaw/pitch for the new gravity direction ---
        alterna$adjustViewRotation(self, previous, current);

        // --- Step 3: compute startGravityQuaternion now that we have newYRot/newXRot ---
        // vanilla rotation = rotationYXZ(-yRot, -xRot, 0)
        float oldYRotRad = (float) Math.toRadians(oldYRot);
        float oldXRotRad = (float) Math.toRadians(oldXRot);
        float newYRotRad = (float) Math.toRadians(self.getYRot());
        float newXRotRad = (float) Math.toRadians(self.getXRot());

        Quaternionf oldVanilla = new Quaternionf().rotationYXZ(-oldYRotRad, -oldXRotRad, 0f);
        Quaternionf newVanilla = new Quaternionf().rotationYXZ(-newYRotRad, -newXRotRad, 0f);

        // startQuat = vanilla(newYRot) * vanilla(oldYRot).conj * qPrev
        // = newVanilla * oldVanilla.conj * qPrev
        Quaternionf startQuat = new Quaternionf(newVanilla)
                .mul(new Quaternionf(oldVanilla).conjugate())
                .mul(qPrev);
        data.setStartGravityQuaternion(startQuat);

        alterna$lastKnownDirection = current;
    }

    /**
     * Physically rotate yaw/pitch so the look direction follows the body
     * when stepping onto a new gravity surface (rotateView = true).
     * Port of GravityChanger RotationAnimation.startRotationAnimation yaw/pitch logic.
     */
    @Unique
    private void alterna$adjustViewRotation(LocalPlayer entity, Direction oldDir, Direction newDir) {
        Vec3 oldLookWorld = RotationUtil.vecPlayerToWorld(
                RotationUtil.rotToVec(entity.getYRot(), entity.getXRot()), oldDir);

        Vec3 newLookWorld;
        if (newDir == oldDir.getOpposite()) {
            newLookWorld = oldLookWorld.scale(-1.0);
        } else {
            // Rotate look direction by physical gravity delta: oldNormal → newNormal
            Vec3 fromNormal = new Vec3(oldDir.step());
            Vec3 toNormal   = new Vec3(newDir.step());
            Quaternionf deltaRot = alterna$rotationBetween(fromNormal, toNormal);
            Vector3f look = new Vector3f(
                    (float) oldLookWorld.x, (float) oldLookWorld.y, (float) oldLookWorld.z);
            look.rotate(deltaRot);
            newLookWorld = new Vec3(look.x, look.y, look.z);
        }

        Vec2 newRot = RotationUtil.vecToRot(RotationUtil.vecWorldToPlayer(newLookWorld, newDir));
        float deltaYaw   = Mth.wrapDegrees(newRot.x - entity.getYRot());
        float deltaPitch = newRot.y - entity.getXRot();

        entity.setYRot(entity.getYRot() + deltaYaw);
        entity.setXRot(entity.getXRot() + deltaPitch);
        entity.yRotO += deltaYaw;
        entity.xRotO += deltaPitch;

        LivingEntity living = entity;
        living.yBodyRot  += deltaYaw;
        living.yBodyRotO += deltaYaw;
        living.yHeadRot  += deltaYaw;
        living.yHeadRotO += deltaYaw;
    }

    /** Quaternion rotating unit vector {@code from} onto unit vector {@code to}. */
    @Unique
    private static Quaternionf alterna$rotationBetween(Vec3 from, Vec3 to) {
        Vec3 axis = from.cross(to);
        double len = axis.length();
        if (len < 1e-9) return new Quaternionf();
        axis = axis.scale(1.0 / len);
        float angle = (float) Math.acos(Mth.clamp(from.dot(to), -1.0, 1.0));
        return new Quaternionf().fromAxisAngleRad(
                new Vector3f((float) axis.x, (float) axis.y, (float) axis.z), angle);
    }
}
