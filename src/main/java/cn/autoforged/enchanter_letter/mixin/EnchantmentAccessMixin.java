package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.item.LetterBinderItem;
import cn.autoforged.enchanter_letter.item.MagicLetterItem;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 附魔适用范围放行：
 * - 本模组添加/修改的附魔（魔法转化/光灵/魔法绑定）：不再限制可附魔物品，
 *   任意物品均可附魔（附魔台/铁砧/命令）；
 * - 消失诅咒（vanishing_curse，原版附魔附加设定）：仍只放行到本模组物品
 *   （手札/手札合订本）。
 */
@Mixin(Enchantment.class)
public class EnchantmentAccessMixin {

    @Inject(method = "canEnchant", at = @At("RETURN"), cancellable = true)
    private void enchanterLetter$allowEnchant(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        Enchantment self = (Enchantment) (Object) this;
        if (isOurModEnchantment(self)) {
            cir.setReturnValue(true);
            return;
        }
        if (isVanishingCurse(self)) {
            if (isModItem(stack)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "isSupportedItem", at = @At("RETURN"), cancellable = true)
    private void enchanterLetter$allowSupportedItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        Enchantment self = (Enchantment) (Object) this;
        if (isOurModEnchantment(self)) {
            cir.setReturnValue(true);
            return;
        }
        if (isVanishingCurse(self) && isModItem(stack)) {
            cir.setReturnValue(true);
        }
    }

    /** 是否为模组添加/修改的附魔（魔法转化/光灵/魔法绑定），这些附魔不再限制适用范围。 */
    private static boolean isOurModEnchantment(Enchantment self) {
        String key = getEnchantmentKey(self);
        return "enchantment.enchanter_letter.magic_conversion".equals(key)
                || "enchantment.enchanter_letter.glowing".equals(key)
                || "enchantment.enchanter_letter.magic_binding".equals(key);
    }

    private static boolean isVanishingCurse(Enchantment self) {
        return "enchantment.minecraft.vanishing_curse".equals(getEnchantmentKey(self));
    }

    private static String getEnchantmentKey(Enchantment self) {
        if (self.description().getContents() instanceof TranslatableContents contents) {
            return contents.getKey();
        }
        return "";
    }

    /** 是否为本模组物品（手札/手札合订本）。 */
    private static boolean isModItem(ItemStack stack) {
        return stack.getItem() instanceof MagicLetterItem || stack.getItem() instanceof LetterBinderItem;
    }
}
