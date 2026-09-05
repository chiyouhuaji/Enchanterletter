package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.config.ModConfig;
import net.minecraft.world.item.ItemStack;

public class TenacityMagicLetterItem extends MagicLetterItem {
    public TenacityMagicLetterItem(Properties properties) {
        super(properties);
    }

    public static void addDamage(ItemStack stack, double amount) {
        ModDataComponents.setDouble(stack, ModDataComponents.DAMAGE_TAKEN, getDamageTaken(stack) + amount);
    }

    public static double getDamageTaken(ItemStack stack) {
        return ModDataComponents.getDouble(stack, ModDataComponents.DAMAGE_TAKEN, 0.0);
    }

    // ===== 每级参数：优先读取物品 NBT，缺省回退配置文件默认（仅影响获得时的初始值） =====
    public static double getDamagePerLevel(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "damage_per_level",
                ModConfig.getInstance().tenacityLetter.damagePerLevel);
    }

    public static double getGrowthPerLevel(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "growth_per_level",
                ModConfig.getInstance().tenacityLetter.growthPerLevel);
    }

    public static double getArmorGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "armor_growth_per_level",
                ModConfig.getInstance().tenacityLetter.armorGrowthPerLevel);
    }

    public static double getToughnessGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "toughness_growth_per_level",
                ModConfig.getInstance().tenacityLetter.toughnessGrowthPerLevel);
    }

    public static double getResistanceGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "resistance_growth_per_level",
                ModConfig.getInstance().tenacityLetter.resistanceGrowthPerLevel);
    }

    @Override
    public int getLevel(ItemStack stack) {
        return (int) (getDamageTaken(stack) / getDamagePerLevel(stack));
    }

    @Override
    public double getMultiplier(ItemStack stack) {
        return getLevel(stack) * getGrowthPerLevel(stack);
    }
}
