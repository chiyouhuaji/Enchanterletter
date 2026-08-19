package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.event.ModCommonEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 玩家首次开启“未刷新”（仍有战利品表，即自然生成且未开过）的箱子时，给宝藏魔法手札计数。
 * 注意：必须用 @Accessor 而非 @Shadow —— Forge 1.20.1 的 SRG 运行时里 @Shadow 字段
 * 解析走 RemapperChain（该环境为空链），只有 @Accessor 的目标会经 refmap 映射（f_59605_）。
 */
@Mixin(RandomizableContainerBlockEntity.class)
public abstract class ChestLootMixin {

    @Accessor("lootTable")
    protected abstract ResourceLocation enchanterLetter$getLootTable();

    @Inject(method = "unpackLootTable(Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"))
    private void onUnpackLootTable(Player player, CallbackInfo ci) {
        if (player == null) return;
        if (!((Object) this instanceof ChestBlockEntity chest)) return;
        if (chest.getLevel() == null || chest.getLevel().isClientSide) return;
        if (enchanterLetter$getLootTable() == null) return;
        ModCommonEvents.onTreasureOpened(player);
    }
}