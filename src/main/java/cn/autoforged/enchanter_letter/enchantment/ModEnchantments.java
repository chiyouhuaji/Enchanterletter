package cn.autoforged.enchanter_letter.enchantment;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class ModEnchantments {
    public static final String USEFULMAGIC_MAGIC = "usefulmagic:magic";

    /** 魔法转化附魔（1.20.1 代码注册；构造器为 protected，使用匿名子类）。不限制适用范围。 */
    public static Enchantment MAGIC_CONVERSION;

    /** 光灵：持有者（玩家/生物）发光，颜色由 NBT glow_color 控制。不限制适用范围。 */
    public static Enchantment GLOWING;

    /** 魔法绑定：死亡不掉落（与消失诅咒排斥，同时存在时魔法绑定失效）。不限制适用范围。 */
    public static Enchantment MAGIC_BINDING;

    public static void register() {
        MAGIC_CONVERSION = Registry.register(BuiltInRegistries.ENCHANTMENT,
                new ResourceLocation(UsefulMagicEnchanterLetterMod.MOD_ID, "magic_conversion"),
                new Enchantment(Enchantment.Rarity.COMMON, EnchantmentCategory.WEAPON,
                        new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
                    /**
                     * 1.20.1 移植：附魔台/铁砧/指令均通过 canEnchant 判定可附魔物品。
                     * 按需求不再对本模组附魔限制适用范围，任意物品均可附魔。
                     */
                    @Override
                    public boolean canEnchant(ItemStack stack) {
                        return true;
                    }
                });
        GLOWING = Registry.register(BuiltInRegistries.ENCHANTMENT,
                new ResourceLocation(UsefulMagicEnchanterLetterMod.MOD_ID, "glowing"),
                new Enchantment(Enchantment.Rarity.COMMON, EnchantmentCategory.BREAKABLE,
                        new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
                    @Override
                    public boolean canEnchant(ItemStack stack) {
                        return true;
                    }
                });
        MAGIC_BINDING = Registry.register(BuiltInRegistries.ENCHANTMENT,
                new ResourceLocation(UsefulMagicEnchanterLetterMod.MOD_ID, "magic_binding"),
                new Enchantment(Enchantment.Rarity.COMMON, EnchantmentCategory.BREAKABLE,
                        new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
                    @Override
                    public boolean canEnchant(ItemStack stack) {
                        return true;
                    }
                });
    }

    public static boolean hasMagicConversion(ItemStack stack) {
        if (stack.isEmpty() || MAGIC_CONVERSION == null) return false;
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(MAGIC_CONVERSION, stack) > 0;
    }

    public static boolean hasGlowing(ItemStack stack) {
        if (stack.isEmpty() || GLOWING == null) return false;
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(GLOWING, stack) > 0;
    }

    public static boolean hasMagicBinding(ItemStack stack) {
        if (stack.isEmpty() || MAGIC_BINDING == null) return false;
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(MAGIC_BINDING, stack) > 0;
    }

    /** 原版消失诅咒（本模组物品可附魔，玩家/生物持有者死亡移除）。 */
    public static boolean hasVanishing(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(
                net.minecraft.world.item.enchantment.Enchantments.VANISHING_CURSE, stack) > 0;
    }

    /** 魔法绑定与消失诅咒同时存在时，魔法绑定失效（按消失诅咒处理）。 */
    public static boolean hasEffectiveMagicBinding(ItemStack stack) {
        return hasMagicBinding(stack) && !hasVanishing(stack);
    }

    /** 读取光灵颜色（NBT 0xRRGGBB）；未设置或 0 返回默认白色 0xFFFFFF。 */
    public static int getGlowColorOrDefault(ItemStack stack) {
        int color = ModDataComponents.getInt(stack, ModDataComponents.GLOW_COLOR, 0);
        return color == 0 ? 0xFFFFFF : color;
    }

    public static ResourceLocation getEffectiveDamageType(ItemStack stack) {
        if (!hasMagicConversion(stack)) {
            return new ResourceLocation(USEFULMAGIC_MAGIC);
        }
        String nbtType = ModDataComponents.getString(stack, ModDataComponents.CONVERSION_DAMAGE_TYPE, "");
        if (nbtType.isEmpty()) {
            return new ResourceLocation(USEFULMAGIC_MAGIC);
        }
        ResourceLocation parsed = ResourceLocation.tryParse(nbtType);
        return parsed != null ? parsed : new ResourceLocation(USEFULMAGIC_MAGIC);
    }

    public static String getConversionDamageTypeOrDefault(ItemStack stack) {
        String nbtType = ModDataComponents.getString(stack, ModDataComponents.CONVERSION_DAMAGE_TYPE, "");
        return nbtType.isEmpty() ? USEFULMAGIC_MAGIC : nbtType;
    }

    public static boolean isValidDamageTypeString(String id) {
        if (id == null || id.isEmpty()) return false;
        return ResourceLocation.tryParse(id) != null;
    }

    public static boolean isValidDamageType(HolderLookup.Provider registries, ResourceLocation id) {
        if (USEFULMAGIC_MAGIC.equals(id.toString())) return true;
        return registries.lookupOrThrow(Registries.DAMAGE_TYPE)
                .get(ResourceKey.create(Registries.DAMAGE_TYPE, id)).isPresent();
    }
}
