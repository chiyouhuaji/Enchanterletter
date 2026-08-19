package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.config.ModConfig;
import net.minecraft.world.item.ItemStack;

public class TenacityMagicLetterItem extends MagicLetterItem {
    public TenacityMagicLetterItem(Properties properties) {
        super(properties);
    }

    public static void addDamage(ItemStack stack, double amount) {
        stack.set(ModDataComponents.DAMAGE_TAKEN, getDamageTaken(stack) + amount);
    }

    public static double getDamageTaken(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.DAMAGE_TAKEN, 0.0);
    }

    @Override
    public int getLevel(ItemStack stack) {
        double damagePerLevel = ModConfig.getInstance().tenacityLetter.damagePerLevel;
        return (int) (getDamageTaken(stack) / damagePerLevel);
    }

    @Override
    public double getMultiplier(ItemStack stack) {
        double growthPerLevel = ModConfig.getInstance().tenacityLetter.growthPerLevel;
        return getLevel(stack) * growthPerLevel;
    }
}
