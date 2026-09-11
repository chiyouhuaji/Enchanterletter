package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 临时手札合订本：由 /letterclone 命令生成，持有期间提供魔法激活效果。
 * 所有收纳袋交互均被禁用，仅作为容器数据用于激活临时手札扫描。
 */
public class TemporaryLetterBinderItem extends BundleItem {
    public static final String SECONDS_KEY = "temporary_binder_seconds";
    public static final int DEFAULT_SECONDS = 600;

    public TemporaryLetterBinderItem(Properties properties) {
        super(properties);
    }

    public static boolean isTemporaryBinder(ItemStack stack) {
        return stack != null && stack.getItem() instanceof TemporaryLetterBinderItem;
    }

    public static int getRemainingSeconds(ItemStack stack) {
        return ModDataComponents.getGrowthInt(stack, SECONDS_KEY, DEFAULT_SECONDS);
    }

    public static void setRemainingSeconds(ItemStack stack, int seconds) {
        ModDataComponents.setGrowthInt(stack, SECONDS_KEY, Math.max(0, seconds));
    }

    public static final String ACTIVATION_COUNT_KEY = "temporary_binder_activation_count";

    /** 读取激活计数：0=未激活，1=已激活一次，2=已激活两次（永久失效）。 */
    public static int getActivationCount(ItemStack stack) {
        return ModDataComponents.getGrowthInt(stack, ACTIVATION_COUNT_KEY, 0);
    }

    public static void setActivationCount(ItemStack stack, int count) {
        ModDataComponents.setGrowthInt(stack, ACTIVATION_COUNT_KEY, Math.max(0, count));
    }

    /** 每次激活时调用：激活计数 +1。 */
    public static void incrementActivation(ItemStack stack) {
        setActivationCount(stack, getActivationCount(stack) + 1);
    }

    /** 是否已永久失效：激活计数 >= 2（已被激活两次及以上，不可再被魔法激活启动）。 */
    public static boolean isPermanentlyDisabled(ItemStack stack) {
        return getActivationCount(stack) >= 2;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.fail(player.getItemInHand(hand));
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot,
                                            ClickAction action, Player player, SlotAccess slotAccess) {
        return false;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        return false;
    }

}