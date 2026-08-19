package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.config.ModConfig;
import net.minecraft.world.item.ItemStack;

public class TreasureMagicLetterItem extends MagicLetterItem {
    public TreasureMagicLetterItem(Properties properties) {
        super(properties);
    }

    public static void addOpen(ItemStack stack) {
        stack.set(ModDataComponents.TREASURE_OPENS, getOpens(stack) + 1);
    }

    public static int getOpens(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.TREASURE_OPENS, 0);
    }

    @Override
    public int getLevel(ItemStack stack) {
        double opensPerLevel = ModConfig.getInstance().treasureLetter.opensPerLevel;
        return (int) (getOpens(stack) / opensPerLevel);
    }

    @Override
    public double getMultiplier(ItemStack stack) {
        double growthPerLevel = ModConfig.getInstance().treasureLetter.growthPerLevel;
        return getLevel(stack) * growthPerLevel;
    }
}
