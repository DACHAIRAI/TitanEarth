package com.earthtitan.client.renderer;

import com.earthtitan.EarthTitanMod;
import com.earthtitan.client.TitanVisuals;
import com.earthtitan.client.model.EarthTitanModel;
import com.earthtitan.client.model.TitanSwordModel;
import com.earthtitan.entity.EarthTitanEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class EarthTitanRenderer extends MobRenderer<EarthTitanEntity, EarthTitanModel> {
    private static final ResourceLocation SWORD_TEXTURE = new ResourceLocation(
            EarthTitanMod.MOD_ID, "textures/entity/sword.png");
    private final TitanSwordModel swordModel;

    public EarthTitanRenderer(EntityRendererProvider.Context context) {
        super(context, new EarthTitanModel(context.bakeLayer(EarthTitanModel.LAYER)), 1.0F);
        this.swordModel = new TitanSwordModel(context.bakeLayer(TitanSwordModel.LAYER));
    }

    @Override
    protected void scale(EarthTitanEntity titan, PoseStack poseStack, float partialTick) {
        float scale = TitanVisuals.scale(titan, partialTick);
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public void render(EarthTitanEntity titan, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        this.shadowRadius = Mth.clamp(TitanVisuals.height(titan, partialTick) * 0.045F, 0.15F, 1.8F);
        super.render(titan, entityYaw, partialTick, poseStack, buffer, packedLight);

        float visualStage = TitanVisuals.stage(titan, partialTick);
        if (visualStage < 5.3F || titan.getStateId() == 9) {
            return;
        }
        poseStack.pushPose();
        float scale = TitanVisuals.scale(titan, partialTick);
        poseStack.scale(scale, scale, scale);
        float age = titan.tickCount + partialTick;
        swordModel.setupAnim(titan, 0.0F, 0.0F, age, 0.0F, 0.0F);
        VertexConsumer swordBuffer = buffer.getBuffer(RenderType.entityCutoutNoCull(SWORD_TEXTURE));
        swordModel.renderToBuffer(poseStack, swordBuffer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EarthTitanEntity titan) {
        return TitanVisuals.texture(Mth.clamp(titan.getStage(), 1, 6));
    }
}
