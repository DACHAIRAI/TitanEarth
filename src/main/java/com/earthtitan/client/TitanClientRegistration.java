package com.earthtitan.client;

import com.earthtitan.EarthTitanMod;
import com.earthtitan.client.model.EarthTitanModel;
import com.earthtitan.client.model.TitanSwordModel;
import com.earthtitan.client.renderer.EarthProjectileRenderer;
import com.earthtitan.client.renderer.EarthTitanRenderer;
import com.earthtitan.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EarthTitanMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TitanClientRegistration {
    private TitanClientRegistration() {
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(EarthTitanModel.LAYER, EarthTitanModel::createBodyLayer);
        event.registerLayerDefinition(TitanSwordModel.LAYER, TitanSwordModel::createLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.EARTH_TITAN.get(), EarthTitanRenderer::new);
        event.registerEntityRenderer(ModEntities.EARTH_PROJECTILE.get(), EarthProjectileRenderer::new);
    }
}
