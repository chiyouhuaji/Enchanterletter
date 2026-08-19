package cn.autoforged.enchanter_letter;

import cn.autoforged.enchanter_letter.config.ModConfig;
import cn.autoforged.enchanter_letter.integration.CuriosIntegration;
import cn.autoforged.enchanter_letter.item.ModItems;
import cn.autoforged.enchanter_letter.network.ConversionDamageTypePayload;
import cn.autoforged.enchanter_letter.network.LetterStoragePayloads;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(UsefulMagicEnchanterLetterMod.MOD_ID)
public class UsefulMagicEnchanterLetterMod {
    public static final String MOD_ID = "enchanter_letter";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public UsefulMagicEnchanterLetterMod(IEventBus modEventBus, ModContainer modContainer) {
        ModConfig.init();
        ModItems.ITEMS.register(modEventBus);
        ModItems.CREATIVE_TABS.register(modEventBus);
        ModDataComponents.DATA_COMPONENT_TYPES.register(modEventBus);
        ConversionDamageTypePayload.register(modEventBus);
        LetterStoragePayloads.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        LOGGER.info("Useful Magic Enchanter Letter (NeoForge) loaded");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CuriosIntegration.registerOurItems(ModItems.ALL_LETTERS);
            // 尽早遍历所有 Curios 槽位注入兼容（槽位数据就绪后由 ServerStarted/TagsUpdated 事件再次注入）
            CuriosIntegration.ensureAllSlotCompat();
        });
    }
}
