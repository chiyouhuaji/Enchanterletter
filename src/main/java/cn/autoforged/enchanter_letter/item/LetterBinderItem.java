package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 手札合订本：工作方式与原版收纳袋（Bundle）一致。
 */
public class LetterBinderItem extends BundleItem {
    public LetterBinderItem(Properties properties) {
        super(properties);
    }

    /** 手札合订本无附魔意义：禁止一切附魔途径（附魔台/铁砧/命令），包含“魔法转化”。 */
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack binder = player.getItemInHand(hand);
        BundleContents currentContents = binder.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);

        // 内容物为空 + 潜行：进入绑定/解绑操作
        if (currentContents.isEmpty() && player.isCrouching()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(binder);
        }

        // 存储：另一只手持有物品时，把物品放入合订本
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = player.getItemInHand(otherHand);
        if (canInsert(held)) {
            BundleContents contents = binder.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
            BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
            int inserted = mutable.tryInsert(held);
            if (inserted > 0) {
                held.shrink(inserted);
                binder.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
                ensureIcon(binder);
                playSound(player, SoundEvents.BUNDLE_INSERT);
                return InteractionResultHolder.sidedSuccess(binder, level.isClientSide);
            }
            return InteractionResultHolder.fail(binder);
        }

        // 潜行右键：全部放出
        if (player.isCrouching()) {
            BundleContents contents = binder.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
            if (!contents.isEmpty()) {
                for (ItemStack inner : contents.itemsCopy()) {
                    player.drop(inner, false);
                }
                binder.set(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
                ensureIcon(binder);
                playSound(player, SoundEvents.BUNDLE_DROP_CONTENTS);
                return InteractionResultHolder.sidedSuccess(binder, level.isClientSide);
            }
            return InteractionResultHolder.fail(binder);
        }

        // 普通右键：取出一件到背包
        BundleContents contents = binder.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        if (!contents.isEmpty()) {
            BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
            ItemStack taken = mutable.removeOne();
            if (taken != null && !taken.isEmpty()) {
                binder.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
                ensureIcon(binder);
                if (!player.getInventory().add(taken)) {
                    player.drop(taken, false);
                }
                playSound(player, SoundEvents.BUNDLE_REMOVE_ONE);
                return InteractionResultHolder.sidedSuccess(binder, level.isClientSide);
            }
        }
        return InteractionResultHolder.fail(binder);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot,
                                            ClickAction action, Player player, SlotAccess slotAccess) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        if (other.isEmpty()) {
            BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
            ItemStack taken = mutable.removeOne();
            if (taken == null || taken.isEmpty()) return false;
            stack.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
            ensureIcon(stack);
            slotAccess.set(taken);
            playSound(player, SoundEvents.BUNDLE_REMOVE_ONE);
            return true;
        }
        if (!canInsert(other)) return false;
        BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
        int inserted = mutable.tryInsert(other);
        if (inserted > 0) {
            other.shrink(inserted);
            stack.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
            ensureIcon(stack);
            playSound(player, SoundEvents.BUNDLE_INSERT);
            return true;
        }
        return false;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        if (slot.getItem().isEmpty()) {
            BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
            ItemStack taken = mutable.removeOne();
            if (taken == null || taken.isEmpty()) return false;
            stack.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
            ensureIcon(stack);
            slot.safeInsert(taken);
            playSound(player, SoundEvents.BUNDLE_REMOVE_ONE);
            return true;
        }
        if (!canInsert(slot.getItem())) return false;
        BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
        int inserted = mutable.tryInsert(slot.getItem());
        if (inserted > 0) {
            slot.getItem().shrink(inserted);
            stack.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
            ensureIcon(stack);
            playSound(player, SoundEvents.BUNDLE_INSERT);
            return true;
        }
        return false;
    }

    private static boolean canInsert(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof MagicLetterItem;
    }

    public static void ensureIcon(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof LetterBinderItem)) return;
        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        int target = contents.isEmpty() ? 0 : 1;
        CustomModelData current = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (current == null || current.value() != target) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(target));
        }
    }

    private static void playSound(Player player, SoundEvent sound) {
        player.playSound(sound, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
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
            Optional<UUID> currentOwner = stack.get(ModDataComponents.BOUND_PLAYER);
            if (currentOwner != null && currentOwner.isPresent() && currentOwner.get().equals(player.getUUID())) {
                stack.remove(ModDataComponents.BOUND_PLAYER);
                player.displayClientMessage(Component.translatable("message." + UsefulMagicEnchanterLetterMod.MOD_ID + ".unbound"), true);
            } else {
                stack.set(ModDataComponents.BOUND_PLAYER, Optional.of(player.getUUID()));
                player.displayClientMessage(Component.translatable("message." + UsefulMagicEnchanterLetterMod.MOD_ID + ".bound"), true);
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        Optional<UUID> owner = stack.get(ModDataComponents.BOUND_PLAYER);
        if (owner != null && owner.isPresent()) {
            String displayName;
            try {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.getConnection() != null) {
                    var playerInfo = mc.getConnection().getPlayerInfo(owner.get());
                    displayName = playerInfo != null ? playerInfo.getProfile().getName() : owner.get().toString();
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
}
