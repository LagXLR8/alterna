package com.huwng.alterna.client.model;

// Geometry generated from the Alterna model spec (cragback_spec.py); animation is hand-written.
import com.huwng.alterna.client.render.state.CragbackRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Cragback model with procedural legs: the entity plants each foot on the real ground
 * and steps in diagonal pairs (see CragbackLegs); here every leg is solved with 2-bone IK
 * (thigh + shin) to reach its foot, the foot stays flat, and the body pitches/rolls with
 * the terrain under it.
 */
public class CragbackModel extends EntityModel<CragbackRenderState> {
    /** Leg/body geometry of one model variant (model pixels). */
    private record Rig(float thigh, float shin, float foot, float bodyX, float bodyY, float bodyZ,
                       float headX, float headY, float headZ, float headPitch, float[][] hips) {}
    private static final Rig ADULT = new Rig(7.5F, 5.5F, 2.0F, 0.0F, 6.0F, 2.0F, 0.0F, 1.0F, -19.0F, 0.38F,
            new float[][]{{-8, 9, -13}, {8, 9, -13}, {-8, 9, 15}, {8, 9, 15}});
    private static final Rig BABY = new Rig(3.5F, 3.0F, 1.0F, 0.0F, 13.0F, 1.0F, 0.0F, 12.0F, -8.0F, 0.3F,
            new float[][]{{-4, 16.5F, -6.5F}, {4, 16.5F, -6.5F}, {-4, 16.5F, 7.5F}, {4, 16.5F, 7.5F}});

    private final Rig rig;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart rightEar;
    private final ModelPart leftEar;
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart[] lowers = new ModelPart[4];
    private final ModelPart[] feet = new ModelPart[4];

    public CragbackModel(ModelPart root) {
        this(root, false);
    }

