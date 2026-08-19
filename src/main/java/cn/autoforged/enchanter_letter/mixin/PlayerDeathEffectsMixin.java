package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.event.LetterEntityEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 玩家死亡（die 入口、掉落生成之前）：
 * - 魔法绑定：物品移入保留池，重生返还（死亡不掉落）；
 * - 消失诅咒：物品死亡移除（不受 keepInventory 保护），合订本内非诅咒手札返还。
 */
@Mixin(Player.class)
public class PlayerDeathEffectsMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void enchanterLetter$onPlayerDeath(DamageSource source, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide) {
            LetterEntityEffects.handlePlayerDeath(player);
        }
    }
}
