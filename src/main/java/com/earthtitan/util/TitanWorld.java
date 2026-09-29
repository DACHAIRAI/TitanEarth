package com.earthtitan.util;

import com.earthtitan.config.TitanConfig;
import com.earthtitan.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.Map;
import java.util.WeakHashMap;

/** Shared limits for all titans/projectiles in a dimension; never loads a chunk. */
public final class TitanWorld {
    private static final Map<ServerLevel, Budget> BUDGETS = new WeakHashMap<>();
    private static final class Budget {
        long tick = Long.MIN_VALUE;
        int blocks, particles, effects;
    }
    private static Budget budget(ServerLevel level) {
        Budget b = BUDGETS.computeIfAbsent(level, ignored -> new Budget());
        if (b.tick != level.getGameTime()) {
            b.tick = level.getGameTime(); b.blocks = 0; b.particles = 0; b.effects = 0;
        }
        return b;
    }
    public static boolean takeBlock(ServerLevel level) {
        Budget b = budget(level);
        if (b.blocks >= TitanConfig.MAX_BLOCK_CHANGES_PER_TICK.get()) return false;
        b.blocks++; return true;
    }
    public static boolean takeEffect(ServerLevel level) {
        Budget b = budget(level);
        if (b.effects >= TitanConfig.MAX_ABSORPTION_TASKS.get()) return false;
        b.effects++; return true;
    }
    public static <T extends ParticleOptions> void particles(ServerLevel level, T type, double x, double y,
            double z, int count, double dx, double dy, double dz, double speed) {
        Budget b = budget(level);
        int allowed = Math.min(count, TitanConfig.MAX_PARTICLES_PER_TICK.get() - b.particles);
        if (allowed <= 0) return;
        b.particles += allowed;
        level.sendParticles(type, x, y, z, allowed, dx, dy, dz, speed);
    }
    public static boolean natural(ServerLevel level, BlockPos pos, BlockState state) {
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()
                || state.isAir() || !state.is(ModTags.ABSORBABLE)
                || state.getDestroySpeed(level, pos) < 0
                || state.hasBlockEntity() && !TitanConfig.ABSORB_CONTAINER_BLOCKS.get()) return false;
        if (state.is(Blocks.BEDROCK) || state.is(Blocks.BARRIER) || state.is(Blocks.COMMAND_BLOCK)
                || state.is(Blocks.CHAIN_COMMAND_BLOCK) || state.is(Blocks.REPEATING_COMMAND_BLOCK)
                || state.is(Blocks.STRUCTURE_BLOCK) || state.is(Blocks.STRUCTURE_VOID)
                || state.is(Blocks.JIGSAW) || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_PORTAL_FRAME)) return false;
        var key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return key != null && !key.getNamespace().equals("earthtitan");
    }
    public static int breakNatural(ServerLevel level, Entity cause, BlockPos center, int radius, int max) {
        if (!TitanConfig.ENABLE_TERRAIN_DAMAGE.get() || max <= 0
                || !net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level, cause)) return 0;
        int changed = 0;
        radius = Math.min(8, radius);
        int attempts = Math.min(384, Math.max(16, max * 8));
        for (int i = 0; i < attempts && changed < max; i++) {
            BlockPos pos = center.offset(level.random.nextInt(radius * 2 + 1) - radius,
                    level.random.nextInt(5) - 3, level.random.nextInt(radius * 2 + 1) - radius);
            if (!level.hasChunkAt(pos) || pos.distSqr(center) > radius * radius) continue;
            BlockState state = level.getBlockState(pos);
            if (natural(level, pos, state)) {
                if (!takeBlock(level)) break;
                if (level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) changed++;
            }
        }
        return changed;
    }
    public static int projectileCount(ServerLevel level) {
        int count = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof com.earthtitan.entity.EarthProjectileEntity && !entity.isRemoved()) count++;
        }
        return count;
    }
    private TitanWorld() { }
}
