package com.earthtitan.registry;

import com.earthtitan.EarthTitanMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EarthTitanMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> EARTH_TITAN_TAB = CREATIVE_MODE_TABS.register("earth_titan_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.earthtitan"))
            .icon(() -> new ItemStack(ModItems.EARTH_TITAN_CORE.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.EARTH_TITAN_CORE.get());
                output.accept(ModItems.EARTH_PICKAXE.get());
            })
            .build());

    private ModTabs() {
    }
}
