package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 鎵嬫湱鍚堣鏈紙1.20.1 绉绘锛夛細宸ヤ綔鏂瑰紡涓庡師鐗堟敹绾宠锛圔undle锛変竴鑷达紙NBT 瀛樺偍锛屾牸寮忎笌鍘熺増鍏煎锛? * "Items" 鍒楄〃 + "Weight" 鏁存暟锛夈€傚彧鍏佽瑁呭叆鏈ā缁勭殑鎵嬫湱锛圡agicLetterItem锛夈€? */
public class LetterBinderItem extends BundleItem {
    private static final int MAX_WEIGHT = 64;
    private static final String TAG_ITEMS = "Items";
    private static final String TAG_WEIGHT = "Weight";

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

        // 0) 鍐呭鐗╀负绌?+ 娼滆锛氳繘鍏ョ粦瀹?瑙ｇ粦鎿嶄綔锛堜笌鍏朵粬鎵嬫湱涓€鑷达紝闀挎寜鍙抽敭 2 绉掕Е鍙戯級
        if (getContentWeight(binder) == 0 && player.isCrouching()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(binder);
        }

        // 1) 瀛樺偍锛氬彟涓€鍙墜鎸佹湁鐗╁搧鏃讹紝鎶婄墿鍝佹斁鍏ュ悎璁㈡湰锛堝師鐗堝閲忛檺鍒讹級
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = player.getItemInHand(otherHand);
        if (canInsert(held)) {
            int inserted = tryInsert(binder, held);
            if (inserted > 0) {
                held.shrink(inserted);
                ensureIcon(binder);
                playSound(player, SoundEvents.BUNDLE_INSERT);
                return InteractionResultHolder.sidedSuccess(binder, level.isClientSide);
            }
            return InteractionResultHolder.fail(binder);
        }

        // 2) 娼滆鍙抽敭锛氬叏閮ㄦ斁鍑?
if (player.isCrouching()) {
            NonNullList<ItemStack> contents = readContents(binder);
            if (!contents.isEmpty()) {
                for (ItemStack inner : contents) {
                    player.drop(inner, false);
                }
                writeContents(binder, NonNullList.withSize(0, ItemStack.EMPTY));
                binder.getOrCreateTag().putInt(TAG_WEIGHT, 0);
                ensureIcon(binder);
                playSound(player, SoundEvents.BUNDLE_DROP_CONTENTS);
                return InteractionResultHolder.sidedSuccess(binder, level.isClientSide);
            }
            return InteractionResultHolder.fail(binder);
        }

        // 3) 鏅€氬彸閿細鍙栧嚭涓€浠跺埌鑳屽寘锛堣儗鍖呮弧鍒欐帀钀斤級
        if (getContentWeight(binder) > 0) {
            ItemStack taken = removeOne(binder);
            if (!taken.isEmpty()) {
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
        if (other.isEmpty()) {
            ItemStack taken = removeOne(stack);
            if (taken.isEmpty()) return false;
            ensureIcon(stack);
            slotAccess.set(taken);
            playSound(player, SoundEvents.BUNDLE_REMOVE_ONE);
            return true;
        }
        if (!canInsert(other)) return false;
        int inserted = tryInsert(stack, other);
        if (inserted > 0) {
            other.shrink(inserted);
            ensureIcon(stack);
            playSound(player, SoundEvents.BUNDLE_INSERT);
            return true;
        }
        return false;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        if (slot.getItem().isEmpty()) {
            ItemStack taken = removeOne(stack);
            if (taken.isEmpty()) return false;
            ensureIcon(stack);
            slot.safeInsert(taken);
            playSound(player, SoundEvents.BUNDLE_REMOVE_ONE);
            return true;
        }
        if (!canInsert(slot.getItem())) return false;
        int inserted = tryInsert(stack, slot.getItem());
        if (inserted > 0) {
            slot.getItem().shrink(inserted);
            ensureIcon(stack);
            playSound(player, SoundEvents.BUNDLE_INSERT);
            return true;
        }
        return false;
    }

    /** 鍚堣鏈彧鍏佽瑁呭叆鏈ā缁勭殑鎵嬫湱锛堜笉鍚悎璁㈡湰鏈韩涓庡叾浠栫墿鍝侊級銆?*/
    private static boolean canInsert(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof MagicLetterItem;
    }

    /** 褰撳墠鍐呭鐗╅噸閲忥紙0~64锛夈€?*/
    public static int getContentWeight(ItemStack stack) {
        if (!stack.hasTag()) return 0;
        return stack.getTag().getInt(TAG_WEIGHT);
    }

    /** 璇诲彇鍐呭鐗╁垪琛紙鍘熺増 NBT 鏍煎紡锛夈€?*/
    public static NonNullList<ItemStack> readContents(ItemStack stack) {
        NonNullList<ItemStack> list = NonNullList.create();
        if (!stack.hasTag()) return list;
        ListTag items = stack.getTag().getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = ItemStack.of(items.getCompound(i));
            if (!s.isEmpty()) list.add(s);
        }
        return list;
    }

