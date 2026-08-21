package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.util.EnchantmentFilterUtil;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Stream;

/**
 * 宝藏附魔开关（配置文件 letter_enchanted.enabled）：
 * 覆盖 EnchantWithLevelsFunction 等通过 EnchantmentHelper.enchantItem
 * 生成随机战利品附魔的路径，生成后移除 treasure 开关已关闭的模组附魔。
 */
@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @Inject(method = "enchantItem(Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/item/ItemStack;ILjava/util/stream/Stream;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"), cancellable = true)
    private static void enchanterLetter$filterTreasureResults(RandomSource random, ItemStack stack, int level,
                                                              Stream<Holder<Enchantment>> possibleEnchantments,
                                                              CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (result == null || result.isEmpty()) return;
        EnchantmentFilterUtil.removeDisabledTreasureEnchantments(result);
        cir.setReturnValue(result);
    }
}
