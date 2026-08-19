package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.item.MagicLetterItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 手札在收纳袋（手札合订本）中的占用大小视为“一格物品”（1/64 格），
 * 使原版 64 格容量可以存放最多 64 张手札（手札仍为 stacksTo(1)，不可堆叠）。
 * 仅调整重量判定，不修改任何原版存取（tryInsert/removeOne）逻辑。
 */
@Mixin(BundleContents.class)
public class BundleWeightMixin {

    @Inject(method = "getWeight", at = @At("RETURN"), cancellable = true)
    private static void treatLettersAsSingleSlot(ItemStack stack, CallbackInfoReturnable<Fraction> cir) {
        if (!stack.isEmpty() && stack.getItem() instanceof MagicLetterItem) {
            cir.setReturnValue(Fraction.getFraction(1, 64));
        }
    }
}
