package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.config.ModConfig;
import net.minecraft.world.item.ItemStack;

public class KillMagicLetterItem extends MagicLetterItem {
    public KillMagicLetterItem(Properties properties) {
        super(properties);
    }

    public static void addKill(ItemStack stack) {
        int current = stack.getOrDefault(ModDataComponents.KILL_COUNT.get(), 0);
        stack.set(ModDataComponents.KILL_COUNT.get(), current + 1);
    }

    public static int getKills(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.KILL_COUNT.get(), 0);
    }

    @Override
    public int getLevel(ItemStack stack) {
        int killsPerLevel = ModConfig.getInstance().killLetter.killsPerLevel;
        return getKills(stack) / killsPerLevel;
    }

    @Override
    public double getMultiplier(ItemStack stack) {
        double growthPerLevel = ModConfig.getInstance().killLetter.growthPerLevel;
        return getLevel(stack) * growthPerLevel;
    }
}
