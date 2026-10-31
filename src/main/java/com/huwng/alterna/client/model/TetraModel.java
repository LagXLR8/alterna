package com.huwng.alterna.client.model;

// Geometry generated from the Alterna model spec (tetra2_spec.py); animation is hand-written.
// Style: chunky body, zero-thickness membrane/crest/tail planes with cut-out silhouettes.
import com.huwng.alterna.client.render.state.TetraRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.core.Direction;
import java.util.EnumSet;

public class TetraModel extends EntityModel<TetraRenderState> {
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart tailTip;
    private final ModelPart rightFrontWing;
    private final ModelPart leftFrontWing;
    private final ModelPart rightFrontWingTip;
    private final ModelPart leftFrontWingTip;
    private final ModelPart rightHindWing;
    private final ModelPart leftHindWing;
    private final ModelPart rightHindWingTip;
    private final ModelPart leftHindWingTip;

    private final boolean baby;

    public TetraModel(ModelPart root) {
        this(root, false);
    }

    public TetraModel(ModelPart root, boolean baby) {
        super(root);
        this.baby = baby;
        this.body = root.getChild("body");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.tail = this.body.getChild("tail");
        this.tailTip = this.tail.getChild("tail_tip");
        this.rightFrontWing = this.body.getChild("right_front_wing");
        this.leftFrontWing = this.body.getChild("left_front_wing");
        this.rightFrontWingTip = this.rightFrontWing.getChild("right_front_wing_tip");
        this.leftFrontWingTip = this.leftFrontWing.getChild("left_front_wing_tip");
        this.rightHindWing = this.body.getChild("right_hind_wing");
        this.leftHindWing = this.body.getChild("left_hind_wing");
        this.rightHindWingTip = this.rightHindWing.getChild("right_hind_wing_tip");
        this.leftHindWingTip = this.leftHindWing.getChild("left_hind_wing_tip");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(26, 0).addBox(-3.5F, -3.5F, -6F, 7F, 7F, 13F)
                .texOffs(82, 32).addBox(-3F, 2.6F, -5F, 6F, 1F, 10F),
                PartPose.offsetAndRotation(0F, 16F, 0F, 0F, 0F, 0F));
        PartDefinition p_neck = p_body.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(0, 48).addBox(-1.5F, -1.5F, -6F, 3F, 4F, 7F),
                PartPose.offsetAndRotation(0F, -1.5F, -5.5F, -0.35F, 0F, 0F));
        PartDefinition p_head = p_neck.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(58, 32).addBox(-2.5F, -4F, -6F, 5F, 5F, 7F)
                .texOffs(0, 0).addBox(0F, -12F, -3F, 0F, 9F, 13F, EnumSet.of(Direction.EAST))
                .texOffs(104, 43).addBox(-1F, -2F, -15F, 2F, 2F, 9F),
                PartPose.offsetAndRotation(0F, 0F, -5.5F, 0.35F, 0F, 0F));
        PartDefinition p_jaw = p_head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(104, 54).addBox(-1F, 0F, -9F, 2F, 1F, 9F),
                PartPose.offsetAndRotation(0F, 0.5F, -5.5F, 0F, 0F, 0F));
        PartDefinition p_rightFrontWing = p_body.addOrReplaceChild("right_front_wing", CubeListBuilder.create()
                .texOffs(80, 73).addBox(-15F, -1F, -1.5F, 15F, 2F, 3F)
                .texOffs(0, 37).addBox(-15F, 0.5F, 1.5F, 15F, 0F, 11F, EnumSet.of(Direction.UP)),
                PartPose.offsetAndRotation(-3.5F, -2F, -3.5F, 0F, 0F, 0F));
        PartDefinition p_rightFrontWingTip = p_rightFrontWing.addOrReplaceChild("right_front_wing_tip", CubeListBuilder.create()
                .texOffs(118, 0).addBox(-1F, -1.5F, -4F, 2F, 2F, 3F)
                .texOffs(84, 13).addBox(-14F, -0.75F, -1F, 14F, 2F, 2F)
                .texOffs(66, 17).addBox(-14F, 0.5F, 1F, 14F, 0F, 15F, EnumSet.of(Direction.UP)),
                PartPose.offsetAndRotation(-15F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_rightHindWing = p_body.addOrReplaceChild("right_hind_wing", CubeListBuilder.create()
                .texOffs(36, 74).addBox(-11F, -1F, -1F, 11F, 2F, 2F)
                .texOffs(0, 65).addBox(-11F, 0.5F, 1F, 11F, 0F, 9F, EnumSet.of(Direction.UP)),
                PartPose.offsetAndRotation(-3.5F, -0.8F, 3.5F, 0F, 0F, 0F));
        PartDefinition p_rightHindWingTip = p_rightHindWing.addOrReplaceChild("right_hind_wing_tip", CubeListBuilder.create()
                .texOffs(118, 10).addBox(-1F, -1F, -3F, 2F, 2F, 2F)
                .texOffs(26, 20).addBox(-9F, -0.5F, -0.5F, 9F, 1F, 1F)
                .texOffs(20, 55).addBox(-9F, 0.5F, 0.5F, 9F, 0F, 10F, EnumSet.of(Direction.UP)),
                PartPose.offsetAndRotation(-11F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_leftFrontWing = p_body.addOrReplaceChild("left_front_wing", CubeListBuilder.create()
                .texOffs(0, 74).mirror().addBox(0F, -1F, -1.5F, 15F, 2F, 3F).mirror(false)
                .texOffs(52, 44).mirror().addBox(0F, 0.5F, 1.5F, 15F, 0F, 11F, EnumSet.of(Direction.UP)).mirror(false),
                PartPose.offsetAndRotation(3.5F, -2F, -3.5F, 0F, 0F, 0F));
        PartDefinition p_leftFrontWingTip = p_leftFrontWing.addOrReplaceChild("left_front_wing_tip", CubeListBuilder.create()
                .texOffs(118, 5).mirror().addBox(-1F, -1.5F, -4F, 2F, 2F, 3F).mirror(false)
                .texOffs(20, 48).mirror().addBox(0F, -0.75F, -1F, 14F, 2F, 2F).mirror(false)
                .texOffs(0, 22).mirror().addBox(0F, 0.5F, 1F, 14F, 0F, 15F, EnumSet.of(Direction.UP)).mirror(false),
                PartPose.offsetAndRotation(15F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_leftHindWing = p_body.addOrReplaceChild("left_hind_wing", CubeListBuilder.create()
                .texOffs(36, 78).mirror().addBox(0F, -1F, -1F, 11F, 2F, 2F).mirror(false)
                .texOffs(40, 65).mirror().addBox(0F, 0.5F, 1F, 11F, 0F, 9F, EnumSet.of(Direction.UP)).mirror(false),
                PartPose.offsetAndRotation(3.5F, -0.8F, 3.5F, 0F, 0F, 0F));
        PartDefinition p_leftHindWingTip = p_leftHindWing.addOrReplaceChild("left_hind_wing_tip", CubeListBuilder.create()
                .texOffs(58, 20).mirror().addBox(-1F, -1F, -3F, 2F, 2F, 2F).mirror(false)
                .texOffs(20, 52).mirror().addBox(0F, -0.5F, -0.5F, 9F, 1F, 1F).mirror(false)
                .texOffs(58, 55).mirror().addBox(0F, 0.5F, 0.5F, 9F, 0F, 10F, EnumSet.of(Direction.UP)).mirror(false),
                PartPose.offsetAndRotation(11F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_tail = p_body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(84, 0).addBox(-2F, 0F, 0F, 4F, 0F, 13F, EnumSet.of(Direction.UP)),
                PartPose.offsetAndRotation(0F, -1.5F, 6.5F, 0.1F, 0F, 0F));
        PartDefinition p_tailTip = p_tail.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                .texOffs(66, 0).addBox(0F, -4F, 0F, 0F, 8F, 9F, EnumSet.of(Direction.EAST))
                .texOffs(96, 64).addBox(-3.5F, 0F, 0F, 7F, 0F, 9F, EnumSet.of(Direction.UP)),
                PartPose.offsetAndRotation(0F, 0F, 13F, 0F, 0F, 0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** The chick: round body, big head, tiny crest and stubby wings. Real size. */
    public static LayerDefinition createBabyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition p_body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.5F, -2.5F, -3.5F, 5F, 5F, 7F)
                .texOffs(44, 9).addBox(-2F, 2.1F, -3F, 4F, 1F, 6F),
                PartPose.offsetAndRotation(0F, 20F, 0F, 0F, 0F, 0F));
        PartDefinition p_neck = p_body.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(54, 16).addBox(-1F, -1F, -2F, 2F, 2F, 2F),
                PartPose.offsetAndRotation(0F, -1.5F, -3F, -0.35F, 0F, 0F));
        PartDefinition p_head = p_neck.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-2.5F, -4F, -4.5F, 5F, 5F, 5F)
                .texOffs(44, 0).addBox(0F, -7F, -2F, 0F, 3F, 6F, EnumSet.of(Direction.EAST))
                .texOffs(12, 19).addBox(-0.5F, -2F, -7.5F, 1F, 1F, 3F),
                PartPose.offsetAndRotation(0F, -0.5F, -1.5F, 0.35F, 0F, 0F));
        PartDefinition p_jaw = p_head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(20, 19).addBox(-0.5F, 0F, -3F, 1F, 1F, 3F),
                PartPose.offsetAndRotation(0F, -1F, -4.5F, 0F, 0F, 0F));
        PartDefinition p_rightFrontWing = p_body.addOrReplaceChild("right_front_wing", CubeListBuilder.create()
                .texOffs(36, 23).addBox(-5F, -0.5F, -0.5F, 5F, 1F, 1F)
                .texOffs(18, 15).addBox(-5F, 0.2F, 0.5F, 5F, 0F, 4F, EnumSet.of(Direction.UP)),
                PartPose.offsetAndRotation(-2.5F, -1.5F, -2F, 0F, 0F, 0F));
        PartDefinition p_rightFrontWingTip = p_rightFrontWing.addOrReplaceChild("right_front_wing_tip", CubeListBuilder.create()
                .texOffs(18, 12).addBox(-0.5F, -0.5F, -1.5F, 1F, 1F, 1F)
                .texOffs(0, 24).addBox(-4F, -0.5F, -0.5F, 4F, 1F, 1F)
                .texOffs(24, 10).addBox(-4F, 0.2F, 0.5F, 4F, 0F, 5F, EnumSet.of(Direction.UP)),
                PartPose.offsetAndRotation(-5F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_rightHindWing = p_body.addOrReplaceChild("right_hind_wing", CubeListBuilder.create()
                .texOffs(36, 25).addBox(-4F, -0.5F, -0.5F, 4F, 1F, 1F)
                .texOffs(28, 20).addBox(-4F, 0.2F, 0.5F, 4F, 0F, 3F, EnumSet.of(Direction.UP)),
                PartPose.offsetAndRotation(-2.5F, -0.7F, 2F, 0F, 0F, 0F));
        PartDefinition p_rightHindWingTip = p_rightHindWing.addOrReplaceChild("right_hind_wing_tip", CubeListBuilder.create()
                .texOffs(12, 17).addBox(-0.5F, -0.5F, -1.5F, 1F, 1F, 1F)
                .texOffs(56, 6).addBox(-3F, -0.5F, -0.5F, 3F, 1F, 1F)
                .texOffs(0, 21).addBox(-3F, 0.2F, 0.5F, 3F, 0F, 3F, EnumSet.of(Direction.UP)),
                PartPose.offsetAndRotation(-4F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_leftFrontWing = p_body.addOrReplaceChild("left_front_wing", CubeListBuilder.create()
                .texOffs(48, 23).mirror().addBox(0F, -0.5F, -0.5F, 5F, 1F, 1F).mirror(false)
                .texOffs(36, 16).mirror().addBox(0F, 0.2F, 0.5F, 5F, 0F, 4F, EnumSet.of(Direction.UP)).mirror(false),
                PartPose.offsetAndRotation(2.5F, -1.5F, -2F, 0F, 0F, 0F));
        PartDefinition p_leftFrontWingTip = p_leftFrontWing.addOrReplaceChild("left_front_wing_tip", CubeListBuilder.create()
                .texOffs(60, 22).mirror().addBox(-0.5F, -0.5F, -1.5F, 1F, 1F, 1F).mirror(false)
                .texOffs(46, 25).mirror().addBox(0F, -0.5F, -0.5F, 4F, 1F, 1F).mirror(false)
                .texOffs(0, 12).mirror().addBox(0F, 0.2F, 0.5F, 4F, 0F, 5F, EnumSet.of(Direction.UP)).mirror(false),
                PartPose.offsetAndRotation(5F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_leftHindWing = p_body.addOrReplaceChild("left_hind_wing", CubeListBuilder.create()
                .texOffs(0, 26).mirror().addBox(0F, -0.5F, -0.5F, 4F, 1F, 1F).mirror(false)
                .texOffs(42, 20).mirror().addBox(0F, 0.2F, 0.5F, 4F, 0F, 3F, EnumSet.of(Direction.UP)).mirror(false),
                PartPose.offsetAndRotation(2.5F, -0.7F, 2F, 0F, 0F, 0F));
        PartDefinition p_leftHindWingTip = p_leftHindWing.addOrReplaceChild("left_hind_wing_tip", CubeListBuilder.create()
                .texOffs(60, 24).mirror().addBox(-0.5F, -0.5F, -1.5F, 1F, 1F, 1F).mirror(false)
                .texOffs(56, 20).mirror().addBox(0F, -0.5F, -0.5F, 3F, 1F, 1F).mirror(false)
                .texOffs(12, 23).mirror().addBox(0F, 0.2F, 0.5F, 3F, 0F, 3F, EnumSet.of(Direction.UP)).mirror(false),
                PartPose.offsetAndRotation(4F, 0F, 0F, 0F, 0F, 0F));
        PartDefinition p_tail = p_body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(0, 17).addBox(-1F, 0F, 0F, 2F, 0F, 4F, EnumSet.of(Direction.UP)),
                PartPose.offsetAndRotation(0F, -1F, 3.5F, 0.15F, 0F, 0F));
        PartDefinition p_tailTip = p_tail.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                .texOffs(56, 0).addBox(0F, -1.5F, 0F, 0F, 3F, 3F, EnumSet.of(Direction.EAST))
                .texOffs(24, 23).addBox(-1.5F, 0.05F, 0F, 3F, 0F, 3F, EnumSet.of(Direction.UP)),
                PartPose.offsetAndRotation(0F, 0F, 4F, 0F, 0F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Moves the pose stack to the tip of the lower beak (used to render a carried item there). */
    public void translateToBeak(PoseStack poseStack) {
        this.root().translateAndRotate(poseStack);
        this.body.translateAndRotate(poseStack);
        this.neck.translateAndRotate(poseStack);
        this.head.translateAndRotate(poseStack);
        this.jaw.translateAndRotate(poseStack);
        if (this.baby) poseStack.translate(0.0F, 0.5F / 16.0F, -2.5F / 16.0F);
        else poseStack.translate(0.0F, 0.5F / 16.0F, -6.5F / 16.0F);
    }

    /** Size of a carried item relative to the beak. */
    public float heldItemScale() {
        return this.baby ? 0.4F : 0.7F;
    }

    /** Mirror-symmetric helper: right side gets the values, left side the mirrored ones. */
    private static void pose(ModelPart right, ModelPart left, float xRot, float yRot, float zRot) {
        right.xRot = xRot; right.yRot = yRot;  right.zRot = zRot;
        left.xRot = xRot;  left.yRot = -yRot;  left.zRot = -zRot;
    }

    @Override
    public void setupAnim(TetraRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float lookYaw = state.yRot * Mth.DEG_TO_RAD;
        float lookPitch = state.xRot * Mth.DEG_TO_RAD;

        // open the beak a little while carrying something
        this.jaw.xRot = !state.heldItem.isEmpty() ? 0.22F : Math.max(0.0F, Mth.sin(age * 0.11F) * 0.06F);

        this.neck.yRot = lookYaw * 0.45F;
        this.head.yRot = lookYaw * 0.3F;
        // tail: idle sway + spring that trails behind turns (procedural, from the entity)
        this.tail.yRot = Mth.sin(age * 0.09F) * 0.08F + state.tailSwing * 0.6F;
        this.tailTip.yRot = Mth.sin(age * 0.09F - 0.7F) * 0.12F + state.tailSwing;

        if (state.perched) {
            // Clinging to a cliff face (the renderer turns it toward the rock): body upright,
            // belly on the stone, four wings spread flat against it, head craned out to watch.
            this.body.xRot = -1.45F;
            this.neck.xRot = 0.5F;
            this.head.xRot = -0.9F + Mth.sin(age * 0.05F) * 0.12F;
            this.neck.yRot = Mth.sin(age * 0.03F) * 0.6F;
            float b = Mth.sin(age * 0.08F) * 0.04F;
            pose(this.rightFrontWing, this.leftFrontWing, 0.0F, -0.5F + b, 0.12F);
            pose(this.rightFrontWingTip, this.leftFrontWingTip, 0.0F, 0.7F, 0.05F);
            pose(this.rightHindWing, this.leftHindWing, 0.0F, -0.45F - b, 0.1F);
            pose(this.rightHindWingTip, this.leftHindWingTip, 0.0F, -0.5F, 0.05F);
            this.tail.xRot = 0.2F;
            return;
        }

        if (!state.flying) {
            // On the ground it walks on all four folded wings, like a pterosaur.
            float pos = state.walkAnimationPos;
            float speed = Math.min(1.0F, state.walkAnimationSpeed);
            float a = Mth.cos(pos * 0.9F) * 0.35F * speed;
            float c = Mth.cos(pos * 0.9F + Mth.PI) * 0.35F * speed;
            this.body.xRot = -0.2F;
            this.body.y += Mth.abs(Mth.sin(pos * 0.9F)) * -0.6F * speed + Mth.sin(age * 0.07F) * 0.15F;
            this.neck.xRot = -0.6F + Mth.sin(pos * 0.9F) * 0.05F * speed;
            this.head.xRot = 0.6F + lookPitch * 0.5F;
            // diagonal pairs move together
            this.rightFrontWing.yRot = -0.5F + a;  this.rightFrontWing.zRot = -1.05F + Math.max(0.0F, -a) * 0.5F;
            this.leftFrontWing.yRot = 0.5F + c;    this.leftFrontWing.zRot = 1.05F - Math.max(0.0F, -c) * 0.5F;
            this.rightHindWing.yRot = 0.4F + c;    this.rightHindWing.zRot = -0.9F + Math.max(0.0F, c) * 0.4F;
            this.leftHindWing.yRot = -0.4F + a;    this.leftHindWing.zRot = 0.9F - Math.max(0.0F, a) * 0.4F;
            pose(this.rightFrontWingTip, this.leftFrontWingTip, 0.0F, 0.3F, 2.6F);
            pose(this.rightHindWingTip, this.leftHindWingTip, 0.0F, 0.0F, 2.3F);
            this.tail.xRot = -0.15F;
            return;
        }

        // Flying. flap blends between gliding (wings spread, gentle ripple) and full beats;
        // the beat phase is integrated on the entity so changing rate never jitters.
        float f = state.flap;
        float ph = state.flapPhase;
        float amp = 0.1F + 0.8F * f;
        float beat = Mth.sin(ph) * amp;
        float tipBeat = Mth.sin(ph - 0.7F) * amp * 0.7F;
        float hindBeat = Mth.sin(ph - 1.0F) * amp * 0.8F;
        float hindTip = Mth.sin(ph - 1.7F) * amp * 0.6F;
        float dihedral = (1.0F - f) * 0.07F;
        this.rightFrontWing.zRot = beat + dihedral;          this.leftFrontWing.zRot = -beat - dihedral;
        this.rightFrontWing.yRot = 0.12F;                     this.leftFrontWing.yRot = -0.12F;
        this.rightFrontWingTip.zRot = tipBeat;                this.leftFrontWingTip.zRot = -tipBeat;
        this.rightHindWing.zRot = hindBeat + dihedral;       this.leftHindWing.zRot = -hindBeat - dihedral;
        this.rightHindWing.yRot = -0.25F;                     this.leftHindWing.yRot = 0.25F;
        this.rightHindWingTip.zRot = hindTip;                 this.leftHindWingTip.zRot = -hindTip;
        this.body.y -= beat * 1.4F;
        // birds hold their head level while the body banks and pitches
        float bank = state.roll * Mth.DEG_TO_RAD, nose = state.flightPitch * Mth.DEG_TO_RAD;
        this.neck.xRot = -0.15F + beat * 0.08F - nose * 0.4F;
        this.head.xRot = 0.15F + lookPitch * 0.3F - nose * 0.35F;
        this.neck.zRot = bank * 0.35F;
        this.head.zRot = bank * 0.35F;
        this.tail.xRot = 0.08F + Mth.sin(ph + 1.2F) * 0.06F + state.tailLift;
        if (state.diving) {
            // stoop: wings swept back along the body, tips folded
            pose(this.rightFrontWing, this.leftFrontWing, 0.0F, 1.05F, 0.25F);
            pose(this.rightFrontWingTip, this.leftFrontWingTip, 0.0F, 0.5F, -0.2F);
            pose(this.rightHindWing, this.leftHindWing, 0.0F, 1.2F, 0.2F);
            pose(this.rightHindWingTip, this.leftHindWingTip, 0.0F, 0.4F, -0.1F);
            this.neck.xRot = 0.25F;
            this.head.xRot = 0.2F;
        }
    }
}
