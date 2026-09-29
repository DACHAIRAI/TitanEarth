package com.earthtitan.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class TitanConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue STAGE_1_REQUIRED_BLOCKS;
    public static final ForgeConfigSpec.IntValue STAGE_2_REQUIRED_BLOCKS;
    public static final ForgeConfigSpec.IntValue STAGE_3_REQUIRED_BLOCKS;
    public static final ForgeConfigSpec.IntValue STAGE_4_REQUIRED_BLOCKS;
    public static final ForgeConfigSpec.IntValue STAGE_5_REQUIRED_BLOCKS;
    public static final ForgeConfigSpec.IntValue STAGE_6_REQUIRED_BLOCKS;
    public static final ForgeConfigSpec.IntValue ABSORPTION_RADIUS;
    public static final ForgeConfigSpec.IntValue MAX_ABSORPTION_PER_TICK;
    public static final ForgeConfigSpec.IntValue MAX_ABSORPTION_ATTEMPTS_PER_TICK;
    public static final ForgeConfigSpec.BooleanValue ENABLE_TERRAIN_DAMAGE;
    public static final ForgeConfigSpec.IntValue MAX_BLOCKS_DESTROYED_PER_ATTACK;
    public static final ForgeConfigSpec.DoubleValue EXPLOSION_POWER;
    public static final ForgeConfigSpec.DoubleValue BOSS_HEALTH_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue ATTACK_COOLDOWN;
    public static final ForgeConfigSpec.IntValue GRAB_COOLDOWN;
    public static final ForgeConfigSpec.IntValue SHOOT_COOLDOWN;
    public static final ForgeConfigSpec.IntValue MAX_TITAN_COUNT;
    public static final ForgeConfigSpec.IntValue MAX_PROJECTILES;
    public static final ForgeConfigSpec.IntValue MAX_DEBRIS_ENTITIES;
    public static final ForgeConfigSpec.IntValue MAX_PARTICLES_PER_TICK;
    public static final ForgeConfigSpec.IntValue MAX_BLOCK_CHANGES_PER_TICK;
    public static final ForgeConfigSpec.IntValue MAX_ABSORPTION_TASKS;
    public static final ForgeConfigSpec.IntValue BEDROCK_SCAN_RADIUS;
    public static final ForgeConfigSpec.BooleanValue ABSORB_CONTAINER_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("growth");
        STAGE_1_REQUIRED_BLOCKS = builder.comment("Blocks required before stage 1 is considered complete.")
                .defineInRange("stage1RequiredBlocks", 0, 0, Integer.MAX_VALUE);
        STAGE_2_REQUIRED_BLOCKS = builder.comment("Blocks required to reach stage 2.")
                .defineInRange("stage2RequiredBlocks", 1000, 1, Integer.MAX_VALUE);
        STAGE_3_REQUIRED_BLOCKS = builder.comment("Blocks required to reach stage 3.")
                .defineInRange("stage3RequiredBlocks", 5000, 2, Integer.MAX_VALUE);
        STAGE_4_REQUIRED_BLOCKS = builder.comment("Blocks required to reach stage 4.")
                .defineInRange("stage4RequiredBlocks", 15000, 3, Integer.MAX_VALUE);
        STAGE_5_REQUIRED_BLOCKS = builder.comment("Blocks required to reach stage 5.")
                .defineInRange("stage5RequiredBlocks", 40000, 4, Integer.MAX_VALUE);
        STAGE_6_REQUIRED_BLOCKS = builder.comment("Blocks required to reach stage 6.")
                .defineInRange("stage6RequiredBlocks", 100000, 5, Integer.MAX_VALUE);
        builder.pop();

        builder.push("absorption");
        ABSORPTION_RADIUS = builder.comment("Maximum block absorption radius around the titan.")
                .defineInRange("absorptionRadius", 24, 4, 64);
        MAX_ABSORPTION_PER_TICK = builder.comment("Hard limit of blocks removed by one titan per tick.")
                .defineInRange("maxAbsorptionPerTick", 8, 1, 64);
        MAX_ABSORPTION_ATTEMPTS_PER_TICK = builder.comment("Random block positions inspected per tick.")
                .defineInRange("maxAbsorptionAttemptsPerTick", 120, 16, 512);
        ABSORB_CONTAINER_BLOCKS = builder.comment("Allow absorption of blocks with block entities. Disabled by default for safety.")
                .define("absorbContainerBlocks", false);
        builder.pop();

        builder.push("combat");
        ATTACK_COOLDOWN = builder.defineInRange("attackCooldown", 45, 5, 600);
        GRAB_COOLDOWN = builder.defineInRange("grabCooldown", 180, 20, 1200);
        SHOOT_COOLDOWN = builder.defineInRange("shootCooldown", 70, 10, 1200);
        EXPLOSION_POWER = builder.defineInRange("explosionPower", 4.0D, 0.0D, 12.0D);
        BOSS_HEALTH_MULTIPLIER = builder.comment("Multiplies configured stage health values.")
                .defineInRange("bossHealthMultiplier", 1.0D, 0.1D, 10.0D);
        MAX_PROJECTILES = builder.defineInRange("maxActiveProjectiles", 64, 4, 512);
        builder.pop();

        builder.push("world_safety");
        ENABLE_TERRAIN_DAMAGE = builder.comment("Allow limited natural terrain damage from titan attacks.")
                .define("enableTerrainDamage", true);
        MAX_BLOCKS_DESTROYED_PER_ATTACK = builder.defineInRange("maxBlocksDestroyedPerAttack", 24, 0, 256);
        MAX_DEBRIS_ENTITIES = builder.defineInRange("maxDebrisEntities", 32, 0, 128);
        MAX_PARTICLES_PER_TICK = builder.defineInRange("maxParticlesPerTick", 160, 16, 1000);
        MAX_BLOCK_CHANGES_PER_TICK = builder.defineInRange("maxBlockChangesPerTick", 64, 1, 512);
        MAX_ABSORPTION_TASKS = builder.defineInRange("maxAbsorptionTasks", 64, 1, 512);
        BEDROCK_SCAN_RADIUS = builder.defineInRange("bedrockScanRadius", 8, 2, 16);
        MAX_TITAN_COUNT = builder.comment("Maximum loaded titans in one dimension, counted when a core is activated.")
                .defineInRange("maxTitanCount", 1, 1, 16);
        builder.pop();

        DEBUG_LOGGING = builder.comment("Enable low-volume debug messages for lifecycle events.")
                .define("debugLogging", false);

        SPEC = builder.build();
    }

    private TitanConfig() {
    }

    public static long thresholdForStage(int stage) {
        return switch (stage) {
            case 1 -> STAGE_1_REQUIRED_BLOCKS.get();
            case 2 -> STAGE_2_REQUIRED_BLOCKS.get();
            case 3 -> STAGE_3_REQUIRED_BLOCKS.get();
            case 4 -> STAGE_4_REQUIRED_BLOCKS.get();
            case 5 -> STAGE_5_REQUIRED_BLOCKS.get();
            case 6 -> STAGE_6_REQUIRED_BLOCKS.get();
            default -> Long.MAX_VALUE;
        };
    }

    public static int stageForAbsorbed(long absorbed) {
        int stage = 1;
        for (int candidate = 2; candidate <= 6; candidate++) {
            if (absorbed >= thresholdForStage(candidate)) {
                stage = candidate;
            }
        }
        return stage;
    }

    public static double healthForStage(int stage) {
        double base = switch (stage) {
            case 1 -> 200.0D;
            case 2 -> 450.0D;
            case 3 -> 650.0D;
            case 4 -> 800.0D;
            case 5 -> 960.0D;
            default -> 1024.0D;
        };
        return Math.min(1024.0D, base * BOSS_HEALTH_MULTIPLIER.get());
    }

    public static double damageForStage(int stage) {
        return switch (stage) {
            case 1 -> 8.0D;
            case 2 -> 16.0D;
            case 3 -> 28.0D;
            case 4 -> 45.0D;
            case 5 -> 72.0D;
            default -> 110.0D;
        };
    }

    public static double followRangeForStage(int stage) {
        return switch (stage) {
            case 1 -> 24.0D;
            case 2 -> 32.0D;
            case 3 -> 48.0D;
            case 4 -> 64.0D;
            case 5 -> 96.0D;
            default -> 128.0D;
        };
    }

    public static double absorptionRadiusForStage(int stage) {
        double configured = ABSORPTION_RADIUS.get();
        return switch (stage) {
            case 1 -> Math.min(configured, 8.0D);
            case 2 -> Math.min(configured, 12.0D);
            case 3 -> Math.min(configured, 16.0D);
            case 4 -> Math.min(configured, 20.0D);
            case 5 -> Math.min(configured, 28.0D);
            default -> configured;
        };
    }

    public static int absorptionLimitForStage(int stage) {
        int configured = MAX_ABSORPTION_PER_TICK.get();
        return Math.min(64, Math.max(1, configured + Math.max(0, stage - 1) * 2));
    }

    public static double scaleForStage(int stage) {
        return switch (stage) {
            case 1 -> 0.90D;
            case 2 -> 1.20D;
            case 3 -> 1.60D;
            case 4 -> 2.10D;
            case 5 -> 3.10D;
            default -> 5.00D;
        };
    }
}
