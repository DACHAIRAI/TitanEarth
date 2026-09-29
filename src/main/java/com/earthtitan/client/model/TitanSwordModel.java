package com.earthtitan.client.model;

import com.earthtitan.EarthTitanMod;
import com.earthtitan.client.TitanVisuals;
import com.earthtitan.entity.EarthTitanEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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

/** A separate textured greatsword pass, exposed at stage six. */
public final class TitanSwordModel extends EntityModel<EarthTitanEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(EarthTitanMod.MOD_ID, "titan_sword"), "main");
    private final ModelPart root;
    private final ModelPart blade;
    private final ModelPart guard;

    public TitanSwordModel(ModelPart root) {
        this.root = root;
        this.blade = root.getChild("blade");
        this.guard = root.getChild("guard");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation edge = new CubeDeformation(0.25F);
        root.addOrReplaceChild("blade", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5, -219, -2, 10, 219, 4, edge)
                        .texOffs(12, 0).addBox(-2, -233, -1, 4, 14, 2, edge),
                PartPose.offset(39, -55, -8));
        root.addOrReplaceChild("guard", CubeListBuilder.create()
                        .texOffs(0, 20).addBox(-30, -8, -4, 60, 8, 8, edge)
                        .texOffs(0, 30).addBox(-4, -8, -10, 8, 8, 20, edge),
                PartPose.offset(39, -55, -8));
        root.addOrReplaceChild("grip", CubeListBuilder.create()
                        .texOffs(0, 38).addBox(-4, 0, -4, 8, 38, 8, edge),
                PartPose.offset(39, -47, -8));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(EarthTitanEntity titan, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float partial = ageInTicks - titan.tickCount;
        float visualStage = TitanVisuals.stage(titan, partial);
        root.visible = visualStage >= 5.35F;
        float actionTicks = titan.getActionTicks() + partial;
        float wave = Mth.sin(Mth.clamp(actionTicks / 18.0F, 0.0F, 1.0F) * Mth.PI);
        switch (titan.getAction()) {
            case 6, 7 -> {
                root.yRot = (titan.getAction() & 1) == 0 ? -wave * 1.25F : wave * 1.25F;
                root.zRot = (titan.getAction() & 1) == 0 ? wave * 0.45F : -wave * 0.45F;
            }
            case 8 -> root.xRot = -wave * 0.7F;
            case 9 -> root.xRot = wave * 0.9F;
            case 13 -> root.yRot = Mth.sin(ageInTicks * 0.1F) * 0.2F;
            default -> root.yRot = Mth.sin(ageInTicks * 0.035F) * 0.04F;
        }
        blade.visible = root.visible;
        guard.visible = root.visible;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer consumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(poseStack, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
