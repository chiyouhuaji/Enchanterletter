package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.util.EnchantmentFilterUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 附魔获取途径（Forge 1.20.1，配置文件 letter_enchanted.enabled）：
 * 该方法同时服务附魔台（isBook=false）与随机战利品/宝藏（isBook=true）。
 * 附魔台：永远不生成模组三个附魔；宝藏：仅当配置开启时生成。
 */
@Mixin(EnchantmentHelper.class)
public class EnchantmentTableFilterMixin {

    @Inject(method = "getAvailableEnchantmentResults", at = @At("RETURN"), cancellable = true)
    private static void enchanterLetter$filterTableResults(int level, ItemStack stack, boolean isBook,
                                                           CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
        List<EnchantmentInstance> list = cir.getReturnValue();
        if (list == null || list.isEmpty()) return;
        boolean tablePath = !isBook;
        list.removeIf(instance -> {
            if (!EnchantmentFilterUtil.isModEnchantment(instance.enchantment)) return false;
            return tablePath || !EnchantmentFilterUtil.isEnabled();
        });
    }
}
