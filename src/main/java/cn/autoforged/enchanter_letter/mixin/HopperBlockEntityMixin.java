package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.event.ModCommonEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 阻止漏斗 / 漏斗矿车自动吸入手札掉落物。
 */
@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin {

    @Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z", at = @At("HEAD"), cancellable = true, remap = true)
    private static void blockLetterSuction(net.minecraft.world.Container container,
                                           net.minecraft.world.entity.item.ItemEntity itemEntity,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (itemEntity != null && ModCommonEvents.isOurLetter(itemEntity.getItem())) {
            cir.setReturnValue(false);
        }
    }
}
