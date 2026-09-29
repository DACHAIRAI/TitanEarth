package com.earthtitan.client;

import com.earthtitan.entity.EarthTitanEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Shared client-side proportions, so absorption reaches the rendered heart during growth. */
public final class TitanVisuals {
    private static final float[] HEIGHTS = {0.0F, 1.8F, 3.0F, 4.5F, 6.5F, 10.0F, 18.0F};
    private static final ResourceLocation DIRT_TEXTURE = new ResourceLocation(
            "minecraft", "textures/block/dirt.png");

    private TitanVisuals() {
    }

    public static float smooth(float progress) {
        float t = Mth.clamp(progress, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    public static float transformation(EarthTitanEntity titan, float partialTick) {
        return titan.getStateId() == 7 && titan.getStage() < 6
                ? smooth((titan.getStateTicks() + partialTick) / 80.0F) : 0.0F;
    }

    public static float stage(EarthTitanEntity titan, float partialTick) {
        return Mth.clamp(titan.getStage(), 1, 6) + transformation(titan, partialTick);
    }

    public static float anatomy(float stage, int firstStage) {
        return Mth.clamp(stage - firstStage + 1.0F, 0.0F, 1.0F);
    }

    public static float bodyY(float stage) {
        return Mth.lerp(anatomy(stage, 2), 3.0F,
                -15.0F - 13.0F * anatomy(stage, 5) - 3.0F * anatomy(stage, 6));
    }

    public static float geometryHeight(float stage) {
        // The model is authored in 1/16-block pixels. Each threshold exposes a
        // larger portion of the same articulated skeleton, so keep the camera
        // scale continuous while the silhouette grows.
        float s = Mth.clamp(stage, 1.0F, 6.0F);
        if (s <= 1.0F) return 36.0F;
        if (s <= 2.0F) return Mth.lerp(s - 1.0F, 36.0F, 254.0F);
        if (s <= 5.0F) return 254.0F;
        return Mth.lerp(s - 5.0F, 254.0F, 288.0F);
    }

    public static float height(EarthTitanEntity titan, float partialTick) {
        int stage = Mth.clamp(titan.getStage(), 1, 6);
        float morph = transformation(titan, partialTick);
        float height = Mth.lerp(morph, HEIGHTS[stage], HEIGHTS[Math.min(6, stage + 1)]);
        // Subtle accumulation between thresholds, smoothly consumed by the transformation.
        float growth = stage < 6 ? Mth.clamp(titan.getGrowthProgress(), 0.0F, 1.0F) : 0.0F;
        return height * (1.0F + growth * 0.025F * (1.0F - morph));
    }

    public static float scale(EarthTitanEntity titan, float partialTick) {
        return height(titan, partialTick) * 16.0F / geometryHeight(stage(titan, partialTick));
    }

    public static float coreHeight(EarthTitanEntity titan, float partialTick) {
        return (13.0F - bodyY(stage(titan, partialTick))) * scale(titan, partialTick) / 16.0F;
    }

    public static ResourceLocation texture(int stage) {
        return DIRT_TEXTURE;
    }
}
