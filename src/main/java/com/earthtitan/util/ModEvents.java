package com.earthtitan.util;

import com.earthtitan.EarthTitanMod;
import com.earthtitan.entity.EarthTitanEntity;
import com.earthtitan.registry.ModBlocks;
import com.earthtitan.registry.ModEntities;
import com.earthtitan.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EarthTitanMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEvents {
    private ModEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.EARTH_TITAN.get(), EarthTitanEntity.createAttributes().build());
    }

    @Mod.EventBusSubscriber(modid = EarthTitanMod.MOD_ID)
    public static final class ForgeBus {
        private ForgeBus() {
        }

        @SubscribeEvent
        public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
            Level level = event.getLevel();
            BlockPos pos = event.getPos();
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.EARTH_TITAN_CORE.get())) {
                return;
            }

            event.setCanceled(true);
            if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
                return;
            }

            Player player = event.getEntity();
            ItemStack held = player.getMainHandItem();
            if (!held.is(ModItems.EARTH_PICKAXE.get())) {
                player.displayClientMessage(Component.translatable("message.earthtitan.wrong_tool").withStyle(ChatFormatting.RED), true);
                return;
            }

            EarthTitanEntity titan = EarthTitanEntity.createActivated(serverLevel, pos, player);
            if (!TitanLedger.get(serverLevel).reserve(titan.getUUID(), com.earthtitan.config.TitanConfig.MAX_TITAN_COUNT.get())) {
                player.displayClientMessage(Component.translatable("message.earthtitan.limit").withStyle(ChatFormatting.RED), true);
                return;
            }

            serverLevel.removeBlock(pos, false);
            serverLevel.addFreshEntity(titan);
            held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
            TitanHooks.onCoreActivated(player, pos);
        }
    }
}
