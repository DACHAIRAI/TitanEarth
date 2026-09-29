package com.earthtitan.client;

import com.earthtitan.EarthTitanMod;
import com.earthtitan.network.TitanNetwork;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

/** Client-only bounded absorption, particles, pulses, and camera shake. */
@Mod.EventBusSubscriber(modid = EarthTitanMod.MOD_ID, value = Dist.CLIENT)
public final class TitanClientEffects {
    private static final int MAX_FLYING = 128;
    private static final int MAX_NEW_PER_WINDOW = 8;
    private static final int WINDOW_TICKS = 15;
    private static final int MAX_PULSES = 40;
    private static final int MAX_PARTICLES_PER_TICK = 160;
    private static final Queue<TitanNetwork.AbsorbPacket> pendingAbsorptions = new ArrayDeque<>();
    private static final List<FlyingBlock> flying = new ArrayList<>();
    private static final List<Pulse> pulses = new ArrayList<>();
    private static ClientLevel stateLevel;
    private static int age;
    private static int windowCreated;
    private static float shake;
    private static int particlesThisTick;

    private TitanClientEffects() {
    }

    public static void receiveAbsorb(TitanNetwork.AbsorbPacket packet) {
        if (!valid(packet.dimension(), packet.from()) || !finite(packet.target())) {
            return;
        }
        if (pendingAbsorptions.size() < MAX_FLYING * 2) {
            pendingAbsorptions.add(packet);
        }
    }

