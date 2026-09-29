package com.earthtitan.network;

import com.earthtitan.EarthTitanMod;
import com.earthtitan.entity.EarthTitanEntity;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;

/** Simple operator command for quickly previewing every titan silhouette. */
@Mod.EventBusSubscriber(modid = EarthTitanMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TitanCommands {
    private TitanCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        var root = Commands.literal("earthtitan").requires(source -> source.hasPermission(2));
        root.then(Commands.literal("stage")
                .then(Commands.argument("stage", IntegerArgumentType.integer(1, 6))
                        .executes(context -> setNearest(context.getSource(),
                                IntegerArgumentType.getInteger(context, "stage"))))
                .then(Commands.argument("titan", EntityArgument.entity())
                        .then(Commands.argument("targetStage", IntegerArgumentType.integer(1, 6))
                                .executes(context -> setExplicit(context.getSource(),
                                        EntityArgument.getEntity(context, "titan"),
                                        IntegerArgumentType.getInteger(context, "targetStage"))))));
        root.then(Commands.literal("stage_of")
                .then(Commands.argument("titan", EntityArgument.entity())
                        .then(Commands.argument("stage", IntegerArgumentType.integer(1, 6))
                                .executes(context -> setExplicit(context.getSource(),
                                        EntityArgument.getEntity(context, "titan"),
                                        IntegerArgumentType.getInteger(context, "stage"))))));
        event.getDispatcher().register(root);
    }

    private static int setNearest(CommandSourceStack source, int stage) {
        Entity sourceEntity = source.getEntity();
        if (sourceEntity == null) {
            source.sendFailure(Component.literal("Run this form as a player, or use /earthtitan stage_of <titan> <stage>."));
            return 0;
        }
        EarthTitanEntity titan = source.getLevel().getEntitiesOfClass(EarthTitanEntity.class,
                        sourceEntity.getBoundingBox().inflate(128.0D), Entity::isAlive).stream()
                .min(Comparator.comparingDouble(candidate -> sourceEntity.distanceToSqr(candidate))).orElse(null);
        if (titan == null) {
            source.sendFailure(Component.literal("No living Earth Titan was found within 128 blocks."));
            return 0;
        }
        return setExplicit(source, titan, stage);
    }

    private static int setExplicit(CommandSourceStack source, Entity entity, int stage) {
        if (!(entity instanceof EarthTitanEntity titan)) {
            source.sendFailure(Component.literal("That entity is not an Earth Titan."));
            return 0;
        }
        titan.setStage(stage, true);
        titan.setNoGravity(false);
        source.sendSuccess(() -> Component.literal("Earth Titan stage set to " + stage + "."), true);
        return 1;
    }
}
