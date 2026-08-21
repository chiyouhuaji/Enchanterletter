package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.util.EnchantmentFilterUtil;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在所有战利品表生成完成后统一处理附魔书：
 * 配置开启时，让光灵/魔法绑定/魔法转化有机会出现在任意本会产出附魔书的战利品表中。
 * 配置关闭时，移除已出现的模组附魔。
 */
@Mixin(LootTable.class)
public class LootTableMixin {

    @Inject(method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
            at = @At("RETURN"), cancellable = true)
    private void enchanterLetter$processEnchantedBooks(LootContext context,
                                                       CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir) {
        ObjectArrayList<ItemStack> list = cir.getReturnValue();
        if (list == null || list.isEmpty()) return;
        for (ItemStack stack : list) {
            if (stack == null || stack.isEmpty()) continue;
            EnchantmentFilterUtil.removeDisabledTreasureEnchantments(stack);
            EnchantmentFilterUtil.maybeAddRandomTreasureEnchantment(stack, context.getRandom());
        }
    }
}