    public static void receiveEffect(TitanNetwork.EffectPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !level.dimension().location().equals(packet.dimension())
                || !Double.isFinite(packet.position().x) || !Double.isFinite(packet.position().y)
                || !Double.isFinite(packet.position().z)) {
            return;
        }
        float strength = Mth.clamp(packet.strength(), 0.1F, 16.0F);
        addPulse(new Pulse(packet.position(), strength, packet.mode()));
        float distance = (float) Math.sqrt(minecraft.gameRenderer.getMainCamera().getPosition()
                .distanceToSqr(packet.position()));
        if (distance < 64.0F) {
            shake = Math.max(shake, Mth.clamp(strength * (1.0F - distance / 64.0F) * 0.11F, 0.0F, 1.25F));
        }
        spawnEffectParticles(level, packet.position(), strength, packet.mode());
    }

    private static boolean valid(ResourceLocation dimension, Vec3 position) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null && minecraft.level.dimension().location().equals(dimension)
                && finite(position);
    }

    private static boolean finite(Vec3 position) {
        return position != null && Double.isFinite(position.x) && Double.isFinite(position.y)
                && Double.isFinite(position.z);
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            particlesThisTick = 0;
            return;
        }
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (stateLevel == null && level != null) {
            stateLevel = level;
        } else if (level != stateLevel) {
            clear();
            stateLevel = level;
        }
        if (level == null) {
            return;
        }
        age++;
        if (age % WINDOW_TICKS == 0) {
            windowCreated = 0;
        }
        int available = Math.min(MAX_NEW_PER_WINDOW - windowCreated, MAX_FLYING - flying.size());
        while (available > 0 && !pendingAbsorptions.isEmpty()) {
            TitanNetwork.AbsorbPacket packet = pendingAbsorptions.remove();
            if (level.dimension().location().equals(packet.dimension())) {
                flying.add(new FlyingBlock(packet, chooseState(packet.blockStateId())));
                windowCreated++;
                available--;
            }
        }
        for (Iterator<FlyingBlock> iterator = flying.iterator(); iterator.hasNext(); ) {
            if (!iterator.next().tick(level)) {
                iterator.remove();
            }
        }
        for (Iterator<Pulse> iterator = pulses.iterator(); iterator.hasNext(); ) {
            Pulse pulse = iterator.next();
            pulse.tick(level);
            if (pulse.life > pulse.maxLife) {
                iterator.remove();
            }
        }
        shake *= 0.86F;
        if (shake < 0.002F) {
            shake = 0.0F;
        }
    }

    @SubscribeEvent
    public static void renderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || flying.isEmpty()) {
            return;
        }
        BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        for (FlyingBlock block : flying) {
            if (!block.visible(level, camera)) {
                continue;
            }
            Vec3 position = block.renderPosition(level, event.getPartialTick());
            pose.pushPose();
            pose.translate(position.x - camera.x, position.y - camera.y, position.z - camera.z);
            pose.mulPose(Axis.YP.rotation(block.rotation + event.getPartialTick() * 0.12F));
            pose.mulPose(Axis.XP.rotation(block.rotation * 0.63F));
            pose.scale(block.size, block.size, block.size);
            BlockPos lightPos = BlockPos.containing(position);
            int light = LightTexture.pack(level.getBrightness(LightLayer.BLOCK, lightPos),
                    level.getBrightness(LightLayer.SKY, lightPos));
            dispatcher.renderSingleBlock(block.state, pose, buffers, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        pose.popPose();
    }

    @SubscribeEvent
    public static void cameraShake(ViewportEvent.ComputeCameraAngles event) {
        if (shake <= 0.0F) {
            return;
        }
        float time = age + (float) event.getPartialTick();
        event.setYaw(event.getYaw() + Mth.sin(time * 1.91F) * shake * 0.90F);
        event.setPitch(event.getPitch() + Mth.cos(time * 2.23F) * shake * 0.65F);
        event.setRoll(event.getRoll() + Mth.sin(time * 1.37F) * shake * 0.42F);
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
        stateLevel = null;
    }

    private static void clear() {
        pendingAbsorptions.clear();
        flying.clear();
        pulses.clear();
        windowCreated = 0;
        shake = 0.0F;
        age = 0;
        particlesThisTick = 0;
    }

    private static BlockState chooseState(int stateId) {
        BlockState state = Block.stateById(stateId);
        return state.isAir() ? Blocks.DIRT.defaultBlockState() : state;
    }

    private static void addPulse(Pulse pulse) {
        if (pulses.size() >= MAX_PULSES) {
            pulses.remove(0);
        }
        pulses.add(pulse);
    }

    private static void spawnEffectParticles(ClientLevel level, Vec3 center, float strength, int mode) {
        int count = Math.min(24, 4 + Mth.ceil(strength * 1.4F));
        ParticleOptions dust = switch (mode) {
            case 1 -> new DustParticleOptions(new Vector3f(0.35F, 0.18F, 0.08F), 1.2F);
            case 2, 3 -> new DustParticleOptions(new Vector3f(0.45F, 0.08F, 0.02F), 1.4F);
            default -> new DustParticleOptions(new Vector3f(0.32F, 0.27F, 0.18F), 1.0F);
        };
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D * i) / count;
            double radius = strength * (0.2D + (i % 5) * 0.09D);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = center.y + 0.15D + (i % 4) * 0.18D;
            spawnParticle(level, dust, x, y, z, Math.cos(angle) * 0.025D, 0.04D, Math.sin(angle) * 0.025D);
        }
        if (mode == 0 || mode == 2 || mode == 3) {
            spawnParticle(level, ParticleTypes.EXPLOSION, center.x, center.y + 0.4D, center.z,
                    strength * 0.02D, 0.0D, 0.0D);
        }
    }

    private static void spawnParticle(ClientLevel level, ParticleOptions options, double x, double y, double z,
                                      double dx, double dy, double dz) {
        if (particlesThisTick++ < MAX_PARTICLES_PER_TICK) {
            level.addParticle(options, x, y, z, dx, dy, dz);
        }
    }

    private static final class FlyingBlock {
        private final Vec3 start;
        private final Vec3 fallbackTarget;
        private final int titanId;
        private final UUID titanUuid;
        private final BlockState state;
        private final float size;
        private final float phase;
        private int life;
        private float rotation;

        private FlyingBlock(TitanNetwork.AbsorbPacket packet, BlockState state) {
            this.start = packet.from();
            this.fallbackTarget = packet.target();
            this.titanId = packet.titanId();
            this.titanUuid = packet.titanUuid();
            this.state = state;
            this.size = 0.12F + (Math.abs(packet.blockStateId()) % 5) * 0.018F;
            this.phase = (float) Math.abs(packet.from().x * 17.0D + packet.from().z * 31.0D) % 6.2831853F;
        }

        private boolean tick(ClientLevel level) {
            life++;
            rotation += 0.32F + size;
            Vec3 target = target(level);
            Vec3 position = position(target, 0.0F);
            if (life < 30 && life % 5 == 0) {
                spawnParticle(level, new BlockParticleOption(ParticleTypes.BLOCK, state), position.x, position.y,
                        position.z, 0.0D, 0.025D, 0.0D);
            }
            boolean arrived = life >= 34 || position.distanceToSqr(target) < 0.55D;
            if (arrived) {
                for (int i = 0; i < 3; i++) {
                    spawnParticle(level, new BlockParticleOption(ParticleTypes.BLOCK, state), target.x, target.y, target.z,
                            (i - 1) * 0.04D, 0.05D, (1 - i) * 0.04D);
                }
            }
            return !arrived && life < 48;
        }

        private Vec3 target(ClientLevel level) {
            Entity entity = level.getEntity(titanId);
            if (entity != null && entity.getUUID().equals(titanUuid)) {
                return entity.position().add(0.0D, entity.getBbHeight() * 0.52D, 0.0D);
            }
            return fallbackTarget;
        }

        private Vec3 position(Vec3 target, float partial) {
            float t = Mth.clamp((life + partial) / 36.0F, 0.0F, 1.0F);
            float eased = 1.0F - (1.0F - t) * (1.0F - t);
            Vec3 base = start.lerp(target, eased);
            float radius = (1.0F - eased) * (0.28F + (Math.abs(phase) % 0.7F));
            float angle = phase + (life + partial) * 0.42F;
            return base.add(Math.cos(angle) * radius, Math.sin(angle * 1.7F) * radius * 0.5D,
                    Math.sin(angle) * radius);
        }

        private Vec3 renderPosition(ClientLevel level, float partial) {
            return position(target(level), partial);
        }

        private boolean visible(ClientLevel level, Vec3 camera) {
            return renderPosition(level, 0.0F).distanceToSqr(camera) < 192.0D * 192.0D;
        }
    }

    private static final class Pulse {
        private final Vec3 center;
        private final float strength;
        private final int mode;
        private final int maxLife;
        private int life;

        private Pulse(Vec3 center, float strength, int mode) {
            this.center = center;
            this.strength = strength;
            this.mode = mode;
            this.maxLife = mode == 1 ? 45 : 28;
        }

        private void tick(ClientLevel level) {
            life++;
            if (life % (mode == 1 ? 4 : 3) != 0) {
                return;
            }
            float t = life / (float) maxLife;
            float radius = strength * (0.4F + t * 3.0F);
            int count = Math.min(16, 4 + Mth.floor(strength));
            for (int i = 0; i < count; i++) {
                double angle = (Math.PI * 2.0D * i) / count + t;
                Vec3 point = center.add(Math.cos(angle) * radius, 0.08D + t * 0.2D,
                        Math.sin(angle) * radius);
                spawnParticle(level, ParticleTypes.CLOUD, point.x, point.y, point.z,
                        -Math.cos(angle) * 0.02D, 0.015D, -Math.sin(angle) * 0.02D);
            }
        }
    }
}