    public CragbackModel(ModelPart root, boolean baby) {
        super(root);
        this.rig = baby ? BABY : ADULT;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.tail = this.body.getChild("tail");
        this.rightEar = this.head.getChild("right_ear");
        this.leftEar = this.head.getChild("left_ear");
        String[] names = {"right_front_leg", "left_front_leg", "right_hind_leg", "left_hind_leg"};
        for (int i = 0; i < 4; i++) {
            this.legs[i] = root.getChild(names[i]);
            this.lowers[i] = this.legs[i].getChild(names[i] + "_lower");
            this.feet[i] = this.lowers[i].getChild(names[i] + "_foot");
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-12F, -17F, -21F, 24F, 21F, 40F)
                .texOffs(128, 34).addBox(-10F, -19F, -19F, 20F, 3F, 30F)
                .texOffs(128, 0).addBox(-11.5F, 1F, -17F, 23F, 4F, 30F),
                PartPose.offsetAndRotation(0F, 6F, 2F, 0F, 0F, 0F));
        PartDefinition p_shoulderPlate = p_body.addOrReplaceChild("shoulder_plate", CubeListBuilder.create()
                .texOffs(132, 67).addBox(-13.5F, -3F, -6F, 27F, 6F, 15F)
                .texOffs(228, 34).addBox(-14F, 0.5F, -4F, 3F, 10F, 11F)
                .texOffs(228, 55).mirror().addBox(11F, 0.5F, -4F, 3F, 10F, 11F).mirror(false),
                PartPose.offsetAndRotation(0F, -17F, -14F, 0F, 0F, 0F));
        PartDefinition p_hipPlate = p_body.addOrReplaceChild("hip_plate", CubeListBuilder.create()
                .texOffs(132, 88).addBox(-12.5F, -2.6F, -5F, 25F, 4F, 14F)
                .texOffs(216, 76).addBox(-13F, 1F, -3F, 3F, 8F, 11F)
                .texOffs(58, 83).mirror().addBox(10F, 1F, -3F, 3F, 8F, 11F).mirror(false),
                PartPose.offsetAndRotation(0F, -17F, 8F, 0F, 0F, 0F));
        PartDefinition p_finFront = p_body.addOrReplaceChild("fin_front", CubeListBuilder.create()
                .texOffs(102, 67).addBox(-1.5F, -9F, -6F, 3F, 10F, 12F)
                .texOffs(234, 0).addBox(-1F, -14F, -4F, 2F, 5F, 8F),
                PartPose.offsetAndRotation(0F, -19F, -10F, 0.12F, 0F, 0F));
        PartDefinition p_finBack = p_body.addOrReplaceChild("fin_back", CubeListBuilder.create()
                .texOffs(122, 106).addBox(-1F, -7F, -5F, 2F, 8F, 10F)
                .texOffs(234, 24).addBox(-0.5F, -10F, -3F, 1F, 3F, 6F),
                PartPose.offsetAndRotation(0F, -19F, 6F, 0.2F, 0F, 0F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 61).addBox(-8F, -9F, -13F, 16F, 16F, 13F)
                .texOffs(58, 61).addBox(-6.5F, -6F, -22F, 13F, 13F, 9F)
                .texOffs(122, 134).addBox(-5.5F, 6F, -21F, 11F, 3F, 8F)
                .texOffs(32, 122).addBox(-9F, -11F, -4F, 18F, 7F, 7F),
                PartPose.offsetAndRotation(0F, 1F, -19F, 0.38F, 0F, 0F));
        PartDefinition p_horn = p_head.addOrReplaceChild("horn", CubeListBuilder.create()
                .texOffs(146, 106).addBox(-3.5F, -9F, -3.5F, 7F, 9F, 7F)
                .texOffs(36, 90).addBox(-2.5F, -15F, -2.5F, 5F, 6F, 5F)
                .texOffs(216, 67).addBox(-1.5F, -19F, -1.5F, 3F, 4F, 3F),
                PartPose.offsetAndRotation(0F, -5F, -18.5F, -0.15F, 0F, 0F));
        PartDefinition p_hornSmall = p_head.addOrReplaceChild("horn_small", CubeListBuilder.create()
                .texOffs(104, 107).addBox(-2F, -4F, -2F, 4F, 4F, 4F),
                PartPose.offsetAndRotation(0F, -9F, -10F, -0.1F, 0F, 0F));
        PartDefinition p_rightEar = p_head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(102, 61).addBox(-3F, -1F, -0.7F, 3F, 2F, 3F),
                PartPose.offsetAndRotation(-7F, -6F, -3F, 0F, 0F, -0.5F));
        PartDefinition p_leftEar = p_head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(114, 61).mirror().addBox(0F, -1F, -0.7F, 3F, 2F, 3F).mirror(false),
                PartPose.offsetAndRotation(7F, -6F, -3F, 0F, 0F, 0.5F));
        PartDefinition p_rightFrontLeg = root.addOrReplaceChild("right_front_leg", CubeListBuilder.create()
                .texOffs(86, 89).addBox(-4.5F, -1F, -4.5F, 9F, 9F, 9F),
                PartPose.offsetAndRotation(-8F, 9F, -13F, 0F, 0F, 0F));
        PartDefinition p_rightFrontLegLower = p_rightFrontLeg.addOrReplaceChild("right_front_leg_lower", CubeListBuilder.create()
                .texOffs(174, 106).addBox(-4F, -1F, -4F, 8F, 7F, 8F),
                PartPose.offsetAndRotation(0F, 7.5F, 0F, 0F, 0F, 0F));
        PartDefinition p_rightFrontLegFoot = p_rightFrontLegLower.addOrReplaceChild("right_front_leg_foot", CubeListBuilder.create()
                .texOffs(82, 122).addBox(-5F, 0F, -5F, 10F, 2F, 10F),
                PartPose.offsetAndRotation(0F, 5.5F, 0F, 0F, 0F, 0F));
        PartDefinition p_leftFrontLeg = root.addOrReplaceChild("left_front_leg", CubeListBuilder.create()
                .texOffs(0, 90).mirror().addBox(-4.5F, -1F, -4.5F, 9F, 9F, 9F).mirror(false),
                PartPose.offsetAndRotation(8F, 9F, -13F, 0F, 0F, 0F));
        PartDefinition p_leftFrontLegLower = p_leftFrontLeg.addOrReplaceChild("left_front_leg_lower", CubeListBuilder.create()
                .texOffs(72, 107).mirror().addBox(-4F, -1F, -4F, 8F, 7F, 8F).mirror(false),
                PartPose.offsetAndRotation(0F, 7.5F, 0F, 0F, 0F, 0F));
        PartDefinition p_leftFrontLegFoot = p_leftFrontLegLower.addOrReplaceChild("left_front_leg_foot", CubeListBuilder.create()
                .texOffs(146, 122).mirror().addBox(-5F, 0F, -5F, 10F, 2F, 10F).mirror(false),
                PartPose.offsetAndRotation(0F, 5.5F, 0F, 0F, 0F, 0F));
        PartDefinition p_rightHindLeg = root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create()
                .texOffs(210, 95).addBox(-4.5F, -1F, -4.5F, 9F, 9F, 9F),
                PartPose.offsetAndRotation(-8F, 9F, 15F, 0F, 0F, 0F));
        PartDefinition p_rightHindLegLower = p_rightHindLeg.addOrReplaceChild("right_hind_leg_lower", CubeListBuilder.create()
                .texOffs(0, 108).addBox(-4F, -1F, -4F, 8F, 7F, 8F),
                PartPose.offsetAndRotation(0F, 7.5F, 0F, 0F, 0F, 0F));
        PartDefinition p_rightHindLegFoot = p_rightHindLegLower.addOrReplaceChild("right_hind_leg_foot", CubeListBuilder.create()
                .texOffs(186, 128).addBox(-5F, 0F, -5F, 10F, 2F, 10F),
                PartPose.offsetAndRotation(0F, 5.5F, 0F, 0F, 0F, 0F));
        PartDefinition p_leftHindLeg = root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create()
                .texOffs(36, 102).mirror().addBox(-4.5F, -1F, -4.5F, 9F, 9F, 9F).mirror(false),
                PartPose.offsetAndRotation(8F, 9F, 15F, 0F, 0F, 0F));
        PartDefinition p_leftHindLegLower = p_leftHindLeg.addOrReplaceChild("left_hind_leg_lower", CubeListBuilder.create()
                .texOffs(206, 113).mirror().addBox(-4F, -1F, -4F, 8F, 7F, 8F).mirror(false),
                PartPose.offsetAndRotation(0F, 7.5F, 0F, 0F, 0F, 0F));
        PartDefinition p_leftHindLegFoot = p_leftHindLegLower.addOrReplaceChild("left_hind_leg_foot", CubeListBuilder.create()
                .texOffs(82, 134).mirror().addBox(-5F, 0F, -5F, 10F, 2F, 10F).mirror(false),
                PartPose.offsetAndRotation(0F, 5.5F, 0F, 0F, 0F, 0F));
        PartDefinition p_tail = p_body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(234, 13).addBox(-1.5F, 0F, 0F, 3F, 3F, 8F),
                PartPose.offsetAndRotation(0F, -13F, 19F, 0.6F, 0F, 0F));
        return LayerDefinition.create(mesh, 256, 256);
    }

    /** The calf: chunky barrel, big head with a horn nub, short legs. Real size (not scaled down). */
    public static LayerDefinition createBabyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6F, -5F, -9F, 12F, 10F, 17F)
                .texOffs(0, 27).addBox(-4F, -6.4F, -6F, 8F, 2F, 11F)
                .texOffs(58, 17).addBox(-5.5F, 4.2F, -7F, 11F, 1F, 14F),
                PartPose.offsetAndRotation(0F, 13F, 1F, 0F, 0F, 0F));
        PartDefinition p_finFront = p_body.addOrReplaceChild("fin_front", CubeListBuilder.create()
                .texOffs(114, 0).addBox(-1F, -3F, -2F, 2F, 3F, 4F),
                PartPose.offsetAndRotation(0F, -6F, -3F, 0.15F, 0F, 0F));
        PartDefinition p_finBack = p_body.addOrReplaceChild("fin_back", CubeListBuilder.create()
                .texOffs(20, 40).addBox(-0.5F, -2F, -1.5F, 1F, 2F, 3F),
                PartPose.offsetAndRotation(0F, -6F, 3F, 0.25F, 0F, 0F));
        PartDefinition p_tail = p_body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(92, 10).addBox(-1F, 0F, 0F, 2F, 2F, 4F),
                PartPose.offsetAndRotation(0F, -3F, 8F, 0.6F, 0F, 0F));
        PartDefinition p_head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(58, 0).addBox(-4.5F, -5F, -7F, 9F, 9F, 8F)
                .texOffs(92, 0).addBox(-3.5F, -2F, -11F, 7F, 6F, 4F),
                PartPose.offsetAndRotation(0F, 12F, -8F, 0.3F, 0F, 0F));
        PartDefinition p_horn = p_head.addOrReplaceChild("horn", CubeListBuilder.create()
                .texOffs(28, 40).addBox(-1F, -2F, -1F, 2F, 2F, 2F),
                PartPose.offsetAndRotation(0F, -2F, -10F, -0.2F, 0F, 0F));
        PartDefinition p_rightEar = p_head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(114, 7).addBox(-2F, -0.5F, -0.6F, 2F, 1F, 2F),
                PartPose.offsetAndRotation(-4.5F, -4F, -2F, 0F, 0F, -0.5F));
        PartDefinition p_leftEar = p_head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(114, 40).mirror().addBox(0F, -0.5F, -0.6F, 2F, 1F, 2F).mirror(false),
                PartPose.offsetAndRotation(4.5F, -4F, -2F, 0F, 0F, 0.5F));
        PartDefinition p_rightFrontLeg = root.addOrReplaceChild("right_front_leg", CubeListBuilder.create()
                .texOffs(108, 10).addBox(-2.5F, -1F, -2.5F, 5F, 5F, 5F),
                PartPose.offsetAndRotation(-4F, 16.5F, -6.5F, 0F, 0F, 0F));
        PartDefinition p_rightFrontLegLower = p_rightFrontLeg.addOrReplaceChild("right_front_leg_lower", CubeListBuilder.create()
                .texOffs(58, 32).addBox(-2F, -1F, -2F, 4F, 4F, 4F),
                PartPose.offsetAndRotation(0F, 3.5F, 0F, 0F, 0F, 0F));
        PartDefinition p_rightFrontLegFoot = p_rightFrontLegLower.addOrReplaceChild("right_front_leg_foot", CubeListBuilder.create()
                .texOffs(0, 40).addBox(-2.5F, 0F, -2.5F, 5F, 1F, 5F),
                PartPose.offsetAndRotation(0F, 3F, 0F, 0F, 0F, 0F));
        PartDefinition p_leftFrontLeg = root.addOrReplaceChild("left_front_leg", CubeListBuilder.create()
                .texOffs(108, 20).mirror().addBox(-2.5F, -1F, -2.5F, 5F, 5F, 5F).mirror(false),
                PartPose.offsetAndRotation(4F, 16.5F, -6.5F, 0F, 0F, 0F));
        PartDefinition p_leftFrontLegLower = p_leftFrontLeg.addOrReplaceChild("left_front_leg_lower", CubeListBuilder.create()
                .texOffs(74, 32).mirror().addBox(-2F, -1F, -2F, 4F, 4F, 4F).mirror(false),
                PartPose.offsetAndRotation(0F, 3.5F, 0F, 0F, 0F, 0F));
        PartDefinition p_leftFrontLegFoot = p_leftFrontLegLower.addOrReplaceChild("left_front_leg_foot", CubeListBuilder.create()
                .texOffs(54, 40).mirror().addBox(-2.5F, 0F, -2.5F, 5F, 1F, 5F).mirror(false),
                PartPose.offsetAndRotation(0F, 3F, 0F, 0F, 0F, 0F));
        PartDefinition p_rightHindLeg = root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create()
                .texOffs(38, 27).addBox(-2.5F, -1F, -2.5F, 5F, 5F, 5F),
                PartPose.offsetAndRotation(-4F, 16.5F, 7.5F, 0F, 0F, 0F));
        PartDefinition p_rightHindLegLower = p_rightHindLeg.addOrReplaceChild("right_hind_leg_lower", CubeListBuilder.create()
                .texOffs(90, 32).addBox(-2F, -1F, -2F, 4F, 4F, 4F),
                PartPose.offsetAndRotation(0F, 3.5F, 0F, 0F, 0F, 0F));
        PartDefinition p_rightHindLegFoot = p_rightHindLegLower.addOrReplaceChild("right_hind_leg_foot", CubeListBuilder.create()
                .texOffs(74, 40).addBox(-2.5F, 0F, -2.5F, 5F, 1F, 5F),
                PartPose.offsetAndRotation(0F, 3F, 0F, 0F, 0F, 0F));
        PartDefinition p_leftHindLeg = root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create()
                .texOffs(108, 30).mirror().addBox(-2.5F, -1F, -2.5F, 5F, 5F, 5F).mirror(false),
                PartPose.offsetAndRotation(4F, 16.5F, 7.5F, 0F, 0F, 0F));
        PartDefinition p_leftHindLegLower = p_leftHindLeg.addOrReplaceChild("left_hind_leg_lower", CubeListBuilder.create()
                .texOffs(38, 37).mirror().addBox(-2F, -1F, -2F, 4F, 4F, 4F).mirror(false),
                PartPose.offsetAndRotation(0F, 3.5F, 0F, 0F, 0F, 0F));
        PartDefinition p_leftHindLegFoot = p_leftHindLegLower.addOrReplaceChild("left_hind_leg_foot", CubeListBuilder.create()
                .texOffs(94, 40).mirror().addBox(-2.5F, 0F, -2.5F, 5F, 1F, 5F).mirror(false),
                PartPose.offsetAndRotation(0F, 3F, 0F, 0F, 0F, 0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(CragbackRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;

        // ---- body follows the ground: pitch/roll from the feet, sinks a little into dips,
        //      rises slightly mid-stride, and breathes
        float pitch = state.legsActive ? -state.bodyPitch : 0.0F;
        float roll = state.legsActive ? -state.bodyRoll : 0.0F;
        float lift = state.legsActive ? state.bodyDrop - state.stepBob : 0.0F;
        float breathe = Mth.sin(age * 0.06F) * 0.25F;
        this.body.xRot = pitch;
        this.body.zRot = roll + (state.legsActive ? state.stepSway : 0.0F);
        this.body.y += lift + breathe;

        // ---- head rides on the body, then looks around / grazes / charges
        float[] hp = this.onBody(this.rig.headX(), this.rig.headY(), this.rig.headZ(), lift);
        this.head.x = hp[0];
        this.head.y = hp[1];
        this.head.z = hp[2];
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.6F;
        this.head.xRot = this.rig.headPitch() + pitch + state.xRot * Mth.DEG_TO_RAD * 0.5F + state.stepBob * 0.03F;
        this.head.zRot = this.body.zRot * 0.6F;
        float low = Math.max(state.eatAnimation, state.drinkAnimation);
        if (low > 0.0F) {
            this.head.xRot = Mth.lerp(low, this.head.xRot, 1.05F + pitch);
            float k = this.rig.thigh() / 7.5F;
            this.head.y += low * 5.0F * k;
            this.head.z -= low * 1.5F * k;
            if (state.eatAnimation > 0.0F) this.head.xRot += Mth.sin(age * 1.6F) * 0.06F * state.eatAnimation;
            if (state.drinkAnimation > 0.0F) this.head.y += Mth.sin(age * 0.9F) * 0.4F * state.drinkAnimation;
        }
        if (state.charging) {
            this.head.xRot = 0.75F + pitch;   // horn levelled for the charge
            this.head.y += 2.0F;
        }
        this.rightEar.zRot += Mth.sin(age * 0.11F) * 0.08F;
        this.leftEar.zRot -= Mth.sin(age * 0.13F + 1.0F) * 0.08F;
        this.tail.yRot = Mth.sin(age * 0.15F) * 0.25F + this.body.zRot * 1.5F;
        this.tail.xRot += Math.min(1.0F, state.walkAnimationSpeed) * 0.3F;

        // ---- legs
        for (int i = 0; i < 4; i++) {
            float[] hip = this.onBody(this.rig.hips()[i][0], this.rig.hips()[i][1], this.rig.hips()[i][2], lift);
            float fx, fy, fz;
            if (state.legsActive) {
                fx = state.footX[i];
                fy = state.footY[i] - this.rig.foot();   // IK reaches the ankle; the flat foot fills the rest
                fz = state.footZ[i];
            } else {
                // no foot data yet (inventory/preview): plain standing pose
                fx = this.rig.hips()[i][0];
                fy = 24.0F - this.rig.foot();
                fz = this.rig.hips()[i][2];
            }
            // front legs bend at the wrist (joint forward), hind legs at the hock (joint back)
            this.solveLeg(i, hip[0], hip[1], hip[2], fx, fy, fz, i < 2);
        }
    }

    /** Where a point that is rigidly attached to the body ends up after the body's pitch/roll/lift. */
    private float[] onBody(float x, float y, float z, float lift) {
        float dx = x - this.rig.bodyX(), dy = y - this.rig.bodyY(), dz = z - this.rig.bodyZ();
        float cx = Mth.cos(this.body.xRot), sx = Mth.sin(this.body.xRot);
        float y1 = dy * cx - dz * sx;
        float z1 = dy * sx + dz * cx;
        float cz = Mth.cos(this.body.zRot), sz = Mth.sin(this.body.zRot);
        float x2 = dx * cz - y1 * sz;
        float y2 = dx * sz + y1 * cz;
        return new float[]{this.rig.bodyX() + x2, this.rig.bodyY() + lift + y2, this.rig.bodyZ() + z1};
    }

    /** Two-bone IK: thigh from the hip, shin to the ankle target; the foot is kept level. */
    private void solveLeg(int i, float hx, float hy, float hz, float tx, float ty, float tz, boolean jointForward) {
        ModelPart upper = this.legs[i], lower = this.lowers[i], foot = this.feet[i];
        upper.x = hx;
        upper.y = hy;
        upper.z = hz;
        float dx = tx - hx, dy = ty - hy, dz = tz - hz;
        float down = Mth.sqrt(dx * dx + dy * dy);           // reach measured in the leg's own swing plane
        upper.zRot = -(float) Math.atan2(dx, Math.max(0.01F, dy));
        float need = Mth.sqrt(down * down + dz * dz);
        // ground just out of reach (e.g. hind feet one block lower): stretch the shin up to 45%
        // rather than leave the foot hanging in the air
        float stretch = Mth.clamp((need - this.rig.thigh() * 0.99F) / this.rig.shin(), 1.0F, 1.45F);
        float shin = this.rig.shin() * stretch;
        lower.yScale = stretch;
        foot.yScale = 1.0F / stretch;
        float r = Mth.clamp(need, Math.abs(this.rig.thigh() - shin) + 0.5F, (this.rig.thigh() + shin) * 0.995F);
        float theta = (float) Math.atan2(dz, down);
        float alpha = (float) Math.acos(Mth.clamp((this.rig.thigh() * this.rig.thigh() + r * r - shin * shin) / (2.0F * this.rig.thigh() * r), -1.0F, 1.0F));
        float beta = (float) Math.acos(Mth.clamp((this.rig.thigh() * this.rig.thigh() + shin * shin - r * r) / (2.0F * this.rig.thigh() * shin), -1.0F, 1.0F));
        if (jointForward) {
            upper.xRot = theta - alpha;
            lower.xRot = Mth.PI - beta;
        } else {
            upper.xRot = theta + alpha;
            lower.xRot = -(Mth.PI - beta);
        }
        foot.xRot = -(upper.xRot + lower.xRot);
        foot.zRot = -upper.zRot;
    }
}
