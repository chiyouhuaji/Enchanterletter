package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import cn.autoforged.enchanter_letter.enchantment.ModEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public abstract class MagicLetterItem extends Item {
    protected static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("+#0.0%");
    protected static final DecimalFormat LEVEL_FORMAT = new DecimalFormat("#,###");
    protected static final DecimalFormat VALUE_FORMAT = new DecimalFormat("+#0.0#");
    /** 进度描述用：不带 "+" 前缀的浮点格式（进度描述内不出现 + 号）。 */
    protected static final DecimalFormat PROGRESS_FORMAT = new DecimalFormat("#,##0.0#");

    public MagicLetterItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, level, tooltipComponents, tooltipFlag);
        int lvl = getLevel(stack, level);
        double multiplier = getMultiplier(stack, level);
        tooltipComponents.add(Component.translatable("tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + ".level", LEVEL_FORMAT.format(lvl))
                .withStyle(ChatFormatting.GOLD));
                tooltipComponents.add(Component.translatable("tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + ".magic_damage_bonus", PERCENT_FORMAT.format(multiplier))
                .withStyle(ChatFormatting.RED));
        // 可增长手札（非阶段/非定制）：显示物品 NBT 记载的每级倍率（伤害/护甲/韧性/抗性），为 0 或空则隐藏
        double[] primary = getGrowthRatesPrimary(stack);
        if (primary != null) {
            addGrowthRateLine(tooltipComponents, "growth_damage", primary[0], PERCENT_FORMAT);
            addGrowthRateLine(tooltipComponents, "growth_armor", primary[1], VALUE_FORMAT);
            addGrowthRateLine(tooltipComponents, "growth_toughness", primary[2], VALUE_FORMAT);
            addGrowthRateLine(tooltipComponents, "growth_resistance", primary[3], PERCENT_FORMAT);
            // 次级倍率（旅行=飞行部分、英雄=高等级部分）
            double[] secondary = getGrowthRatesSecondary(stack);
            if (secondary != null) {
                addGrowthRateLine(tooltipComponents, "growth_damage2", secondary[0], PERCENT_FORMAT);
                addGrowthRateLine(tooltipComponents, "growth_armor2", secondary[1], VALUE_FORMAT);
                addGrowthRateLine(tooltipComponents, "growth_toughness2", secondary[2], VALUE_FORMAT);
                addGrowthRateLine(tooltipComponents, "growth_resistance2", secondary[3], PERCENT_FORMAT);
            }
            // 升级所需次数（取自物品 NBT 每级参数，非正不显示）
            double required = getRequiredPerLevelForTooltip(stack);
            if (required > 0 && Double.isFinite(required)) {
                tooltipComponents.add(Component.translatable("tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + ".required_per_level", LEVEL_FORMAT.format(required))
                        .withStyle(ChatFormatting.DARK_GREEN));
            }
        }
        // 防御属性：护甲值 / 护甲韧性（直接数值）、抗性提升（减免比例）
        LetterStats.Entry stats = LetterStats.of(stack, level);
        if (stats != null) {
            if (stats.armor > 0) {
                tooltipComponents.add(Component.translatable("tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + ".armor_bonus", VALUE_FORMAT.format(stats.armor))
                        .withStyle(ChatFormatting.YELLOW));
            }
            if (stats.toughness > 0) {
                tooltipComponents.add(Component.translatable("tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + ".toughness_bonus", VALUE_FORMAT.format(stats.toughness))
                        .withStyle(ChatFormatting.BLUE));
            }
            if (stats.resistance > 0) {
                tooltipComponents.add(Component.translatable("tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + ".resistance_bonus", PERCENT_FORMAT.format(stats.resistance))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        if (ModEnchantments.hasMagicConversion(stack)) {
            ResourceLocation effectiveType = ModEnchantments.getEffectiveDamageType(stack);
            tooltipComponents.add(Component.translatable("tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + ".conversion_damage_type", effectiveType.toString())
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
        // 非阶段/非定制手札：显示当前行为进度总计数（阶段手札与定制手札无计数值/无进度）
        if (!(this instanceof StageMagicLetterItem) && !(this instanceof CustomMagicLetterItem)) {
            String progress = getProgressRingText(stack, level);
            if (progress != null && !progress.isEmpty()) {
                tooltipComponents.add(Component.literal(progress).withStyle(ChatFormatting.GRAY));
            }
        }
        Optional<UUID> owner = ModDataComponents.getBoundPlayer(stack);
        if (owner.isPresent()) {
            String displayName;
            try {
                var mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.getConnection() != null) {
                    var playerInfo = mc.getConnection().getPlayerInfo(owner.get());
                    if (playerInfo != null) {
                        displayName = playerInfo.getProfile().getName();
                    } else {
                        displayName = owner.get().toString();
                    }
                } else {
                    displayName = owner.get().toString();
                }
            } catch (Throwable e) {
                displayName = owner.get().toString();
            }
            tooltipComponents.add(Component.translatable("tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + ".bound", displayName)
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 40;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!(livingEntity instanceof Player player)) return stack;
        if (!level.isClientSide) {
            Optional<UUID> currentOwner = ModDataComponents.getBoundPlayer(stack);
            if (currentOwner.isPresent() && currentOwner.get().equals(player.getUUID())) {
                ModDataComponents.removeBoundPlayer(stack);
                player.displayClientMessage(Component.translatable("message." + UsefulMagicEnchanterLetterMod.MOD_ID + ".unbound"), true);
            } else {
                ModDataComponents.setBoundPlayer(stack, player.getUUID());
                player.displayClientMessage(Component.translatable("message." + UsefulMagicEnchanterLetterMod.MOD_ID + ".bound"), true);
            }
        }
        return stack;
    }

    public abstract int getLevel(ItemStack stack);

    public abstract double getMultiplier(ItemStack stack);

    /**
     * 带世界上下文的等级（默认委托给仅依赖物品的版本）。
     * 时间手札等按世界时间计算进度的手札需重写此方法。
     */
    public int getLevel(ItemStack stack, Level level) {
        return getLevel(stack);
    }

    /**
     * 带世界上下文的倍率（默认委托给仅依赖物品的版本）。
     * 时间手札等按世界时间计算进度的手札需重写此方法。
     */
    public double getMultiplier(ItemStack stack, Level level) {
        return getMultiplier(stack);
    }

    /**
     * 可增长手札（非阶段/非定制）的“主倍率组”（tooltip 用）：{伤害倍率, 护甲倍率, 韧性倍率, 抗性倍率}，
     * 取自物品 NBT 记载的每级参数；非可增长手札返回 null。
     */
    protected double[] getGrowthRatesPrimary(ItemStack stack) {
        if (this instanceof ExperienceMagicLetterItem m) {
            return new double[]{ m.getGrowthPerLevel(stack), m.getArmorGrowth(stack), m.getToughnessGrowth(stack), m.getResistanceGrowth(stack) };
        }
        if (this instanceof KillMagicLetterItem m) {
            return new double[]{ m.getGrowthPerLevel(stack), m.getArmorGrowth(stack), m.getToughnessGrowth(stack), m.getResistanceGrowth(stack) };
        }
        if (this instanceof FishingMagicLetterItem m) {
            return new double[]{ m.getGrowthPerLevel(stack), m.getArmorGrowth(stack), m.getToughnessGrowth(stack), m.getResistanceGrowth(stack) };
        }
        if (this instanceof TravelMagicLetterItem m) {
            return new double[]{ m.getWalkGrowth(stack), m.getArmorGrowth(stack), m.getToughnessGrowth(stack), m.getResistanceGrowth(stack) };
        }
        if (this instanceof TreasureMagicLetterItem m) {
            return new double[]{ m.getGrowthPerLevel(stack), m.getArmorGrowth(stack), m.getToughnessGrowth(stack), m.getResistanceGrowth(stack) };
        }
        if (this instanceof TimeMagicLetterItem m) {
            return new double[]{ m.getGrowthPerLevel(stack), m.getArmorGrowth(stack), m.getToughnessGrowth(stack), m.getResistanceGrowth(stack) };
        }
        if (this instanceof TenacityMagicLetterItem m) {
            return new double[]{ m.getGrowthPerLevel(stack), m.getArmorGrowth(stack), m.getToughnessGrowth(stack), m.getResistanceGrowth(stack) };
        }
        if (this instanceof HeroMagicLetterItem m) {
            return new double[]{ m.getGrowthLow(stack), m.getArmorGrowth(stack), m.getToughnessGrowth(stack), m.getResistanceGrowth(stack) };
        }
        return null;
    }

    /**
     * 可增长手札的“次级倍率组”（tooltip 用）：{次级伤害倍率, 次级护甲倍率, 次级韧性倍率, 次级抗性倍率}。
     * 仅旅行（飞行部分）与英雄（高等级部分）有次级倍率；无次级返回 null。
     */
    protected double[] getGrowthRatesSecondary(ItemStack stack) {
        if (this instanceof TravelMagicLetterItem m) {
            return new double[]{ m.getFlyGrowth(stack), m.getArmor2Growth(stack), m.getToughness2Growth(stack), m.getResistance2Growth(stack) };
        }
        if (this instanceof HeroMagicLetterItem m) {
            return new double[]{ m.getGrowthHigh(stack), m.getArmor2Growth(stack), m.getToughness2Growth(stack), m.getResistance2Growth(stack) };
        }
        return null;
    }

    /**
     * 可增长手札的“升级所需次数”（tooltip 用，取自物品 NBT 每级参数）。
     * 非可增长手札返回 -1（不显示）。
     */
    protected double getRequiredPerLevelForTooltip(ItemStack stack) {
        if (this instanceof ExperienceMagicLetterItem m) return m.getExpPerLevel(stack);
        if (this instanceof KillMagicLetterItem m) return m.getKillsPerLevel(stack);
        if (this instanceof FishingMagicLetterItem m) return m.getFishPerLevel(stack);
        if (this instanceof TravelMagicLetterItem m) return m.getWalkDistancePerLevel(stack);
        if (this instanceof TreasureMagicLetterItem m) return m.getOpensPerLevel(stack);
        if (this instanceof TimeMagicLetterItem m) return m.getSecondsPerLevel(stack);
        if (this instanceof TenacityMagicLetterItem m) return m.getDamagePerLevel(stack);
        if (this instanceof HeroMagicLetterItem m) return m.getVictoriesPerLevel(stack);
        return -1;
    }

    /** 追加一条倍率 tooltip 行；倍率为 0 或非有限（空）时隐藏。 */
    private static void addGrowthRateLine(List<Component> tooltipComponents, String key, double value, DecimalFormat format) {
        if (value <= 0 || !Double.isFinite(value)) return;
        tooltipComponents.add(Component.translatable("tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + "." + key, format.format(value))
                .withStyle(ChatFormatting.DARK_GREEN));
    }

    /**
     * 非阶段手札的行为进度总计数文本（tooltip 显示当前进度）。
     * 各子类按其行为计数显示；返回 null 表示不显示。
     */
    protected String getProgressRingText(ItemStack stack, Level level) {
        if (this instanceof KillMagicLetterItem) {
            return Component.translatable("tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + ".progress_kill",
                    LEVEL_FORMAT.format(KillMagicLetterItem.getKills(stack))).getString();
        }
        if (this instanceof TenacityMagicLetterItem) {
            return Component.translatable("tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + ".progress_tenacity",
                    PROGRESS_FORMAT.format(TenacityMagicLetterItem.getDamageTaken(stack))).getString();
        }
        if (this instanceof ExperienceMagicLetterItem) {
            return Component.translatable("tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + ".progress_experience",
                    LEVEL_FORMAT.format((long) ExperienceMagicLetterItem.getExperience(stack))).getString();
        }
        if (this instanceof FishingMagicLetterItem) {
            return Component.translatable("tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + ".progress_fish",
                    LEVEL_FORMAT.format(FishingMagicLetterItem.getFish(stack))).getString();
        }
        if (this instanceof TreasureMagicLetterItem) {
            return Component.translatable("tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + ".progress_treasure",
                    LEVEL_FORMAT.format(TreasureMagicLetterItem.getOpens(stack))).getString();
        }
        if (this instanceof HeroMagicLetterItem) {
            // 低级袭击 / 高级袭击：低级阶段（等级 < highLevelStart）所需胜利次数记为低级，超出部分记为高级；两者相加 = 总袭击胜利数
            int victories = HeroMagicLetterItem.getVictories(stack);
            int perLevel = Math.max(1, HeroMagicLetterItem.getVictoriesPerLevel(stack));
            int start = Math.max(1, HeroMagicLetterItem.getHighLevelStart(stack));
            long lowThreshold = (long) perLevel * (start - 1);
            long lowPart = Math.min(victories, lowThreshold);
            long highPart = victories - lowPart;
            return Component.translatable("tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + ".progress_hero",
                    LEVEL_FORMAT.format(lowPart), LEVEL_FORMAT.format(highPart)).getString();
        }
        if (this instanceof TravelMagicLetterItem) {
            return Component.translatable("tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + ".progress_travel",
                    PROGRESS_FORMAT.format(TravelMagicLetterItem.getWalkDistance(stack)),
                    PROGRESS_FORMAT.format(TravelMagicLetterItem.getFlyDistance(stack))).getString();
        }
        if (this instanceof TimeMagicLetterItem) {
            int t = getLevel(stack, level);
            return Component.translatable("tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + ".progress_time",
                    LEVEL_FORMAT.format(t)).getString();
        }
        return null;
    }
}
