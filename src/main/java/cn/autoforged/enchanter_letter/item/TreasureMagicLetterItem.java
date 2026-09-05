package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.config.ModConfig;
import net.minecraft.world.item.ItemStack;

public class TreasureMagicLetterItem extends MagicLetterItem {
    public TreasureMagicLetterItem(Properties properties) {
        super(properties);
    }

    public static void addOpen(ItemStack stack) {
        ModDataComponents.setInt(stack, ModDataComponents.TREASURE_OPENS, getOpens(stack) + 1);
    }

    public static int getOpens(ItemStack stack) {
        return ModDataComponents.getInt(stack, ModDataComponents.TREASURE_OPENS, 0);
    }

    // ===== 每级参数：优先读取物品 NBT，缺省回退配置文件默认 =====
    public static double getOpensPerLevel(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "opens_per_level",
                ModConfig.getInstance().treasureLetter.opensPerLevel);
    }

    public static double getGrowthPerLevel(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "growth_per_level",
                ModConfig.getInstance().treasureLetter.growthPerLevel);
    }

    public static double getArmorGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "armor_growth_per_level",
                ModConfig.getInstance().treasureLetter.armorGrowthPerLevel);
    }

    public static double getToughnessGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "toughness_growth_per_level",
                ModConfig.getInstance().treasureLetter.toughnessGrowthPerLevel);
    }

    public static double getResistanceGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "resistance_growth_per_level",
                ModConfig.getInstance().treasureLetter.resistanceGrowthPerLevel);
    }

    @Override
    public int getLevel(ItemStack stack) {
        return (int) (getOpens(stack) / getOpensPerLevel(stack));
    }

    @Override
    public double getMultiplier(ItemStack stack) {
        return getLevel(stack) * getGrowthPerLevel(stack);
    }
}
