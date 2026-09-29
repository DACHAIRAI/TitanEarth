package com.earthtitan.registry;

import com.earthtitan.EarthTitanMod;
import com.earthtitan.block.EarthTitanCoreBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, EarthTitanMod.MOD_ID);

    public static final RegistryObject<Block> EARTH_TITAN_CORE = BLOCKS.register("earth_titan_core", EarthTitanCoreBlock::new);

    private ModBlocks() {
    }
}
