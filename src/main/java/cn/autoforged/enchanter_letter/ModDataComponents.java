package cn.autoforged.enchanter_letter;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
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
}
