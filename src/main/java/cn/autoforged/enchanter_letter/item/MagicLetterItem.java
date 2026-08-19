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
                    VALUE_FORMAT.format(TenacityMagicLetterItem.getDamageTaken(stack))).getString();
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
            return Component.translatable("tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + ".progress_hero",
                    LEVEL_FORMAT.format(HeroMagicLetterItem.getVictories(stack))).getString();
        }
        if (this instanceof TravelMagicLetterItem) {
            return Component.translatable("tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + ".progress_travel",
                    VALUE_FORMAT.format(TravelMagicLetterItem.getWalkDistance(stack)),
                    VALUE_FORMAT.format(TravelMagicLetterItem.getFlyDistance(stack))).getString();
        }
        if (this instanceof TimeMagicLetterItem) {
            int t = getLevel(stack, level);
            return Component.translatable("tooltip." + UsefulMagicEnchanterLetterMod.MOD_ID + ".progress_time",
                    LEVEL_FORMAT.format(t)).getString();
        }
        return null;
    }
}
