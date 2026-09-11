package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.config.ModConfig;
import cn.autoforged.enchanter_letter.effect.ModMagicActivation;
import cn.autoforged.enchanter_letter.effect.ModMagicObstruction;
import cn.autoforged.enchanter_letter.enchantment.ModEnchantments;
import cn.autoforged.enchanter_letter.integration.AccessoriesIntegration;
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

    private static final ResourceLocation ARMOR_BONUS_ID =
            ResourceLocation.fromNamespaceAndPath("enchanter_letter", "armor_bonus");

    public static Entry of(ItemStack stack, Level level) {
        return fromStack(stack, level);
    }

    public static void syncArmorModifiers(Player player) {
        syncArmorModifiers((LivingEntity) player);
    }

    public static void syncArmorModifiers(LivingEntity entity) {
        double[] bonus = armorToughnessBonus(entity, entity.level());
        setModifier(entity, Attributes.ARMOR, bonus[0]);
        setModifier(entity, Attributes.ARMOR_TOUGHNESS, bonus[1]);
    }

    private static void setModifier(LivingEntity entity, Holder<Attribute> attribute, double value) {
        AttributeInstance inst = entity.getAttribute(attribute);
        if (inst == null) return;
        inst.removeModifier(ARMOR_BONUS_ID);
        if (value > 0) {
            inst.addTransientModifier(new AttributeModifier(ARMOR_BONUS_ID,
                    value, AttributeModifier.Operation.ADD_VALUE));
        }
    }

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
            Optional<UUID> bound = stack.get(ModDataComponents.BOUND_PLAYER);
            if (bound != null && bound.isPresent() && !bound.get().equals(player.getUUID())) {
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

    public static double effectiveArmor(LivingEntity player, Level level) {
        if (ModMagicObstruction.blocksArmor(player)) return 0;
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

    public static double effectiveToughness(LivingEntity player, Level level) {
        if (ModMagicObstruction.blocksArmor(player)) return 0;
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

    public static double effectiveResistance(LivingEntity player, Level level) {
        if (ModMagicObstruction.blocksAll(player)) return 0;
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

    public static double[] armorToughnessBonus(LivingEntity player, Level level) {
        if (ModMagicObstruction.blocksArmor(player)) return new double[]{0, 0};
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
            return new Entry(stack, lv, time.getMultiplier(stack, level),
                    lv * TimeMagicLetterItem.getArmorGrowth(stack), lv * TimeMagicLetterItem.getToughnessGrowth(stack), lv * TimeMagicLetterItem.getResistanceGrowth(stack),
                    enchanted, type);
        }
        if (item instanceof TravelMagicLetterItem) {
            double walkLevels = Math.floor(TravelMagicLetterItem.getWalkDistance(stack) / TravelMagicLetterItem.getWalkDistancePerLevel(stack));
            double flyLevels = Math.floor(TravelMagicLetterItem.getFlyDistance(stack) / TravelMagicLetterItem.getFlyDistancePerLevel(stack));
            return new Entry(stack, (int) (walkLevels + flyLevels),
                    walkLevels * TravelMagicLetterItem.getWalkGrowth(stack) + flyLevels * TravelMagicLetterItem.getFlyGrowth(stack),
                    walkLevels * TravelMagicLetterItem.getArmorGrowth(stack) + flyLevels * TravelMagicLetterItem.getArmor2Growth(stack),
                    walkLevels * TravelMagicLetterItem.getToughnessGrowth(stack) + flyLevels * TravelMagicLetterItem.getToughness2Growth(stack),
                    walkLevels * TravelMagicLetterItem.getResistanceGrowth(stack) + flyLevels * TravelMagicLetterItem.getResistance2Growth(stack),
                    enchanted, type);
        }
        if (item instanceof HeroMagicLetterItem hero) {
            int lv = hero.getLevel(stack);
            boolean high = lv >= HeroMagicLetterItem.getHighLevelStart(stack);
            return new Entry(stack, lv,
                    lv * (high ? HeroMagicLetterItem.getGrowthHigh(stack) : HeroMagicLetterItem.getGrowthLow(stack)),
                    lv * (high ? HeroMagicLetterItem.getArmor2Growth(stack) : HeroMagicLetterItem.getArmorGrowth(stack)),
                    lv * (high ? HeroMagicLetterItem.getToughness2Growth(stack) : HeroMagicLetterItem.getToughnessGrowth(stack)),
                    lv * (high ? HeroMagicLetterItem.getResistance2Growth(stack) : HeroMagicLetterItem.getResistanceGrowth(stack)),
                    enchanted, type);
        }
        if (item instanceof StageMagicLetterItem stage) {
            int lv = stage.getLevel(stack);
            ModConfig.StageLettersConfig cfg = config.stageLetters;
            return new Entry(stack, lv, stage.getMultiplier(stack),
                    stageValue(cfg.armorValues, lv, lv * 2.0),
                    stageValue(cfg.toughnessValues, lv, lv * 1.0),
                    stageValue(cfg.resistanceValues, lv, 0.0),
                    enchanted, type);
        }
        if (item instanceof ExperienceMagicLetterItem ml) return simple(stack, level, ml, ExperienceMagicLetterItem.getArmorGrowth(stack), ExperienceMagicLetterItem.getToughnessGrowth(stack), ExperienceMagicLetterItem.getResistanceGrowth(stack), enchanted, type);
        if (item instanceof KillMagicLetterItem ml) return simple(stack, level, ml, KillMagicLetterItem.getArmorGrowth(stack), KillMagicLetterItem.getToughnessGrowth(stack), KillMagicLetterItem.getResistanceGrowth(stack), enchanted, type);
        if (item instanceof FishingMagicLetterItem ml) return simple(stack, level, ml, FishingMagicLetterItem.getArmorGrowth(stack), FishingMagicLetterItem.getToughnessGrowth(stack), FishingMagicLetterItem.getResistanceGrowth(stack), enchanted, type);
        if (item instanceof TreasureMagicLetterItem ml) return simple(stack, level, ml, TreasureMagicLetterItem.getArmorGrowth(stack), TreasureMagicLetterItem.getToughnessGrowth(stack), TreasureMagicLetterItem.getResistanceGrowth(stack), enchanted, type);
        if (item instanceof TenacityMagicLetterItem ml) return simple(stack, level, ml, TenacityMagicLetterItem.getArmorGrowth(stack), TenacityMagicLetterItem.getToughnessGrowth(stack), TenacityMagicLetterItem.getResistanceGrowth(stack), enchanted, type);
        if (item instanceof CustomMagicLetterItem custom) {
            return new Entry(stack, 0, custom.getMultiplier(stack),
                    CustomMagicLetterItem.getCustomArmor(stack),
                    CustomMagicLetterItem.getCustomToughness(stack),
                    CustomMagicLetterItem.getCustomResistance(stack),
                    enchanted, type);
        }
        return null;
    }

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
        for (var stack : AccessoriesIntegration.getAccessoriesStacks(entity)) scanStack(entity, stack, consumer);
    }

    private static void scanStack(LivingEntity entity, ItemStack stack, java.util.function.Consumer<ItemStack> consumer) {
        if (stack.isEmpty()) return;
        consumer.accept(stack);
        if (stack.getItem() instanceof LetterBinderItem) {
            BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
            for (ItemStack inner : contents.itemsCopy()) {
                if (!inner.isEmpty()) consumer.accept(inner);
            }
        } else if (stack.getItem() instanceof TemporaryLetterBinderItem && ModMagicActivation.isActive(entity)) {
            BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
            for (ItemStack inner : contents.itemsCopy()) {
                if (!inner.isEmpty()) consumer.accept(inner);
            }
        }
    }
}
