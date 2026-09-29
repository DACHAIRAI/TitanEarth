package com.earthtitan.network;

import com.earthtitan.EarthTitanMod;
import com.earthtitan.client.TitanClientPackets;
import com.earthtitan.entity.EarthTitanEntity;
import com.earthtitan.util.TitanWorld;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;
import java.util.function.Consumer;

/** Visual-only packets. No client-to-server messages or client-authored world changes exist. */
public final class TitanNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(EarthTitanMod.MOD_ID, "visuals"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();
    private static boolean initialized;
    private static Consumer<AbsorbPacket> absorbReceiver;
    private static Consumer<EffectPacket> effectReceiver;

    private TitanNetwork() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        CHANNEL.messageBuilder(AbsorbPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AbsorbPacket::encode)
                .decoder(AbsorbPacket::decode)
                .consumerMainThread((packet, context) -> {
                    if (absorbReceiver != null) {
                        absorbReceiver.accept(packet);
                    }
                }).add();
        CHANNEL.messageBuilder(EffectPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EffectPacket::encode)
                .decoder(EffectPacket::decode)
                .consumerMainThread((packet, context) -> {
                    if (effectReceiver != null) {
                        effectReceiver.accept(packet);
                    }
                }).add();
        // A separate-class, non-capturing method reference is important here. The common
        // packet handlers only retain java.util.function.Consumer, never a Minecraft client type.
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> TitanClientPackets::install);
        initialized = true;
    }

    public static void installClientReceivers(Consumer<AbsorbPacket> absorb, Consumer<EffectPacket> effect) {
        absorbReceiver = absorb;
        effectReceiver = effect;
    }

    /** Call before/after removing a block, passing the original Block.getId(state). */
    public static void absorb(ServerLevel level, Vec3 from, EarthTitanEntity titan, int blockStateId) {
        if (!initialized || titan.isRemoved() || titan.level() != level || !finite(from)) {
            return;
        }
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> titan),
                new AbsorbPacket(level.dimension().location(), from,
                        titan.position().add(0.0D, titan.getBbHeight() * 0.55D, 0.0D),
                        titan.getId(), titan.getUUID(), blockStateId));
    }

    /** Modes: 0 shockwave, 1 transformation, 2 explosion, 3 death, 4 footstep, 5 grab. */
    public static void effect(ServerLevel level, Vec3 pos, float strength, int mode) {
        if (!initialized || !finite(pos) || !Float.isFinite(strength) || !TitanWorld.takeEffect(level)) {
            return;
        }
        EffectPacket packet = new EffectPacket(level.dimension().location(), pos,
                Mth.clamp(strength, 0.1F, 16.0F), Mth.clamp(mode, 0, 5));
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                pos.x, pos.y, pos.z, 96.0D, level.dimension())), packet);
    }

    private static boolean finite(Vec3 point) {
        return Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }

    public record AbsorbPacket(ResourceLocation dimension, Vec3 from, Vec3 target, int titanId, UUID titanUuid,
                               int blockStateId) {
            private static void encode(AbsorbPacket packet, FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(packet.dimension);
            writePosition(buffer, packet.from);
            writePosition(buffer, packet.target);
            buffer.writeVarInt(packet.titanId);
            buffer.writeUUID(packet.titanUuid);
            buffer.writeVarInt(packet.blockStateId);
        }

        private static AbsorbPacket decode(FriendlyByteBuf buffer) {
            return new AbsorbPacket(buffer.readResourceLocation(), readPosition(buffer),
                    readPosition(buffer), buffer.readVarInt(), buffer.readUUID(), buffer.readVarInt());
        }
    }

    public record EffectPacket(ResourceLocation dimension, Vec3 position, float strength, int mode) {
        private static void encode(EffectPacket packet, FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(packet.dimension);
            writePosition(buffer, packet.position);
            buffer.writeFloat(packet.strength);
            buffer.writeByte(packet.mode);
        }

        private static EffectPacket decode(FriendlyByteBuf buffer) {
            return new EffectPacket(buffer.readResourceLocation(), readPosition(buffer),
                    buffer.readFloat(), buffer.readUnsignedByte());
        }
    }

    private static void writePosition(FriendlyByteBuf buffer, Vec3 position) {
        buffer.writeDouble(position.x);
        buffer.writeDouble(position.y);
        buffer.writeDouble(position.z);
    }

    private static Vec3 readPosition(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}
