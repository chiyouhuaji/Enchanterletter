package cn.autoforged.enchanter_letter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

/**
 * 1.20.1 移植：物品数据使用 NBT 标签存储（替代 1.21.1 的 DataComponents）。
 */
public class ModDataComponents {
    public static final String EXPERIENCE_PROGRESS = "experience_progress";
    public static final String KILL_COUNT = "kill_count";
    public static final String BOUND_PLAYER = "bound_player";
    public static final String CONVERSION_DAMAGE_TYPE = "conversion_damage_type";
    public static final String FISH_COUNT = "fish_count";
    public static final String WALK_DISTANCE = "walk_distance";
    public static final String FLY_DISTANCE = "fly_distance";
    public static final String TREASURE_OPENS = "treasure_opens";
    public static final String DAMAGE_TAKEN = "damage_taken";
    public static final String RAID_VICTORIES = "raid_victories";
    public static final String GLOW_COLOR = "glow_color";

    /** 定制手札：直接写入 NBT 的伤害倍率/护甲/韧性/抗性数值，不参与计数成长。 */
    public static final String CUSTOM_DAMAGE = "custom_damage";
    public static final String CUSTOM_ARMOR = "custom_armor";
    public static final String CUSTOM_TOUGHNESS = "custom_toughness";
    public static final String CUSTOM_RESISTANCE = "custom_resistance";

    public static double getDouble(ItemStack stack, String key, double def) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(key, Tag.TAG_DOUBLE) ? tag.getDouble(key) : def;
    }

    public static int getInt(ItemStack stack, String key, int def) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(key, Tag.TAG_INT) ? tag.getInt(key) : def;
    }

    public static String getString(ItemStack stack, String key, String def) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : def;
    }

    public static Optional<UUID> getBoundPlayer(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(BOUND_PLAYER, Tag.TAG_STRING)) {
            try {
                return Optional.of(UUID.fromString(tag.getString(BOUND_PLAYER)));
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static void setDouble(ItemStack stack, String key, double value) {
        stack.getOrCreateTag().putDouble(key, value);
    }

    public static void setInt(ItemStack stack, String key, int value) {
        stack.getOrCreateTag().putInt(key, value);
    }

    public static void setString(ItemStack stack, String key, String value) {
        stack.getOrCreateTag().putString(key, value);
    }

    public static void setBoundPlayer(ItemStack stack, UUID uuid) {
        stack.getOrCreateTag().putString(BOUND_PLAYER, uuid.toString());
    }

    public static void removeBoundPlayer(ItemStack stack) {
        if (stack.hasTag()) {
            stack.getTag().remove(BOUND_PLAYER);
        }
    }
}
