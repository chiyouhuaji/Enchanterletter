package cn.autoforged.enchanter_letter.enchantment;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import cn.autoforged.enchanter_letter.config.ModConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;

public class ModEnchantments {
    public static final ResourceKey<Enchantment> MAGIC_CONVERSION = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(UsefulMagicEnchanterLetterMod.MOD_ID, "magic_conversion"));

    /** 光灵：持有者（玩家/生物）发光，颜色由 NBT/组件 glow_color 控制。 */
    public static final ResourceKey<Enchantment> GLOWING = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(UsefulMagicEnchanterLetterMod.MOD_ID, "glowing"));

    /** 魔法绑定：死亡不掉落（与消失诅咒排斥，同时存在时魔法绑定失效）。 */
    public static final ResourceKey<Enchantment> MAGIC_BINDING = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(UsefulMagicEnchanterLetterMod.MOD_ID, "magic_binding"));

    /** 原版消失诅咒（本模组物品可附魔，玩家/生物持有者死亡移除）。 */
    public static final ResourceKey<Enchantment> VANISHING_CURSE = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.withDefaultNamespace("vanishing_curse"));

    public static final String USEFULMAGIC_MAGIC = "usefulmagic:magic";

    public static boolean hasMagicConversion(ItemStack stack) {
        return hasEnchantment(stack, MAGIC_CONVERSION);
    }

    public static boolean hasGlowing(ItemStack stack) {
        return hasEnchantment(stack, GLOWING);
    }

    public static boolean hasMagicBinding(ItemStack stack) {
        return hasEnchantment(stack, MAGIC_BINDING);
    }

    public static boolean hasVanishing(ItemStack stack) {
        return hasEnchantment(stack, VANISHING_CURSE);
    }

    /** 魔法绑定与消失诅咒同时存在时，魔法绑定失效（按消失诅咒处理）。 */
    public static boolean hasEffectiveMagicBinding(ItemStack stack) {
        return hasMagicBinding(stack) && !hasVanishing(stack);
    }

    private static boolean hasEnchantment(ItemStack stack, ResourceKey<Enchantment> key) {
        if (stack.isEmpty()) return false;
        ItemEnchantments enchantments = stack.getEnchantments();
        if (enchantments.isEmpty()) return false;
        return enchantments.keySet().stream().anyMatch(holder -> holder.is(key));
    }

    /** 直接给物品添加模组附魔（不受 /letterenchanted 功能开关影响）。 */
    public static void addMagicConversion(ItemStack stack, RegistryAccess registries) {
        addEnchantment(stack, registries, MAGIC_CONVERSION);
    }

    public static void addGlowing(ItemStack stack, RegistryAccess registries) {
        addEnchantment(stack, registries, GLOWING);
    }

    public static void addMagicBinding(ItemStack stack, RegistryAccess registries) {
        addEnchantment(stack, registries, MAGIC_BINDING);
    }

    /** 模组侧消失标记的 NBT 键（非零表示本模组通过 addVanishing 标记过消失，需强制销毁）。 */
    public static final String MOD_APPLIED_VANISHING = "mod_applied_vanishing";

    /** 是否为本模组通过 addVanishing 标记的消失（强制销毁，不受 /lettervanish 开关影响）。 */
    public static boolean isModAppliedVanishing(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return ModDataComponents.getGrowthInt(stack, MOD_APPLIED_VANISHING, 0) != 0;
    }

    /** 直接给物品添加原版消失诅咒（不受 /lettervanish 开关影响），同时注册进强制消失白名单。 */
    public static void addVanishing(ItemStack stack, RegistryAccess registries) {
        addEnchantment(stack, registries, VANISHING_CURSE);
        // 同时打上模组标记：强制销毁，即使 /lettervanish 关闭也按消失诅咒销毁。
        if (!stack.isEmpty()) {
            ModDataComponents.setGrowthInt(stack, MOD_APPLIED_VANISHING, 1);
        }
        registerForceVanishWhitelist(stack);
    }

    /**
     * 把物品注册进强制消失白名单（幂等）：白名单成员无视 /lettervanish 开关，始终强制销毁。
     * addVanishing 调用时同步写入；供 shouldVanishClear 检索白名单成员。
     */
    public static void registerForceVanishWhitelist(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        List<String> whitelist = ModConfig.getInstance().letterVanish.forceVanishWhitelist;
        if (whitelist == null) return;
        String itemKey = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (!whitelist.contains(itemKey)) {
            whitelist.add(itemKey);
        }
    }

    private static void addEnchantment(ItemStack stack, RegistryAccess registries, ResourceKey<Enchantment> key) {
        if (stack.isEmpty() || registries == null) return;
        registries.registryOrThrow(Registries.ENCHANTMENT).getHolder(key).ifPresent(holder -> stack.enchant(holder, 1));
    }

    /** 读取光灵颜色（0xRRGGBB）；未设置或 0 返回默认白色 0xFFFFFF。 */
    public static int getGlowColorOrDefault(ItemStack stack) {
        int color = stack.getOrDefault(ModDataComponents.GLOW_COLOR.get(), 0);
        return color == 0 ? 0xFFFFFF : color;
    }

    /**
     * 获取手札实际生效的伤害类型。
     * 有 魔法转化 附魔时：读取 NBT 中的 conversion_damage_type，
     *   若为空或无效则默认 usefulmagic:magic。
     * 无 魔法转化 附魔时：忽略 NBT，始终返回 usefulmagic:magic。
     */
    public static ResourceLocation getEffectiveDamageType(ItemStack stack) {
        if (!hasMagicConversion(stack)) {
            return ResourceLocation.parse(USEFULMAGIC_MAGIC);
        }
        String nbtType = stack.getOrDefault(ModDataComponents.CONVERSION_DAMAGE_TYPE.get(), "");
        if (nbtType.isEmpty()) {
            return ResourceLocation.parse(USEFULMAGIC_MAGIC);
        }
        ResourceLocation parsed = ResourceLocation.tryParse(nbtType);
        return parsed != null ? parsed : ResourceLocation.parse(USEFULMAGIC_MAGIC);
    }

    public static String getConversionDamageTypeOrDefault(ItemStack stack) {
        String nbtType = stack.getOrDefault(ModDataComponents.CONVERSION_DAMAGE_TYPE.get(), "");
        return nbtType.isEmpty() ? USEFULMAGIC_MAGIC : nbtType;
    }

    public static boolean isValidDamageTypeString(String id) {
        if (id == null || id.isEmpty()) return false;
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        return parsed != null;
    }

    /**
     * 校验 damageType 字符串是否对应注册表中已注册的伤害类型。
     * 用于命令端校验，客户端 tab 补全由命令参数自动提供。
     */
    public static boolean isValidDamageType(HolderLookup.Provider registries, ResourceLocation id) {
        if (USEFULMAGIC_MAGIC.equals(id.toString())) return true;
        return registries.lookupOrThrow(Registries.DAMAGE_TYPE)
                .get(ResourceKey.create(Registries.DAMAGE_TYPE, id)).isPresent();
    }
}
