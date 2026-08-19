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

    @Override
    public int getLevel(ItemStack stack) {
        double fishPerLevel = ModConfig.getInstance().fishingLetter.fishPerLevel;
        return (int) (getFish(stack) / fishPerLevel);
    }

    @Override
    public double getMultiplier(ItemStack stack) {
        double growthPerLevel = ModConfig.getInstance().fishingLetter.growthPerLevel;
        return getLevel(stack) * growthPerLevel;
    }
}
