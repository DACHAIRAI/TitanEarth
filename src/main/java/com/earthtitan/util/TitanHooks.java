package com.earthtitan.util;

import com.earthtitan.EarthTitanMod;
import com.earthtitan.entity.EarthTitanEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** Small, stable extension points for packs and future integrations. */
public final class TitanHooks {
    private TitanHooks() {
    }

    public static void onCoreActivated(Player player, BlockPos pos) {
        debug("onCoreActivated: player={}, pos={}", player.getGameProfile().getName(), pos);
    }

    public static void onBlockAbsorbed(EarthTitanEntity titan, BlockPos pos, BlockState state) {
        debug("onBlockAbsorbed: stage={}, block={}, pos={}", titan.getStage(), state.getBlock().builtInRegistryHolder().key().location(), pos);
    }

    public static void onStageUp(EarthTitanEntity titan, int previous, int current) {
        debug("onStageUp: {} -> {}", previous, current);
    }

    public static void onShoot(EarthTitanEntity titan, int variant) {
        debug("onShoot: stage={}, variant={}", titan.getStage(), variant);
    }

    public static void onGrab(EarthTitanEntity titan, Player player) {
        debug("onGrab: target={}", player.getGameProfile().getName());
    }

    public static void onSwordAttack(EarthTitanEntity titan) {
        debug("onSwordAttack: stage={}", titan.getStage());
    }

    public static void onExplosion(EarthTitanEntity titan) {
        debug("onExplosion: stage={}", titan.getStage());
    }

    public static void onBedrockDetected(EarthTitanEntity titan, BlockPos pos) {
        debug("onBedrockDetected: pos={}", pos);
    }

    public static void onTitanDeath(EarthTitanEntity titan) {
        debug("onTitanDeath: absorbed={}", titan.getAbsorbedBlocks());
    }

    private static void debug(String message, Object... args) {
        if (com.earthtitan.config.TitanConfig.DEBUG_LOGGING.get()) {
            EarthTitanMod.LOGGER.info("[Earth Titan] " + message, args);
        }
    }
}
