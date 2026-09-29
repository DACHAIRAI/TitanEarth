package com.earthtitan.registry;

import com.earthtitan.EarthTitanMod;
import com.earthtitan.item.EarthPickaxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, EarthTitanMod.MOD_ID);

    public static final RegistryObject<Item> EARTH_TITAN_CORE = ITEMS.register("earth_titan_core",
            () -> new BlockItem(ModBlocks.EARTH_TITAN_CORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> EARTH_PICKAXE = ITEMS.register("earth_pickaxe", EarthPickaxeItem::new);

    private ModItems() {
    }
}
