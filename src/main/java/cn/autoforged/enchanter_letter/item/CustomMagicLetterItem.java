package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.ModDataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * 定制手札：不受等级成长影响，等级恒为 0。
 * 伤害倍率/护甲/韧性/抗性四项数值均直接读取物品 NBT，默认全为 0。
 * 不参与计数/进度成长，也没有进度栏位。
 */
public class CustomMagicLetterItem extends MagicLetterItem {
    public CustomMagicLetterItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getLevel(ItemStack stack) {
        return 0;
    }

    @Override
    public double getMultiplier(ItemStack stack) {
        return ModDataComponents.getDouble(stack, ModDataComponents.CUSTOM_DAMAGE, 0.0);
    }

    public static double getCustomArmor(ItemStack stack) {
        return ModDataComponents.getDouble(stack, ModDataComponents.CUSTOM_ARMOR, 0.0);
    }

    public static double getCustomToughness(ItemStack stack) {
        return ModDataComponents.getDouble(stack, ModDataComponents.CUSTOM_TOUGHNESS, 0.0);
    }

    public static double getCustomResistance(ItemStack stack) {
        return ModDataComponents.getDouble(stack, ModDataComponents.CUSTOM_RESISTANCE, 0.0);
    }
}