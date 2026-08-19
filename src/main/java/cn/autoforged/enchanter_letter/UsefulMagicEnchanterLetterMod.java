package cn.autoforged.enchanter_letter;

import cn.autoforged.enchanter_letter.config.ModConfig;
import cn.autoforged.enchanter_letter.enchantment.ModEnchantments;
import cn.autoforged.enchanter_letter.integration.CuriosIntegration;
import cn.autoforged.enchanter_letter.item.ModItems;
import cn.autoforged.enchanter_letter.network.ConversionDamageTypePayload;
import cn.autoforged.enchanter_letter.network.LetterStoragePayloads;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(UsefulMagicEnchanterLetterMod.MOD_ID)
public class UsefulMagicEnchanterLetterMod {
    public static final String MOD_ID = "enchanter_letter";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public UsefulMagicEnchanterLetterMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModConfig.init();
        ModItems.ITEMS.register(modEventBus);
        ModItems.CREATIVE_TABS.register(modEventBus);
        ModEnchantments.ENCHANTMENTS.register(modEventBus);
        ConversionDamageTypePayload.register();
        LetterStoragePayloads.register();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(cn.autoforged.enchanter_letter.event.ModCommonEvents.class);
        LOGGER.info("Useful Magic Enchanter Letter (NeoForge/Forge 1.20.1) loaded");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CuriosIntegration.registerOurItems(ModItems.ALL_LETTERS);
            CuriosIntegration.ensureAllSlotCompat();
        });
    }
}