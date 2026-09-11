package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModItems {
    public static final List<Supplier<? extends Item>> ALL_LETTERS = new ArrayList<>();

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, UsefulMagicEnchanterLetterMod.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, UsefulMagicEnchanterLetterMod.MOD_ID);

    public static final RegistryObject<ExperienceMagicLetterItem> EXPERIENCE_MAGIC_LETTER =
            register("experience_magic_letter",
                    () -> new ExperienceMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<KillMagicLetterItem> KILL_MAGIC_LETTER =
            register("kill_magic_letter",
                    () -> new KillMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<FishingMagicLetterItem> FISHING_MAGIC_LETTER =
            register("fishing_magic_letter",
                    () -> new FishingMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<TravelMagicLetterItem> TRAVEL_MAGIC_LETTER =
            register("travel_magic_letter",
                    () -> new TravelMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<TreasureMagicLetterItem> TREASURE_MAGIC_LETTER =
            register("treasure_magic_letter",
                    () -> new TreasureMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<TimeMagicLetterItem> TIME_MAGIC_LETTER =
            register("time_magic_letter",
                    () -> new TimeMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<TenacityMagicLetterItem> TENACITY_MAGIC_LETTER =
            register("tenacity_magic_letter",
                    () -> new TenacityMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<HeroMagicLetterItem> HERO_MAGIC_LETTER =
            register("hero_magic_letter",
                    () -> new HeroMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<CustomMagicLetterItem> CUSTOM_MAGIC_LETTER =
            register("custom_magic_letter",
                    () -> new CustomMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<LetterBinderItem> LETTER_BINDER =
            register("letter_binder",
                    () -> new LetterBinderItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<TemporaryLetterBinderItem> TEMPORARY_LETTER_BINDER =
            registerBinder("temporary_letter_binder",
                    () -> new TemporaryLetterBinderItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<StageMagicLetterItem> STAGE_1_MAGIC_LETTER =
            register("stage_1_magic_letter",
                    () -> new StageMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON).fireResistant(), 1));

    public static final RegistryObject<StageMagicLetterItem> STAGE_2_MAGIC_LETTER =
            register("stage_2_magic_letter",
                    () -> new StageMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON).fireResistant(), 2));

    public static final RegistryObject<StageMagicLetterItem> STAGE_3_MAGIC_LETTER =
            register("stage_3_magic_letter",
                    () -> new StageMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant(), 3));

    public static final RegistryObject<StageMagicLetterItem> STAGE_4_MAGIC_LETTER =
            register("stage_4_magic_letter",
                    () -> new StageMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant(), 4));

    public static final RegistryObject<StageMagicLetterItem> STAGE_5_MAGIC_LETTER =
            register("stage_5_magic_letter",
                    () -> new StageMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant(), 5));

    public static final RegistryObject<StageMagicLetterItem> STAGE_6_MAGIC_LETTER =
            register("stage_6_magic_letter",
                    () -> new StageMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant(), 6));

    public static final RegistryObject<StageMagicLetterItem> STAGE_7_MAGIC_LETTER =
            register("stage_7_magic_letter",
                    () -> new StageMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant(), 7));

    public static final RegistryObject<StageMagicLetterItem> STAGE_8_MAGIC_LETTER =
            register("stage_8_magic_letter",
                    () -> new StageMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant(), 8));

    public static final RegistryObject<StageMagicLetterItem> STAGE_9_MAGIC_LETTER =
            register("stage_9_magic_letter",
                    () -> new StageMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), 9));

    public static final RegistryObject<StageMagicLetterItem> STAGE_10_MAGIC_LETTER =
            register("stage_10_magic_letter",
                    () -> new StageMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), 10));

    private static <T extends Item> RegistryObject<T> register(String name, Supplier<T> supplier) {
        RegistryObject<T> item = ITEMS.register(name, supplier);
        ALL_LETTERS.add(item);
        return item;
    }

    /** 合订本/临时合订本不是手札，不加入 ALL_LETTERS。 */
    private static <T extends Item> RegistryObject<T> registerBinder(String name, Supplier<T> supplier) {
        return ITEMS.register(name, supplier);
    }

    public static final ResourceKey<CreativeModeTab> MAGIC_LETTERS_TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                    new ResourceLocation(UsefulMagicEnchanterLetterMod.MOD_ID, "magic_letters"));

    public static final RegistryObject<CreativeModeTab> MAGIC_LETTERS_TAB =
            CREATIVE_TABS.register("magic_letters",
                    () -> CreativeModeTab.builder()
                            .icon(() -> {
                                // 标签页图标使用"合订本_有手札"贴图（CustomModelData=1 切换）
                                ItemStack iconStack = new ItemStack(LETTER_BINDER.get());
                                iconStack.getOrCreateTag().putInt("CustomModelData", 1);
                                return iconStack;
                            })
                            .title(Component.translatable("itemGroup." + UsefulMagicEnchanterLetterMod.MOD_ID))
                            .displayItems((params, output) -> {
                                output.accept(EXPERIENCE_MAGIC_LETTER.get());
                                output.accept(KILL_MAGIC_LETTER.get());
                                output.accept(FISHING_MAGIC_LETTER.get());
                                output.accept(TRAVEL_MAGIC_LETTER.get());
                                output.accept(TREASURE_MAGIC_LETTER.get());
                                output.accept(TIME_MAGIC_LETTER.get());
                                output.accept(TENACITY_MAGIC_LETTER.get());
                                output.accept(HERO_MAGIC_LETTER.get());
                                output.accept(CUSTOM_MAGIC_LETTER.get());
                                output.accept(LETTER_BINDER.get());
                                output.accept(TEMPORARY_LETTER_BINDER.get());
                                output.accept(STAGE_1_MAGIC_LETTER.get());
                                output.accept(STAGE_2_MAGIC_LETTER.get());
                                output.accept(STAGE_3_MAGIC_LETTER.get());
                                output.accept(STAGE_4_MAGIC_LETTER.get());
                                output.accept(STAGE_5_MAGIC_LETTER.get());
                                output.accept(STAGE_6_MAGIC_LETTER.get());
                                output.accept(STAGE_7_MAGIC_LETTER.get());
                                output.accept(STAGE_8_MAGIC_LETTER.get());
                                output.accept(STAGE_9_MAGIC_LETTER.get());
                                output.accept(STAGE_10_MAGIC_LETTER.get());
                            })
                            .build());
}
