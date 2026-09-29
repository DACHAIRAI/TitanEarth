package com.earthtitan.entity;

import com.earthtitan.EarthTitanMod;
import com.earthtitan.config.TitanConfig;
import com.earthtitan.network.TitanNetwork;
import com.earthtitan.registry.ModBlocks;
import com.earthtitan.registry.ModSounds;
import com.earthtitan.registry.ModTags;
import com.earthtitan.util.TitanHooks;
import com.earthtitan.util.TitanLedger;
import com.earthtitan.util.TitanWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Server-authoritative six-stage Earth Titan. Its block absorption deliberately
 * samples a bounded number of loaded positions instead of making one entity per
 * block, keeping the feature usable in both single-player and dedicated servers.
 */
public final class EarthTitanEntity extends PathfinderMob {
    public static final int STATE_IDLE = 0;
    public static final int STATE_SEARCHING = 1;
    public static final int STATE_CHASING = 2;
    public static final int STATE_ABSORBING = 3;
    public static final int STATE_ATTACKING = 4;
    public static final int STATE_SHOOTING = 5;
    public static final int STATE_GRABBING = 6;
    public static final int STATE_TRANSFORMING = 7;
    public static final int STATE_DYING_BY_BEDROCK = 8;
    public static final int STATE_DEAD = 9;
    public static final int STATE_ACTIVATING = 10;
    public static final int STATE_DYING = 11;

    public static final int ACTION_NONE = 0;
    public static final int ACTION_PUNCH = 1;
    public static final int ACTION_DOUBLE_PUNCH = 2;
    public static final int ACTION_SLAM = 3;
    public static final int ACTION_GRAB = 4;
    public static final int ACTION_SHOOT = 5;
    public static final int ACTION_SLASH = 6;
    public static final int ACTION_HEAVY_SLASH = 7;
    public static final int ACTION_GROUND_CUTTER = 8;
    public static final int ACTION_VERTICAL_SMASH = 9;
    public static final int ACTION_STOMP = 10;
    public static final int ACTION_EXPLOSION = 11;
    public static final int ACTION_BARRAGE = 12;
    public static final int ACTION_CATAPULT = 13;

