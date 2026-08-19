package cn.autoforged.enchanter_letter.data;

import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import cn.autoforged.enchanter_letter.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = UsefulMagicEnchanterLetterMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModDataGenerators {
    private static final String[] CURIOS_SLOTS = {"curio", "hands", "ring", "belt", "necklace", "back", "head", "bracelet"};

    @SubscribeEvent
    public static void onGatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper helper = event.getExistingFileHelper();

        generator.addProvider(event.includeClient(), new ModItemModelGen(output, helper));
        generator.addProvider(event.includeServer(), new ModCuriosTagsGen(output, lookupProvider, helper));
    }

    private static class ModItemModelGen extends ItemModelProvider {
        public ModItemModelGen(PackOutput output, ExistingFileHelper helper) {
            super(output, UsefulMagicEnchanterLetterMod.MOD_ID, helper);
        }

        @Override
        protected void registerModels() {
            basicItem(ModItems.EXPERIENCE_MAGIC_LETTER.get());
            basicItem(ModItems.KILL_MAGIC_LETTER.get());
            basicItem(ModItems.FISHING_MAGIC_LETTER.get());
            basicItem(ModItems.TRAVEL_MAGIC_LETTER.get());
            basicItem(ModItems.TREASURE_MAGIC_LETTER.get());
            basicItem(ModItems.TIME_MAGIC_LETTER.get());
            basicItem(ModItems.TENACITY_MAGIC_LETTER.get());
            basicItem(ModItems.HERO_MAGIC_LETTER.get());
            // 手札合订本：默认使用"空合订本"贴图；custom_model_data=1 时切换为"有手札"贴图（创造模式标签页图标）
            withExistingParent("letter_binder", "item/generated")
                    .texture("layer0", modLoc("item/letter_binder"))
                    .override()
                    .predicate(mcLoc("custom_model_data"), 1.0F)
                    // 不校验存在性：letter_binder_full 由本 Provider 随后生成，
                    // getExistingFile 只查 --existing 目录（src/main/resources）。
                    .model(new net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile(modLoc("item/letter_binder_full")))
                    .end();
            withExistingParent("letter_binder_full", "item/generated")
                    .texture("layer0", modLoc("item/letter_binder_full"));
            basicItem(ModItems.STAGE_1_MAGIC_LETTER.get());
            basicItem(ModItems.STAGE_2_MAGIC_LETTER.get());
            basicItem(ModItems.STAGE_3_MAGIC_LETTER.get());
            basicItem(ModItems.STAGE_4_MAGIC_LETTER.get());
            basicItem(ModItems.STAGE_5_MAGIC_LETTER.get());
            basicItem(ModItems.STAGE_6_MAGIC_LETTER.get());
            basicItem(ModItems.STAGE_7_MAGIC_LETTER.get());
            basicItem(ModItems.STAGE_8_MAGIC_LETTER.get());
            basicItem(ModItems.STAGE_9_MAGIC_LETTER.get());
            basicItem(ModItems.STAGE_10_MAGIC_LETTER.get());
        }
    }

    private static class ModCuriosTagsGen extends ItemTagsProvider {
        public ModCuriosTagsGen(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper helper) {
            super(output, lookupProvider, completedFuture(), completedFuture(), UsefulMagicEnchanterLetterMod.MOD_ID, helper);
        }

        private static <T> CompletableFuture<TagsProvider.TagLookup<T>> completedFuture() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            for (String slot : CURIOS_SLOTS) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM,
                        ResourceLocation.fromNamespaceAndPath("curios", slot));
                for (var supplier : ModItems.ALL_LETTERS) {
                    ResourceKey<Item> key = BuiltInRegistries.ITEM.getResourceKey(supplier.get()).orElseThrow();
                    tag(tag).add(key);
                }
                // 手札合订本同样允许放入任意 Curios 槽位
                ResourceKey<Item> binderKey = BuiltInRegistries.ITEM.getResourceKey(ModItems.LETTER_BINDER.get()).orElseThrow();
                tag(tag).add(binderKey);
            }
        }
    }
}
