package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.item.LetterBinderItem;
import cn.autoforged.enchanter_letter.item.MagicLetterItem;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 附魔放行：
 * - 魔法绑定（magic_binding）：不限制模组物品，任意物品均可附魔（附魔台/铁砧/命令）；
 * - 消失诅咒（vanishing_curse）：允许附魔到本模组所有物品（手札/手札合订本）。
 * 光灵（glowing）由 supported_items 标签控制，无需放行。
 */
@Mixin(Enchantment.class)
public class EnchantmentAccessMixin {

    @Inject(method = "canEnchant", at = @At("RETURN"), cancellable = true)
    private void enchanterLetter$allowEnchant(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        Enchantment self = (Enchantment) (Object) this;
        String key = "";
        if (self.description().getContents() instanceof TranslatableContents contents) {
            key = contents.getKey();
        }
        if ("enchantment.enchanter_letter.magic_binding".equals(key)) {
            // 魔法绑定：只允许附魔到本模组物品，或已注册到饰品模组（Curios/Accessories）物品标签的物品
            if (isModItem(stack) || hasAccessoryTag(stack)) {
                cir.setReturnValue(true);
            }
            return;
        }
        if ("enchantment.minecraft.vanishing_curse".equals(key)) {
            if (stack.getItem() instanceof MagicLetterItem || stack.getItem() instanceof LetterBinderItem) {
                cir.setReturnValue(true);
            }
        }
    }

    /** 是否为本模组物品（手札/手札合订本）。 */
    private static boolean isModItem(ItemStack stack) {
        return stack.getItem() instanceof MagicLetterItem || stack.getItem() instanceof LetterBinderItem;
    }

    /** 是否注册到饰品模组（Curios / Accessories）的物品标签，例如 #curios:*、#accessories:*。 */
    private static boolean hasAccessoryTag(ItemStack stack) {
        return stack.getItemHolder().tags()
                .anyMatch(tag -> {
                    String ns = tag.location().getNamespace();
                    return "curios".equals(ns) || "accessories".equals(ns);
                });
    }
}
