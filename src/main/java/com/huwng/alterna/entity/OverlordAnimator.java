package com.huwng.alterna.entity;

import com.huwng.alterna.client.render.state.ArmorOverlordRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Client-only: turns the Overlord's synced attack events, procedural legs and target into the
 * pose numbers its model needs (feet, body attitude, where each hooked arm reaches, glow).
 * Kept out of the entity class so a dedicated server never loads render classes.
 */
public final class OverlordAnimator {
    private OverlordAnimator() {}

    /** Fills the procedural parts of the render state (legs, body attitude, arms, glow). */
    public static void extract(ArmorOverlordEntity o, ArmorOverlordRenderState state, float partial) {
        double px = state.x, py = state.y, pz = state.z;
        float yaw = state.bodyRot * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
        double rx = -Mth.cos(yaw), rz = -Mth.sin(yaw);
        float s = 16.0F / ArmorOverlordEntity.SCALE;

        OverlordLegs legs = o.legs();
        state.legsActive = legs != null && legs.ready();
        if (legs != null && state.legsActive) {
            for (int i = 0; i < 4; i++) {
                Vec3 f = legs.foot(i, partial);
                double dx = f.x - px, dy = f.y - py, dz = f.z - pz;
                state.footX[i] = (float) (-(dx * rx + dz * rz) * s);
                state.footY[i] = (float) (24.0 - dy * s);
                state.footZ[i] = (float) (-(dx * fx + dz * fz) * s);
            }
            state.bodyPitch = Mth.lerp(partial, legs.pitchO, legs.pitch);
            state.bodyRoll = Mth.lerp(partial, legs.rollO, legs.roll);
            state.bodyDrop = -Mth.lerp(partial, legs.dropO, legs.drop) * s;
            state.stepBob = Mth.lerp(partial, legs.bobO, legs.bob) * s * 0.35F;
            state.stepSway = Mth.lerp(partial, legs.swayO, legs.sway);
        }
        state.brainGlow = Mth.lerp(partial, o.glowO, o.glow);
        if (o.clRoar > 0) state.brainGlow *= 0.85F + 0.15F * Mth.sin(state.ageInTicks * 1.7F);
        if (o.deathTime > 0) state.brainGlow *= Math.max(0.0F, 1.0F - (o.deathTime + partial) / 30.0F);

        // where the arms reach: the prey (chest height) or the ground in front
        Entity t = o.level().getEntity(o.targetId());
        Vec3 aim = t != null ? t.getPosition(partial).add(0, t.getBbHeight() * 0.55, 0)
                : new Vec3(px + fx * 3.5, py + 0.5, pz + fz * 3.5);
        double adx = aim.x - px, ady = aim.y - py, adz = aim.z - pz;
        float ax = (float) (-(adx * rx + adz * rz) * s), ay = (float) (24.0 - ady * s), az = (float) (-(adx * fx + adz * fz) * s);
        boolean hunting = o.isHunting();

        for (int i = 0; i < 2; i++) {
            state.armReach[i] = 0.0F;
            state.bladeOpen[i] = hunting ? 0.25F : 0.0F;
        }
        state.stomp = 0.0F;

        if (o.clRoar > 0) {
            // rear up and spread the hooks
            float k = envelope(o.clRoar, ArmorOverlordEntity.ROAR_TIME, partial, 5.0F);
            for (int i = 0; i < 2; i++) {
                float side = i == 0 ? -1.0F : 1.0F;
                reach(state, i, side * 22.0F, -26.0F, -14.0F, k, k * 0.8F);
            }
            state.stomp = -0.6F * k;
        }
        if (o.clHookWind > 0) {
            // cock the right hook far back over the cap
            float k = 1.0F - Mth.clamp((o.clHookWind - partial) / ArmorOverlordEntity.HOOK_WINDUP, 0.0F, 1.0F);
            float e = k * k * (3.0F - 2.0F * k);
            reach(state, 0, -12.0F, -30.0F, 6.0F, e, 0.0F);
        }
        if (o.clHookStrike > 0) {
            float age = (ArmorOverlordEntity.HOOK_TIME - ArmorOverlordEntity.HOOK_WINDUP) - (o.clHookStrike - partial);     // 0.. ticks since the strike
            float k = age < 4.0F ? 1.0F : Mth.clamp(1.0F - (age - 4.0F) / 9.0F, 0.0F, 1.0F);
            reach(state, 0, ax, ay, az, k, 1.0F);
        }
        if (o.clSlash > 0) {
            float age = ArmorOverlordEntity.SLASH_TIME - (o.clSlash - partial);
            for (int i = 0; i < 2; i++) {
                float side = i == 0 ? -1.0F : 1.0F;
                float hit = i == 0 ? ArmorOverlordEntity.SLASH_HIT_1 : ArmorOverlordEntity.SLASH_HIT_2;
                float raise = Mth.clamp(age / 6.0F, 0.0F, 1.0F) * Mth.clamp((hit - age) / 2.0F, 0.0F, 1.0F);
                float strike = Mth.clamp(1.0F - Math.abs(age - hit) / 3.5F, 0.0F, 1.0F);
                float release = Mth.clamp((ArmorOverlordEntity.SLASH_TIME - age) / 5.0F, 0.0F, 1.0F);
                if (strike > 0.0F) {
                    reach(state, i, ax + side * 4.0F, ay + 6.0F, az, Math.min(strike * 1.6F, 1.0F), 1.0F);
                } else if (age < hit) {
                    reach(state, i, side * 16.0F, -32.0F, -16.0F, raise, 0.6F);
                } else {
                    reach(state, i, side * 10.0F, -10.0F, -24.0F, release * 0.6F, 0.4F);
                }
            }
        }
        if (o.clStomp > 0) {
            float age = ArmorOverlordEntity.STOMP_TIME - (o.clStomp - partial);
            float stomp;
            if (age < ArmorOverlordEntity.STOMP_HIT - 2) {
                float k = age / (ArmorOverlordEntity.STOMP_HIT - 2);
                stomp = -0.9F * k * k * (3.0F - 2.0F * k);                 // rears up
            } else if (age < ArmorOverlordEntity.STOMP_HIT) {
                stomp = Mth.lerp((age - (ArmorOverlordEntity.STOMP_HIT - 2)) / 2.0F, -0.9F, 1.0F);   // slams down
            } else {
                stomp = Mth.clamp(1.0F - (age - ArmorOverlordEntity.STOMP_HIT) / 12.0F, 0.0F, 1.0F);
            }
            state.stomp = stomp;
            float tuck = Mth.clamp(1.0F - Math.abs(age - ArmorOverlordEntity.STOMP_HIT) / 8.0F, 0.0F, 1.0F);
            for (int i = 0; i < 2; i++) {
                float side = i == 0 ? -1.0F : 1.0F;
                reach(state, i, side * 14.0F, -2.0F, -12.0F, tuck, 0.0F);
            }
        }
        if (o.deathTime > 0) {
            float d = Mth.clamp((o.deathTime + partial) / 30.0F, 0.0F, 1.0F);
            state.stomp = Math.max(state.stomp, d * 3.0F);           // sinks onto its legs
            for (int i = 0; i < 2; i++) state.bladeOpen[i] = 0.0F;
        }
    }

    private static void reach(ArmorOverlordRenderState state, int arm, float x, float y, float z, float weight, float open) {
        if (weight <= state.armReach[arm] && state.armReach[arm] > 0.0F) return;
        state.armX[arm] = x;
        state.armY[arm] = y;
        state.armZ[arm] = z;
        state.armReach[arm] = Mth.clamp(weight, 0.0F, 1.0F);
        state.bladeOpen[arm] = Math.max(state.bladeOpen[arm], open * weight);
    }

    private static float envelope(int ticks, int total, float partial, float fade) {
        if (ticks <= 0) return 0.0F;
        float t = ticks - partial;
        return Math.min(Mth.clamp((total - t) / fade, 0.0F, 1.0F), Mth.clamp(t / fade, 0.0F, 1.0F));
    }

}
