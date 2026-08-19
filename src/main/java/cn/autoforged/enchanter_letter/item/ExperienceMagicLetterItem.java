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

    @Override
    public int getLevel(ItemStack stack) {
        double expPerLevel = ModConfig.getInstance().experienceLetter.expPerLevel;
        return (int) (getExperience(stack) / expPerLevel);
    }

    @Override
    public double getMultiplier(ItemStack stack) {
        double growthPerLevel = ModConfig.getInstance().experienceLetter.growthPerLevel;
        return getLevel(stack) * growthPerLevel;
    }
}
