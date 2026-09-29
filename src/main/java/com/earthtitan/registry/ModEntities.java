package com.earthtitan.registry;

import com.earthtitan.EarthTitanMod;
import com.earthtitan.entity.EarthProjectileEntity;
import com.earthtitan.entity.EarthTitanEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EarthTitanMod.MOD_ID);

    public static final RegistryObject<EntityType<EarthTitanEntity>> EARTH_TITAN = ENTITY_TYPES.register("earth_titan",
            () -> EntityType.Builder.of(EarthTitanEntity::new, MobCategory.MONSTER)
                    .sized(1.4F, 3.0F)
                    .clientTrackingRange(128)
                    .updateInterval(2)
                    .build(EarthTitanMod.MOD_ID + ":earth_titan"));

    public static final RegistryObject<EntityType<EarthProjectileEntity>> EARTH_PROJECTILE = ENTITY_TYPES.register("earth_projectile",
            () -> EntityType.Builder.<EarthProjectileEntity>of(EarthProjectileEntity::new, MobCategory.MISC)
                    .sized(0.55F, 0.55F)
                    .clientTrackingRange(96)
                    .updateInterval(1)
                    .build(EarthTitanMod.MOD_ID + ":earth_projectile"));

    private ModEntities() {
    }
}
