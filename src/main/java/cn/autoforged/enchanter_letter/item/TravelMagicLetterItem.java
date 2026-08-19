package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.config.ModConfig;
import net.minecraft.world.item.ItemStack;

public class TravelMagicLetterItem extends MagicLetterItem {
    public TravelMagicLetterItem(Properties properties) {
        super(properties);
    }

    public static void addDistance(ItemStack stack, double walk, double fly) {
        stack.set(ModDataComponents.WALK_DISTANCE.get(), getWalkDistance(stack) + walk);
        stack.set(ModDataComponents.FLY_DISTANCE.get(), getFlyDistance(stack) + fly);
    }

    public static double getWalkDistance(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.WALK_DISTANCE.get(), 0.0);
    }

    public static double getFlyDistance(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FLY_DISTANCE.get(), 0.0);
    }

    @Override
    public int getLevel(ItemStack stack) {
        ModConfig.TravelLetterConfig cfg = ModConfig.getInstance().travelLetter;
        int walkLevel = (int) (getWalkDistance(stack) / cfg.walkDistancePerLevel);
        int flyLevel = (int) (getFlyDistance(stack) / cfg.flyDistancePerLevel);
        return walkLevel + flyLevel;
    }

    @Override
    public double getMultiplier(ItemStack stack) {
        ModConfig.TravelLetterConfig cfg = ModConfig.getInstance().travelLetter;
        double walkLevels = Math.floor(getWalkDistance(stack) / cfg.walkDistancePerLevel);
        double flyLevels = Math.floor(getFlyDistance(stack) / cfg.flyDistancePerLevel);
        return walkLevels * cfg.walkGrowthPerLevel + flyLevels * cfg.flyGrowthPerLevel;
    }
}
