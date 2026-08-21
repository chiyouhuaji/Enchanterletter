package cn.autoforged.enchanter_letter.util;

import cn.autoforged.enchanter_letter.config.ModConfig;
import cn.autoforged.enchanter_letter.enchantment.ModEnchantments;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * 允许宝藏获得附魔的静态配置应用工具（Forge 1.20.1）。
 * 配置只读一次：letter_enchanted.enabled 为总开关，开启后
 * 光灵/魔法绑定/魔法转化可作为宝藏/随机战利品出现；
 * 关闭后宝藏不会生成它们。附魔台不生成；铁砧/命令不受影响。
 */
public final class EnchantmentFilterUtil {
    private EnchantmentFilterUtil() {
    }

    /** 是否为模组三个可生存获取的附魔之一。 */
    public static boolean isModEnchantment(Enchantment enchantment) {
        return enchantment == ModEnchantments.GLOWING.get()
                || enchantment == ModEnchantments.MAGIC_BINDING.get()
                || enchantment == ModEnchantments.MAGIC_CONVERSION.get();
    }

    /** 总开关：是否允许这三个附魔通过生存途径（附魔台/宝藏）获得（启动时读取缓存）。 */
    public static boolean isEnabled() {
        return ModConfig.isLetterEnchantedEnabled();
    }

    /** 从随机战利品生成结果中移除被关闭的模组附魔。 */
    public static void removeDisabledTreasureEnchantments(ItemStack stack) {
        if (stack.isEmpty() || isEnabled()) return;
        Map<Enchantment, Integer> enchantments = new HashMap<>(EnchantmentHelper.getEnchantments(stack));
        boolean changed = false;
        for (Enchantment enchantment : new java.util.ArrayList<>(enchantments.keySet())) {
            if (isModEnchantment(enchantment)) {
                enchantments.remove(enchantment);
                changed = true;
            }
        }
        if (changed) {
            EnchantmentHelper.setEnchantments(enchantments, stack);
        }
    }

    /**
     * 开关开启时，给随机战利品中出现的附魔书按各自配置概率附加模组附魔。
     * 只处理附魔书；不覆盖原版标签，因此不会破坏原版随机附魔池/箱子内容。
     * 每个附魔独立判定：一本附魔书可能同时带上多个模组附魔。
     */
    public static void maybeAddRandomTreasureEnchantment(ItemStack stack, RandomSource random) {
        if (stack.isEmpty() || !isEnabled() || !stack.is(Items.ENCHANTED_BOOK)) return;
        Map<Enchantment, Integer> enchantments = new HashMap<>(EnchantmentHelper.getEnchantments(stack));
        tryAdd(stack, random, enchantments, ModEnchantments.GLOWING.get(),
                ModConfig.getLetterEnchantedGlowingChance());
        tryAdd(stack, random, enchantments, ModEnchantments.MAGIC_BINDING.get(),
                ModConfig.getLetterEnchantedMagicBindingChance());
        tryAdd(stack, random, enchantments, ModEnchantments.MAGIC_CONVERSION.get(),
                ModConfig.getLetterEnchantedMagicConversionChance());
    }

    private static void tryAdd(ItemStack stack, RandomSource random, Map<Enchantment, Integer> enchantments,
                               Enchantment enchantment, double chance) {
        if (chance <= 0 || enchantments.containsKey(enchantment)) return;
        if (random.nextDouble() < chance) {
            enchantments.put(enchantment, 1);
            EnchantmentHelper.setEnchantments(enchantments, stack);
        }
    }
}
