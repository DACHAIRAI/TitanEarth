package com.earthtitan;

import com.earthtitan.config.TitanConfig;
import com.earthtitan.registry.ModBlocks;
import com.earthtitan.registry.ModEntities;
import com.earthtitan.registry.ModItems;
import com.earthtitan.registry.ModSounds;
import com.earthtitan.registry.ModTabs;
import com.earthtitan.network.TitanNetwork;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EarthTitanMod.MOD_ID)
public final class EarthTitanMod {
    public static final String MOD_ID = "earthtitan";
    public static final String MOD_NAME = "Earth Titan";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EarthTitanMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITY_TYPES.register(modBus);
        ModSounds.SOUND_EVENTS.register(modBus);
        ModTabs.CREATIVE_MODE_TABS.register(modBus);
        TitanNetwork.init();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TitanConfig.SPEC);
        LOGGER.info("Earth Titan is awakening");
    }
}
