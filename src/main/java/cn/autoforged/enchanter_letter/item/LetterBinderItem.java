package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.item.MagicLetterItem;
import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 鎵嬫湱鍚堣鏈細宸ヤ綔鏂瑰紡涓庡師鐗堟敹绾宠锛圔undle锛変竴鑷达紝浣跨敤鍘熺増瀹归噺鏈哄埗锛堜笉鏀瑰彉瀹归噺锛夈€? * 鎵€鏈夋墜鏈潎涓?stacksTo(1)锛堢姝㈠爢鍙狅級锛屾瘡涓墜鏈湪鍘熺増瀹归噺涓崰婊?1 鏍硷紝
 * 涓€涓悎璁㈡湰鍘熺敓鍙斁鍏ユ渶澶?64 寮犱笉鍚岀殑鎵嬫湱锛堥噸閲忎笂闄?64锛夈€? * - 鍙抽敭 + 鍙︿竴鍙墜鎸佹湁鐗╁搧锛氭妸璇ョ墿鍝佸瓨鍏ュ悎璁㈡湰锛? * - 鍙抽敭锛堝彟涓€鍙墜涓虹┖锛夛細浠庡悎璁㈡湰涓彇鍑轰竴浠跺埌鑳屽寘锛堣儗鍖呮弧鍒欐帀钀斤級锛? * - 鍐呭鐗╀负绌烘椂娼滆 + 鍙抽敭锛堥暱鎸夛級锛氱粦瀹?瑙ｇ粦鍚堣鏈紙閫昏緫涓庡叾浠栨墜鏈竴鑷达級锛? * - 娼滆 + 鍙抽敭锛堝唴瀹圭墿闈炵┖锛夛細鎶婂悎璁㈡湰鍐呭叏閮ㄧ墿鍝佹斁鍑恒€? * - 鐗╁搧鏍忎氦浜掞細榧犳爣鎷垮彇鐗╁搧鍙抽敭鍚堣鏈牸瀛愭敹绾筹紱鏈嬁鍙栨椂鍙抽敭鎸夋斁鍏ラ『搴忓彇鍑轰竴浠躲€? * - 鎮诞鏄剧ず鍐呭鐗╁皬鐗╁搧鏍忥紙鍘熺増 BundleTooltip锛夈€? * 鏀惧叆鍏朵腑鐨勬墜鏈湪鎵弿鐜╁鐗╁搧鏍忔椂浼氳瑙嗕负鐢熸晥锛堣 ModCommonEvents.scanAllSlots锛夈€? * 鍙厑璁歌鍏ユ湰妯＄粍鐨勬墜鏈紙MagicLetterItem锛夛紝涓嶅厑璁歌鍏ュ悎璁㈡湰鏈韩鎴栧叾浠栫墿鍝併€? */
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

        // 0) 鍐呭鐗╀负绌?+ 娼滆锛氳繘鍏ョ粦瀹?瑙ｇ粦鎿嶄綔锛堜笌鍏朵粬鎵嬫湱涓€鑷达紝闀挎寜鍙抽敭 2 绉掕Е鍙戯級
        if (currentContents.isEmpty() && player.isCrouching()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(binder);
        }

        // 1) 瀛樺偍锛氬彟涓€鍙墜鎸佹湁鐗╁搧鏃讹紝鎶婄墿鍝佹斁鍏ュ悎璁㈡湰锛堝師鐗堝閲忛檺鍒讹級
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

        // 2) 娼滆鍙抽敭锛氬叏閮ㄦ斁鍑猴紙isCrouching 涓哄悓姝ュ埌鏈嶅姟绔殑娼滆鐘舵€侊級
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

        // 3) 鏅€氬彸閿細鍙栧嚭涓€浠跺埌鑳屽寘锛堣儗鍖呮弧鍒欐帀钀斤級
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

    /**
     * 鐗╁搧鏍忎氦浜掞細榧犳爣鎷垮彇鐗╁搧鏃跺彸閿悎璁㈡湰鏍煎瓙 鈫?鏀剁撼锛堝師鐗堝閲忛檺鍒讹級锛涙湭鎷垮彇鏃跺彸閿?鈫?鎸夋斁鍏ラ『搴忓彇鍑轰竴浠躲€?     */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot,
                                            ClickAction action, Player player, SlotAccess slotAccess) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        if (other.isEmpty()) {
            // 鏈嬁鍙栫墿鍝侊細鍙栧嚭涓€浠跺埌榧犳爣
            BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
            ItemStack taken = mutable.removeOne();
            if (taken == null || taken.isEmpty()) return false;
            stack.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
            ensureIcon(stack);
            slotAccess.set(taken);
            playSound(player, SoundEvents.BUNDLE_REMOVE_ONE);
            return true;
        }
        // 鎷垮彇鐗╁搧锛氫粎鍏佽鏀剁撼鏈ā缁勬墜鏈紙鍘熺増瀹归噺闄愬埗锛?
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

    /**
     * 鐗╁搧鏍忎氦浜掞細鎵嬫嬁鍚堣鏈彸閿叾浠栫墿鍝佹牸 鈫?鏀剁撼璇ユ牸鐗╁搧锛涘彸閿┖鏍?鈫?鍙栧嚭鍐呭鐗╂斁鍏ヨ鏍笺€?     */
    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        if (slot.getItem().isEmpty()) {
            // 绌烘牸锛氬彇鍑哄唴瀹圭墿鏀惧叆璇ユ牸锛堟寜鏀惧叆椤哄簭锛?
BundleContents.Mutable mutable = new BundleContents.Mutable(contents);
            ItemStack taken = mutable.removeOne();
            if (taken == null || taken.isEmpty()) return false;
            stack.set(DataComponents.BUNDLE_CONTENTS, mutable.toImmutable());
            ensureIcon(stack);
            slot.safeInsert(taken);
            playSound(player, SoundEvents.BUNDLE_REMOVE_ONE);
            return true;
        }
        // 闈炵┖鏍硷細浠呭厑璁告敹绾虫湰妯＄粍鎵嬫湱锛堝師鐗堝閲忛檺鍒讹級
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

    /** 鍚堣鏈彧鍏佽瑁呭叆鏈ā缁勭殑鎵嬫湱锛堜笉鍚悎璁㈡湰鏈韩涓庡叾浠栫墿鍝侊級銆?*/
    private static boolean canInsert(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof MagicLetterItem;
    }

    /**
     * 鍚屾鏄剧ず鍥炬爣锛氬唴瀹圭墿涓虹┖ 鈫?绌哄悎璁㈡湰锛坈ustom_model_data=0锛夛紱鏈夊唴瀹圭墿 鈫?鏈夋墜鏈悎璁㈡湰锛坈ustom_model_data=1锛夈€?     */
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

    /** 缁戝畾/瑙ｇ粦閫昏緫涓庡叾浠栨墜鏈竴鑷达細闀挎寜瀹屾垚鍚庯紝鏈嶅姟鍣ㄧ鍐欏叆/绉婚櫎 BOUND_PLAYER 缁勪欢銆?*/
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!(livingEntity instanceof Player player)) return stack;
        if (!level.isClientSide) {
            Optional<UUID> currentOwner = stack.get(ModDataComponents.BOUND_PLAYER.get());
            if (currentOwner != null && currentOwner.isPresent() && currentOwner.get().equals(player.getUUID())) {
                stack.remove(ModDataComponents.BOUND_PLAYER.get());
                player.displayClientMessage(Component.translatable("message." + UsefulMagicEnchanterLetterMod.MOD_ID + ".unbound"), true);
            } else {
                stack.set(ModDataComponents.BOUND_PLAYER.get(), Optional.of(player.getUUID()));
                player.displayClientMessage(Component.translatable("message." + UsefulMagicEnchanterLetterMod.MOD_ID + ".bound"), true);
            }
        }
        return stack;
    }

    /** 鏄剧ず缁戝畾鐘舵€侊紙涓庡叾浠栨墜鏈牸寮忎竴鑷达級銆?*/
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        Optional<UUID> owner = stack.get(ModDataComponents.BOUND_PLAYER.get());
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
