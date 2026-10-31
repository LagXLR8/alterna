package com.huwng.alterna.client.model;

// Geometry generated from the Alterna model spec (overlord_spec.py); animation is hand-written.
import com.huwng.alterna.client.render.state.ArmorOverlordRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import java.util.EnumSet;

/**
 * Armor Overlord: a mushroom-capped carapace on four huge arching legs, a glowing brain
 * cluster under the brim and two long hooked arms.
 *
 * Legs are fully procedural: every foot is planted on the real ground by OverlordLegs and
 * each leg is solved here with 2-bone IK (a yaw "hip" aims the leg at its foot, then the
 * thigh rises to a high knee and the shin comes down to the foot). The arms are IK too, so
 * the blades really reach for whatever they hook or slash.
 */
public class ArmorOverlordModel extends EntityModel<ArmorOverlordRenderState> {
    private static final float THIGH = 38.0F, SHIN = 66.0F, UPPER_ARM = 15.0F, FORE_ARM = 13.0F, BLADE = 14.0F;
    private static final float BODY_X = 0.0F, BODY_Y = -16.0F, BODY_Z = 0.0F;
    /** Hip and shoulder rest positions relative to the body pivot. */
    private static final float[][] HIPS = {{-12, -4, -12}, {12, -4, -12}, {-12, -4, 12}, {12, -4, 12}};
    private static final float[][] SHOULDERS = {{-8, 3, -9}, {8, 3, -9}};
    private static final String[] LEGS = {"right_front", "left_front", "right_hind", "left_hind"};

    private final ModelPart body, brain, abdomen, twigs;
    private final ModelPart[] hips = new ModelPart[4], thighs = new ModelPart[4], shins = new ModelPart[4];
    private final ModelPart[] arms = new ModelPart[2], forearms = new ModelPart[2], blades = new ModelPart[2];

