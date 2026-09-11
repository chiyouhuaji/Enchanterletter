package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 临时手札合订本：手札合订本的时限替代品。
 *
 * 与手札合订本的区别：
 * - 默认不生效；持有者获得“魔法激活”效果期间才让内部手札参与手札效果结算；
 * - 不允许取出/存放内容物（保留容量数据但禁止合订本交互）；
 * - 获得时自动给持有者附加“魔法激活”效果，时长由物品 NBT 配置；
 * - 效果消失时自动清除身上所有临时手札合订本；
 * - 掉落物生成时直接销毁，不允许成为掉落物。
 */
public class TemporaryLetterBinderItem extends BundleItem {
    public static final String TEMP_SECONDS_KEY = "temporary_binder_seconds";
    public static final String GRANTED_KEY = "temporary_binder_granted";

    public TemporaryLetterBinderItem(Properties properties) {
        super(properties);
    }

    public static boolean isTemporaryBinder(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof TemporaryLetterBinderItem;
    }

    /** 读取 NBT 配置的魔法激活时长（秒）。 */
    public static int getRemainingSeconds(ItemStack stack) {
        return ModDataComponents.getGrowthInt(stack, TEMP_SECONDS_KEY, 600);
    }

    /** 写入 NBT 配置的魔法激活时长（秒）。 */
    public static void setRemainingSeconds(ItemStack stack, int seconds) {
        ModDataComponents.setGrowthInt(stack, TEMP_SECONDS_KEY, Math.max(1, seconds));
    }

    /** 是否已发放过魔法激活效果（granted 记录为 1）。 */
    public static boolean isGranted(ItemStack stack) {
        return ModDataComponents.getGrowthInt(stack, GRANTED_KEY, 0) != 0;
    }

    /** 标记已发放魔法激活效果（granted 翻转为 1，防止重复发放）。 */
    public static void setGranted(ItemStack stack) {
        ModDataComponents.setGrowthInt(stack, GRANTED_KEY, 1);
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

    /** 临时合订本无附魔意义。 */
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    /** 禁止一切合订本交互（不能取出/存放内容物）。 */
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

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 0;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

}
