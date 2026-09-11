package cn.autoforged.enchanter_letter.event;

import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import cn.autoforged.enchanter_letter.config.ModClientConfig;
import cn.autoforged.enchanter_letter.config.ModConfig;
import cn.autoforged.enchanter_letter.enchantment.ModEnchantments;
import cn.autoforged.enchanter_letter.effect.ModMagicObstruction;
import cn.autoforged.enchanter_letter.integration.CuriosIntegration;
import cn.autoforged.enchanter_letter.item.LetterBinderItem;
import cn.autoforged.enchanter_letter.item.LetterStats;
import cn.autoforged.enchanter_letter.item.MagicLetterItem;
import cn.autoforged.enchanter_letter.item.TimeMagicLetterItem;
import cn.autoforged.enchanter_letter.network.LetterStoragePayloads;
import cn.autoforged.enchanter_letter.storage.LetterStorageClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = UsefulMagicEnchanterLetterMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
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
    /** 魔法阻碍使手札失效时词条的颜色（与效果同色 0x7B2D9A）。 */
    private static final int COLOR_DISABLED = 0xFF7B2D9A;
    private static final String KEY_TOGGLE_HUD = "key." + UsefulMagicEnchanterLetterMod.MOD_ID + ".toggle_hud";
    /** Curios 槽位兼容注入间隔（tick，默认每 5 秒，作为事件触发的兜底）。 */
    private static final int ACCESSORY_COMPAT_INTERVAL = 100;
    /** 客户端饰品栏缓存刷新间隔（tick）：避免每 tick 反射遍历 Curios 槽位，最低 1 tick 延迟（0.05 秒）。 */
    private static final int CURIOS_CACHE_INTERVAL = 5;
    /** HUD 手札条目缓存刷新间隔（tick）：避免每帧全槽扫描，最低 1 tick 延迟（0.05 秒）。 */
    private static final int HUD_ENTRIES_INTERVAL = 5;

    private static KeyMapping toggleHudKey;
    private static List<ItemStack> cachedCuriosStacks = List.of();
    private static List<LetterStats.Entry> cachedHudEntries = List.of();
    private static int accessoryCompatCooldown = ACCESSORY_COMPAT_INTERVAL;
    private static int curiosCacheCooldown = 0;
    private static int hudEntriesCooldown = 0;

    public static void registerKeyBinding(RegisterKeyMappingsEvent event) {
        ModClientConfig.init();
        toggleHudKey = new KeyMapping(KEY_TOGGLE_HUD, ModClientConfig.getInstance().hudToggleKeyCode, KEY_CATEGORY);
        event.register(toggleHudKey);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        // 饰品栏缓存：带冷却刷新，避免每 tick 反射遍历（HUD 用缓存，0.1 秒内无感）
        if (--curiosCacheCooldown <= 0) {
            curiosCacheCooldown = CURIOS_CACHE_INTERVAL;
            if (client.player != null) {
                cachedCuriosStacks = CuriosIntegration.getCuriosStacks(client.player);
            }
        }
        // Curios 兼容注入（客户端槽位表）
        if (--accessoryCompatCooldown <= 0) {
            accessoryCompatCooldown = ACCESSORY_COMPAT_INTERVAL;
            CuriosIntegration.ensureAllSlotCompat();
        }
        if (toggleHudKey != null && toggleHudKey.consumeClick()) {
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
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        renderHud(event.getGuiGraphics());
    }

    /**
     * /letterstorage 截取模式：拦截原版“空手左键攻击”，把准星指向的实体 id 上报服务端。
     * 仅在 交互模式开启 且 主手为空 时拦截；有其他键绑定处理时不生效。
     */
    @SubscribeEvent
    public static void onInteractionKeyTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) return;
        if (!LetterStorageClient.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;
        if (!player.getMainHandItem().isEmpty()) return; // 仅“空手”拦截
        event.setCanceled(true); // 拦截原版空手左键攻击
        Entity target = mc.crosshairPickEntity;
        if (target == null || target == player) return;
        LetterStoragePayloads.sendCaptureToServer(target.getUUID());
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var itemKey = event.getItemStack().getItemHolder().unwrapKey().orElse(null);
        if (itemKey == null) return;
        var loc = itemKey.location();
        if (!"usefulmagic".equals(loc.getNamespace())) return;
        if ("magic_axe".equals(loc.getPath())) return;
        double totalMultiplier = getEffectiveTotal(mc);
        if (totalMultiplier > 0) {
            event.getToolTip().add(Component.translatable(
                    "tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + ".wand_magic_bonus",
                    PERCENT_FORMAT.format(totalMultiplier)));
        }
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

    /** 魔法阻碍是否使当前 HUD 显示模式下的手札条目失效（客户端按玩家状态判断）。 */
    private static boolean isObstructedForMode(Minecraft mc, int mode) {
        if (mc.player == null) return false;
        int level = ModMagicObstruction.getLevel(mc.player);
        switch (mode) {
            case MODE_ARMOR:
            case MODE_TOUGHNESS:
                return level >= 2;
            case MODE_RESISTANCE:
                return level >= 3;
            default:
                return level >= 1;
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
        boolean obstructed = isObstructedForMode(mc, mode);
        int color = obstructed ? COLOR_DISABLED : (effective ? 0xFFFFFF : 0x666666);
        guiGraphics.drawString(mc.font, display, 4, y, color, true);
        return true;
    }

    private static boolean isOurLetter(ItemStack stack) {
        return stack.getItem() instanceof MagicLetterItem;
    }



    /** 手札“身份”键：同物品、同倍率、同伤害类型、同附魔状态视为相同（除转化状态外完全相同）。 */
    private static String letterKey(ItemStack stack, double mult) {
        return stack.getItem() + "|" + mult + "|" + ModEnchantments.getEffectiveDamageType(stack) + "|"
                + ModEnchantments.hasMagicConversion(stack);
    }

    /** 实际生效的总倍率：允许多张时求和，否则只取倍率最高的一张；相同手札按配置去重。 */
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

    /** 收集玩家所有槽位（含手札合订本内容与 Curios 饰品槽位）中的手札物品。 */
    private static List<ItemStack> collectAllStacks(Minecraft mc) {
        List<ItemStack> result = new ArrayList<>();
        if (mc.player == null) return result;
        var inventory = mc.player.getInventory();
        for (var stack : inventory.items) collectStack(stack, result);
        for (var stack : inventory.armor) collectStack(stack, result);
        for (var stack : inventory.offhand) collectStack(stack, result);
        for (var stack : cachedCuriosStacks) collectStack(stack, result);
        return result;
    }

    private static void collectStack(ItemStack stack, List<ItemStack> out) {
        if (stack.isEmpty()) return;
        out.add(stack);
        if (stack.getItem() instanceof LetterBinderItem) {
            BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
            for (ItemStack inner : contents.itemsCopy()) {
                if (!inner.isEmpty()) out.add(inner);
            }
        }
    }

    private static double getLetterMultiplier(ItemStack stack, Level level) {
        if (stack.isEmpty()) return 0;
        var item = stack.getItem();
        if (item instanceof TimeMagicLetterItem) {
            return TimeMagicLetterItem.getMultiplier(level, stack);
        }
        if (item instanceof MagicLetterItem ml) {
            return ml.getMultiplier(stack);
        }
        return 0;
    }
}
