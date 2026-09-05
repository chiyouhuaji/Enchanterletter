package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.config.ModConfig;
import net.minecraft.world.item.ItemStack;

public class FishingMagicLetterItem extends MagicLetterItem {
    public FishingMagicLetterItem(Properties properties) {
        super(properties);
    }

    public static void addFish(ItemStack stack) {
        ModDataComponents.setInt(stack, ModDataComponents.FISH_COUNT, getFish(stack) + 1);
    }

    public static int getFish(ItemStack stack) {
        return ModDataComponents.getInt(stack, ModDataComponents.FISH_COUNT, 0);
    }

    // ===== 每级参数：优先读取物品 NBT，缺省回退配置文件默认 =====
    public static double getFishPerLevel(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "fish_per_level",
                ModConfig.getInstance().fishingLetter.fishPerLevel);
    }

    public static double getGrowthPerLevel(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "growth_per_level",
                ModConfig.getInstance().fishingLetter.growthPerLevel);
    }

    public static double getArmorGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "armor_growth_per_level",
                ModConfig.getInstance().fishingLetter.armorGrowthPerLevel);
    }

    public static double getToughnessGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "toughness_growth_per_level",
                ModConfig.getInstance().fishingLetter.toughnessGrowthPerLevel);
    }

    public static double getResistanceGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "resistance_growth_per_level",
                ModConfig.getInstance().fishingLetter.resistanceGrowthPerLevel);
    }

    @Override
    public int getLevel(ItemStack stack) {
        return (int) (getFish(stack) / getFishPerLevel(stack));
    }

    @Override
    public double getMultiplier(ItemStack stack) {
        return getLevel(stack) * getGrowthPerLevel(stack);
    }
}
