package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.event.LetterEntityEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric 1.20.1 魔法绑定死亡保留兜底：
 * 直接拦截 PlayerInventory.dropAll（实际把背包/盔甲/副手物品转为掉落物的入口）。
 * 在掉落实处把魔法绑定物品移出掉落列表并加入保留池，重生后统一归还；
 * 同时兜底处理强制消失开启时的消失诅咒物品。
 */
@Mixin(Inventory.class)
public class PlayerInventoryMixin {

    @Shadow
    @Final
    public Player player;

    @Inject(method = "dropAll", at = @At("HEAD"))
    private void enchanterLetter$onDropAll(CallbackInfo ci) {
        Inventory self = (Inventory) (Object) this;
        LetterEntityEffects.handlePlayerInventoryDropAll(self.player);
    }
}
