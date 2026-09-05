package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.config.ModConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TimeMagicLetterItem extends MagicLetterItem {
    public TimeMagicLetterItem(Properties properties) {
        super(properties);
    }

    // ===== 每级参数：优先读取物品 NBT，缺省回退配置文件默认 =====
    public static double getSecondsPerLevel(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "seconds_per_level",
                ModConfig.getInstance().timeLetter.secondsPerLevel);
    }

    public static double getGrowthPerLevel(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "growth_per_level",
                ModConfig.getInstance().timeLetter.growthPerLevel);
    }

    public static double getArmorGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "armor_growth_per_level",
                ModConfig.getInstance().timeLetter.armorGrowthPerLevel);
    }

    public static double getToughnessGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "toughness_growth_per_level",
                ModConfig.getInstance().timeLetter.toughnessGrowthPerLevel);
    }

    public static double getResistanceGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "resistance_growth_per_level",
                ModConfig.getInstance().timeLetter.resistanceGrowthPerLevel);
    }

    /**
     * 按世界开启时间（服务器 GameTime）计算倍率，与玩家持有时间无关。
     */
    public static double getMultiplier(Level level, ItemStack stack) {
        if (level == null) return 0;
        double seconds = level.getGameTime() / 20.0;
        double levels = Math.floor(seconds / getSecondsPerLevel(stack));
        return levels * getGrowthPerLevel(stack);
    }

    @Override
    public int getLevel(ItemStack stack) {
        return 0;
    }

    @Override
    public double getMultiplier(ItemStack stack) {
        return 0;
    }

    @Override
    public int getLevel(ItemStack stack, Level level) {
        if (level == null) return 0;
        double seconds = level.getGameTime() / 20.0;
        return (int) Math.floor(seconds / getSecondsPerLevel(stack));
    }

    @Override
    public double getMultiplier(ItemStack stack, Level level) {
        return getMultiplier(level, stack);
    }
}