    private static final EntityDataAccessor<Integer> DATA_STAGE =
            SynchedEntityData.defineId(EarthTitanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_ABSORBED =
            SynchedEntityData.defineId(EarthTitanEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> DATA_GROWTH_PROGRESS =
            SynchedEntityData.defineId(EarthTitanEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(EarthTitanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ACTION =
            SynchedEntityData.defineId(EarthTitanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STATE_TICKS =
            SynchedEntityData.defineId(EarthTitanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ACTION_TICKS =
            SynchedEntityData.defineId(EarthTitanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SHAKE_PULSE =
            SynchedEntityData.defineId(EarthTitanEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.translatable("boss.earthtitan.name", 1, 0L),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
    private UUID ownerUuid;
    private UUID savedTargetUuid;
    private int stateTicks;
    private int actionTicks;
    private int actionDuration;
    private int transitionTargetStage;
    private int grabbedEntityId = -1;
    private int deathTimer;
    private int shootCooldown;
    private int grabCooldown;
    private int attackCooldown;
    private int slamCooldown;
    private int explosionCooldown;
    private int absorptionCooldown;
    private int lastFootstepTick;
    private boolean debrisSpawned;

    public EarthTitanEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 100;
        this.setPersistenceRequired();
        this.navigation.setCanFloat(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1024.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.ATTACK_DAMAGE, 110.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 128.0D)
                .add(Attributes.ARMOR, 18.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0D);
    }

    public static EarthTitanEntity createActivated(ServerLevel level, BlockPos pos, Player owner) {
        EarthTitanEntity titan = new EarthTitanEntity(com.earthtitan.registry.ModEntities.EARTH_TITAN.get(), level);
        titan.moveTo(pos.getX() + 0.5D, pos.getY() + 0.25D, pos.getZ() + 0.5D, 0.0F, 0.0F);
        titan.ownerUuid = owner.getUUID();
        titan.setStage(1, false);
        titan.setState(STATE_ACTIVATING);
        titan.setNoGravity(true);
        titan.setHealth((float) TitanConfig.healthForStage(1));
        return titan;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_STAGE, 1);
        this.entityData.define(DATA_ABSORBED, 0L);
        this.entityData.define(DATA_GROWTH_PROGRESS, 0.0F);
        this.entityData.define(DATA_STATE, STATE_IDLE);
        this.entityData.define(DATA_ACTION, ACTION_NONE);
        this.entityData.define(DATA_STATE_TICKS, 0);
        this.entityData.define(DATA_ACTION_TICKS, 0);
        this.entityData.define(DATA_SHAKE_PULSE, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) this.level();
        this.stateTicks++;
        this.actionTicks++;
        this.entityData.set(DATA_STATE_TICKS, this.stateTicks);
        this.entityData.set(DATA_ACTION_TICKS, this.actionTicks);
        decrementCooldowns();

        if (getStateId() == STATE_DYING_BY_BEDROCK || getStateId() == STATE_DYING || getStateId() == STATE_DEAD) {
            tickDeathSequence(serverLevel);
            return;
        }
        if (getStateId() == STATE_ACTIVATING) {
            tickActivation(serverLevel);
            updateBossBar(serverLevel);
            return;
        }
        if (getStateId() == STATE_TRANSFORMING) {
            tickTransformation(serverLevel);
            updateBossBar(serverLevel);
            return;
        }
        if (getAction() != ACTION_NONE) {
            tickAction(serverLevel);
            updateBossBar(serverLevel);
            return;
        }

        if (this.tickCount % 20 == 0 && detectBedrock(serverLevel)) {
            beginBedrockDeath(serverLevel);
            return;
        }
        updateGrowthProgress();
        maybeStartTransformation(serverLevel);
        if (getStateId() == STATE_TRANSFORMING) {
            return;
        }
        LivingEntity target = acquireTarget(serverLevel);
        boolean absorbed = absorbNearbyBlocks(serverLevel);
        if (target != null && target.isAlive()) {
            setState(absorbed ? STATE_ABSORBING : STATE_CHASING);
            tickChase(serverLevel, target);
        } else {
            setState(absorbed ? STATE_ABSORBING : STATE_SEARCHING);
            this.navigation.stop();
            if (this.tickCount % 40 == 0) {
                playRandomRoar(serverLevel, 0.65F);
            }
        }
        if (absorbed && this.tickCount % 8 == 0) {
            triggerShake();
        }
        updateBossBar(serverLevel);
    }

    private void decrementCooldowns() {
        if (shootCooldown > 0) shootCooldown--;
        if (grabCooldown > 0) grabCooldown--;
        if (attackCooldown > 0) attackCooldown--;
        if (slamCooldown > 0) slamCooldown--;
        if (explosionCooldown > 0) explosionCooldown--;
        if (absorptionCooldown > 0) absorptionCooldown--;
    }

    private void tickActivation(ServerLevel level) {
        this.setNoGravity(true);
        this.setDeltaMovement(new Vec3(0.0D, 0.018D, 0.0D));
        if (stateTicks % 3 == 0) {
            sendDust(level, 4, 0.8D);
        }
        if (stateTicks == 1) {
            playSound(level, ModSounds.ACTIVATION.get(), 1.3F, 0.8F);
            sendMessage(level, "message.earthtitan.awakened");
            TitanNetwork.effect(level, position(), 1.0F, 1);
        } else if (stateTicks == 45) {
            playSound(level, ModSounds.ACTIVATION_ROAR.get(), 2.0F, 0.65F);
            triggerShake();
        } else if (stateTicks == 72) {
            absorbNearbyBlocks(level);
            sendShockwave(level, 2.2F, 2.0F);
        }
        if (stateTicks >= 100) {
            this.setNoGravity(false);
            setState(STATE_IDLE);
            playSound(level, ModSounds.GROWTH_1.get(), 1.5F, 0.85F);
            TitanHooks.onStageUp(this, 0, 1);
        }
    }

    private void tickTransformation(ServerLevel level) {
        this.setNoGravity(true);
        float progress = Mth.clamp(stateTicks / 80.0F, 0.0F, 1.0F);
        this.setDeltaMovement(new Vec3(0.0D, stateTicks < 40 ? 0.035D : -0.025D, 0.0D));
        if (stateTicks % 2 == 0) {
            sendDust(level, Math.min(10, TitanConfig.MAX_PARTICLES_PER_TICK.get() / 20), 1.2D + progress * 2.5D);
        }
        if (stateTicks == 1) {
            playSound(level, growthSoundForStage(transitionTargetStage), 2.0F, 0.7F);
            TitanNetwork.effect(level, position(), 2.0F + transitionTargetStage * 0.3F, 1);
            sendMessage(level, "message.earthtitan.stage", transitionTargetStage);
        }
        if (stateTicks % 10 == 0) {
            triggerShake();
        }
        if (stateTicks >= 80) {
            int previous = getStage();
            setStage(transitionTargetStage, true);
            this.setNoGravity(false);
            setState(STATE_IDLE);
            playSound(level, growthSoundForStage(transitionTargetStage), 2.5F, 0.75F);
            sendShockwave(level, 2.4F + transitionTargetStage * 0.35F, 2.0F + transitionTargetStage * 0.2F);
            TitanHooks.onStageUp(this, previous, transitionTargetStage);
            if (transitionTargetStage == 6) {
                sendMessage(level, "message.earthtitan.final_form");
            }
        }
    }

    private void tickChase(ServerLevel level, LivingEntity target) {
        double distance = distanceToSqr(target);
        double speed = 0.55D + getStage() * 0.055D;
        if (distance > attackRangeForStage() * attackRangeForStage()) {
            this.navigation.moveTo(target, speed);
        } else {
            this.navigation.stop();
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            chooseAttack(level, target);
        }
        if (getStage() >= 5 && this.onGround() && this.tickCount - lastFootstepTick > 28 && horizontalDistanceTo(target) > 4.0D) {
            lastFootstepTick = tickCount;
            playSound(level, getStage() == 6 ? ModSounds.FOOTSTEP_HEAVY.get() : ModSounds.FOOTSTEP_1.get(), 1.2F, 0.8F);
            sendShockwave(level, getStage() == 6 ? 1.7F : 1.1F, 1.0F);
            TitanNetwork.effect(level, position(), getStage() == 6 ? 1.8F : 1.0F, 4);
        }
    }

    private void chooseAttack(ServerLevel level, LivingEntity target) {
        double distance = Math.sqrt(distanceToSqr(target));
        if (getStage() >= 6 && distance <= 11.0D && attackCooldown == 0) {
            int choice = random.nextInt(4);
            startAction(choice == 0 ? ACTION_SLASH : choice == 1 ? ACTION_HEAVY_SLASH : choice == 2 ? ACTION_GROUND_CUTTER : ACTION_VERTICAL_SMASH,
                    STATE_ATTACKING, choice == 1 ? 38 : 30);
            attackCooldown = TitanConfig.ATTACK_COOLDOWN.get();
        } else if (getStage() >= 4 && distance <= 8.0D && grabCooldown == 0) {
            startAction(ACTION_GRAB, STATE_GRABBING, 82);
            grabCooldown = TitanConfig.GRAB_COOLDOWN.get();
        } else if (getStage() >= 5 && distance <= 10.0D && slamCooldown == 0) {
            startAction(getStage() == 6 ? ACTION_STOMP : ACTION_SLAM, STATE_ATTACKING, 34);
            slamCooldown = TitanConfig.ATTACK_COOLDOWN.get() + 20;
        } else if (getStage() >= 3 && distance <= TitanConfig.followRangeForStage(getStage()) && shootCooldown == 0) {
            int action = getStage() >= 6 && random.nextInt(4) == 0 ? ACTION_BARRAGE : ACTION_SHOOT;
            startAction(action, STATE_SHOOTING, action == ACTION_BARRAGE ? 52 : 28);
            shootCooldown = TitanConfig.SHOOT_COOLDOWN.get();
        } else if (attackCooldown == 0) {
            startAction(getStage() >= 2 && random.nextBoolean() ? ACTION_PUNCH : ACTION_SLAM,
                    STATE_ATTACKING, 26);
            attackCooldown = TitanConfig.ATTACK_COOLDOWN.get();
        }
    }

    private void startAction(int action, int state, int duration) {
        this.entityData.set(DATA_ACTION, action);
        this.actionTicks = 0;
        this.entityData.set(DATA_ACTION_TICKS, 0);
        this.actionDuration = duration;
        this.grabbedEntityId = -1;
        setState(state);
        this.navigation.stop();
    }

    private void tickAction(ServerLevel level) {
        this.navigation.stop();
        LivingEntity target = getTarget();
        switch (getAction()) {
            case ACTION_PUNCH -> {
                if (actionTicks == 14) performPunch(level, false);
            }
            case ACTION_DOUBLE_PUNCH -> {
                if (actionTicks == 13 || actionTicks == 20) performPunch(level, actionTicks == 20);
            }
            case ACTION_SLAM, ACTION_STOMP -> {
                if (actionTicks == 18) performSlam(level, getAction() == ACTION_STOMP);
            }
            case ACTION_GRAB -> tickGrab(level, target);
            case ACTION_SHOOT -> {
                if (actionTicks == 14) fireProjectile(level, false);
            }
            case ACTION_BARRAGE -> {
                if (actionTicks == 16 || actionTicks == 24 || actionTicks == 32) fireProjectile(level, true);
            }
            case ACTION_CATAPULT -> {
                if (actionTicks == 22) fireProjectile(level, false);
            }
            case ACTION_SLASH, ACTION_HEAVY_SLASH, ACTION_GROUND_CUTTER, ACTION_VERTICAL_SMASH -> {
                if (actionTicks == 20) performSwordAttack(level, getAction());
            }
            case ACTION_EXPLOSION -> {
                if (actionTicks == 48) performTitanExplosion(level);
            }
            default -> {
            }
        }
        if (actionTicks >= actionDuration) {
            this.entityData.set(DATA_ACTION, ACTION_NONE);
            this.actionTicks = 0;
            this.entityData.set(DATA_ACTION_TICKS, 0);
            this.grabbedEntityId = -1;
            setState(STATE_IDLE);
        }
    }

    private void tickGrab(ServerLevel level, @Nullable LivingEntity target) {
        if (actionTicks == 16 && target != null && target.isAlive() && distanceToSqr(target) <= 14.0D * 14.0D) {
            grabbedEntityId = target.getId();
            setState(STATE_GRABBING);
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.LEVITATION, 70, 0, false, true));
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 70, 3, false, true));
            playSound(level, ModSounds.GRAB.get(), 1.7F, 0.75F);
            TitanNetwork.effect(level, target.position(), 1.5F, 5);
            if (target instanceof Player player) {
                TitanHooks.onGrab(this, player);
            }
        }
        if (grabbedEntityId >= 0 && actionTicks >= 22 && actionTicks % 15 == 0) {
            Entity entity = level.getEntity(grabbedEntityId);
            if (entity instanceof LivingEntity victim && victim.isAlive()) {
                victim.hurt(damageSources().mobAttack(this), (float) (TitanConfig.damageForStage(getStage()) * 0.35D));
                victim.setDeltaMovement(new Vec3(0.0D, 0.08D, 0.0D));
                victim.hurtMarked = true;
            }
        }
        if (grabbedEntityId >= 0 && actionTicks == 68) {
            Entity entity = level.getEntity(grabbedEntityId);
            if (entity instanceof LivingEntity victim && victim.isAlive()) {
                victim.removeEffect(net.minecraft.world.effect.MobEffects.LEVITATION);
                victim.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
                Vec3 away = victim.position().subtract(position()).normalize();
                victim.push(away.x * 1.5D, 0.85D, away.z * 1.5D);
                victim.hurtMarked = true;
            }
        }
    }

    private void performPunch(ServerLevel level, boolean heavy) {
        Vec3 forward = horizontalLook();
        Vec3 center = position().add(forward.scale(2.5D)).add(0.0D, getBbHeight() * 0.55D, 0.0D);
        double damage = TitanConfig.damageForStage(getStage()) * (heavy ? 1.35D : 0.85D);
        damageEntities(level, center, heavy ? 4.5D : 3.2D, heavy ? 2.5D : 1.8D, damage,
                heavy ? 1.4D : 0.85D, 0.48D);
        playSound(level, heavy ? ModSounds.HAND_ATTACK_HEAVY.get() : ModSounds.HAND_ATTACK.get(), 1.8F, heavy ? 0.65F : 0.82F);
        sendDust(level, Math.min(24, TitanConfig.MAX_PARTICLES_PER_TICK.get()), heavy ? 2.0D : 1.1D);
        sendShockwave(level, heavy ? 2.0F : 1.1F, heavy ? 2.0F : 1.0F);
        triggerShake();
    }

    private void performSlam(ServerLevel level, boolean stomp) {
        double radius = stomp ? 11.0D : 7.0D;
        double damage = TitanConfig.damageForStage(getStage()) * (stomp ? 1.5D : 1.15D);
        damageEntities(level, position(), radius, Math.max(2.0D, getBbHeight() * 0.6D), damage,
                stomp ? 1.65D : 1.1D, stomp ? 0.72D : 0.52D);
        if (TitanConfig.ENABLE_TERRAIN_DAMAGE.get()) {
            destroyNaturalBlocks(level, blockPosition(), stomp ? 5 : 3, TitanConfig.MAX_BLOCKS_DESTROYED_PER_ATTACK.get());
        }
        playSound(level, ModSounds.GROUND_SLAM.get(), 2.4F, stomp ? 0.55F : 0.72F);
        sendShockwave(level, stomp ? 3.7F : 2.5F, stomp ? 2.7F : 1.7F);
        TitanNetwork.effect(level, position(), stomp ? 3.5F : 2.3F, stomp ? 4 : 0);
        triggerShake();
    }

    private void performSwordAttack(ServerLevel level, int action) {
        Vec3 forward = horizontalLook();
        Vec3 center = position().add(forward.scale(4.0D)).add(0.0D, getBbHeight() * 0.6D, 0.0D);
        double radius = action == ACTION_HEAVY_SLASH ? 9.0D : 7.0D;
        double damage = TitanConfig.damageForStage(getStage()) * (action == ACTION_HEAVY_SLASH ? 2.0D : 1.3D);
        damageEntities(level, center, radius, getBbHeight() * 0.55D, damage,
                action == ACTION_HEAVY_SLASH ? 2.0D : 1.2D, action == ACTION_VERTICAL_SMASH ? 0.95D : 0.56D);
        if (action == ACTION_GROUND_CUTTER || action == ACTION_VERTICAL_SMASH) {
            destroyNaturalBlocks(level, blockPosition().offset((int) (forward.x * 3.0D), 0, (int) (forward.z * 3.0D)),
                    action == ACTION_VERTICAL_SMASH ? 5 : 3, TitanConfig.MAX_BLOCKS_DESTROYED_PER_ATTACK.get());
            sendShockwave(level, action == ACTION_VERTICAL_SMASH ? 4.5F : 3.0F, action == ACTION_VERTICAL_SMASH ? 3.0F : 2.0F);
        }
        playSound(level, action == ACTION_GROUND_CUTTER ? ModSounds.GROUND_SLAM.get() : ModSounds.SWORD_HIT.get(), 2.0F,
                action == ACTION_HEAVY_SLASH ? 0.55F : 0.78F);
        TitanHooks.onSwordAttack(this);
        triggerShake();
    }

    private void performTitanExplosion(ServerLevel level) {
        float power = TitanConfig.EXPLOSION_POWER.get().floatValue() + getStage() * 0.7F;
        level.explode(this, getX(), getY() + Math.min(getBbHeight(), 6.0D), getZ(), power, Level.ExplosionInteraction.NONE);
        if (TitanConfig.ENABLE_TERRAIN_DAMAGE.get()) {
            destroyNaturalBlocks(level, blockPosition(), 6, TitanConfig.MAX_BLOCKS_DESTROYED_PER_ATTACK.get());
        }
        damageEntities(level, position(), power * 3.0D, power * 2.0D, TitanConfig.damageForStage(getStage()) * 2.5D,
                2.2D, 0.95D);
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + getBbHeight() * 0.45D, getZ(), 6, 1.5D, 1.5D, 1.5D, 0.1D);
        playSound(level, ModSounds.EARTH_EXPLOSION.get(), 2.5F, 0.55F);
        TitanHooks.onExplosion(this);
        TitanNetwork.effect(level, position(), power, 2);
        triggerShake();
    }

    private void fireProjectile(ServerLevel level, boolean barrage) {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        int variant;
        if (getStage() >= 6) {
            variant = barrage ? (2 + random.nextInt(4)) : (random.nextInt(5) == 0 ? EarthProjectileEntity.VARIANT_CANNON : EarthProjectileEntity.VARIANT_BOULDER);
        } else {
            variant = getStage() == 3 ? random.nextInt(3) : (random.nextBoolean() ? EarthProjectileEntity.VARIANT_HEAVY : EarthProjectileEntity.VARIANT_SPEAR);
        }
        int count = barrage ? Math.min(5, getStage() - 1) : 1;
        if (TitanWorld.projectileCount(level) + count > TitanConfig.MAX_PROJECTILES.get()) {
            return;
        }
        for (int i = 0; i < count; i++) {
            EarthProjectileEntity projectile = EarthProjectileEntity.create(level, this, variant);
            Vec3 muzzle = position().add(0.0D, Math.max(1.0D, getBbHeight() * 0.67D), 0.0D)
                    .add(horizontalLook().scale(1.8D + i * 0.45D));
            projectile.setPos(muzzle.x, muzzle.y, muzzle.z);
            Vec3 aim = target.getEyePosition().subtract(muzzle).normalize();
            if (barrage) {
                aim = aim.add((random.nextDouble() - 0.5D) * 0.12D, (random.nextDouble() - 0.5D) * 0.08D,
                        (random.nextDouble() - 0.5D) * 0.12D).normalize();
            }
            projectile.shoot(aim.x, aim.y, aim.z, projectile.speedForVariant(), 1.5F);
            level.addFreshEntity(projectile);
        }
        SoundEvent sound = switch (variant) {
            case EarthProjectileEntity.VARIANT_BOULDER, EarthProjectileEntity.VARIANT_CANNON -> ModSounds.EARTH_SHOT_BOULDER.get();
            case EarthProjectileEntity.VARIANT_HEAVY -> ModSounds.EARTH_SHOT_HEAVY.get();
            default -> switch (random.nextInt(3)) {
                case 0 -> ModSounds.EARTH_SHOT_1.get();
                case 1 -> ModSounds.EARTH_SHOT_2.get();
                default -> ModSounds.EARTH_SHOT_3.get();
            };
        };
        playSound(level, sound, 1.8F, 0.65F + random.nextFloat() * 0.15F);
        sendDust(level, Math.min(24, TitanConfig.MAX_PARTICLES_PER_TICK.get()), 1.4D);
        TitanHooks.onShoot(this, variant);
    }

    private void damageEntities(ServerLevel level, Vec3 center, double radius, double yRadius,
                                 double damage, double horizontalKnockback, double verticalKnockback) {
        AABB box = new AABB(center.x - radius, center.y - yRadius, center.z - radius,
                center.x + radius, center.y + yRadius, center.z + radius);
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, box,
                victim -> victim != this && victim.isAlive() && !(victim instanceof EarthTitanEntity));
        for (LivingEntity victim : victims) {
            if (victim.hurt(damageSources().mobAttack(this), (float) damage)) {
                Vec3 away = victim.position().subtract(position());
                if (away.lengthSqr() < 0.0001D) away = new Vec3(0.0D, 0.0D, 1.0D);
                away = away.normalize();
                victim.push(away.x * horizontalKnockback, verticalKnockback, away.z * horizontalKnockback);
                victim.hurtMarked = true;
            }
        }
    }

