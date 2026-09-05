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

    // ===== 每级参数：优先读取物品 NBT，缺省回退配置文件默认（仅影响获得时的初始值） =====
    public static double getWalkDistancePerLevel(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "walk_distance_per_level",
                ModConfig.getInstance().travelLetter.walkDistancePerLevel);
    }

    public static double getFlyDistancePerLevel(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "fly_distance_per_level",
                ModConfig.getInstance().travelLetter.flyDistancePerLevel);
    }

    public static double getWalkGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "walk_growth_per_level",
                ModConfig.getInstance().travelLetter.walkGrowthPerLevel);
    }

    public static double getFlyGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "fly_growth_per_level",
                ModConfig.getInstance().travelLetter.flyGrowthPerLevel);
    }

    public static double getArmorGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "armor_growth_per_level",
                ModConfig.getInstance().travelLetter.armorGrowthPerLevel);
    }

    public static double getArmor2Growth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "armor2_growth_per_level",
                ModConfig.getInstance().travelLetter.armor2GrowthPerLevel);
    }

    public static double getToughnessGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "toughness_growth_per_level",
                ModConfig.getInstance().travelLetter.toughnessGrowthPerLevel);
    }

    public static double getToughness2Growth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "toughness2_growth_per_level",
                ModConfig.getInstance().travelLetter.toughness2GrowthPerLevel);
    }

    public static double getResistanceGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "resistance_growth_per_level",
                ModConfig.getInstance().travelLetter.resistanceGrowthPerLevel);
    }

    public static double getResistance2Growth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "resistance2_growth_per_level",
                ModConfig.getInstance().travelLetter.resistance2GrowthPerLevel);
    }

    @Override
    public int getLevel(ItemStack stack) {
        int walkLevel = (int) (getWalkDistance(stack) / getWalkDistancePerLevel(stack));
        int flyLevel = (int) (getFlyDistance(stack) / getFlyDistancePerLevel(stack));
        return walkLevel + flyLevel;
    }

    @Override
    public double getMultiplier(ItemStack stack) {
        double walkLevels = Math.floor(getWalkDistance(stack) / getWalkDistancePerLevel(stack));
        double flyLevels = Math.floor(getFlyDistance(stack) / getFlyDistancePerLevel(stack));
        return walkLevels * getWalkGrowth(stack) + flyLevels * getFlyGrowth(stack);
    }
}