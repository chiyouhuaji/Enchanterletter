package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.config.ModConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TimeMagicLetterItem extends MagicLetterItem {
    public TimeMagicLetterItem(Properties properties) {
        super(properties);
    }

    /**
     * 按世界开启时间（服务器 GameTime）计算倍率，与玩家持有时间无关。
     */
    public static double getMultiplier(Level level) {
        if (level == null) return 0;
        ModConfig.TimeLetterConfig cfg = ModConfig.getInstance().timeLetter;
        double seconds = level.getGameTime() / 20.0;
        double levels = Math.floor(seconds / cfg.secondsPerLevel);
        return levels * cfg.growthPerLevel;
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
        ModConfig.TimeLetterConfig cfg = ModConfig.getInstance().timeLetter;
        double seconds = level.getGameTime() / 20.0;
        return (int) Math.floor(seconds / cfg.secondsPerLevel);
    }

    @Override
    public double getMultiplier(ItemStack stack, Level level) {
        return getMultiplier(level);
    }
}
