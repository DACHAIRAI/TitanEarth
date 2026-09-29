package com.earthtitan.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class EarthPickaxeItem extends PickaxeItem {
    private static final Tier EARTH_TIER = new Tier() {
        @Override
        public int getUses() {
            return 2048;
        }

        @Override
        public float getSpeed() {
            return 12.0F;
        }

        @Override
        public float getAttackDamageBonus() {
            return 5.0F;
        }

        @Override
        public int getLevel() {
            return 4;
        }

        @Override
        public int getEnchantmentValue() {
            return 18;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(net.minecraft.world.item.Items.DIAMOND, net.minecraft.world.item.Items.OBSIDIAN);
        }
    };

    public EarthPickaxeItem() {
        super(EARTH_TIER, 3, -2.6F, new Item.Properties().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.earthtitan.earth_pickaxe"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
