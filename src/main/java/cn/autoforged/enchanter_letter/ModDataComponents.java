package cn.autoforged.enchanter_letter;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

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
}
