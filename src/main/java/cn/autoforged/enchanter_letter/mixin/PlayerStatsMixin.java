package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.event.ModCommonEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 捕获一条鱼时给垂钓魔法手札计数（服务器统计 minecraft:fish_caught）。
 */
@Mixin(ServerPlayer.class)
public class PlayerStatsMixin {

    @Inject(method = "awardStat", at = @At("HEAD"))
    private void onAwardStat(Stat<?> stat, int amount, CallbackInfo ci) {
        if (stat.getType() == Stats.CUSTOM
                && stat.getValue() instanceof ResourceLocation rl
                && Stats.FISH_CAUGHT.equals(rl)) {
            ModCommonEvents.onFishCaught((ServerPlayer) (Object) this);
        }
    }
}
