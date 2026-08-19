package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.config.ModConfig;
import cn.autoforged.enchanter_letter.enchantment.ModEnchantments;
import cn.autoforged.enchanter_letter.integration.CuriosIntegration;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 手札的防御属性结算（护甲值、护甲韧性、抗性提升）。
 * <p>
 * - 护甲值 / 护甲韧性：直接增加数值（非倍率），随手札等级同步增长；
 * - 抗性提升：减免比例（参照原版抗性提升药水，最终减免 = 等级 × 每级增长，
 *   应用时取 min(减免, 服务器配置上限)）；
 * - 生效规则：allow_multiple_letters 关闭时各属性独立取最大；开启时全部相加；
 *   这些属性不受“魔法转化”附魔影响（附魔手札同样提供防御属性）。
 */
public class LetterStats {

    /** 单张手札的防御属性条目（HUD 逐条显示用）。 */
    public static class Entry {
        public final ItemStack stack;
        public final int level;
        public final double damageMultiplier;
        public final double armor;
        public final double toughness;
        public final double resistance;
        public final boolean enchanted;
        public final ResourceLocation damageType;

        public Entry(ItemStack stack, int level, double damageMultiplier, double armor,
                     double toughness, double resistance, boolean enchanted, ResourceLocation damageType) {
            this.stack = stack;
            this.level = level;
            this.damageMultiplier = damageMultiplier;
            this.armor = armor;
            this.toughness = toughness;
            this.resistance = resistance;
            this.enchanted = enchanted;
            this.damageType = damageType;
        }
    }

    private LetterStats() {
    }

    /** 护甲/韧性属性常驻加成的固定 id（叠加与移除使用同一个）。 */
    private static final ResourceLocation ARMOR_BONUS_ID =
            ResourceLocation.fromNamespaceAndPath("enchanter_letter", "armor_bonus");

    /**
     * 计算单张手札的防御属性（物品描述 tooltip 用）。
     * 非手札或非玩家可携带物品返回 null。
     */
    public static Entry of(ItemStack stack, Level level) {
        return fromStack(stack, level);
    }

    /**
     * 把玩家携带手札提供的“护甲值 / 护甲韧性”以原版属性形式常驻叠加到实体属性上
     * （服务端周期调用；属性变化会自动同步到客户端，玩家护甲条与
     * Overloaded Armor Bar 等模组均可正常显示）。
     */
    public static void syncArmorModifiers(Player player) {
        syncArmorModifiers((LivingEntity) player);
    }

    /** 把手札护甲/韧性以属性形式叠加到任意实体（玩家/生物）上。 */
    public static void syncArmorModifiers(LivingEntity player) {
        double[] bonus = armorToughnessBonus(player, player.level());
        setModifier(player, Attributes.ARMOR, bonus[0]);
        setModifier(player, Attributes.ARMOR_TOUGHNESS, bonus[1]);
    }