    public ArmorOverlordModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.brain = this.body.getChild("brain");
        this.abdomen = this.body.getChild("abdomen");
        this.twigs = this.body.getChild("twigs");
        for (int i = 0; i < 4; i++) {
            this.hips[i] = root.getChild(LEGS[i] + "_hip");
            this.thighs[i] = this.hips[i].getChild(LEGS[i] + "_thigh");
            this.shins[i] = this.thighs[i].getChild(LEGS[i] + "_shin");
        }
        String[] sides = {"right", "left"};
        for (int i = 0; i < 2; i++) {
            this.arms[i] = root.getChild(sides[i] + "_arm");
            this.forearms[i] = this.arms[i].getChild(sides[i] + "_forearm");
            this.blades[i] = this.forearms[i].getChild(sides[i] + "_blade");
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 102).addBox(-13F, -3F, -13F, 26F, 9F, 26F),
                PartPose.offsetAndRotation(0F, -16F, 0F, 0F, 0F, 0F));
        PartDefinition p_cap = p_body.addOrReplaceChild("cap", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-26F, -3F, -26F, 52F, 3F, 52F)
                .texOffs(0, 55).addBox(-21F, -8F, -21F, 42F, 5F, 42F)
                .texOffs(104, 102).addBox(-15F, -12F, -15F, 30F, 4F, 30F)
                .texOffs(168, 83).addBox(-8F, -14F, -8F, 16F, 2F, 16F),
                PartPose.offsetAndRotation(0F, -3F, 0F, 0F, 0F, 0F));
        PartDefinition p_teeth = p_body.addOrReplaceChild("teeth", CubeListBuilder.create()
                .texOffs(240, 51).addBox(-18.5F, 0F, -23.6F, 1F, 3F, 1F)
                .texOffs(100, 137).addBox(-14.5F, 0F, -23.6F, 1F, 5F, 1F)
                .texOffs(244, 51).addBox(-10.5F, 0F, -23.6F, 1F, 3F, 1F)
                .texOffs(248, 140).addBox(-6.5F, 0F, -23.6F, 1F, 5F, 1F)
                .texOffs(100, 143).addBox(-2.5F, 0F, -23.6F, 1F, 3F, 1F)
                .texOffs(252, 140).addBox(1.5F, 0F, -23.6F, 1F, 5F, 1F)
                .texOffs(100, 147).addBox(5.5F, 0F, -23.6F, 1F, 3F, 1F)
                .texOffs(240, 141).addBox(9.5F, 0F, -23.6F, 1F, 5F, 1F)
                .texOffs(100, 151).addBox(13.5F, 0F, -23.6F, 1F, 3F, 1F)
                .texOffs(244, 141).addBox(17.5F, 0F, -23.6F, 1F, 5F, 1F)
                .texOffs(84, 152).addBox(-23.6F, 0F, -16.5F, 1F, 3F, 1F)
                .texOffs(88, 152).addBox(22.6F, 0F, -16.5F, 1F, 3F, 1F)
                .texOffs(92, 152).addBox(-23.6F, 0F, -8.5F, 1F, 3F, 1F)
                .texOffs(96, 152).addBox(22.6F, 0F, -8.5F, 1F, 3F, 1F)
                .texOffs(100, 155).addBox(-23.6F, 0F, -0.5F, 1F, 3F, 1F)
                .texOffs(240, 155).addBox(22.6F, 0F, -0.5F, 1F, 3F, 1F)
                .texOffs(244, 155).addBox(-23.6F, 0F, 7.5F, 1F, 3F, 1F)
                .texOffs(248, 155).addBox(22.6F, 0F, 7.5F, 1F, 3F, 1F)
                .texOffs(252, 155).addBox(-23.6F, 0F, 15.5F, 1F, 3F, 1F)
                .texOffs(84, 156).addBox(22.6F, 0F, 15.5F, 1F, 3F, 1F),
                PartPose.offsetAndRotation(0F, -3F, 0F, 0F, 0F, 0F));
        PartDefinition p_brain = p_body.addOrReplaceChild("brain", CubeListBuilder.create()
                .texOffs(184, 199).addBox(-6F, -2F, -5F, 12F, 7F, 9F)
                .texOffs(232, 83).addBox(-9F, 0F, -3F, 4F, 4F, 5F)
                .texOffs(232, 92).addBox(5F, 0F, -3F, 4F, 4F, 5F)
                .texOffs(232, 179).addBox(-3F, 5F, -4F, 6F, 3F, 5F),
                PartPose.offsetAndRotation(0F, 5F, -11F, 0F, 0F, 0F));
        PartDefinition p_abdomen = p_body.addOrReplaceChild("abdomen", CubeListBuilder.create()
                .texOffs(188, 136).addBox(-7F, 0F, -6F, 14F, 11F, 12F)
                .texOffs(0, 205).addBox(-5F, 11F, -4F, 10F, 7F, 8F),
                PartPose.offsetAndRotation(0F, 5F, 3F, 0.15F, 0F, 0F));
        PartDefinition p_twigs = p_body.addOrReplaceChild("twigs", CubeListBuilder.create()
                .texOffs(250, 86).addBox(-4F, 0F, -1F, 1F, 7F, 1F)
                .texOffs(250, 94).addBox(-2F, 1F, -2F, 1F, 6F, 1F)
                .texOffs(252, 125).addBox(1F, 1F, -2F, 1F, 6F, 1F)
                .texOffs(248, 125).addBox(3F, 0F, -1F, 1F, 7F, 1F),
                PartPose.offsetAndRotation(0F, 12F, -12F, 0F, 0F, 0F));
        PartDefinition p_rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(232, 0).addBox(-1.5F, -1F, -1.5F, 3F, 17F, 3F),
                PartPose.offsetAndRotation(-8F, -13F, -9F, 0F, 0F, 0F));
        PartDefinition p_rightForearm = p_rightArm.addOrReplaceChild("right_forearm", CubeListBuilder.create()
                .texOffs(248, 20).addBox(-1F, -1F, -1F, 2F, 15F, 2F),
                PartPose.offsetAndRotation(0F, 15F, 0F, 0F, 0F, 0F));
        PartDefinition p_rightBlade = p_rightForearm.addOrReplaceChild("right_blade", CubeListBuilder.create()
                .texOffs(252, 54).addBox(-0.5F, -1F, -0.5F, 1F, 15F, 1F)
                .texOffs(208, 0).addBox(0F, 0F, -4F, 0F, 16F, 6F, EnumSet.of(Direction.EAST)),
                PartPose.offsetAndRotation(0F, 13F, 0F, 0F, 0F, 0F));
        PartDefinition p_leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(244, 0).mirror().addBox(-1.5F, -1F, -1.5F, 3F, 17F, 3F).mirror(false),
                PartPose.offsetAndRotation(8F, -13F, -9F, 0F, 0F, 0F));
        PartDefinition p_leftForearm = p_leftArm.addOrReplaceChild("left_forearm", CubeListBuilder.create()
                .texOffs(248, 37).mirror().addBox(-1F, -1F, -1F, 2F, 15F, 2F).mirror(false),
                PartPose.offsetAndRotation(0F, 15F, 0F, 0F, 0F, 0F));
        PartDefinition p_leftBlade = p_leftForearm.addOrReplaceChild("left_blade", CubeListBuilder.create()
                .texOffs(252, 70).mirror().addBox(-0.5F, -1F, -0.5F, 1F, 15F, 1F).mirror(false)
                .texOffs(220, 0).mirror().addBox(0F, 0F, -4F, 0F, 16F, 6F, EnumSet.of(Direction.EAST)).mirror(false),
                PartPose.offsetAndRotation(0F, 13F, 0F, 0F, 0F, 0F));
        PartDefinition p_rightFrontHip = root.addOrReplaceChild("right_front_hip", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-12F, -20F, -12F, 0F, 0F, 0F));
        PartDefinition p_rightFrontThigh = p_rightFrontHip.addOrReplaceChild("right_front_thigh", CubeListBuilder.create()
                .texOffs(36, 212).addBox(0F, -3F, -3F, 38F, 6F, 6F)
                .texOffs(208, 22).addBox(32F, -5F, -5F, 10F, 10F, 10F)
                .texOffs(240, 42).addBox(36F, -12F, -1F, 2F, 7F, 2F)
                .texOffs(248, 133).addBox(40F, -10F, 1F, 2F, 5F, 2F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_rightFrontShin = p_rightFrontThigh.addOrReplaceChild("right_front_shin", CubeListBuilder.create()
                .texOffs(168, 55).addBox(0F, -7F, -7F, 28F, 14F, 14F)
                .texOffs(188, 159).addBox(28F, -5F, -5F, 22F, 10F, 10F)
                .texOffs(208, 42).addBox(50F, -3F, -3F, 10F, 6F, 6F)
                .texOffs(84, 144).addBox(60F, -1F, -1F, 6F, 2F, 2F),
                PartPose.offsetAndRotation(38F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_leftFrontHip = root.addOrReplaceChild("left_front_hip", CubeListBuilder.create(),
                PartPose.offsetAndRotation(12F, -20F, -12F, 0F, 0F, 0F));
        PartDefinition p_leftFrontThigh = p_leftFrontHip.addOrReplaceChild("left_front_thigh", CubeListBuilder.create()
                .texOffs(124, 219).addBox(0F, -3F, -3F, 38F, 6F, 6F)
                .texOffs(64, 192).addBox(32F, -5F, -5F, 10F, 10F, 10F)
                .texOffs(224, 125).addBox(36F, -12F, -1F, 2F, 7F, 2F)
                .texOffs(240, 134).addBox(40F, -10F, 1F, 2F, 5F, 2F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_leftFrontShin = p_leftFrontThigh.addOrReplaceChild("left_front_shin", CubeListBuilder.create()
                .texOffs(104, 136).addBox(0F, -7F, -7F, 28F, 14F, 14F)
                .texOffs(0, 165).addBox(28F, -5F, -5F, 22F, 10F, 10F)
                .texOffs(224, 101).addBox(50F, -3F, -3F, 10F, 6F, 6F)
                .texOffs(240, 147).addBox(60F, -1F, -1F, 6F, 2F, 2F),
                PartPose.offsetAndRotation(38F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_rightHindHip = root.addOrReplaceChild("right_hind_hip", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-12F, -20F, 12F, 0F, 0F, 0F));
        PartDefinition p_rightHindThigh = p_rightHindHip.addOrReplaceChild("right_hind_thigh", CubeListBuilder.create()
                .texOffs(0, 224).addBox(0F, -3F, -3F, 38F, 6F, 6F)
                .texOffs(104, 192).addBox(32F, -5F, -5F, 10F, 10F, 10F)
                .texOffs(232, 125).addBox(36F, -12F, -1F, 2F, 7F, 2F)
                .texOffs(84, 137).addBox(40F, -10F, 1F, 2F, 5F, 2F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_rightHindShin = p_rightHindThigh.addOrReplaceChild("right_hind_shin", CubeListBuilder.create()
                .texOffs(0, 137).addBox(0F, -7F, -7F, 28F, 14F, 14F)
                .texOffs(168, 179).addBox(28F, -5F, -5F, 22F, 10F, 10F)
                .texOffs(224, 113).addBox(50F, -3F, -3F, 10F, 6F, 6F)
                .texOffs(84, 148).addBox(60F, -1F, -1F, 6F, 2F, 2F),
                PartPose.offsetAndRotation(38F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_leftHindHip = root.addOrReplaceChild("left_hind_hip", CubeListBuilder.create(),
                PartPose.offsetAndRotation(12F, -20F, 12F, 0F, 0F, 0F));
        PartDefinition p_leftHindThigh = p_leftHindHip.addOrReplaceChild("left_hind_thigh", CubeListBuilder.create()
                .texOffs(88, 231).addBox(0F, -3F, -3F, 38F, 6F, 6F)
                .texOffs(144, 199).addBox(32F, -5F, -5F, 10F, 10F, 10F)
                .texOffs(240, 125).addBox(36F, -12F, -1F, 2F, 7F, 2F)
                .texOffs(92, 137).addBox(40F, -10F, 1F, 2F, 5F, 2F),
                PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_leftHindShin = p_leftHindThigh.addOrReplaceChild("left_hind_shin", CubeListBuilder.create()
                .texOffs(84, 164).addBox(0F, -7F, -7F, 28F, 14F, 14F)
                .texOffs(0, 185).addBox(28F, -5F, -5F, 22F, 10F, 10F)
                .texOffs(212, 215).addBox(50F, -3F, -3F, 10F, 6F, 6F)
                .texOffs(240, 151).addBox(60F, -1F, -1F, 6F, 2F, 2F),
                PartPose.offsetAndRotation(38F, 0F, 0F, 0F, 0F, 0F));
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(ArmorOverlordRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;

        // ---- body rides on the legs: tilts with the ground, bobs with each stride, crouches to stomp
        float pitch = state.legsActive ? -state.bodyPitch : 0.0F;
        float roll = state.legsActive ? -state.bodyRoll + state.stepSway : 0.0F;
        float lift = (state.legsActive ? state.bodyDrop - state.stepBob : 0.0F) + state.stomp * 6.0F
                + Mth.sin(age * 0.05F) * 0.5F;
        this.body.xRot = pitch;
        this.body.zRot = roll;
        this.body.y += lift;
        float pulse = 1.0F + Mth.sin(age * 0.2F) * 0.03F * (0.5F + state.brainGlow);
        this.brain.xScale = pulse;
        this.brain.yScale = pulse;
        this.brain.zScale = pulse;
        this.abdomen.xRot += Mth.sin(age * 0.07F) * 0.05F;
        this.abdomen.zRot = Mth.sin(age * 0.05F) * 0.04F;
        this.twigs.xRot = Mth.sin(age * 0.23F) * 0.15F;

        // ---- legs
        for (int i = 0; i < 4; i++) {
            float[] hip = this.onBody(HIPS[i][0], HIPS[i][1], HIPS[i][2], lift);
            float fx, fy, fz;
            if (state.legsActive) {
                fx = state.footX[i];
                fy = state.footY[i];
                fz = state.footZ[i];
            } else {
                // inventory / first frame: feet spread out on flat ground
                fx = hip[0] + Math.signum(HIPS[i][0]) * 28.0F;
                fy = 24.0F;
                fz = hip[2] + Math.signum(HIPS[i][2]) * 28.0F;
            }
            this.solveLeg(i, hip, fx, fy, fz);
        }

        // ---- hook arms: hang folded like a mantis, or reach for their target
        for (int i = 0; i < 2; i++) {
            float side = i == 0 ? -1.0F : 1.0F;
            float[] sh = this.onBody(SHOULDERS[i][0], SHOULDERS[i][1], SHOULDERS[i][2], lift);
            float sway = Mth.sin(age * 0.09F + i * 1.7F);
            // idle: hand held low and forward, slowly feeling about
            float ix = sh[0] + side * (2.0F + sway), iy = sh[1] + 20.0F + sway * 1.5F, iz = sh[2] - 9.0F + Mth.cos(age * 0.07F + i) * 2.0F;
            float k = state.armReach[i];
            float tx = Mth.lerp(k, ix, state.armX[i]), ty = Mth.lerp(k, iy, state.armY[i]), tz = Mth.lerp(k, iz, state.armZ[i]);
            this.solveArm(i, sh, tx, ty, tz);
            this.blades[i].xRot = Mth.lerp(state.bladeOpen[i], 1.2F, 0.15F);
        }
    }

    /** Where a point fixed to the body ends up after the body's pitch/roll and lift. */
    private float[] onBody(float x, float y, float z, float lift) {
        float cx = Mth.cos(this.body.xRot), sx = Mth.sin(this.body.xRot);
        float y1 = y * cx - z * sx;
        float z1 = y * sx + z * cx;
        float cz = Mth.cos(this.body.zRot), sz = Mth.sin(this.body.zRot);
        float x2 = x * cz - y1 * sz;
        float y2 = x * sz + y1 * cz;
        return new float[]{BODY_X + x2, BODY_Y + lift + y2, BODY_Z + z1};
    }

    /**
     * Leg IK. The hip part only turns (yaw) to face the foot; inside that vertical plane the
     * thigh (built along +x) is raised so the knee arches high above, and the shin bends down
     * to put the claw on the foot position.
     */
    private void solveLeg(int i, float[] hip, float fx, float fy, float fz) {
        ModelPart h = this.hips[i];
        h.x = hip[0];
        h.y = hip[1];
        h.z = hip[2];
        float dx = fx - hip[0], dy = fy - hip[1], dz = fz - hip[2];
        float horiz = Mth.sqrt(dx * dx + dz * dz);
        h.yRot = (float) Math.atan2(-dz, dx);
        float r = Mth.clamp(Mth.sqrt(horiz * horiz + dy * dy), SHIN - THIGH + 1.0F, (THIGH + SHIN) * 0.995F);
        float theta = (float) Math.atan2(dy, horiz);
        float alpha = (float) Math.acos(Mth.clamp((THIGH * THIGH + r * r - SHIN * SHIN) / (2.0F * THIGH * r), -1.0F, 1.0F));
        float beta = (float) Math.acos(Mth.clamp((THIGH * THIGH + SHIN * SHIN - r * r) / (2.0F * THIGH * SHIN), -1.0F, 1.0F));
        this.thighs[i].zRot = theta - alpha;        // knee up
        this.shins[i].zRot = Mth.PI - beta;          // shin bends down to the foot
    }

    /**
     * Arm IK: the arm (built along +y) yaws toward the target, then upper arm + forearm bend to
     * reach it. A hook strike at something further away than the arm can reach shoots the
     * forearm out telescopically (it snaps back as the reach fades).
     */
    private void solveArm(int i, float[] sh, float tx, float ty, float tz) {
        ModelPart arm = this.arms[i];
        arm.x = sh[0];
        arm.y = sh[1];
        arm.z = sh[2];
        float dx = tx - sh[0], dy = ty - sh[1], dz = tz - sh[2];
        float horiz = Mth.sqrt(dx * dx + dz * dz);
        arm.yRot = (float) Math.atan2(dx, dz);
        float fore = FORE_ARM + BLADE * 0.6F;
        float dist = Mth.sqrt(horiz * horiz + dy * dy);
        float stretch = Mth.clamp((dist - UPPER_ARM * 0.9F) / fore, 1.0F, 3.2F);
        fore *= stretch;
        float reach = UPPER_ARM + fore;
        float r = Mth.clamp(dist, fore - UPPER_ARM + 1.0F, reach * 0.995F);
        float theta = (float) Math.atan2(horiz, dy);
        float alpha = (float) Math.acos(Mth.clamp((UPPER_ARM * UPPER_ARM + r * r - fore * fore) / (2.0F * UPPER_ARM * r), -1.0F, 1.0F));
        float beta = (float) Math.acos(Mth.clamp((UPPER_ARM * UPPER_ARM + fore * fore - r * r) / (2.0F * UPPER_ARM * fore), -1.0F, 1.0F));
        arm.xRot = theta + alpha;                    // elbow raised, mantis-like
        this.forearms[i].xRot = -(Mth.PI - beta);
        this.forearms[i].yScale = stretch;
        this.blades[i].yScale = 1.0F / stretch;      // keep the blade its own size
    }
}
