package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.util.EnchantmentFilterUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 宝藏附魔开关（配置文件 letter_enchanted.enabled）：
 * 覆盖 EnchantRandomlyFunction（钓鱼/未生成战利品宝箱的附魔书等）生成路径。
 * 关闭时移除模组附魔；开启时随机附加一个模组宝藏附魔。
 * 不修改原版 on_random_loot 标签，避免破坏原版箱子战利品。
 */
@Mixin(EnchantRandomlyFunction.class)
public class EnchantRandomlyFunctionMixin {

    @Inject(method = "run", at = @At("RETURN"), cancellable = true)
    private void enchanterLetter$filterTreasureResults(ItemStack stack, LootContext context,
                                                       CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (result == null || result.isEmpty()) return;
        EnchantmentFilterUtil.removeDisabledTreasureEnchantments(result);
        EnchantmentFilterUtil.maybeAddRandomTreasureEnchantment(result, context.getRandom(), context.getLevel().registryAccess());
        cir.setReturnValue(result);
    }
}