    private static void setModifier(LivingEntity player, Holder<Attribute> attribute, double value) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return;
        inst.removeModifier(ARMOR_BONUS_ID);
        if (value > 0) {
            inst.addTransientModifier(new AttributeModifier(ARMOR_BONUS_ID,
                    value, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /**
     * 收集玩家所有手札的防御属性条目（背包/盔甲/副手/Curios 饰品栏/合订本内容物）。
     * 按“除转化状态外完全相同（同物品/伤害倍率/伤害类型/附魔状态）”去重；
     * 手札任一属性 > 0 即视为参与结算。
     */
    public static List<Entry> collectEntries(LivingEntity player, Level level) {
        List<Entry> result = new ArrayList<>();
        boolean dedup = !ModConfig.getInstance().stackingRules.allowSameLetters;
        Set<String> seen = new HashSet<>();
        scanAllSlots(player, stack -> {
            Entry entry = fromStack(stack, level);
            if (entry == null) return;
            if (entry.damageMultiplier <= 0 && entry.armor <= 0 && entry.toughness <= 0 && entry.resistance <= 0) {
                return;
            }
            Optional<UUID> bound = stack.get(ModDataComponents.BOUND_PLAYER.get());
            if (bound != null && bound.isPresent() && !bound.get().equals(player.getUUID())) {
                // 背包内存在不属于此玩家的绑定手札：不生效（交由服务器 tick 定期弹出）
                return;
            }
            if (dedup) {
                String key = stack.getItem() + "|" + entry.damageMultiplier + "|" + entry.damageType + "|" + entry.enchanted;
                if (!seen.add(key)) return;
            }
            result.add(entry);
        });
        return result;
    }

    /**
     * 有效护甲值：allow_multiple_letters 关闭时取所有手札中最大的护甲值，开启时全部相加。
     */
    public static double effectiveArmor(LivingEntity player, Level level) {
        List<Entry> entries = collectEntries(player, level);
        if (entries.isEmpty()) return 0;
        if (ModConfig.getInstance().stackingRules.allowMultipleLetters) {
            double total = 0;
            for (Entry e : entries) total += e.armor;
            return total;
        }
        double max = 0;
        for (Entry e : entries) max = Math.max(max, e.armor);
        return max;
    }

    /**
     * 有效护甲韧性：allow_multiple_letters 关闭时取所有手札中最大的韧性值，开启时全部相加。
     */
    public static double effectiveToughness(LivingEntity player, Level level) {
        List<Entry> entries = collectEntries(player, level);
        if (entries.isEmpty()) return 0;
        if (ModConfig.getInstance().stackingRules.allowMultipleLetters) {
            double total = 0;
            for (Entry e : entries) total += e.toughness;
            return total;
        }
        double max = 0;
        for (Entry e : entries) max = Math.max(max, e.toughness);
        return max;
    }

    /**
     * 有效抗性减免比例：allow_multiple_letters 关闭时取所有手札中最大的减免，开启时全部相加。
     */
    public static double effectiveResistance(LivingEntity player, Level level) {
        List<Entry> entries = collectEntries(player, level);
        if (entries.isEmpty()) return 0;
        if (ModConfig.getInstance().stackingRules.allowMultipleLetters) {
            double total = 0;
            for (Entry e : entries) total += e.resistance;
            return total;
        }
        double max = 0;
        for (Entry e : entries) max = Math.max(max, e.resistance);
        return max;
    }

    /**
     * 护甲 / 护甲韧性加成（用于护甲减伤计算时叠加到实体属性上）。
     * 返回 {护甲, 韧性}。
     */
    public static double[] armorToughnessBonus(LivingEntity player, Level level) {
        return new double[]{effectiveArmor(player, level), effectiveToughness(player, level)};
    }

    private static Entry fromStack(ItemStack stack, Level level) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();
        ModConfig config = ModConfig.getInstance();
        boolean enchanted = ModEnchantments.hasMagicConversion(stack);
        ResourceLocation type = ModEnchantments.getEffectiveDamageType(stack);

        if (item instanceof TimeMagicLetterItem time) {
            int lv = time.getLevel(stack, level);
            ModConfig.TimeLetterConfig cfg = config.timeLetter;
            return new Entry(stack, lv, TimeMagicLetterItem.getMultiplier(level),
                    lv * cfg.armorGrowthPerLevel, lv * cfg.toughnessGrowthPerLevel, lv * cfg.resistanceGrowthPerLevel,
                    enchanted, type);
        }
        if (item instanceof TravelMagicLetterItem) {
            ModConfig.TravelLetterConfig cfg = config.travelLetter;
            double walkLevels = Math.floor(TravelMagicLetterItem.getWalkDistance(stack) / cfg.walkDistancePerLevel);
            double flyLevels = Math.floor(TravelMagicLetterItem.getFlyDistance(stack) / cfg.flyDistancePerLevel);
            return new Entry(stack, (int) (walkLevels + flyLevels),
                    walkLevels * cfg.walkGrowthPerLevel + flyLevels * cfg.flyGrowthPerLevel,
                    walkLevels * cfg.armorGrowthPerLevel + flyLevels * cfg.armor2GrowthPerLevel,
                    walkLevels * cfg.toughnessGrowthPerLevel + flyLevels * cfg.toughness2GrowthPerLevel,
                    walkLevels * cfg.resistanceGrowthPerLevel + flyLevels * cfg.resistance2GrowthPerLevel,
                    enchanted, type);
        }
        if (item instanceof HeroMagicLetterItem hero) {
            int lv = hero.getLevel(stack);
            ModConfig.HeroLetterConfig cfg = config.heroLetter;
            boolean high = lv >= cfg.highLevelStart;
            return new Entry(stack, lv,
                    lv * (high ? cfg.growthHighLevels : cfg.growthLowLevels),
                    lv * (high ? cfg.armor2GrowthPerLevel : cfg.armorGrowthPerLevel),
                    lv * (high ? cfg.toughness2GrowthPerLevel : cfg.toughnessGrowthPerLevel),
                    lv * (high ? cfg.resistance2GrowthPerLevel : cfg.resistanceGrowthPerLevel),
                    enchanted, type);
        }
        if (item instanceof StageMagicLetterItem stage) {
            int lv = stage.getLevel(stack);
            ModConfig.StageLettersConfig cfg = config.stageLetters;
            // 每张阶段手札独立配置（查表），与等级/阶段号解耦；未配置时回退原默认值
            return new Entry(stack, lv, stage.getMultiplier(stack),
                    stageValue(cfg.armorValues, lv, lv * 2.0),
                    stageValue(cfg.toughnessValues, lv, lv * 1.0),
                    stageValue(cfg.resistanceValues, lv, 0.0),
                    enchanted, type);
        }
        if (item instanceof ExperienceMagicLetterItem ml) return simple(stack, level, ml, config.experienceLetter.armorGrowthPerLevel, config.experienceLetter.toughnessGrowthPerLevel, config.experienceLetter.resistanceGrowthPerLevel, enchanted, type);
        if (item instanceof KillMagicLetterItem ml) return simple(stack, level, ml, config.killLetter.armorGrowthPerLevel, config.killLetter.toughnessGrowthPerLevel, config.killLetter.resistanceGrowthPerLevel, enchanted, type);
        if (item instanceof FishingMagicLetterItem ml) return simple(stack, level, ml, config.fishingLetter.armorGrowthPerLevel, config.fishingLetter.toughnessGrowthPerLevel, config.fishingLetter.resistanceGrowthPerLevel, enchanted, type);
        if (item instanceof TreasureMagicLetterItem ml) return simple(stack, level, ml, config.treasureLetter.armorGrowthPerLevel, config.treasureLetter.toughnessGrowthPerLevel, config.treasureLetter.resistanceGrowthPerLevel, enchanted, type);
        if (item instanceof TenacityMagicLetterItem ml) return simple(stack, level, ml, config.tenacityLetter.armorGrowthPerLevel, config.tenacityLetter.toughnessGrowthPerLevel, config.tenacityLetter.resistanceGrowthPerLevel, enchanted, type);
        if (item instanceof CustomMagicLetterItem custom) {
            return new Entry(stack, 0, custom.getMultiplier(stack),
                    CustomMagicLetterItem.getCustomArmor(stack),
                    CustomMagicLetterItem.getCustomToughness(stack),
                    CustomMagicLetterItem.getCustomResistance(stack),
                    enchanted, type);
        }
        return null;
    }

    /** 阶段手札查表取值：数组为空、索引越界或值为 null 时回退到默认值。 */
    private static double stageValue(List<Double> values, int stage, double fallback) {
        if (values == null || stage < 1 || stage > values.size()) return fallback;
        Double v = values.get(stage - 1);
        return v == null ? fallback : v;
    }

    private static Entry simple(ItemStack stack, Level level, MagicLetterItem ml,
                                double armorGrowth, double toughnessGrowth, double resistanceGrowth,
                                boolean enchanted, ResourceLocation type) {
        int lv = ml.getLevel(stack, level);
        return new Entry(stack, lv, ml.getMultiplier(stack, level),
                lv * armorGrowth, lv * toughnessGrowth, lv * resistanceGrowth,
                enchanted, type);
    }

    private static void scanAllSlots(LivingEntity entity, java.util.function.Consumer<ItemStack> consumer) {
        if (entity instanceof Player player) {
            var inv = player.getInventory();
            for (var stack : inv.items) scanStack(entity, stack, consumer);
            for (var stack : inv.armor) scanStack(entity, stack, consumer);
            for (var stack : inv.offhand) scanStack(entity, stack, consumer);
        } else {
            for (var stack : entity.getArmorSlots()) scanStack(entity, stack, consumer);
            for (var stack : entity.getHandSlots()) scanStack(entity, stack, consumer);
        }
        for (var stack : CuriosIntegration.getCuriosStacks(entity)) scanStack(entity, stack, consumer);
    }

    private static void scanStack(LivingEntity entity, ItemStack stack, java.util.function.Consumer<ItemStack> consumer) {
        if (stack.isEmpty()) return;
        consumer.accept(stack);
        if (stack.getItem() instanceof LetterBinderItem) {
            BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
            for (ItemStack inner : contents.itemsCopy()) {
                if (!inner.isEmpty()) consumer.accept(inner);
            }
        }
    }
}
