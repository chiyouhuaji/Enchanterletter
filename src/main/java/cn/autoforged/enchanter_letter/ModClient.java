package cn.autoforged.enchanter_letter;

import cn.autoforged.enchanter_letter.event.ModClientEvents;
import cn.autoforged.enchanter_letter.integration.AccessoriesIntegration;
import net.fabricmc.api.ClientModInitializer;

public class ModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 1.20.1 修复：Accessories beta.48 渲染层硬依赖 Sodium API，
        // 为本模组物品注册“不渲染”渲染器，避免未装 Sodium 时崩溃。
        AccessoriesIntegration.initClient();
        AccessoriesIntegration.registerNoRenderClient();
        ModClientEvents.register();
    }
}
