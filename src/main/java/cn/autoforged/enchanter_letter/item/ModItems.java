package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModItems {
    public static final List<Supplier<? extends Item>> ALL_LETTERS = new ArrayList<>();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(UsefulMagicEnchanterLetterMod.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, UsefulMagicEnchanterLetterMod.MOD_ID);

    public static final DeferredItem<ExperienceMagicLetterItem> EXPERIENCE_MAGIC_LETTER =
            register("experience_magic_letter",
                    () -> new ExperienceMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<KillMagicLetterItem> KILL_MAGIC_LETTER =
            register("kill_magic_letter",
                    () -> new KillMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<FishingMagicLetterItem> FISHING_MAGIC_LETTER =
            register("fishing_magic_letter",
                    () -> new FishingMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<TravelMagicLetterItem> TRAVEL_MAGIC_LETTER =
            register("travel_magic_letter",
                    () -> new TravelMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<TreasureMagicLetterItem> TREASURE_MAGIC_LETTER =
            register("treasure_magic_letter",
                    () -> new TreasureMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<TimeMagicLetterItem> TIME_MAGIC_LETTER =
            register("time_magic_letter",
                    () -> new TimeMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<TenacityMagicLetterItem> TENACITY_MAGIC_LETTER =
            register("tenacity_magic_letter",
                    () -> new TenacityMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<HeroMagicLetterItem> HERO_MAGIC_LETTER =
            register("hero_magic_letter",
                    () -> new HeroMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<CustomMagicLetterItem> CUSTOM_MAGIC_LETTER =
            register("custom_magic_letter",
                    () -> new CustomMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredItem<LetterBinderItem> LETTER_BINDER =
            registerBinder("letter_binder",
                    () -> new LetterBinderItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                            // 必须带默认 BUNDLE_CONTENTS 组件，原版收纳袋的物品栏交互/悬浮小物品栏才生效
                            .component(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS,
                                    net.minecraft.world.item.component.BundleContents.EMPTY)));

    public static final DeferredItem<StageMagicLetterItem> STAGE_1_MAGIC_LETTER =
            register("stage_1_magic_letter",
                    () -> new StageMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON).fireResistant(), 1));

    public static final DeferredItem<StageMagicLetterItem> STAGE_2_MAGIC_LETTER =
            register("stage_2_magic_letter",
                    () -> new StageMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON).fireResistant(), 2));

    public static final DeferredItem<StageMagicLetterItem> STAGE_3_MAGIC_LETTER =
            register("stage_3_magic_letter",
                    () -> new StageMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant(), 3));

    public static final DeferredItem<StageMagicLetterItem> STAGE_4_MAGIC_LETTER =
            register("stage_4_magic_letter",
                    () -> new StageMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant(), 4));

    public static final DeferredItem<StageMagicLetterItem> STAGE_5_MAGIC_LETTER =
            register("stage_5_magic_letter",
                    () -> new StageMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant(), 5));

    public static final DeferredItem<StageMagicLetterItem> STAGE_6_MAGIC_LETTER =
            register("stage_6_magic_letter",
                    () -> new StageMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant(), 6));

    public static final DeferredItem<StageMagicLetterItem> STAGE_7_MAGIC_LETTER =
            register("stage_7_magic_letter",
                    () -> new StageMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant(), 7));

    public static final DeferredItem<StageMagicLetterItem> STAGE_8_MAGIC_LETTER =
            register("stage_8_magic_letter",
                    () -> new StageMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant(), 8));

    public static final DeferredItem<StageMagicLetterItem> STAGE_9_MAGIC_LETTER =
            register("stage_9_magic_letter",
                    () -> new StageMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), 9));

    public static final DeferredItem<StageMagicLetterItem> STAGE_10_MAGIC_LETTER =
            register("stage_10_magic_letter",
                    () -> new StageMagicLetterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), 10));

    private static <T extends Item> DeferredItem<T> register(String name, Supplier<T> supplier) {
        DeferredItem<T> item = ITEMS.register(name, supplier);
        ALL_LETTERS.add(item);
        return item;
    }

    /** 合订本不是手札，不参与手札扫描与 Curios 注册，仅注册物品。 */
    private static <T extends Item> DeferredItem<T> registerBinder(String name, Supplier<T> supplier) {
        return ITEMS.register(name, supplier);
    }

    public static final ResourceKey<CreativeModeTab> MAGIC_LETTERS_TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                    ResourceLocation.fromNamespaceAndPath(UsefulMagicEnchanterLetterMod.MOD_ID, "magic_letters"));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAGIC_LETTERS_TAB =
            CREATIVE_TABS.register("magic_letters",
                    () -> CreativeModeTab.builder()
                            .icon(() -> {
                                // 标签页图标使用"合订本_有手札"贴图（custom_model_data=1 切换）
                                ItemStack iconStack = new ItemStack(LETTER_BINDER.get());
                                iconStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
                                return iconStack;
                            })
                            .title(Component.translatable("itemGroup." + UsefulMagicEnchanterLetterMod.MOD_ID))
                            .displayItems((params, output) -> {
                                output.accept(EXPERIENCE_MAGIC_LETTER.get());
                                output.accept(KILL_MAGIC_LETTER.get());
                                // 新增手札排在阶段魔法手札之前
                                output.accept(FISHING_MAGIC_LETTER.get());
                                output.accept(TRAVEL_MAGIC_LETTER.get());
                                output.accept(TREASURE_MAGIC_LETTER.get());
                                output.accept(TIME_MAGIC_LETTER.get());
                                output.accept(TENACITY_MAGIC_LETTER.get());
                                output.accept(HERO_MAGIC_LETTER.get());
                                output.accept(CUSTOM_MAGIC_LETTER.get());
                                output.accept(LETTER_BINDER.get());
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
