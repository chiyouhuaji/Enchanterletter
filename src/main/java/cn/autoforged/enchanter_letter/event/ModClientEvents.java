package cn.autoforged.enchanter_letter.event;

import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import cn.autoforged.enchanter_letter.config.ModClientConfig;
import cn.autoforged.enchanter_letter.config.ModConfig;
import cn.autoforged.enchanter_letter.enchantment.ModEnchantments;
import cn.autoforged.enchanter_letter.integration.AccessoriesIntegration;
import cn.autoforged.enchanter_letter.integration.CuriosIntegration;
import cn.autoforged.enchanter_letter.item.LetterBinderItem;
import cn.autoforged.enchanter_letter.item.LetterStats;
import cn.autoforged.enchanter_letter.item.MagicLetterItem;
import cn.autoforged.enchanter_letter.item.TimeMagicLetterItem;
import cn.autoforged.enchanter_letter.network.LetterStoragePayloads;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModClientEvents {
    private static final ResourceLocation HUD_OVERLAY =
            ResourceLocation.fromNamespaceAndPath(UsefulMagicEnchanterLetterMod.MOD_ID, "magic_letter_hud");
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("+#0.0%");
    private static final DecimalFormat VALUE_FORMAT = new DecimalFormat("+#0.0#");
    private static final String KEY_CATEGORY = "key.category." + UsefulMagicEnchanterLetterMod.MOD_ID;

    /** HUD 循环模式：0=伤害增益 1=护甲值增益 2=护甲韧性增益 3=抗性提升增益 4=关闭。 */
    private static final int MODE_DAMAGE = 0;
    private static final int MODE_ARMOR = 1;
    private static final int MODE_TOUGHNESS = 2;
    private static final int MODE_RESISTANCE = 3;
    private static final int MODE_OFF = 4;
    /** 列表属性标题颜色：红=伤害 黄=护甲值 蓝=护甲韧性 紫=伤害抗性。 */
    private static final int COLOR_DAMAGE = 0xFFFF5555;
    private static final int COLOR_ARMOR = 0xFFFFFF55;
    private static final int COLOR_TOUGHNESS = 0xFF5555FF;
    private static final int COLOR_RESISTANCE = 0xFFAA00AA;
    private static final String KEY_TOGGLE_HUD = "key." + UsefulMagicEnchanterLetterMod.MOD_ID + ".toggle_hud";
    private static final int ACCESSORY_COMPAT_INTERVAL = 100;
    /** 客户端饰品栏缓存刷新间隔（tick）：避免每 tick 反射遍历 Curios 槽位，最低 1 tick 延迟（0.05 秒）。 */
    private static final int CURIOS_CACHE_INTERVAL = 2;
    /** HUD 手札条目缓存刷新间隔（tick）：避免每帧全槽扫描，最低 1 tick 延迟（0.05 秒）。 */
    private static final int HUD_ENTRIES_INTERVAL = 2;

    private static KeyMapping toggleHudKey;
    private static List<ItemStack> cachedCuriosStacks = List.of();
    private static List<LetterStats.Entry> cachedHudEntries = List.of();
    private static int accessoryCompatCooldown = ACCESSORY_COMPAT_INTERVAL;
    private static int curiosCacheCooldown = 0;
    private static int hudEntriesCooldown = 0;

    private static KeyMapping getToggleHudKey() {
        if (toggleHudKey == null) {
            ModClientConfig.init();
            toggleHudKey = new KeyMapping(KEY_TOGGLE_HUD, ModClientConfig.getInstance().hudToggleKeyCode, KEY_CATEGORY);
        }
        return toggleHudKey;
    }

    public static void register() {
        KeyBindingHelper.registerKeyBinding(getToggleHudKey());
        // /letterstorage 交互模式开关（S2C）
        LetterStoragePayloads.registerClientReceiver();

        // /letterstorage 截取模式：空手左键攻击拦截由 MinecraftAttackInterceptMixin
        // 在 startAttack/continueAttack 两个派发入口统一处理（见该 mixin 注释），
        // 不依赖 START_CLIENT_TICK 的 tick 帧对齐，高帧率下也不会漏拦截。

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 饰品栏缓存：带冷却刷新，避免每 tick 反射遍历（HUD 用缓存，0.1 秒内无感）
            if (--curiosCacheCooldown <= 0) {
                curiosCacheCooldown = CURIOS_CACHE_INTERVAL;
                if (client.player != null) {
                    cachedCuriosStacks = CuriosIntegration.getCuriosStacks(client.player);
                }
            }
            if (--accessoryCompatCooldown <= 0) {
                accessoryCompatCooldown = ACCESSORY_COMPAT_INTERVAL;
                CuriosIntegration.ensureAllSlotCompat();
                AccessoriesIntegration.ensureAllSlotCompat(client.level);
            }
            if (getToggleHudKey().consumeClick()) {
                // N 键循环：伤害增益 -> 护甲值增益 -> 护甲韧性增益 -> 抗性提升增益（关闭则跳过）-> 关闭
                ModClientConfig cfg = ModClientConfig.getInstance();
                int mode = cfg.hudMode + 1;
                if (mode > MODE_OFF) mode = MODE_DAMAGE;
                if (mode == MODE_RESISTANCE && !ModConfig.getInstance().letterResistance.enabled) {
                    mode = MODE_OFF;
                }
                cfg.hudMode = mode;
                ModClientConfig.save();
            }
        });

        HudRenderCallback.EVENT.register((guiGraphics, deltaTracker) -> {
            renderHud(guiGraphics);
        });

        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            var mc = Minecraft.getInstance();
            if (mc.player == null) return;
            var itemKey = stack.getItemHolder().unwrapKey().orElse(null);
            if (itemKey == null) return;
            var loc = itemKey.location();
            if (!"usefulmagic".equals(loc.getNamespace())) return;
            if ("magic_axe".equals(loc.getPath())) return;
            double totalMultiplier = getEffectiveTotal(mc);
            if (totalMultiplier > 0) {
                lines.add(Component.translatable(
                        "tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + ".wand_magic_bonus",
                        PERCENT_FORMAT.format(totalMultiplier)));
            }
        });
    }

    private static void renderHud(GuiGraphics guiGraphics) {
        int mode = ModClientConfig.getInstance().hudMode;
        if (mode == MODE_OFF) return;
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;
        boolean multi = ModConfig.getInstance().stackingRules.allowMultipleLetters;
        // 手札条目：带冷却刷新缓存，避免每帧全槽扫描（含 Curios 反射与合订本内容物遍历）
        if (--hudEntriesCooldown <= 0) {
            hudEntriesCooldown = HUD_ENTRIES_INTERVAL;
            cachedHudEntries = LetterStats.collectEntries(player, mc.level);
        }
        List<LetterStats.Entry> entries = cachedHudEntries;

        // 最上方标题：按当前列表属性着色（红=伤害 黄=护甲值 蓝=护甲韧性 紫=伤害抗性）
        int titleColor;
        String titleKey;
        switch (mode) {
            case MODE_ARMOR: titleColor = COLOR_ARMOR; titleKey = "hud." + UsefulMagicEnchanterLetterMod.MOD_ID + ".mode_armor"; break;
            case MODE_TOUGHNESS: titleColor = COLOR_TOUGHNESS; titleKey = "hud." + UsefulMagicEnchanterLetterMod.MOD_ID + ".mode_toughness"; break;
            case MODE_RESISTANCE: titleColor = COLOR_RESISTANCE; titleKey = "hud." + UsefulMagicEnchanterLetterMod.MOD_ID + ".mode_resistance"; break;
            default: titleColor = COLOR_DAMAGE; titleKey = "hud." + UsefulMagicEnchanterLetterMod.MOD_ID + ".mode_damage";
        }
        guiGraphics.drawString(mc.font, Component.translatable(titleKey), 4, 2, titleColor, true);

        // 未开启 allow_multiple_letters 时：各属性独立取最大值，等于最大值的白字，其余灰字
        double max = 0;
        for (LetterStats.Entry e : entries) {
            max = Math.max(max, valueOf(mode, e));
        }
        int y = 14;
        for (LetterStats.Entry e : entries) {
            double v = valueOf(mode, e);
            if (v <= 0) continue;
            boolean effective = multi || Math.abs(v - max) < 1e-9;
            if (handleHudEntry(guiGraphics, mc, e, mode, y, effective)) y += 10;
        }
    }

    private static double valueOf(int mode, LetterStats.Entry e) {
        switch (mode) {
            case MODE_ARMOR: return e.armor;
            case MODE_TOUGHNESS: return e.toughness;
            case MODE_RESISTANCE: return e.resistance;
            default: return e.damageMultiplier;
        }
    }

    private static boolean handleHudEntry(GuiGraphics guiGraphics, Minecraft mc, LetterStats.Entry e, int mode, int y, boolean effective) {
        Component display;
        if (mode == MODE_ARMOR) {
            display = Component.translatable("hud." + UsefulMagicEnchanterLetterMod.MOD_ID + ".magic_letter_armor",
                    e.stack.getDisplayName(), e.level, VALUE_FORMAT.format(e.armor));
        } else if (mode == MODE_TOUGHNESS) {
            display = Component.translatable("hud." + UsefulMagicEnchanterLetterMod.MOD_ID + ".magic_letter_toughness",
                    e.stack.getDisplayName(), e.level, VALUE_FORMAT.format(e.toughness));
        } else if (mode == MODE_RESISTANCE) {
            display = Component.translatable("hud." + UsefulMagicEnchanterLetterMod.MOD_ID + ".magic_letter_resistance",
                    e.stack.getDisplayName(), e.level, PERCENT_FORMAT.format(e.resistance));
        } else {
            if (e.enchanted) {
                display = Component.translatable("hud." + UsefulMagicEnchanterLetterMod.MOD_ID + ".magic_letter_conversion",
                        e.stack.getDisplayName(), e.level, PERCENT_FORMAT.format(e.damageMultiplier), e.damageType);
            } else {
                display = Component.translatable("hud." + UsefulMagicEnchanterLetterMod.MOD_ID + ".magic_letter",
                        e.stack.getDisplayName(), e.level, PERCENT_FORMAT.format(e.damageMultiplier));
            }
        }
        int color = effective ? 0xFFFFFF : 0x666666;
        guiGraphics.drawString(mc.font, display, 4, y, color, true);
        return true;
    }

    private static String letterKey(ItemStack stack, double mult) {
        return stack.getItem() + "|" + mult + "|" + ModEnchantments.getEffectiveDamageType(stack) + "|"
                + ModEnchantments.hasMagicConversion(stack);
    }

    private static double getEffectiveTotal(Minecraft mc) {
        boolean multi = ModConfig.getInstance().stackingRules.allowMultipleLetters;
        boolean dedup = !ModConfig.getInstance().stackingRules.allowSameLetters;
        double total = 0;
        double max = 0;
        Set<String> seen = new HashSet<>();
        for (var stack : collectAllStacks(mc)) {
            double mult = getLetterMultiplier(stack, mc.level);
            if (mult <= 0) continue;
            if (dedup && !seen.add(letterKey(stack, mult))) continue;
            total += mult;
            if (mult > max) max = mult;
        }
        return multi ? total : max;
    }

    private static List<ItemStack> collectAllStacks(Minecraft mc) {
        List<ItemStack> result = new ArrayList<>();
        if (mc.player == null) return result;
        var inventory = mc.player.getInventory();
        for (var stack : inventory.items) collectStack(stack, result);
        for (var stack : inventory.armor) collectStack(stack, result);
        for (var stack : inventory.offhand) collectStack(stack, result);
        for (var stack : cachedCuriosStacks) collectStack(stack, result);
        for (var stack : AccessoriesIntegration.getAccessoriesStacks(mc.player)) collectStack(stack, result);
        return result;
    }

    private static void collectStack(ItemStack stack, List<ItemStack> out) {
        if (stack.isEmpty()) return;
        out.add(stack);
        if (stack.getItem() instanceof LetterBinderItem) {
            net.minecraft.world.item.component.BundleContents contents = stack.getOrDefault(
                    net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS,
                    net.minecraft.world.item.component.BundleContents.EMPTY);
            for (ItemStack inner : contents.itemsCopy()) {
                if (!inner.isEmpty()) out.add(inner);
            }
        }
    }

    private static double getLetterMultiplier(ItemStack stack, Level level) {
        if (stack.isEmpty()) return 0;
        var item = stack.getItem();
        if (item instanceof TimeMagicLetterItem time) {
            return time.getMultiplier(stack, level);
        }
        if (item instanceof MagicLetterItem ml) {
            return ml.getMultiplier(stack);
        }
        return 0;
    }
}
