package cn.autoforged.enchanter_letter;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;
import java.util.UUID;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(UsefulMagicEnchanterLetterMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> EXPERIENCE_PROGRESS =
            DATA_COMPONENT_TYPES.registerComponentType("experience_progress",
                    builder -> builder.persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> KILL_COUNT =
            DATA_COMPONENT_TYPES.registerComponentType("kill_count",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Optional<UUID>>> BOUND_PLAYER =
            DATA_COMPONENT_TYPES.registerComponentType("bound_player",
                    builder -> builder.persistent(ExtraCodecs.optionalEmptyMap(UUIDUtil.CODEC))
                            .networkSynchronized(ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC)));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> CONVERSION_DAMAGE_TYPE =
            DATA_COMPONENT_TYPES.registerComponentType("conversion_damage_type",
                    builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FISH_COUNT =
            DATA_COMPONENT_TYPES.registerComponentType("fish_count",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> WALK_DISTANCE =
            DATA_COMPONENT_TYPES.registerComponentType("walk_distance",
                    builder -> builder.persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> FLY_DISTANCE =
            DATA_COMPONENT_TYPES.registerComponentType("fly_distance",
                    builder -> builder.persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TREASURE_OPENS =
            DATA_COMPONENT_TYPES.registerComponentType("treasure_opens",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> DAMAGE_TAKEN =
            DATA_COMPONENT_TYPES.registerComponentType("damage_taken",
                    builder -> builder.persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RAID_VICTORIES =
            DATA_COMPONENT_TYPES.registerComponentType("raid_victories",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));

    /** 光灵附魔的发光颜色（0xRRGGBB；0 表示默认白色）。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GLOW_COLOR =
            DATA_COMPONENT_TYPES.registerComponentType("glow_color",
                    builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));

    /** 定制手札：直接写入物品组件的伤害倍率/护甲/韧性/抗性数值，不参与计数成长。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> CUSTOM_DAMAGE =
            DATA_COMPONENT_TYPES.registerComponentType("custom_damage",
                    builder -> builder.persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> CUSTOM_ARMOR =
            DATA_COMPONENT_TYPES.registerComponentType("custom_armor",
                    builder -> builder.persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> CUSTOM_TOUGHNESS =
            DATA_COMPONENT_TYPES.registerComponentType("custom_toughness",
                    builder -> builder.persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> CUSTOM_RESISTANCE =
            DATA_COMPONENT_TYPES.registerComponentType("custom_resistance",
                    builder -> builder.persistent(Codec.DOUBLE).networkSynchronized(ByteBufCodecs.DOUBLE));

    /**
     * 可增长手札（非阶段、非定制）的每级参数（倍率 / 升级所需次数 / 防御成长）存于物品即内建 CUSTOM_DATA 组件，
     * 以键值形式写入同一个 CompoundTag。配置文件默认值仅作为物品获得时的初始默认；
     * /letterset 对这类手札直接写物品 NBT（需手持），不写配置文件；
     * 等级/倍率/防御结算每次从 NBT 读取，命令修改后即时重算，避免不同步。
     */
    private static CompoundTag getGrowthTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
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
