package cn.autoforged.enchanter_letter;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;
import java.util.UUID;

public class ModDataComponents {
    private static DataComponentType<Double> simpleDouble(String name) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(UsefulMagicEnchanterLetterMod.MOD_ID, name),
                DataComponentType.<Double>builder().persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE).build());
    }

    private static DataComponentType<Integer> simpleInt(String name) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(UsefulMagicEnchanterLetterMod.MOD_ID, name),
                DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT).build());
    }

    private static DataComponentType<String> simpleString(String name) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(UsefulMagicEnchanterLetterMod.MOD_ID, name),
                DataComponentType.<String>builder().persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
    }

    /** 强制在 onInitialize 阶段加载本类，避免注册表冻结后才初始化导致崩溃。 */
    public static void init() {
    }

    public static final DataComponentType<Double> EXPERIENCE_PROGRESS = simpleDouble("experience_progress");
    public static final DataComponentType<Integer> KILL_COUNT = simpleInt("kill_count");
    public static final DataComponentType<Optional<UUID>> BOUND_PLAYER = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(UsefulMagicEnchanterLetterMod.MOD_ID, "bound_player"),
            DataComponentType.<Optional<UUID>>builder()
                    .persistent(ExtraCodecs.optionalEmptyMap(UUIDUtil.CODEC))
                    .networkSynchronized(ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC))
                    .build());
    public static final DataComponentType<String> CONVERSION_DAMAGE_TYPE = simpleString("conversion_damage_type");
    public static final DataComponentType<Integer> FISH_COUNT = simpleInt("fish_count");
    public static final DataComponentType<Double> WALK_DISTANCE = simpleDouble("walk_distance");
    public static final DataComponentType<Double> FLY_DISTANCE = simpleDouble("fly_distance");
    public static final DataComponentType<Integer> TREASURE_OPENS = simpleInt("treasure_opens");
    public static final DataComponentType<Double> DAMAGE_TAKEN = simpleDouble("damage_taken");
    public static final DataComponentType<Integer> RAID_VICTORIES = simpleInt("raid_victories");
    public static final DataComponentType<Integer> GLOW_COLOR = simpleInt("glow_color");

    /** 定制手札：直接写入物品的伤害倍率/护甲/韧性/抗性数值，不参与计数成长。 */
    public static final DataComponentType<Double> CUSTOM_DAMAGE = simpleDouble("custom_damage");
    public static final DataComponentType<Double> CUSTOM_ARMOR = simpleDouble("custom_armor");
    public static final DataComponentType<Double> CUSTOM_TOUGHNESS = simpleDouble("custom_toughness");
    public static final DataComponentType<Double> CUSTOM_RESISTANCE = simpleDouble("custom_resistance");

    // ==================== 成长手札逐级参数（写入物品 CustomData NBT） ====================

    private static CompoundTag getGrowthTag(ItemStack stack) {
        CustomData c = stack.get(DataComponents.CUSTOM_DATA);
        return c == null ? new CompoundTag() : c.copyTag();
    }

    public static double getGrowthDouble(ItemStack stack, String key, double def) {
        CompoundTag tag = getGrowthTag(stack);
        return tag.contains(key, Tag.TAG_DOUBLE) ? tag.getDouble(key) : def;
    }

    public static int getGrowthInt(ItemStack stack, String key, int def) {
        CompoundTag tag = getGrowthTag(stack);
        return tag.contains(key, Tag.TAG_INT) ? tag.getInt(key) : def;
    }

    public static void setGrowthDouble(ItemStack stack, String key, double value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putDouble(key, value));
    }

    public static void setGrowthInt(ItemStack stack, String key, int value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(key, value));
    }

    /** 获取物品 letter 数据 tag（CUSTOM_DATA 的副本），药水词条等也存于此。 */
    public static CompoundTag getLetterData(ItemStack stack) {
        return getGrowthTag(stack);
    }

    /** 写回物品 letter 数据 tag。 */
    public static void setLetterData(ItemStack stack, CompoundTag tag) {
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }
}
