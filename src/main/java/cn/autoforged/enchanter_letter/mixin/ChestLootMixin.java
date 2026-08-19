package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.event.ModCommonEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 玩家首次开启“未刷新”（仍有战利品表，即自然生成且未开过）的箱子时，给宝藏魔法手札计数。
 */
@Mixin(RandomizableContainer.class)
public interface ChestLootMixin {

    @Inject(method = "unpackLootTable", at = @At("HEAD"))
    private void onUnpackLootTable(Player player, CallbackInfo ci) {
        if (player == null) return;
        if (!((Object) this instanceof ChestBlockEntity chest)) return;
        if (chest.getLevel() == null || chest.getLevel().isClientSide) return;
        ResourceKey<LootTable> lootTable = ((RandomizableContainer) this).getLootTable();
        if (lootTable == null) return;
        ModCommonEvents.onTreasureOpened(player);
    }
}
