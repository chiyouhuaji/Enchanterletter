package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.config.ModConfig;
import net.minecraft.world.item.ItemStack;

public class ExperienceMagicLetterItem extends MagicLetterItem {
    public ExperienceMagicLetterItem(Properties properties) {
        super(properties);
    }

    public static void addExperience(ItemStack stack, double amount) {
        stack.set(ModDataComponents.EXPERIENCE_PROGRESS, getExperience(stack) + amount);
    }

    public static double getExperience(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.EXPERIENCE_PROGRESS, 0.0);
    }

    public static double getExpPerLevel(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "exp_per_level", ModConfig.getInstance().experienceLetter.expPerLevel);
    }

    public static double getGrowthPerLevel(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "growth_per_level", ModConfig.getInstance().experienceLetter.growthPerLevel);
    }

    public static double getArmorGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "armor_growth_per_level", ModConfig.getInstance().experienceLetter.armorGrowthPerLevel);
    }

    public static double getToughnessGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "toughness_growth_per_level", ModConfig.getInstance().experienceLetter.toughnessGrowthPerLevel);
    }

    public static double getResistanceGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "resistance_growth_per_level", ModConfig.getInstance().experienceLetter.resistanceGrowthPerLevel);
    }

    @Override
    public int getLevel(ItemStack stack) {
        return (int) (getExperience(stack) / getExpPerLevel(stack));
    }

    @Override
    public double getMultiplier(ItemStack stack) {
        return getLevel(stack) * getGrowthPerLevel(stack);
    }
}