    private LivingEntity acquireTarget(ServerLevel level) {
        LivingEntity current = getTarget();
        if (current == null && savedTargetUuid != null) {
            current = level.getPlayerByUUID(savedTargetUuid);
            if (current != null) {
                setTarget(current);
                savedTargetUuid = null;
            }
        }
        double maxDistance = TitanConfig.followRangeForStage(getStage());
        if (current != null && current.isAlive() && current.level() == level
                && distanceToSqr(current) <= maxDistance * maxDistance * 1.25D) {
            return current;
        }
        List<ServerPlayer> players = level.getPlayers(player -> player.isAlive()
                && !player.isSpectator()
                && distanceToSqr(player) <= maxDistance * maxDistance);
        if (players.isEmpty()) {
            setTarget(null);
            return null;
        }
        Player owner = ownerUuid == null ? null : level.getPlayerByUUID(ownerUuid);
        if (owner != null && players.contains(owner)) {
            setTarget(owner);
            return owner;
        }
        Player nearest = players.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        setTarget(nearest);
        return nearest;
    }

    private boolean absorbNearbyBlocks(ServerLevel level) {
        if (absorptionCooldown > 0 || !net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level, this)) {
            return false;
        }
        int limit = TitanConfig.absorptionLimitForStage(getStage());
        int attempts = Math.min(TitanConfig.MAX_ABSORPTION_ATTEMPTS_PER_TICK.get(), 32 + limit * 8);
        int radius = Mth.ceil((float) TitanConfig.absorptionRadiusForStage(getStage()));
        int absorbed = 0;
        BlockPos origin = blockPosition();
        for (int i = 0; i < attempts && absorbed < limit; i++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            int dy = random.nextInt(Math.max(3, radius + 1)) - Math.max(1, radius / 3);
            BlockPos pos = origin.offset(dx, dy, dz);
            if (pos.distSqr(origin) > (double) radius * radius || !level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!isAbsorbable(level, pos, state)) {
                continue;
            }
            if (!TitanWorld.takeBlock(level)) {
                break;
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(this, state));
            level.levelEvent(2001, pos, Block.getId(state));
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    Math.min(6, TitanConfig.MAX_PARTICLES_PER_TICK.get()), 0.3D, 0.3D, 0.3D, 0.04D);
            TitanNetwork.absorb(level, Vec3.atCenterOf(pos), this, Block.getId(state));
            TitanHooks.onBlockAbsorbed(this, pos, state);
            if (random.nextInt(4) == 0) {
                playSound(level, getStage() >= 5 ? ModSounds.EARTH_ABSORB_HEAVY.get() : ModSounds.EARTH_ABSORB.get(),
                        getStage() >= 5 ? 0.8F : 0.45F, 0.8F + random.nextFloat() * 0.25F);
            }
            setAbsorbedBlocks(getAbsorbedBlocks() + 1L);
            absorbed++;
        }
        absorptionCooldown = getStage() >= 5 ? 0 : 1;
        return absorbed > 0;
    }

    private boolean isAbsorbable(ServerLevel level, BlockPos pos, BlockState state) {
        if (!TitanWorld.natural(level, pos, state) || state.is(ModBlocks.EARTH_TITAN_CORE.get())) {
            return false;
        }
        return state.getDestroySpeed(level, pos) >= 0.0F;
    }

    private void maybeStartTransformation(ServerLevel level) {
        int desired = TitanConfig.stageForAbsorbed(getAbsorbedBlocks());
        if (desired > getStage() && desired <= 6) {
            transitionTargetStage = desired;
            setState(STATE_TRANSFORMING);
            this.stateTicks = 0;
            this.entityData.set(DATA_STATE_TICKS, 0);
            this.entityData.set(DATA_ACTION, ACTION_NONE);
            this.actionTicks = 0;
            this.setNoGravity(true);
            this.navigation.stop();
        }
    }

    private void updateGrowthProgress() {
        long current = getAbsorbedBlocks();
        int stage = getStage();
        if (stage >= 6) {
            this.entityData.set(DATA_GROWTH_PROGRESS, 1.0F);
            return;
        }
        long from = TitanConfig.thresholdForStage(stage);
        long to = TitanConfig.thresholdForStage(stage + 1);
        float progress = to <= from ? 1.0F : (float) Mth.clamp((current - from) / (double) (to - from), 0.0D, 1.0D);
        this.entityData.set(DATA_GROWTH_PROGRESS, progress);
    }

    private boolean detectBedrock(ServerLevel level) {
        int radius = TitanConfig.BEDROCK_SCAN_RADIUS.get();
        BlockPos origin = blockPosition();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -radius / 2; y <= radius; y++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (level.hasChunkAt(pos) && level.getBlockState(pos).is(Blocks.BEDROCK)) {
                        TitanHooks.onBedrockDetected(this, pos);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void beginBedrockDeath(ServerLevel level) {
        if (getStateId() == STATE_DYING_BY_BEDROCK || getStateId() == STATE_DEAD) {
            return;
        }
        setTarget(null);
        this.navigation.stop();
        this.entityData.set(DATA_ACTION, ACTION_NONE);
        this.actionTicks = 0;
        this.deathTimer = 0;
        this.debrisSpawned = false;
        this.setNoGravity(true);
        setState(STATE_DYING_BY_BEDROCK);
        playSound(level, ModSounds.TITAN_BREAK.get(), 2.8F, 0.55F);
        playSound(level, ModSounds.TITAN_DEATH.get(), 2.5F, 0.65F);
        sendMessage(level, "message.earthtitan.bedrock");
        TitanNetwork.effect(level, position(), 4.0F, 3);
        TitanHooks.onTitanDeath(this);
        triggerShake();
    }

    private void tickDeathSequence(ServerLevel level) {
        deathTimer++;
        this.entityData.set(DATA_STATE_TICKS, deathTimer);
        this.setDeltaMovement(new Vec3(0.0D, deathTimer < 100 ? 0.018D : -0.035D, 0.0D));
        if (deathTimer % 4 == 0 && deathTimer < 125) {
            int count = Math.min(16, TitanConfig.MAX_PARTICLES_PER_TICK.get());
            level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + getBbHeight() * 0.45D, getZ(), count,
                    Math.max(1.0D, getBbWidth() * 0.25D), Math.max(1.0D, getBbHeight() * 0.15D),
                    Math.max(1.0D, getBbWidth() * 0.25D), 0.08D);
            if (deathTimer % 12 == 0) {
                level.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() * 0.5D, getZ(), 8,
                        getBbWidth() * 0.22D, getBbHeight() * 0.18D, getBbWidth() * 0.22D, 0.04D);
            }
        }
        if (deathTimer == 35 && !debrisSpawned) {
            spawnDebris(level);
            debrisSpawned = true;
        }
        if (deathTimer == 92) {
            level.explode(this, getX(), getY() + Math.min(4.0D, getBbHeight() * 0.3D), getZ(),
                    TitanConfig.EXPLOSION_POWER.get().floatValue() + 3.0F, Level.ExplosionInteraction.NONE);
            if (TitanConfig.ENABLE_TERRAIN_DAMAGE.get()) {
                destroyNaturalBlocks(level, blockPosition(), 5, TitanConfig.MAX_BLOCKS_DESTROYED_PER_ATTACK.get());
            }
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + getBbHeight() * 0.4D, getZ(), 1, 0, 0, 0, 0);
            TitanNetwork.effect(level, position(), 6.0F, 2);
            triggerShake();
        }
        if (deathTimer == 120) {
            setState(STATE_DEAD);
            sendMessage(level, "message.earthtitan.destroyed");
        }
        if (deathTimer >= 150) {
            discard();
        }
    }

    private void spawnDebris(ServerLevel level) {
        int count = Math.min(TitanConfig.MAX_DEBRIS_ENTITIES.get(), 32);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double radius = 1.0D + random.nextDouble() * Math.max(2.0D, getBbWidth() * 0.32D);
            BlockState state = random.nextBoolean() ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState();
            double x = getX() + Math.cos(angle) * radius;
            double y = getY() + random.nextDouble() * Math.max(2.0D, getBbHeight() * 0.7D);
            double z = getZ() + Math.sin(angle) * radius;
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z,
                    3, 0.18D, 0.18D, 0.18D, 0.22D);
            level.sendParticles(ParticleTypes.POOF, x, y, z, 1, 0.08D, 0.08D, 0.08D, 0.1D);
        }
        playSound(level, ModSounds.TITAN_BREAK.get(), 2.2F, 0.72F);
    }

    private void sendShockwave(ServerLevel level, float strength, double radius) {
        level.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.2D, getZ(),
                Math.min(32, TitanConfig.MAX_PARTICLES_PER_TICK.get()), radius, 0.3D, radius, 0.05D);
        TitanNetwork.effect(level, position(), strength, 0);
        damageEntities(level, position(), radius * 2.0D, Math.max(1.5D, getBbHeight() * 0.22D),
                TitanConfig.damageForStage(getStage()) * 0.25D, strength * 0.35D, strength * 0.2D);
    }

    private void destroyNaturalBlocks(ServerLevel level, BlockPos center, int radius, int max) {
        TitanWorld.breakNatural(level, this, center, radius, max);
    }

    private void sendDust(ServerLevel level, int count, double spread) {
        count = Math.min(count, TitanConfig.MAX_PARTICLES_PER_TICK.get());
        if (count <= 0) return;
        BlockState state = getStage() >= 3 ? Blocks.STONE.defaultBlockState() : Blocks.DIRT.defaultBlockState();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                getX(), getY() + Math.min(getBbHeight() * 0.45D, 5.0D), getZ(), count,
                spread, Math.min(spread, 2.5D), spread, 0.07D);
        level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3D, getZ(), Math.min(8, count),
                spread * 0.8D, 0.5D, spread * 0.8D, 0.04D);
    }

    private void triggerShake() {
        this.entityData.set(DATA_SHAKE_PULSE, this.entityData.get(DATA_SHAKE_PULSE) + 1);
    }

    private void updateBossBar(ServerLevel level) {
        float health = Mth.clamp(getHealth() / Math.max(1.0F, getMaxHealth()), 0.0F, 1.0F);
        bossBar.setProgress(health);
        bossBar.setName(Component.translatable("boss.earthtitan.name", getStage(), getAbsorbedBlocks()));
        for (ServerPlayer player : level.players()) {
            double distance = player.distanceToSqr(this);
            if (distance <= 160.0D * 160.0D && player.isAlive()) {
                bossBar.addPlayer(player);
            } else {
                bossBar.removePlayer(player);
            }
        }
    }

    private void sendMessage(ServerLevel level, String key, Object... args) {
        Component message = Component.translatable(key, args);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(this) <= 192.0D * 192.0D) {
                player.displayClientMessage(message, false);
            }
        }
    }

    private void playRandomRoar(ServerLevel level, float volume) {
        SoundEvent sound = switch (random.nextInt(3)) {
            case 0 -> ModSounds.ROAR_1.get();
            case 1 -> ModSounds.ROAR_2.get();
            default -> ModSounds.ROAR_3.get();
        };
        playSound(level, sound, volume, 0.68F + random.nextFloat() * 0.2F);
    }

    private void playSound(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, getX(), getY(), getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    private SoundEvent growthSoundForStage(int stage) {
        return switch (stage) {
            case 2 -> ModSounds.GROWTH_1.get();
            case 3 -> ModSounds.GROWTH_2.get();
            case 4 -> ModSounds.GROWTH_3.get();
            case 5 -> ModSounds.GROWTH_4.get();
            default -> ModSounds.GROWTH_5.get();
        };
    }

    private Vec3 horizontalLook() {
        Vec3 look = getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        return horizontal.lengthSqr() < 0.0001D ? new Vec3(0.0D, 0.0D, 1.0D) : horizontal.normalize();
    }

    private double horizontalDistanceTo(Entity entity) {
        double dx = entity.getX() - getX();
        double dz = entity.getZ() - getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private double attackRangeForStage() {
        return switch (getStage()) {
            case 1 -> 3.5D;
            case 2 -> 5.0D;
            case 3 -> 6.0D;
            case 4 -> 8.0D;
            case 5 -> 10.0D;
            default -> 12.0D;
        };
    }

    public int getStage() {
        return Mth.clamp(this.entityData.get(DATA_STAGE), 1, 6);
    }

    public void setStage(int stage, boolean heal) {
        stage = Mth.clamp(stage, 1, 6);
        int previous = getStage();
        this.entityData.set(DATA_STAGE, stage);
        AttributeInstanceHandle.update(this, stage);
        this.refreshDimensions();
        updateGrowthProgress();
        if (heal || previous != stage) {
            this.setHealth((float) TitanConfig.healthForStage(stage));
        }
    }

    private static final class AttributeInstanceHandle {
        private static void update(EarthTitanEntity titan, int stage) {
            if (titan.getAttribute(Attributes.MAX_HEALTH) != null) {
                titan.getAttribute(Attributes.MAX_HEALTH).setBaseValue(TitanConfig.healthForStage(stage));
            }
            if (titan.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                titan.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(TitanConfig.damageForStage(stage));
            }
            if (titan.getAttribute(Attributes.FOLLOW_RANGE) != null) {
                titan.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(TitanConfig.followRangeForStage(stage));
            }
            if (titan.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
                titan.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.14D + stage * 0.012D);
            }
        }
    }

    public long getAbsorbedBlocks() {
        return this.entityData.get(DATA_ABSORBED);
    }

    private void setAbsorbedBlocks(long value) {
        this.entityData.set(DATA_ABSORBED, Math.max(0L, value));
        updateGrowthProgress();
    }

    public float getGrowthProgress() {
        return this.entityData.get(DATA_GROWTH_PROGRESS);
    }

    public int getStateId() {
        return this.entityData.get(DATA_STATE);
    }

    private void setState(int state) {
        if (this.entityData.get(DATA_STATE) != state) {
            this.entityData.set(DATA_STATE, state);
            this.stateTicks = 0;
            this.entityData.set(DATA_STATE_TICKS, 0);
        }
    }

    public int getStateTicks() {
        return this.entityData.get(DATA_STATE_TICKS);
    }

    public int getAction() {
        return this.entityData.get(DATA_ACTION);
    }

    public int getActionTicks() {
        return this.entityData.get(DATA_ACTION_TICKS);
    }

    public int getShakePulse() {
        return this.entityData.get(DATA_SHAKE_PULSE);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float scale = (float) TitanConfig.scaleForStage(getStage());
        return EntityDimensions.scalable(1.4F * scale, 3.0F * scale);
    }

    @Override
    public float getEyeHeight(Pose pose) {
        return getBbHeight() * 0.82F;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (getStateId() == STATE_DYING_BY_BEDROCK || getStateId() == STATE_DYING || getStateId() == STATE_DEAD) {
            return false;
        }
        return super.hurt(source, Math.max(0.1F, amount * 0.25F));
    }

    @Override
    public boolean canBeCollidedWith() {
        return !isRemoved();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Stage", getStage());
        tag.putLong("AbsorbedBlocks", getAbsorbedBlocks());
        tag.putFloat("GrowthProgress", getGrowthProgress());
        tag.putInt("State", getStateId());
        tag.putInt("StateTicks", stateTicks);
        tag.putInt("Action", getAction());
        tag.putInt("ActionTicks", actionTicks);
        tag.putInt("TransitionTargetStage", transitionTargetStage);
        tag.putInt("DeathTimer", deathTimer);
        tag.putInt("ShootCooldown", shootCooldown);
        tag.putInt("GrabCooldown", grabCooldown);
        tag.putInt("AttackCooldown", attackCooldown);
        tag.putInt("SlamCooldown", slamCooldown);
        tag.putInt("ExplosionCooldown", explosionCooldown);
        tag.putInt("AbsorptionCooldown", absorptionCooldown);
        tag.putBoolean("DebrisSpawned", debrisSpawned);
        if (ownerUuid != null) tag.putUUID("Owner", ownerUuid);
        if (getTarget() != null) tag.putUUID("Target", getTarget().getUUID());
        else if (savedTargetUuid != null) tag.putUUID("Target", savedTargetUuid);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setStage(tag.getInt("Stage"), false);
        setAbsorbedBlocks(tag.getLong("AbsorbedBlocks"));
        this.entityData.set(DATA_GROWTH_PROGRESS, tag.getFloat("GrowthProgress"));
        this.entityData.set(DATA_STATE, tag.getInt("State"));
        this.stateTicks = tag.getInt("StateTicks");
        this.entityData.set(DATA_STATE_TICKS, stateTicks);
        this.entityData.set(DATA_ACTION, tag.getInt("Action"));
        this.actionTicks = tag.getInt("ActionTicks");
        this.entityData.set(DATA_ACTION_TICKS, actionTicks);
        transitionTargetStage = tag.getInt("TransitionTargetStage");
        deathTimer = tag.getInt("DeathTimer");
        shootCooldown = tag.getInt("ShootCooldown");
        grabCooldown = tag.getInt("GrabCooldown");
        attackCooldown = tag.getInt("AttackCooldown");
        slamCooldown = tag.getInt("SlamCooldown");
        explosionCooldown = tag.getInt("ExplosionCooldown");
        absorptionCooldown = tag.getInt("AbsorptionCooldown");
        debrisSpawned = tag.getBoolean("DebrisSpawned");
        if (tag.hasUUID("Owner")) ownerUuid = tag.getUUID("Owner");
        if (tag.hasUUID("Target")) savedTargetUuid = tag.getUUID("Target");
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        bossBar.removeAllPlayers();
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            TitanLedger.get(serverLevel).release(getUUID());
        }
        super.remove(reason);
    }
}
