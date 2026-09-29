package com.earthtitan.entity;

import com.earthtitan.registry.ModEntities;
import com.earthtitan.registry.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public final class EarthProjectileEntity extends Projectile implements ItemSupplier {
    public static final int VARIANT_EARTH = 0;
    public static final int VARIANT_STONE = 1;
    public static final int VARIANT_HEAVY = 2;
    public static final int VARIANT_SPEAR = 3;
    public static final int VARIANT_BOULDER = 4;
    public static final int VARIANT_EXPLOSIVE = 5;
    public static final int VARIANT_CANNON = 6;
    public static final int VARIANT_CATAPULT = 7;

    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_VARIANT =
            net.minecraft.network.syncher.SynchedEntityData.defineId(EarthProjectileEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.INT);

    private int life;

    public EarthProjectileEntity(EntityType<? extends EarthProjectileEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static EarthProjectileEntity create(Level level, LivingEntity owner, int variant) {
        EarthProjectileEntity projectile = new EarthProjectileEntity(ModEntities.EARTH_PROJECTILE.get(), level);
        projectile.setOwner(owner);
        projectile.setVariant(variant);
        return projectile;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_VARIANT, VARIANT_EARTH);
    }

    @Override
    public void tick() {
        super.tick();
        life++;
        Vec3 movement = getDeltaMovement();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hit.getType() != HitResult.Type.MISS) {
            handleHit(hit);
            return;
        }
        setPos(getX() + movement.x, getY() + movement.y, getZ() + movement.z);
        setDeltaMovement(movement.scale(0.985D));
        setYRot((float) (Math.atan2(movement.x, movement.z) * 180.0D / Math.PI));
        setXRot((float) (Math.atan2(movement.y, movement.horizontalDistance()) * 180.0D / Math.PI));
        if (!level().isClientSide && life % 2 == 0) {
            level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockForVariant().defaultBlockState()),
                    getX(), getY(), getZ(), 0.0D, 0.02D, 0.0D);
        }
        if (life > 160 || position().y < level().getMinBuildHeight() - 8 || position().y > level().getMaxBuildHeight() + 32) {
            discard();
        }
    }

    private void handleHit(HitResult hit) {
        if (hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            if (entity != getOwner() && entity instanceof LivingEntity living) {
                float damage = damageForVariant();
                Entity owner = getOwner();
                if (owner instanceof LivingEntity livingOwner) {
                    living.hurt(level().damageSources().mobAttack(livingOwner), damage);
                } else {
                    living.hurt(level().damageSources().generic(), damage);
                }
                living.push(getDeltaMovement().x * 0.35D, 0.35D + Math.abs(getDeltaMovement().y) * 0.2D,
                        getDeltaMovement().z * 0.35D);
                living.hurtMarked = true;
            }
        }
        if (isExplosive()) {
            level().explode(getOwner(), getX(), getY(), getZ(), explosionPower(), ExplosionInteraction.NONE);
            level().addAlwaysVisibleParticle(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        } else {
            level().levelEvent(2001, blockPosition(), net.minecraft.world.level.block.Block.getId(blockForVariant().defaultBlockState()));
        }
        if (!level().isClientSide) {
            level().playSound(null, blockPosition(), isExplosive() ? ModSounds.EARTH_EXPLOSION.get() : ModSounds.EARTH_ABSORB.get(),
                    net.minecraft.sounds.SoundSource.HOSTILE, 0.9F, 0.7F + random.nextFloat() * 0.2F);
        }
        discard();
    }

    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        Vec3 direction = new Vec3(x, y, z).normalize();
        if (inaccuracy > 0.0F) {
            direction = direction.add((random.nextGaussian() * 0.0075D * inaccuracy),
                    (random.nextGaussian() * 0.0075D * inaccuracy),
                    (random.nextGaussian() * 0.0075D * inaccuracy)).normalize();
        }
        setDeltaMovement(direction.scale(velocity));
        hasImpulse = true;
    }

    public float speedForVariant() {
        return switch (getVariant()) {
            case VARIANT_HEAVY, VARIANT_BOULDER, VARIANT_CANNON -> 0.75F;
            case VARIANT_SPEAR -> 1.65F;
            case VARIANT_CATAPULT -> 0.9F;
            default -> 1.2F;
        };
    }

    private float damageForVariant() {
        return switch (getVariant()) {
            case VARIANT_HEAVY -> 36.0F;
            case VARIANT_SPEAR -> 42.0F;
            case VARIANT_BOULDER -> 65.0F;
            case VARIANT_EXPLOSIVE -> 48.0F;
            case VARIANT_CANNON -> 90.0F;
            case VARIANT_CATAPULT -> 72.0F;
            case VARIANT_STONE -> 24.0F;
            default -> 14.0F;
        };
    }

    private float explosionPower() {
        return switch (getVariant()) {
            case VARIANT_CANNON -> 4.8F;
            case VARIANT_CATAPULT -> 4.2F;
            default -> 2.8F;
        };
    }

    private boolean isExplosive() {
        return getVariant() == VARIANT_EXPLOSIVE || getVariant() == VARIANT_CANNON || getVariant() == VARIANT_CATAPULT;
    }

    private net.minecraft.world.level.block.Block blockForVariant() {
        return switch (getVariant()) {
            case VARIANT_STONE, VARIANT_SPEAR, VARIANT_CANNON -> Blocks.STONE;
            case VARIANT_HEAVY, VARIANT_BOULDER, VARIANT_CATAPULT -> Blocks.COBBLESTONE;
            default -> Blocks.DIRT;
        };
    }

    public int getVariant() {
        return this.entityData.get(DATA_VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(DATA_VARIANT, Math.max(VARIANT_EARTH, Math.min(VARIANT_CATAPULT, variant)));
    }

    public float getVisualScale() {
        return switch (getVariant()) {
            case VARIANT_HEAVY -> 1.35F;
            case VARIANT_BOULDER, VARIANT_CANNON, VARIANT_CATAPULT -> 1.8F;
            case VARIANT_SPEAR -> 1.1F;
            default -> 0.85F;
        };
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(blockForVariant().asItem());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
        tag.putInt("Life", life);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getInt("Variant"));
        life = tag.getInt("Life");
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
