package cn.autoforged.enchanter_letter.util;

import cn.autoforged.enchanter_letter.config.ModConfig;
import cn.autoforged.enchanter_letter.enchantment.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * 允许宝藏获得附魔的静态配置应用工具（1.21.1）。
 * 配置只读一次：letter_enchanted.enabled 为总开关，开启后
 * 光灵/魔法绑定/魔法转化可作为宝藏/随机战利品出现；
 * 关闭后宝藏不会生成它们。附魔台不生成；铁砧/命令不受影响。
 */
public final class EnchantmentFilterUtil {
    private EnchantmentFilterUtil() {
    }

    /** 是否为模组三个可生存获取的附魔之一。 */
    public static boolean isModEnchantment(Holder<Enchantment> holder) {
        return holder.is(ModEnchantments.GLOWING)
                || holder.is(ModEnchantments.MAGIC_BINDING)
                || holder.is(ModEnchantments.MAGIC_CONVERSION);
    }

    /** 总开关：是否允许这三个附魔通过生存途径（附魔台/宝藏）获得（启动时读取缓存）。 */
    public static boolean isEnabled() {
        return ModConfig.isLetterEnchantedEnabled();
    }

    /**
     * 从随机战利品生成结果中移除被关闭的模组附魔（普通物品与附魔书均处理）。
     * 开关开启时不移除。
     */
    public static void removeDisabledTreasureEnchantments(ItemStack stack) {
        if (stack.isEmpty() || isEnabled()) return;
        removeIfPresent(stack, ModEnchantments.GLOWING, DataComponents.ENCHANTMENTS);
        removeIfPresent(stack, ModEnchantments.GLOWING, DataComponents.STORED_ENCHANTMENTS);
        removeIfPresent(stack, ModEnchantments.MAGIC_BINDING, DataComponents.ENCHANTMENTS);
        removeIfPresent(stack, ModEnchantments.MAGIC_BINDING, DataComponents.STORED_ENCHANTMENTS);
        removeIfPresent(stack, ModEnchantments.MAGIC_CONVERSION, DataComponents.ENCHANTMENTS);
        removeIfPresent(stack, ModEnchantments.MAGIC_CONVERSION, DataComponents.STORED_ENCHANTMENTS);
    }

    /**
     * 开关开启时，给随机战利品中出现的附魔书按各自配置概率附加模组附魔。
     * 只处理附魔书；不覆盖原版标签，因此不会破坏原版随机附魔池/箱子内容。
     * 每个附魔独立判定：一本附魔书可能同时带上多个模组附魔。
     */
    public static void maybeAddRandomTreasureEnchantment(ItemStack stack, RandomSource random, RegistryAccess registryAccess) {
        if (stack.isEmpty() || !isEnabled() || !stack.is(Items.ENCHANTED_BOOK)) return;
        var registry = registryAccess.registryOrThrow(Registries.ENCHANTMENT);
        ItemEnchantments current = stack.getEnchantments();
        tryAdd(stack, random, current, registry.getHolderOrThrow(ModEnchantments.GLOWING),
                ModConfig.getLetterEnchantedGlowingChance());
        tryAdd(stack, random, current, registry.getHolderOrThrow(ModEnchantments.MAGIC_BINDING),
                ModConfig.getLetterEnchantedMagicBindingChance());
        tryAdd(stack, random, current, registry.getHolderOrThrow(ModEnchantments.MAGIC_CONVERSION),
                ModConfig.getLetterEnchantedMagicConversionChance());
    }

    private static void tryAdd(ItemStack stack, RandomSource random, ItemEnchantments current,
                               Holder<Enchantment> holder, double chance) {
        if (chance <= 0 || current.getLevel(holder) > 0) return;
        if (random.nextDouble() < chance) {
            try {
                stack.enchant(holder, 1);
            } catch (Exception ignored) {
                // 某些物品/附魔组合不支持时保持原样，绝不因此破坏战利品生成
            }
        }
    }

    private static void removeIfPresent(ItemStack stack, ResourceKey<Enchantment> key,
                                        DataComponentType<ItemEnchantments> componentType) {
        ItemEnchantments enchantments = stack.getOrDefault(componentType, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return;
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(enchantments);
        mutable.removeIf(holder -> holder.is(key));
        ItemEnchantments result = mutable.toImmutable();
        if (!result.equals(enchantments)) {
            if (result.isEmpty()) {
                stack.remove(componentType);
            } else {
                stack.set(componentType, result);
            }
        }
    }
}
