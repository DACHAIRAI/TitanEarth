package com.earthtitan.registry;

import com.earthtitan.EarthTitanMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final TagKey<Block> ABSORBABLE = TagKey.create(Registries.BLOCK,
            new ResourceLocation(EarthTitanMod.MOD_ID, "absorbable"));

    private ModTags() {
    }
}
