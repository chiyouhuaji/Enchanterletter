package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.item.MagicLetterItem;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.20.1 移植：手札在合订本中占 1 格容量（重量 1），
 * 与 1.21.1 的 BundleContents.getWeight 覆盖等效（64 张手札装满合订本）。
 * 注意：1.20.1 的 BundleItem.getWeight 返回 int（不是 float），
 * 必须用 CallbackInfoReturnable<Integer>，否则 tooltip 渲染时
 * getReturnValueI 强转 Integer 抛 ClassCastException。
 */
@Mixin(BundleItem.class)
public class BundleWeightMixin {

    @Inject(method = "getWeight", at = @At("RETURN"), cancellable = true)
    private static void letterWeightOne(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.getItem() instanceof MagicLetterItem) {
            cir.setReturnValue(1);
        }
    }
}
