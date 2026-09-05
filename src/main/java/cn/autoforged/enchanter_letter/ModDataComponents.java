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

    /**
     * 可增长手札（非阶段、非定制）的每级参数（倍率 / 升级所需次数 / 防御成长）以扁平键存于物品 NBT。
     * 配置文件默认值仅作为物品缺省时的回退；/letterset 对这类手札直接写物品 NBT（需手持），不写配置；
     * 等级/倍率/防御结算每次从 NBT 读取，命令修改后即时重算，避免不同步。
     */
    public static double getGrowthDouble(ItemStack stack, String key, double def) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(key, Tag.TAG_DOUBLE) ? tag.getDouble(key) : def;
    }

    public static int getGrowthInt(ItemStack stack, String key, int def) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(key, Tag.TAG_INT) ? tag.getInt(key) : def;
    }

    public static void setGrowthDouble(ItemStack stack, String key, double value) {
        stack.getOrCreateTag().putDouble(key, value);
    }

    public static void setGrowthInt(ItemStack stack, String key, int value) {
        stack.getOrCreateTag().putInt(key, value);
    }

    /** 获取物品 letter 数据 tag（物品 NBT 的活引用；无 tag 时返回新空 tag），药水词条等也存于此。 */
    public static CompoundTag getLetterData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag : new CompoundTag();
    }

    /** 写回物品 letter 数据 tag。 */
    public static void setLetterData(ItemStack stack, CompoundTag tag) {
        stack.setTag(tag);
    }
}
