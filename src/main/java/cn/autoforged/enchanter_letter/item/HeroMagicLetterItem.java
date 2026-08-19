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

    @Override
    public int getLevel(ItemStack stack) {
        int victoriesPerLevel = ModConfig.getInstance().heroLetter.victoriesPerLevel;
        return getVictories(stack) / Math.max(1, victoriesPerLevel);
    }

    @Override
    public double getMultiplier(ItemStack stack) {
        ModConfig.HeroLetterConfig cfg = ModConfig.getInstance().heroLetter;
        int level = getLevel(stack);
        double mult = 0;
        for (int i = 1; i <= level; i++) {
            mult += (i < cfg.highLevelStart) ? cfg.growthLowLevels : cfg.growthHighLevels;
        }
        return mult;
    }
}
