package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.event.ModCommonEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onItemEntityHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (ModCommonEvents.shouldProtectItemDrop((ItemEntity) (Object) this, source)) {
            cir.setReturnValue(false);
        }
    }

    /** 手札掉落物每 tick 再次保证无重力且速度归零。 */
    @Inject(method = "tick", at = @At("TAIL"))
    private void keepLetterDropStill(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (ModCommonEvents.isOurLetter(self.getItem())) {
            self.setNoGravity(true);
            self.setDeltaMovement(0.0, 0.0, 0.0);
        }
    }
}