    /** 鍐欏叆鍐呭鐗╁垪琛紙鍘熺増 NBT 鏍煎紡锛夈€?*/
    public static void writeContents(ItemStack stack, NonNullList<ItemStack> contents) {
        ListTag items = new ListTag();
        for (ItemStack s : contents) {
            if (!s.isEmpty()) {
                items.add(s.save(new CompoundTag()));
            }
        }
        stack.getOrCreateTag().put(TAG_ITEMS, items);
    }

    /** 鐗╁搧鍦ㄥ悎璁㈡湰涓殑閲嶉噺锛堜笌鍘熺増涓€鑷达細鎵嬫湱鍗犳弧 1 鏍硷級銆?*/
    private static int itemWeight(ItemStack stack) {
        if (stack.getItem() instanceof MagicLetterItem) return 1;
        if (stack.getItem() instanceof BundleItem) return 4;
        return Math.max(1, (int) Math.ceil(1.0F / stack.getMaxStackSize()));
    }

    /** 灏濊瘯鏀惧叆涓€浠剁墿鍝侊紙姣忔鏀惧叆 1 浠讹紝鎵嬫湱涓嶅彲鍫嗗彔锛夈€傝繑鍥炴斁鍏ユ暟閲忥紙0 鎴?1锛夈€?*/
    private static int tryInsert(ItemStack binder, ItemStack insert) {
        if (insert.isEmpty()) return 0;
        NonNullList<ItemStack> contents = readContents(binder);
        int weight = getContentWeight(binder);
        if (weight + itemWeight(insert) > MAX_WEIGHT) return 0;
        contents.add(insert.copy());
        writeContents(binder, contents);
        binder.getOrCreateTag().putInt(TAG_WEIGHT, weight + itemWeight(insert));
        return 1;
    }

    /** 鍙栧嚭涓€浠讹紙鎸夋斁鍏ラ『搴忥級銆?*/
    private static ItemStack removeOne(ItemStack binder) {
        NonNullList<ItemStack> contents = readContents(binder);
        if (contents.isEmpty()) return ItemStack.EMPTY;
        ItemStack taken = contents.remove(contents.size() - 1);
        writeContents(binder, contents);
        int weight = Math.max(0, getContentWeight(binder) - itemWeight(taken));
        binder.getOrCreateTag().putInt(TAG_WEIGHT, weight);
        return taken;
    }

    /**
     * 鍚屾鏄剧ず鍥炬爣锛氬唴瀹圭墿涓虹┖ 鈫?绌哄悎璁㈡湰锛坈ustom_model_data=0锛夛紱鏈夊唴瀹圭墿 鈫?鏈夋墜鏈悎璁㈡湰锛坈ustom_model_data=1锛夈€?     */
    public static void ensureIcon(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof LetterBinderItem)) return;
        int target = getContentWeight(stack) > 0 ? 1 : 0;
        stack.getOrCreateTag().putInt("CustomModelData", target);
    }

    private static void playSound(Player player, SoundEvent sound) {
        player.playSound(sound, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
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

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, level, tooltipComponents, tooltipFlag);
        Optional<UUID> owner = ModDataComponents.getBoundPlayer(stack);
        if (owner.isPresent()) {
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
