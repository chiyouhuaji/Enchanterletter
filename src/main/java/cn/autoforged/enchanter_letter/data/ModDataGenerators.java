package cn.autoforged.enchanter_letter.data;

import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.world.item.Item;

import java.util.concurrent.CompletableFuture;

public class ModDataGenerators implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        var pack = generator.createPack();
        pack.addProvider(ModItemModelGen::new);
        pack.addProvider(ModCuriosTagsGen::new);
    }

    private static class ModItemModelGen extends FabricModelProvider {
        public ModItemModelGen(FabricDataOutput output) {
            super(output);
        }

        @Override
        public void generateBlockStateModels(net.minecraft.data.models.BlockModelGenerators gen) {}

        @Override
        public void generateItemModels(ItemModelGenerators gen) {
            generateFlatItem(gen, cn.autoforged.enchanter_letter.item.ModItems.EXPERIENCE_MAGIC_LETTER.get());
            generateFlatItem(gen, cn.autoforged.enchanter_letter.item.ModItems.KILL_MAGIC_LETTER.get());
            generateFlatItem(gen, cn.autoforged.enchanter_letter.item.ModItems.FISHING_MAGIC_LETTER.get());
            generateFlatItem(gen, cn.autoforged.enchanter_letter.item.ModItems.TRAVEL_MAGIC_LETTER.get());
            generateFlatItem(gen, cn.autoforged.enchanter_letter.item.ModItems.TREASURE_MAGIC_LETTER.get());
            generateFlatItem(gen, cn.autoforged.enchanter_letter.item.ModItems.TIME_MAGIC_LETTER.get());
            generateFlatItem(gen, cn.autoforged.enchanter_letter.item.ModItems.TENACITY_MAGIC_LETTER.get());
            generateFlatItem(gen, cn.autoforged.enchanter_letter.item.ModItems.HERO_MAGIC_LETTER.get());
            // 手札合订本模型为手写文件（src/generated/resources），含 custom_model_data 覆盖，不使用自动生成
            generateFlatItem(gen, cn.autoforged.enchanter_letter.item.ModItems.STAGE_1_MAGIC_LETTER.get());
            generateFlatItem(gen, cn.autoforged.enchanter_letter.item.ModItems.STAGE_2_MAGIC_LETTER.get());
            generateFlatItem(gen, cn.autoforged.enchanter_letter.item.ModItems.STAGE_3_MAGIC_LETTER.get());
            generateFlatItem(gen, cn.autoforged.enchanter_letter.item.ModItems.STAGE_4_MAGIC_LETTER.get());
            generateFlatItem(gen, cn.autoforged.enchanter_letter.item.ModItems.STAGE_5_MAGIC_LETTER.get());
            generateFlatItem(gen, cn.autoforged.enchanter_letter.item.ModItems.STAGE_6_MAGIC_LETTER.get());
            generateFlatItem(gen, cn.autoforged.enchanter_letter.item.ModItems.STAGE_7_MAGIC_LETTER.get());
            generateFlatItem(gen, cn.autoforged.enchanter_letter.item.ModItems.STAGE_8_MAGIC_LETTER.get());
            generateFlatItem(gen, cn.autoforged.enchanter_letter.item.ModItems.STAGE_9_MAGIC_LETTER.get());
            generateFlatItem(gen, cn.autoforged.enchanter_letter.item.ModItems.STAGE_10_MAGIC_LETTER.get());
        }

        private void generateFlatItem(ItemModelGenerators gen, Item item) {
            gen.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
        }
    }

    private static class ModCuriosTagsGen extends FabricTagProvider.ItemTagProvider {
        private static final String[] CURIOS_SLOTS = {"curio", "hands", "ring", "belt", "necklace", "back", "head", "bracelet"};

        public ModCuriosTagsGen(FabricDataOutput output, CompletableFuture<net.minecraft.core.HolderLookup.Provider> lookupProvider) {
            super(output, lookupProvider);
        }

        @Override
        protected void addTags(net.minecraft.core.HolderLookup.Provider provider) {
            for (String slot : CURIOS_SLOTS) {
                // FabricTagProvider.ItemTagProvider 会把 ITEM 标签写到
                // data/curios/tags/items/<slot>.json（tags/items 是固定目录，不能再拼 "items/" 前缀）。
                var tag = tag(net.minecraft.tags.TagKey.create(
                        net.minecraft.core.registries.Registries.ITEM,
                        ResourceLocation.fromNamespaceAndPath("curios", slot)));
                for (var supplier : cn.autoforged.enchanter_letter.item.ModItems.ALL_LETTERS) {
                    tag.add(BuiltInRegistries.ITEM.getResourceKey(supplier.get()).get());
                }
                // 手札合订本同样允许放入任意 Curios 槽位
                tag.add(BuiltInRegistries.ITEM.getResourceKey(cn.autoforged.enchanter_letter.item.ModItems.LETTER_BINDER.get()).get());
            }
        }
    }
}
