package cn.autoforged.enchanter_letter;

import cn.autoforged.enchanter_letter.command.LetterCommands;
import cn.autoforged.enchanter_letter.command.LetterDamageCommand;
import cn.autoforged.enchanter_letter.command.LetterEffectCommand;
import cn.autoforged.enchanter_letter.command.LetterStorageCommand;
import cn.autoforged.enchanter_letter.config.ModConfig;
import cn.autoforged.enchanter_letter.effect.ModEffects;
import cn.autoforged.enchanter_letter.enchantment.ModEnchantments;
import cn.autoforged.enchanter_letter.event.ModCommonEvents;
import cn.autoforged.enchanter_letter.integration.AccessoriesIntegration;
import cn.autoforged.enchanter_letter.integration.CuriosIntegration;
import cn.autoforged.enchanter_letter.item.ModItems;
import cn.autoforged.enchanter_letter.network.ConversionDamageTypePayload;
import cn.autoforged.enchanter_letter.network.LetterStoragePayloads;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsefulMagicEnchanterLetterMod implements ModInitializer {
    public static final String MOD_ID = "enchanter_letter";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModConfig.init();
        ModEffects.register();
        ModEnchantments.register();
        ModItems.registerItems();
        // 阻止 Accessories 右键抢先装备（canEquipFromUse=false），保证手札 shift+右键绑定可用
        AccessoriesIntegration.registerNoEquipFromUse();
        ConversionDamageTypePayload.register();
        LetterStoragePayloads.register();
        ModCommonEvents.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LetterDamageCommand.register(dispatcher);
            LetterCommands.register(dispatcher);
            LetterStorageCommand.register(dispatcher);
            LetterEffectCommand.register(dispatcher);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            CuriosIntegration.registerOurItems(ModItems.ALL_LETTERS);
            CuriosIntegration.ensureAllSlotCompat();
            AccessoriesIntegration.ensureAllSlotCompat(server.overworld());
        });
        LOGGER.info("Useful Magic Enchanter Letter (Fabric 1.20.1) loaded");
    }
}