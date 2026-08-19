package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.event.LetterEntityEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class MobDeathEffectsMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void enchanterLetter$onLivingDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player)) {
            LetterEntityEffects.handleMobDeath(self);
        }
    }
}