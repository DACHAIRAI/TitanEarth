package com.earthtitan.client.model;

import com.earthtitan.client.TitanVisuals;
import com.earthtitan.entity.EarthTitanEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import com.earthtitan.EarthTitanMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * One 18-block skeleton whose pieces are progressively exposed as the titan
 * grows. Child joints are real ModelPart hierarchies, rather than independent
 * cubes, so punch/grab/slash motion stays articulated at any stage.
 */
public final class EarthTitanModel extends EntityModel<EarthTitanEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(EarthTitanMod.MOD_ID, "earth_titan"), "main");

    private final ModelPart root;
    private final ModelPart core;
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart shoulderLeft;
    private final ModelPart shoulderRight;
    private final ModelPart upperArmLeft;
    private final ModelPart upperArmRight;
    private final ModelPart forearmLeft;
    private final ModelPart forearmRight;
    private final ModelPart handLeft;
    private final ModelPart handRight;
    private final ModelPart artilleryLeft;
    private final ModelPart artilleryRight;
    private final ModelPart hipLeft;
    private final ModelPart hipRight;
    private final ModelPart thighLeft;
    private final ModelPart thighRight;
    private final ModelPart shinLeft;
    private final ModelPart shinRight;
    private final ModelPart footLeft;
    private final ModelPart footRight;
    private final ModelPart[] fingersLeft = new ModelPart[5];
    private final ModelPart[] fingersRight = new ModelPart[5];
    private final ModelPart[] crystals = new ModelPart[6];

    public EarthTitanModel(ModelPart root) {
        this.root = root;
        this.core = root.getChild("core");
        this.body = root.getChild("body");
        this.neck = root.getChild("neck");
        this.head = root.getChild("head");
        this.jaw = head.getChild("jaw");
        this.shoulderLeft = root.getChild("shoulder_left");
        this.shoulderRight = root.getChild("shoulder_right");
        this.upperArmLeft = shoulderLeft.getChild("upper_arm");
        this.upperArmRight = shoulderRight.getChild("upper_arm");
        this.forearmLeft = upperArmLeft.getChild("forearm");
        this.forearmRight = upperArmRight.getChild("forearm");
        this.handLeft = forearmLeft.getChild("hand");
        this.handRight = forearmRight.getChild("hand");
        this.artilleryLeft = root.getChild("artillery_left");
        this.artilleryRight = root.getChild("artillery_right");
        this.hipLeft = root.getChild("hip_left");
        this.hipRight = root.getChild("hip_right");
        this.thighLeft = hipLeft.getChild("thigh");
        this.thighRight = hipRight.getChild("thigh");
        this.shinLeft = thighLeft.getChild("shin");
        this.shinRight = thighRight.getChild("shin");
        this.footLeft = shinLeft.getChild("foot");
        this.footRight = shinRight.getChild("foot");
        for (int i = 0; i < 5; i++) {
            fingersLeft[i] = handLeft.getChild("finger_" + i);
            fingersRight[i] = handRight.getChild("finger_" + i);
        }
        for (int i = 0; i < crystals.length; i++) {
            crystals[i] = body.getChild("crystal_" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation stone = new CubeDeformation(0.8F);

        root.addOrReplaceChild("core", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-18, -36, -18, 36, 36, 36, stone),
                PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 40).addBox(-32, -188, -18, 64, 134, 36, stone)
                        .texOffs(0, 100).addBox(-24, -54, -15, 48, 18, 30, stone),
                PartPose.ZERO);
        for (int i = 0; i < 6; i++) {
            float x = (i - 2.5F) * 8.0F;
            body.addOrReplaceChild("crystal_" + i, CubeListBuilder.create()
                            .texOffs(72, 0).addBox(-4, -18 - (i % 2) * 8, -4, 8, 18 + (i % 2) * 8, 8, stone),
                    PartPose.offset(x, -185, -19));
        }
        root.addOrReplaceChild("neck", CubeListBuilder.create()
                        .texOffs(80, 40).addBox(-13, -205, -13, 26, 28, 26, stone),
                PartPose.ZERO);
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 80).addBox(-22, -254, -20, 44, 50, 40, stone)
                        .texOffs(80, 80).addBox(-26, -227, -4, 52, 18, 8, stone),
                PartPose.ZERO);
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                        .texOffs(0, 112).addBox(-20, -4, -17, 40, 12, 34, stone),
                PartPose.offset(0, -210, 0));

        addArm(root, "shoulder_left", -40, true, stone);
        addArm(root, "shoulder_right", 40, false, stone);
        root.addOrReplaceChild("artillery_left", CubeListBuilder.create()
                        .texOffs(96, 0).addBox(-15, -170, -13, 30, 34, 26, stone)
                        .texOffs(96, 32).addBox(-9, -205, -9, 18, 40, 18, stone),
                PartPose.offset(-43, 0, -1));
        root.addOrReplaceChild("artillery_right", CubeListBuilder.create()
                        .texOffs(96, 0).addBox(-15, -170, -13, 30, 34, 26, stone)
                        .texOffs(96, 32).addBox(-9, -205, -9, 18, 40, 18, stone),
                PartPose.offset(43, 0, -1));

        addLeg(root, "hip_left", -17, true, stone);
        addLeg(root, "hip_right", 17, false, stone);
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void addArm(PartDefinition root, String name, float x, boolean left, CubeDeformation stone) {
        PartDefinition shoulder = root.addOrReplaceChild(name, CubeListBuilder.create()
                        .texOffs(64, 48).addBox(-16, -16, -16, 32, 32, 32, stone),
                PartPose.offset(x, -172, 0));
        PartDefinition upper = shoulder.addOrReplaceChild("upper_arm", CubeListBuilder.create()
                        .texOffs(80, 0).addBox(-12, 0, -12, 24, 54, 24, stone),
                PartPose.offset(0, 12, 0));
        PartDefinition forearm = upper.addOrReplaceChild("forearm", CubeListBuilder.create()
                        .texOffs(80, 36).addBox(-13, 0, -13, 26, 52, 26, stone),
                PartPose.offset(0, 52, 0));
        PartDefinition hand = forearm.addOrReplaceChild("hand", CubeListBuilder.create()
                        .texOffs(80, 88).addBox(-15, 0, -14, 30, 27, 28, stone),
                PartPose.offset(0, 52, 0));
        for (int i = 0; i < 5; i++) {
            float fingerX = (i - 2) * 6.0F;
            hand.addOrReplaceChild("finger_" + i, CubeListBuilder.create()
                            .texOffs(112, 112).addBox(-2.5F, 0, -2.5F, 5, 18, 5, stone),
                    PartPose.offset(fingerX, 24, left ? -5 : 5));
        }
    }

    private static void addLeg(PartDefinition root, String name, float x, boolean left, CubeDeformation stone) {
        PartDefinition hip = root.addOrReplaceChild(name, CubeListBuilder.create()
                        .texOffs(32, 48).addBox(-18, -18, -17, 36, 36, 34, stone),
                PartPose.offset(x, -126, 0));
        PartDefinition thigh = hip.addOrReplaceChild("thigh", CubeListBuilder.create()
                        .texOffs(0, 48).addBox(-15, 0, -15, 30, 62, 30, stone),
                PartPose.offset(0, 14, 0));
        PartDefinition shin = thigh.addOrReplaceChild("shin", CubeListBuilder.create()
                        .texOffs(32, 84).addBox(-13, 0, -13, 26, 50, 26, stone),
                PartPose.offset(0, 62, 0));
        shin.addOrReplaceChild("foot", CubeListBuilder.create()
                        .texOffs(64, 112).addBox(-17, -20, -26, 34, 20, 48, stone),
                PartPose.offset(0, 50, left ? -5 : 5));
    }

    @Override
    public void setupAnim(EarthTitanEntity titan, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float partial = ageInTicks - titan.tickCount;
        float visualStage = TitanVisuals.stage(titan, partial);
        setVisibility(visualStage);

        float idle = Mth.sin(ageInTicks * 0.055F);
        float breathe = 1.0F + idle * 0.025F;
        core.yRot = ageInTicks * 0.012F;
        core.xRot = idle * 0.035F;
        core.xScale = breathe;
        core.zScale = breathe;
        body.yRot = Mth.sin(ageInTicks * 0.018F) * 0.025F;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.35F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.28F;
        jaw.xRot = 0.03F + Mth.sin(ageInTicks * 0.12F) * 0.025F;

        float walk = Mth.sin(limbSwing * 0.7F) * limbSwingAmount;
        thighLeft.xRot = walk * 0.18F;
        thighRight.xRot = -walk * 0.18F;
        shinLeft.xRot = Math.max(0.0F, -walk) * 0.25F;
        shinRight.xRot = Math.max(0.0F, walk) * 0.25F;
        upperArmLeft.xRot = -walk * 0.10F;
        upperArmRight.xRot = walk * 0.10F;
        forearmLeft.xRot = 0.08F;
        forearmRight.xRot = 0.08F;

        switch (titan.getStateId()) {
            case 1 -> { // search
                head.yRot += Mth.sin(ageInTicks * 0.11F) * 0.32F;
                head.xRot += Mth.sin(ageInTicks * 0.07F) * 0.08F;
            }
            case 2 -> { // chase
                body.xRot = -0.04F;
                upperArmLeft.xRot -= walk * 0.35F;
                upperArmRight.xRot += walk * 0.35F;
            }
            case 3 -> absorbPose(ageInTicks);
            case 4 -> actionPose(titan, ageInTicks);
            case 5 -> shootPose(titan, ageInTicks);
            case 6 -> grabPose(titan, ageInTicks);
            case 7 -> transformPose(titan, ageInTicks);
            case 8, 11 -> deathPose(titan, ageInTicks);
            case 9 -> deadPose(titan, ageInTicks);
            case 10 -> activatePose(titan, ageInTicks);
            default -> { }
        }
        if (titan.getAction() != 0 && titan.getStateId() != 4 && titan.getStateId() != 5
                && titan.getStateId() != 6) {
            actionPose(titan, ageInTicks);
        }
    }

    private void setVisibility(float stage) {
        core.visible = true;
        boolean bodyOn = stage >= 1.75F;
        body.visible = bodyOn;
        neck.visible = stage >= 1.9F;
        head.visible = stage >= 1.95F;
        boolean artillery = stage >= 2.75F;
        artilleryLeft.visible = artillery;
        artilleryRight.visible = artillery;
        boolean arms = stage >= 3.65F;
        shoulderLeft.visible = arms;
        shoulderRight.visible = arms;
        boolean fingers = stage >= 3.9F;
        for (ModelPart finger : fingersLeft) finger.visible = fingers;
        for (ModelPart finger : fingersRight) finger.visible = fingers;
        boolean legs = stage >= 4.7F;
        hipLeft.visible = legs;
        hipRight.visible = legs;
        boolean feet = stage >= 5.3F;
        footLeft.visible = feet;
        footRight.visible = feet;
        for (ModelPart crystal : crystals) crystal.visible = bodyOn;
    }

    private void absorbPose(float age) {
        float pulse = 0.15F + Mth.sin(age * 0.23F) * 0.11F;
        upperArmLeft.zRot = -0.35F - pulse;
        upperArmRight.zRot = 0.35F + pulse;
        forearmLeft.xRot = -0.35F - pulse;
        forearmRight.xRot = -0.35F - pulse;
        handLeft.yRot = -pulse;
        handRight.yRot = pulse;
        core.yRot += age * 0.035F;
    }

    private void actionPose(EarthTitanEntity titan, float age) {
        int action = titan.getAction();
        float ticks = titan.getActionTicks() + (age - titan.tickCount);
        float wave = Mth.sin(Mth.clamp(ticks / 18.0F, 0.0F, 1.0F) * Mth.PI);
        float side = (action & 1) == 0 ? 1.0F : -1.0F;
        switch (action) {
            case 1 -> punch(wave, side);
            case 2 -> { punch(wave, -1.0F); punch(wave, 1.0F); }
            case 3, 10, 11 -> slam(wave);
            case 4 -> grabPose(titan, age);
            case 5 -> shootPose(titan, age);
            case 6, 7 -> slash(wave, side, action == 7);
            case 8 -> groundCutter(wave);
            case 9 -> verticalSmash(wave);
            case 12 -> barrage(wave);
            case 13 -> catapult(wave);
            default -> { }
        }
    }

    private void punch(float wave, float side) {
        ModelPart upper = side > 0 ? upperArmRight : upperArmLeft;
        ModelPart forearm = side > 0 ? forearmRight : forearmLeft;
        ModelPart hand = side > 0 ? handRight : handLeft;
        upper.xRot = -1.25F * wave;
        upper.yRot = 0.18F * side;
        forearm.xRot = -1.1F * wave;
        hand.xRot = -0.15F * wave;
    }

    private void slam(float wave) {
        upperArmLeft.xRot = -2.0F * wave;
        upperArmRight.xRot = -2.0F * wave;
        forearmLeft.xRot = -0.2F * wave;
        forearmRight.xRot = -0.2F * wave;
        body.xRot = -0.15F * wave;
    }

    private void slash(float wave, float side, boolean heavy) {
        shoulderLeft.zRot = -side * (0.55F + wave * 0.6F);
        shoulderRight.zRot = side * (0.55F + wave * 0.6F);
        upperArmLeft.yRot = -side * wave * (heavy ? 1.0F : 0.65F);
        upperArmRight.yRot = -side * wave * (heavy ? 1.0F : 0.65F);
        forearmLeft.xRot = -0.8F * wave;
        forearmRight.xRot = -0.8F * wave;
    }

    private void groundCutter(float wave) {
        upperArmLeft.xRot = -1.5F * wave;
        upperArmRight.xRot = -1.5F * wave;
        forearmLeft.zRot = -0.6F * wave;
        forearmRight.zRot = 0.6F * wave;
    }

    private void verticalSmash(float wave) {
        upperArmLeft.xRot = -2.35F * wave;
        upperArmRight.xRot = -2.35F * wave;
        forearmLeft.xRot = -0.3F * wave;
        forearmRight.xRot = -0.3F * wave;
    }

    private void barrage(float wave) {
        upperArmLeft.xRot = -1.2F * wave;
        upperArmRight.xRot = -1.2F * wave;
        forearmLeft.xRot = -1.2F * wave;
        forearmRight.xRot = -1.2F * wave;
        handLeft.yRot = wave * 0.4F;
        handRight.yRot = -wave * 0.4F;
    }

    private void catapult(float wave) {
        shoulderLeft.zRot = -0.8F * wave;
        shoulderRight.zRot = 0.8F * wave;
        forearmLeft.xRot = -1.7F * wave;
        forearmRight.xRot = -1.7F * wave;
    }

    private void shootPose(EarthTitanEntity titan, float age) {
        float pulse = 0.05F + Mth.sin((titan.getActionTicks() + age - titan.tickCount) * 0.3F) * 0.08F;
        upperArmLeft.xRot = -0.9F - pulse;
        upperArmRight.xRot = -0.9F - pulse;
        forearmLeft.xRot = -1.0F;
        forearmRight.xRot = -1.0F;
        handLeft.xRot = -0.15F;
        handRight.xRot = -0.15F;
    }

    private void grabPose(EarthTitanEntity titan, float age) {
        float close = Mth.clamp((titan.getActionTicks() + age - titan.tickCount) / 16.0F, 0.0F, 1.0F);
        upperArmLeft.xRot = -0.7F * close;
        upperArmRight.xRot = -0.7F * close;
        forearmLeft.xRot = -0.9F * close;
        forearmRight.xRot = -0.9F * close;
        handLeft.yRot = 0.45F * close;
        handRight.yRot = -0.45F * close;
        for (int i = 0; i < 5; i++) {
            fingersLeft[i].xRot = -0.55F * close;
            fingersRight[i].xRot = -0.55F * close;
        }
    }

    private void transformPose(EarthTitanEntity titan, float age) {
        float spin = (titan.getStateTicks() + age - titan.tickCount) * 0.065F;
        core.yRot += spin;
        body.yRot -= spin * 0.45F;
        head.yRot += Mth.sin(spin) * 0.15F;
        float tremor = Mth.sin(age * 0.8F) * 0.045F;
        shoulderLeft.zRot = tremor;
        shoulderRight.zRot = -tremor;
    }

    private void deathPose(EarthTitanEntity titan, float age) {
        float fall = Mth.clamp((titan.getStateTicks() + age - titan.tickCount) / 120.0F, 0.0F, 1.0F);
        body.zRot = fall * 1.05F;
        head.zRot = fall * 0.6F;
        upperArmLeft.xRot = fall * 0.8F;
        upperArmRight.xRot = fall * 0.8F;
        thighLeft.xRot = -fall * 0.5F;
        thighRight.xRot = -fall * 0.5F;
    }

    private void deadPose(EarthTitanEntity titan, float age) {
        body.zRot = 1.08F;
        head.zRot = 0.62F;
        upperArmLeft.xRot = 0.82F;
        upperArmRight.xRot = 0.82F;
        thighLeft.xRot = -0.52F;
        thighRight.xRot = -0.52F;
    }

    private void activatePose(EarthTitanEntity titan, float age) {
        float progress = Mth.clamp((titan.getStateTicks() + age - titan.tickCount) / 100.0F, 0.0F, 1.0F);
        float pulse = Mth.sin(age * 0.22F) * (0.04F + progress * 0.06F);
        core.yRot += age * (0.018F + progress * 0.025F);
        core.xRot = pulse;
        body.yRot = -pulse * 2.0F;
        shoulderLeft.zRot = -pulse;
        shoulderRight.zRot = pulse;
        head.yRot += Mth.sin(age * 0.13F) * 0.08F * progress;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
