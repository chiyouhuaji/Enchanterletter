package cn.autoforged.enchanter_letter.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "enchanter_letter.json";

    @SerializedName("experience_letter")
    public ExperienceLetterConfig experienceLetter = new ExperienceLetterConfig();

    @SerializedName("kill_letter")
    public KillLetterConfig killLetter = new KillLetterConfig();

    @SerializedName("fishing_letter")
    public FishingLetterConfig fishingLetter = new FishingLetterConfig();

    @SerializedName("travel_letter")
    public TravelLetterConfig travelLetter = new TravelLetterConfig();

    @SerializedName("treasure_letter")
    public TreasureLetterConfig treasureLetter = new TreasureLetterConfig();

    @SerializedName("time_letter")
    public TimeLetterConfig timeLetter = new TimeLetterConfig();

    @SerializedName("tenacity_letter")
    public TenacityLetterConfig tenacityLetter = new TenacityLetterConfig();

    @SerializedName("hero_letter")
    public HeroLetterConfig heroLetter = new HeroLetterConfig();

    @SerializedName("stage_letters")
    public StageLettersConfig stageLetters = new StageLettersConfig();

    @SerializedName("stacking_rules")
    public StackingRulesConfig stackingRules = new StackingRulesConfig();

    @SerializedName("magic_conversion")
    public MagicConversionConfig magicConversion = new MagicConversionConfig();

    @SerializedName("letter_bonus")
    public LetterBonusConfig letterBonus = new LetterBonusConfig();

    @SerializedName("letter_resistance")
    public LetterResistanceConfig letterResistance = new LetterResistanceConfig();

    @SerializedName("letter_glowing")
    public LetterGlowingConfig letterGlowing = new LetterGlowingConfig();

    @SerializedName("letter_cleanup")
    public LetterCleanupConfig letterCleanup = new LetterCleanupConfig();

    @SerializedName("letter_vanish")
    public LetterVanishConfig letterVanish = new LetterVanishConfig();

    @SerializedName("letter_respawn")
    public LetterRespawnConfig letterRespawn = new LetterRespawnConfig();

    @SerializedName("letter_binding")
    public LetterBindingConfig letterBinding = new LetterBindingConfig();

    public static class ExperienceLetterConfig {
        @SerializedName("exp_per_level")
        public double expPerLevel = 1000.0;

        @SerializedName("growth_per_level")
        public double growthPerLevel = 0.1;

        @SerializedName("armor_growth_per_level")
        public double armorGrowthPerLevel = 0.1;

        @SerializedName("toughness_growth_per_level")
        public double toughnessGrowthPerLevel = 0.01;

        @SerializedName("resistance_growth_per_level")
        public double resistanceGrowthPerLevel = 0.0;
    }

    public static class KillLetterConfig {
        @SerializedName("kills_per_level")
        public int killsPerLevel = 50;

        @SerializedName("growth_per_level")
        public double growthPerLevel = 0.1;

        @SerializedName("armor_growth_per_level")
        public double armorGrowthPerLevel = 0.1;

        @SerializedName("toughness_growth_per_level")
        public double toughnessGrowthPerLevel = 0.01;

        @SerializedName("resistance_growth_per_level")
        public double resistanceGrowthPerLevel = 0.0;
    }

    public static class FishingLetterConfig {
        @SerializedName("fish_per_level")
        public int fishPerLevel = 100;

        @SerializedName("growth_per_level")
        public double growthPerLevel = 0.1;

        @SerializedName("armor_growth_per_level")
        public double armorGrowthPerLevel = 0.1;

        @SerializedName("toughness_growth_per_level")
        public double toughnessGrowthPerLevel = 0.01;

        @SerializedName("resistance_growth_per_level")
        public double resistanceGrowthPerLevel = 0.0;
    }

    public static class TravelLetterConfig {
        @SerializedName("walk_distance_per_level")
        public double walkDistancePerLevel = 5000.0;

        @SerializedName("fly_distance_per_level")
        public double flyDistancePerLevel = 20000.0;

        @SerializedName("walk_growth_per_level")
        public double walkGrowthPerLevel = 0.1;

        @SerializedName("fly_growth_per_level")
        public double flyGrowthPerLevel = 0.1;

        @SerializedName("armor_growth_per_level")
        public double armorGrowthPerLevel = 0.1;

        @SerializedName("armor2_growth_per_level")
        public double armor2GrowthPerLevel = 0.1;

        @SerializedName("toughness_growth_per_level")
        public double toughnessGrowthPerLevel = 0.01;

        @SerializedName("toughness2_growth_per_level")
        public double toughness2GrowthPerLevel = 0.01;

        @SerializedName("resistance_growth_per_level")
        public double resistanceGrowthPerLevel = 0.0;

        @SerializedName("resistance2_growth_per_level")
        public double resistance2GrowthPerLevel = 0.0;
    }

    public static class TreasureLetterConfig {
        @SerializedName("opens_per_level")
        public int opensPerLevel = 20;

        @SerializedName("growth_per_level")
        public double growthPerLevel = 0.1;

        @SerializedName("armor_growth_per_level")
        public double armorGrowthPerLevel = 0.1;

        @SerializedName("toughness_growth_per_level")
        public double toughnessGrowthPerLevel = 0.01;

        @SerializedName("resistance_growth_per_level")
        public double resistanceGrowthPerLevel = 0.0;
    }

    public static class TimeLetterConfig {
        @SerializedName("seconds_per_level")
        public double secondsPerLevel = 3600.0;

        @SerializedName("growth_per_level")
        public double growthPerLevel = 0.1;

        @SerializedName("armor_growth_per_level")
        public double armorGrowthPerLevel = 0.1;

        @SerializedName("toughness_growth_per_level")
        public double toughnessGrowthPerLevel = 0.01;

        @SerializedName("resistance_growth_per_level")
        public double resistanceGrowthPerLevel = 0.0;
    }

    public static class TenacityLetterConfig {
        @SerializedName("damage_per_level")
        public double damagePerLevel = 10000.0;

        @SerializedName("growth_per_level")
        public double growthPerLevel = 0.1;

        @SerializedName("armor_growth_per_level")
        public double armorGrowthPerLevel = 0.1;

        @SerializedName("toughness_growth_per_level")
        public double toughnessGrowthPerLevel = 0.01;

        @SerializedName("resistance_growth_per_level")
        public double resistanceGrowthPerLevel = 0.0;
    }

    public static class HeroLetterConfig {
        @SerializedName("victories_per_level")
        public int victoriesPerLevel = 1;

        @SerializedName("growth_low_levels")
        public double growthLowLevels = 0.1;

        @SerializedName("growth_high_levels")
        public double growthHighLevels = 0.2;

        @SerializedName("high_level_start")
        public int highLevelStart = 4;

        @SerializedName("armor_growth_per_level")
        public double armorGrowthPerLevel = 0.1;

        @SerializedName("armor2_growth_per_level")
        public double armor2GrowthPerLevel = 0.1;

        @SerializedName("toughness_growth_per_level")
        public double toughnessGrowthPerLevel = 0.01;

        @SerializedName("toughness2_growth_per_level")
        public double toughness2GrowthPerLevel = 0.01;

        @SerializedName("resistance_growth_per_level")
        public double resistanceGrowthPerLevel = 0.0;

        @SerializedName("resistance2_growth_per_level")
        public double resistance2GrowthPerLevel = 0.0;
    }

    public static class StageLettersConfig {
        @SerializedName("multipliers")
        public List<Double> multipliers = new ArrayList<>();
        {
            multipliers.add(1.0);
            multipliers.add(2.0);
            multipliers.add(3.0);
            multipliers.add(4.0);
            multipliers.add(5.0);
            multipliers.add(6.0);
            multipliers.add(7.0);
            multipliers.add(8.0);
            multipliers.add(9.0);
            multipliers.add(10.0);
        }

        // 阶段魔法手札的防御属性：每张阶段手札独立配置（与等级/阶段号解耦，查表生效）。
        // 默认值保留原“每级增长”的数值：护甲 stage_n = n × 2，韧性 stage_n = n × 1，抗性全 0。
        @SerializedName("armor_values")
        public List<Double> armorValues = new ArrayList<>();
        {
            armorValues.add(2.0);
            armorValues.add(4.0);
            armorValues.add(6.0);
            armorValues.add(8.0);
            armorValues.add(10.0);
            armorValues.add(12.0);
            armorValues.add(14.0);
            armorValues.add(16.0);
            armorValues.add(18.0);
            armorValues.add(20.0);
        }

        @SerializedName("toughness_values")
        public List<Double> toughnessValues = new ArrayList<>();
        {
            toughnessValues.add(1.0);
            toughnessValues.add(2.0);
            toughnessValues.add(3.0);
            toughnessValues.add(4.0);
            toughnessValues.add(5.0);
            toughnessValues.add(6.0);
            toughnessValues.add(7.0);
            toughnessValues.add(8.0);
            toughnessValues.add(9.0);
            toughnessValues.add(10.0);
        }

        // 抗性提升：所有手札默认 0，仅由用户自行调配
        @SerializedName("resistance_values")
        public List<Double> resistanceValues = new ArrayList<>();
        {
            resistanceValues.add(0.0);
            resistanceValues.add(0.0);
            resistanceValues.add(0.0);
            resistanceValues.add(0.0);
            resistanceValues.add(0.0);
            resistanceValues.add(0.0);
            resistanceValues.add(0.0);
            resistanceValues.add(0.0);
            resistanceValues.add(0.0);
            resistanceValues.add(0.0);
        }
    }

    public static class StackingRulesConfig {
        /**
         * 允许多张手札同时生效。为 false 时（默认），无论是否附魔，只判断所有手札中倍率最高的一张生效。
         */
        @SerializedName("allow_multiple_letters")
        public boolean allowMultipleLetters = false;

        /**
         * 两个除转化状态外完全相同的（同物品、同倍率、同伤害类型）手札是否同时生效。
         * 为 false 时（默认）重复手札只生效一张；为 true 时每张都生效。
         */
        @SerializedName("allow_same_letters")
        public boolean allowSameLetters = false;
    }

    public static class MagicConversionConfig {
        @SerializedName("interval_seconds")
        public double intervalSeconds = 0.5;

        @SerializedName("blacklist_entity_names")
        public String blacklistEntityNames = "";
    }

    public static class LetterBonusConfig {
        /**
         * 默认增益的伤害类型。经逆向分析，UsefulMagic 3.1.1（NeoForge/Fabric）
         * 全部自定义伤害类型只有 usefulmagic:magic（UsefulMagicDamageTypes.MAGIC
         * 与 data/usefulmagic/damage_type/magic.json），其余均为原版类型。
         */
        @SerializedName("default_bonus_damage_types")
        public String defaultBonusDamageTypes = "usefulmagic:magic";
    }

    /**
     * 光灵附魔的默认发光颜色（RGB，0-255）。默认 0 0 0 表示白色。
     * 可用 /lettercolor <r> <g> <b> 或物品 NBT 单独修改。
     */
    public static class LetterGlowingConfig {
        @SerializedName("default_red")
        public int defaultRed = 0;

        @SerializedName("default_green")
        public int defaultGreen = 0;

        @SerializedName("default_blue")
        public int defaultBlue = 0;
    }

    /**
     * 定时清理（/letterclean）：清理绑定到指定 UUID 的手札/合订本掉落物。
     * 默认每 600 秒（10 分钟）清理一次零 UUID（00000000-...）掉落物。
     */
    public static class LetterCleanupConfig {
        @SerializedName("enabled")
        public boolean enabled = true;

        @SerializedName("interval_seconds")
        public double intervalSeconds = 600.0;

        @SerializedName("target_uuids")
        public List<String> targetUuids = new ArrayList<>();
        {
            targetUuids.add("00000000-0000-0000-0000-000000000000");
        }
    }

    /**
     * 强制消失（/lettervanish）：默认关闭。
     * 开启后玩家死亡时无论是否开启原版“死亡不掉落”，都会检索背包/饰品栏内
     * 带有消失诅咒的物品并删除，同时阻止带有消失诅咒的物品形成掉落物。
     * 关闭时使用原版机制（死亡不掉落下物品保留），不做任何干涉。
     */
    public static class LetterVanishConfig {
        @SerializedName("enabled")
        public boolean enabled = false;
    }

    /**
     * 重生返还延迟（/letterdelay）：游戏刻，默认 20 tick（1 秒）。
     * 玩家复活后等待该时长再执行：
     * 1) 魔法绑定饰品"强制装回原饰品槽位并覆盖"（避免槽位尚未加载完成导致有槽位却装回失败）；
     * 2) 消失诅咒物品扫描删除（检查玩家身上所有槽位含合订本内容物，检测到立即删除）。
     */
    public static class LetterRespawnConfig {
        @SerializedName("restore_delay_ticks")
        public int restoreDelayTicks = 20;
    }

    /**
     * 绑定物品弹出白名单（/letterbinding）：列表中的玩家 UUID 可以拾取并持有
     * 任意玩家 ID 绑定的手札/合订本，不会被自动弹出。默认仅保留示例条目。
     */
    public static class LetterBindingConfig {
        @SerializedName("uuid_whitelist")
        public List<String> uuidWhitelist = new ArrayList<>();
        {
            uuidWhitelist.add("00000000-0000-0000-0000-000000000000");
        }
    }

    /**
     * 抗性提升（服务器）功能：开关 + 允许的最大抗性减免比例（参照原版抗性药水，默认上限 80%）。
     */
    public static class LetterResistanceConfig {
        @SerializedName("enabled")
        public boolean enabled = true;

        @SerializedName("limit")
        public double limit = 0.8;
    }

    private transient String cachedBonusRaw;
    private transient List<ResourceLocation> cachedBonusTypes = List.of();

    private transient String cachedBlacklistRaw;
    private transient List<ResourceLocation> cachedBlacklistNames = List.of();

    /**
     * 解析配置文件中所有手札默认增益的伤害类型列表。
     */
    public List<ResourceLocation> getDefaultBonusDamageTypes() {
        String raw = letterBonus.defaultBonusDamageTypes;
        if (!raw.equals(cachedBonusRaw)) {
            List<ResourceLocation> parsed = new ArrayList<>();
            for (String part : splitList(raw)) {
                ResourceLocation rl = ResourceLocation.tryParse(part);
                if (rl != null) parsed.add(rl);
            }
            cachedBonusTypes = parsed;
            cachedBonusRaw = raw;
        }
        return cachedBonusTypes;
    }

    /**
     * 解析黑名单实体名称列表。
     */
    public List<ResourceLocation> getBlacklistEntityNames() {
        String raw = magicConversion.blacklistEntityNames;
        if (!raw.equals(cachedBlacklistRaw)) {
            List<ResourceLocation> parsed = new ArrayList<>();
            for (String part : splitList(raw)) {
                ResourceLocation rl = ResourceLocation.tryParse(part);
                if (rl != null) parsed.add(rl);
            }
            cachedBlacklistNames = parsed;
            cachedBlacklistRaw = raw;
        }
        return cachedBlacklistNames;
    }

    private static List<String> splitList(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return result;
        for (String part : raw.split("[,;]")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    private static ModConfig instance;

    public static ModConfig getInstance() {
        if (instance == null) {
            instance = new ModConfig();
        }
        return instance;
    }

    public static void init() {
        Path configDir = Path.of("config");
        Path configFile = configDir.resolve(FILE_NAME);
        try {
            if (Files.exists(configFile)) {
                String content = Files.readString(configFile);
                instance = GSON.fromJson(content, ModConfig.class);
                if (instance == null) instance = new ModConfig();
            } else {
                instance = new ModConfig();
                Files.createDirectories(configDir);
                Files.writeString(configFile, GSON.toJson(instance));
            }
        } catch (IOException e) {
            instance = new ModConfig();
        }
    }

    public static void save() {
        Path configDir = Path.of("config");
        Path configFile = configDir.resolve(FILE_NAME);
        try {
            Files.createDirectories(configDir);
            Files.writeString(configFile, GSON.toJson(getInstance()));
        } catch (IOException ignored) {
        }
    }
}
