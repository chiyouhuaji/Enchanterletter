package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.event.ModCommonEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 村庄袭击胜利（Raid 为村庄英雄名单添加玩家）时给英雄手札计数。
 */
@Mixin(Raid.class)
public class RaidMixin {

    @Inject(method = "addHeroOfTheVillage", at = @At("HEAD"))
    private void onAddHeroOfTheVillage(Entity entity, CallbackInfo ci) {
        if (entity instanceof Player player && !entity.level().isClientSide) {
            ModCommonEvents.onRaidVictory(player);
        }
    }
}
