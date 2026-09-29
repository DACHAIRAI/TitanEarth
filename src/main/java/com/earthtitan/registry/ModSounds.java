package com.earthtitan.registry;

import com.earthtitan.EarthTitanMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, EarthTitanMod.MOD_ID);

    public static final RegistryObject<SoundEvent> ACTIVATION = register("activation");
    public static final RegistryObject<SoundEvent> ACTIVATION_ROAR = register("activation_roar");
    public static final RegistryObject<SoundEvent> GROWTH_1 = register("growth_1");
    public static final RegistryObject<SoundEvent> GROWTH_2 = register("growth_2");
    public static final RegistryObject<SoundEvent> GROWTH_3 = register("growth_3");
    public static final RegistryObject<SoundEvent> GROWTH_4 = register("growth_4");
    public static final RegistryObject<SoundEvent> GROWTH_5 = register("growth_5");
    public static final RegistryObject<SoundEvent> EARTH_ABSORB = register("earth_absorb");
    public static final RegistryObject<SoundEvent> EARTH_ABSORB_HEAVY = register("earth_absorb_heavy");
    public static final RegistryObject<SoundEvent> EARTH_SHOT_1 = register("earth_shot_1");
    public static final RegistryObject<SoundEvent> EARTH_SHOT_2 = register("earth_shot_2");
    public static final RegistryObject<SoundEvent> EARTH_SHOT_3 = register("earth_shot_3");
    public static final RegistryObject<SoundEvent> EARTH_SHOT_HEAVY = register("earth_shot_heavy");
    public static final RegistryObject<SoundEvent> EARTH_SHOT_BOULDER = register("earth_shot_boulder");
    public static final RegistryObject<SoundEvent> EARTH_EXPLOSION = register("earth_explosion");
    public static final RegistryObject<SoundEvent> HAND_ATTACK = register("hand_attack");
    public static final RegistryObject<SoundEvent> HAND_ATTACK_HEAVY = register("hand_attack_heavy");
    public static final RegistryObject<SoundEvent> GRAB = register("grab");
    public static final RegistryObject<SoundEvent> ROAR_1 = register("roar_1");
    public static final RegistryObject<SoundEvent> ROAR_2 = register("roar_2");
    public static final RegistryObject<SoundEvent> ROAR_3 = register("roar_3");
    public static final RegistryObject<SoundEvent> FOOTSTEP_1 = register("footstep_1");
    public static final RegistryObject<SoundEvent> FOOTSTEP_2 = register("footstep_2");
    public static final RegistryObject<SoundEvent> FOOTSTEP_HEAVY = register("footstep_heavy");
    public static final RegistryObject<SoundEvent> SWORD_SWING = register("sword_swing");
    public static final RegistryObject<SoundEvent> SWORD_HIT = register("sword_hit");
    public static final RegistryObject<SoundEvent> GROUND_SLAM = register("ground_slam");
    public static final RegistryObject<SoundEvent> TITAN_CHARGE = register("titan_charge");
    public static final RegistryObject<SoundEvent> TITAN_DEATH = register("titan_death");
    public static final RegistryObject<SoundEvent> TITAN_BREAK = register("titan_break");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(EarthTitanMod.MOD_ID, name)));
    }

    private ModSounds() {
    }
}
