package cn.autoforged.enchanter_letter.enchantment;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEnchantments {
    public static final String USEFULMAGIC_MAGIC = "usefulmagic:magic";

    /**
     * 魔法转化附魔。Forge 1.20.1 注册表在 commonSetup 时已锁定，
     * 必须使用 DeferredRegister（在 RegisterEvent 阶段自动注册）。
     */
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, UsefulMagicEnchanterLetterMod.MOD_ID);

    public static final RegistryObject<Enchantment> MAGIC_CONVERSION =
            ENCHANTMENTS.register("magic_conversion", () ->
                    new Enchantment(Enchantment.Rarity.COMMON, EnchantmentCategory.WEAPON,
                            new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
                        /**
                         * 1.20.1 移植：附魔台/铁砧/指令均通过 canEnchant 判定可附魔物品。
                         * 等效 1.21.1 数据包附魔的 supported_items 标签
                         * #enchanter_letter:magic_conversion_enchantable（经验/杀戮/阶段 1-10 手札），
                         * 与 1.21.1 完全一致，否则手札无法附上魔法转化附魔。
                         */
                        @Override
                        public boolean canEnchant(ItemStack stack) {
                            net.minecraft.world.item.Item item = stack.getItem();
                            return item == cn.autoforged.enchanter_letter.item.ModItems.EXPERIENCE_MAGIC_LETTER.get()
                                    || item == cn.autoforged.enchanter_letter.item.ModItems.KILL_MAGIC_LETTER.get()
                                    || item instanceof cn.autoforged.enchanter_letter.item.StageMagicLetterItem;
                        }
                    });

    /** 光灵：持有者（玩家/生物）发光，颜色由 NBT glow_color 控制。允许本模组所有手札/合订本。 */
    public static final RegistryObject<Enchantment> GLOWING =
            ENCHANTMENTS.register("glowing", () ->
                    new Enchantment(Enchantment.Rarity.COMMON, EnchantmentCategory.BREAKABLE,
                            new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
                        @Override
                        public boolean canEnchant(ItemStack stack) {
                            net.minecraft.world.item.Item item = stack.getItem();
                            return item instanceof cn.autoforged.enchanter_letter.item.MagicLetterItem
                                    || item instanceof cn.autoforged.enchanter_letter.item.LetterBinderItem;
                        }
                    });

    /** 魔法绑定：死亡不掉落（与消失诅咒排斥，同时存在时魔法绑定失效）。只允许本模组物品或饰品模组标签物品附魔。 */
    public static final RegistryObject<Enchantment> MAGIC_BINDING =
            ENCHANTMENTS.register("magic_binding", () ->
                    new Enchantment(Enchantment.Rarity.COMMON, EnchantmentCategory.BREAKABLE,
                            new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
                        @Override
                        public boolean canEnchant(ItemStack stack) {
                            if (stack.isEmpty()) return false;
                            net.minecraft.world.item.Item item = stack.getItem();
                            if (item instanceof cn.autoforged.enchanter_letter.item.MagicLetterItem
                                    || item instanceof cn.autoforged.enchanter_letter.item.LetterBinderItem) {
                                return true;
                            }
                            // 已注册到饰品模组（Curios / Accessories）物品标签
                            try {
                                return stack.getTags().anyMatch(tag -> {
                                    String ns = tag.location().getNamespace();
                                    return "curios".equals(ns) || "accessories".equals(ns);
                                });
                            } catch (Exception ignored) {
                                return false;
                            }
                        }
                    });

    public static boolean hasMagicConversion(ItemStack stack) {
        if (stack.isEmpty() || !MAGIC_CONVERSION.isPresent()) return false;
        return stack.getEnchantmentLevel(MAGIC_CONVERSION.get()) > 0;
    }

    public static boolean hasGlowing(ItemStack stack) {
        if (stack.isEmpty() || !GLOWING.isPresent()) return false;
        return stack.getEnchantmentLevel(GLOWING.get()) > 0;
    }

    public static boolean hasMagicBinding(ItemStack stack) {
        if (stack.isEmpty() || !MAGIC_BINDING.isPresent()) return false;
        return stack.getEnchantmentLevel(MAGIC_BINDING.get()) > 0;
    }

    /** 原版消失诅咒（本模组物品可附魔，玩家/生物持有者死亡移除）。 */
    public static boolean hasVanishing(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.VANISHING_CURSE) > 0;
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