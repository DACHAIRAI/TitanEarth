package com.earthtitan.client.renderer;

import com.earthtitan.entity.EarthProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;

/** Small block-shaped projectile; the larger flying absorption visuals are not entities. */
public final class EarthProjectileRenderer extends EntityRenderer<EarthProjectileEntity> {
    private final BlockRenderDispatcher dispatcher;

    public EarthProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.dispatcher = Minecraft.getInstance().getBlockRenderer();
        this.shadowRadius = 0.18F;
    }

    @Override
    public void render(EarthProjectileEntity projectile, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.18D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, projectile.yRotO, projectile.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, projectile.xRotO, projectile.getXRot())));
        poseStack.mulPose(Axis.ZP.rotationDegrees((projectile.tickCount + partialTick) * 6.0F));
        float scale = Mth.clamp(projectile.getVisualScale(), 0.05F, 8.0F);
        poseStack.scale(scale, scale, scale);
        dispatcher.renderSingleBlock(stateFor(projectile.getVariant()), poseStack, buffer, packedLight,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(projectile, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static BlockState stateFor(int variant) {
        return switch (variant) {
            case 1 -> Blocks.STONE.defaultBlockState();
            case 2 -> Blocks.DEEPSLATE.defaultBlockState();
            case 3 -> Blocks.POINTED_DRIPSTONE.defaultBlockState();
            case 4 -> Blocks.COBBLESTONE.defaultBlockState();
            case 5 -> Blocks.TNT.defaultBlockState();
            case 6 -> Blocks.IRON_BLOCK.defaultBlockState();
            case 7 -> Blocks.GRAVEL.defaultBlockState();
            default -> Blocks.DIRT.defaultBlockState();
        };
    }

    @Override
    public ResourceLocation getTextureLocation(EarthProjectileEntity projectile) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
