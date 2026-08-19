package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.event.ModCommonEvents;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public class PlayerMixin {

    @Inject(method = "giveExperiencePoints", at = @At("TAIL"))
    private void onGiveExperiencePoints(int amount, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        ModCommonEvents.onXpGained(self, amount);
    }
}
