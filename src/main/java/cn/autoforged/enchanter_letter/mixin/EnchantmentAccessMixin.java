package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.item.LetterBinderItem;
import cn.autoforged.enchanter_letter.item.MagicLetterItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 附魔放行（Forge 1.20.1）：
 * - 魔法绑定（magic_binding）：canEnchant 已在注册时覆盖为任意物品均 true，无需放行；
 * - 消失诅咒（vanishing_curse）：允许附魔到本模组所有物品（手札/手札合订本）。
 * 光灵（glowing）的 canEnchant 已在注册时限定本模组物品，无需放行。
 * 通过 getDescriptionId() 识别附魔（Equivalent "enchantment.<ns>.<path>"）。
 */
@Mixin(Enchantment.class)
public class EnchantmentAccessMixin {

    @Inject(method = "canEnchant", at = @At("RETURN"), cancellable = true)
    private void enchanterLetter$allowEnchant(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        Enchantment self = (Enchantment) (Object) this;
        if ("enchantment.minecraft.vanishing_curse".equals(self.getDescriptionId())) {
            if (stack.getItem() instanceof MagicLetterItem || stack.getItem() instanceof LetterBinderItem) {
                cir.setReturnValue(true);
            }
        }
    }
}