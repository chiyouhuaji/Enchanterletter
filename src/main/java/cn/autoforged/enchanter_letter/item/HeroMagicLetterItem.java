package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.config.ModConfig;
import net.minecraft.world.item.ItemStack;

public class HeroMagicLetterItem extends MagicLetterItem {
    public HeroMagicLetterItem(Properties properties) {
        super(properties);
    }

    public static void addVictory(ItemStack stack) {
        stack.set(ModDataComponents.RAID_VICTORIES.get(), getVictories(stack) + 1);
    }

    public static int getVictories(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.RAID_VICTORIES.get(), 0);
    }

    public static int getVictoriesPerLevel(ItemStack stack) {
        return ModDataComponents.getGrowthInt(stack, "victories_per_level",
                ModConfig.getInstance().heroLetter.victoriesPerLevel);
    }

    public static int getHighLevelStart(ItemStack stack) {
        return ModDataComponents.getGrowthInt(stack, "high_level_start",
                ModConfig.getInstance().heroLetter.highLevelStart);
    }

    public static double getGrowthLow(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "growth_low_levels",
                ModConfig.getInstance().heroLetter.growthLowLevels);
    }

    public static double getGrowthHigh(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "growth_high_levels",
                ModConfig.getInstance().heroLetter.growthHighLevels);
    }

    public static double getArmorGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "armor_growth_per_level",
                ModConfig.getInstance().heroLetter.armorGrowthPerLevel);
    }

    public static double getArmor2Growth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "armor2_growth_per_level",
                ModConfig.getInstance().heroLetter.armor2GrowthPerLevel);
    }

    public static double getToughnessGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "toughness_growth_per_level",
                ModConfig.getInstance().heroLetter.toughnessGrowthPerLevel);
    }

    public static double getToughness2Growth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "toughness2_growth_per_level",
                ModConfig.getInstance().heroLetter.toughness2GrowthPerLevel);
    }

    public static double getResistanceGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "resistance_growth_per_level",
                ModConfig.getInstance().heroLetter.resistanceGrowthPerLevel);
    }

    public static double getResistance2Growth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "resistance2_growth_per_level",
                ModConfig.getInstance().heroLetter.resistance2GrowthPerLevel);
    }

    @Override
    public int getLevel(ItemStack stack) {
        int perLevel = Math.max(1, getVictoriesPerLevel(stack));
        return getVictories(stack) / perLevel;
    }

    @Override
    public double getMultiplier(ItemStack stack) {
        int level = getLevel(stack);
        int start = Math.max(1, getHighLevelStart(stack));
        double mult = 0;
        for (int i = 1; i <= level; i++) {
            double rate = (i < start) ? getGrowthLow(stack) : getGrowthHigh(stack);
            mult += rate;
        }
        return mult;
    }
}