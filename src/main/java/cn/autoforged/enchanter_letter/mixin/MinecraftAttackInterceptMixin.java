package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.network.LetterStoragePayloads;
import cn.autoforged.enchanter_letter.storage.LetterStorageClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * /letterstorage 截取模式：在“攻击”真正派发前拦截原版空手左键攻击。
 *
 * <p>原实现放在 {@code ClientTickEvents.START_CLIENT_TICK} 里消费 {@code keyAttack}，
 * 但 {@code Minecraft#handleKeybinds} 在每一渲染帧（frame）都会执行，而 START_CLIENT_TICK
 * 仅在 20 TPS 游戏刻（tick）触发——高帧率下攻击键击会在“未触发 tick 的帧”里被
 * {@code handleKeybinds} 直接派发为 {@code startAttack}，从而绕过拦截。</p>
 *
 * <p>这里直接拦截攻击的两个派发入口，保证无论单次点击还是长按破坏、无论帧率高低，
 * 截取模式下空手都无法攻击，并把准星指向的实体 id 上报服务端：</p>
 * <ul>
 *   <li>{@code startAttack}：单次点击（含攻击实体/开始破坏方块），取消派发并上报实体；</li>
 *   <li>{@code continueAttack}：长按继续破坏方块，直接取消。</li>
 * </ul>
 */
@Mixin(Minecraft.class)
public class MinecraftAttackInterceptMixin {

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void enchanterLetter$interceptStartAttack(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = (Minecraft) (Object) this;
        if (!shouldIntercept(mc)) return;
        sendCapture(mc);
        cir.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void enchanterLetter$interceptContinueAttack(boolean held, CallbackInfo cir) {
        Minecraft mc = (Minecraft) (Object) this;
        if (!shouldIntercept(mc)) return;
        cir.cancel();
    }

    /** 截取模式开启 且 主手为空（仅“空手”拦截）。 */
    private static boolean shouldIntercept(Minecraft mc) {
        if (!LetterStorageClient.isActive()) return false;
        var player = mc.player;
        return player != null && player.getMainHandItem().isEmpty();
    }

    /** 把准星指向的实体 id 上报服务端（无实体或指向自己则不上报）。 */
    private static void sendCapture(Minecraft mc) {
        Entity target = mc.crosshairPickEntity;
        if (target == null || target == mc.player) return;
        LetterStoragePayloads.sendCaptureToServer(target.getUUID());
    }
}
