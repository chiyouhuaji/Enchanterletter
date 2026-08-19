package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.event.LetterEntityEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerDeathEffectsMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void enchanterLetter$onPlayerDeath(DamageSource source, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        LetterEntityEffects.handlePlayerDeath(self);
    }
}