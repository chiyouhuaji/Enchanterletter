package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.config.ModConfig;
import net.minecraft.world.item.ItemStack;

public class KillMagicLetterItem extends MagicLetterItem {
    public KillMagicLetterItem(Properties properties) {
        super(properties);
    }

    public static void addKill(ItemStack stack) {
        int current = ModDataComponents.getInt(stack, ModDataComponents.KILL_COUNT, 0);
        ModDataComponents.setInt(stack, ModDataComponents.KILL_COUNT, current + 1);
    }

    public static int getKills(ItemStack stack) {
        return ModDataComponents.getInt(stack, ModDataComponents.KILL_COUNT, 0);
    }

    // ===== 每级参数：优先读取物品 NBT，缺省回退配置文件默认（仅影响获得时的初始值） =====
    public static double getKillsPerLevel(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "kills_per_level",
                ModConfig.getInstance().killLetter.killsPerLevel);
    }

    public static double getGrowthPerLevel(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "growth_per_level",
                ModConfig.getInstance().killLetter.growthPerLevel);
    }

    public static double getArmorGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "armor_growth_per_level",
                ModConfig.getInstance().killLetter.armorGrowthPerLevel);
    }

    public static double getToughnessGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "toughness_growth_per_level",
                ModConfig.getInstance().killLetter.toughnessGrowthPerLevel);
    }

    public static double getResistanceGrowth(ItemStack stack) {
        return ModDataComponents.getGrowthDouble(stack, "resistance_growth_per_level",
                ModConfig.getInstance().killLetter.resistanceGrowthPerLevel);
    }

    @Override
    public int getLevel(ItemStack stack) {
        return (int) (getKills(stack) / getKillsPerLevel(stack));
    }

    @Override
    public double getMultiplier(ItemStack stack) {
        return getLevel(stack) * getGrowthPerLevel(stack);
    }
}
