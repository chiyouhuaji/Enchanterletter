package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.enchantment.ModEnchantments;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class HandItemUseMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (level.isClientSide && player.isCreative()) {
            ItemStack self = (ItemStack) (Object) this;
            if (self.getItem() == Items.ENCHANTED_BOOK) {
                if (ModEnchantments.hasMagicConversion(self)) {
                    Minecraft.getInstance().setScreen(
                            new cn.autoforged.enchanter_letter.screen.ConversionDamageTypeScreen(self));
                    cir.setReturnValue(InteractionResultHolder.success(self));
                }
            }
        }
    }
}
