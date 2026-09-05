package cn.autoforged.enchanter_letter.command;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.config.ModConfig;
import cn.autoforged.enchanter_letter.event.ModCommonEvents;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import cn.autoforged.enchanter_letter.item.CustomMagicLetterItem;
import cn.autoforged.enchanter_letter.item.ExperienceMagicLetterItem;
import cn.autoforged.enchanter_letter.item.FishingMagicLetterItem;
import cn.autoforged.enchanter_letter.item.HeroMagicLetterItem;
import cn.autoforged.enchanter_letter.item.KillMagicLetterItem;
import cn.autoforged.enchanter_letter.item.LetterBinderItem;
import cn.autoforged.enchanter_letter.item.MagicLetterItem;
import cn.autoforged.enchanter_letter.item.TenacityMagicLetterItem;
import cn.autoforged.enchanter_letter.item.TimeMagicLetterItem;
import cn.autoforged.enchanter_letter.item.TravelMagicLetterItem;
import cn.autoforged.enchanter_letter.item.TreasureMagicLetterItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * /lettermulti [true|false] —— 开关“允许多张手札同时生效”配置（不带参数则翻转）。
 * /lettersame  [true|false] —— 开关“两个除转化状态外完全相同的手札是否同时生效”（不带参数则翻转）。
 * /letterset <type> <param> [index] <value> —— 修改手札所需次数 / 每次伤害数值。
 *   type: experience|kill|fishing|travel|treasure|time|tenacity|hero|stage（可 tab 补全）
 *   param: time|time2|damage|damage2（可 tab 补全）
 *   stage 类型需使用 damage + index(1-10) 直接设置对应阶段手札的倍率。
 * /lettertype add|delete <damage_type> —— 添加/移除默认增益伤害类型（tab 检索注册表）。
 * /letterentity add|delete <entity_type> —— 添加/移除魔法转化实体黑名单（tab 检索注册表）。
 * /letterback [player] —— 检索已加载掉落物，把绑定到执行者（或指定在线玩家）的手札/合订本传送到其所在坐标。
 * /letterinterval [value] —— 查询/修改魔法转化结算间隔（interval_seconds，秒，需大于 0.001）。
 */
public class LetterCommands {
    private static final String[] LETTER_TYPES = {
            "experience", "kill", "fishing", "travel", "treasure", "time", "tenacity", "hero",
            "custom",
            "stage_1", "stage_2", "stage_3", "stage_4", "stage_5",
            "stage_6", "stage_7", "stage_8", "stage_9", "stage_10"
    };
    private static final String[] LETTER_PARAMS = {
            "time", "time2", "damage", "damage2",
            "armor", "armor2", "toughness", "toughness2", "resistance", "resistance2"
    };

    /** 成长型手札类型（其 /letterset 数值直接写入物品 NBT，不修改配置文件）。 */
    private static final Set<String> GROWTH_TYPES = Set.of(
            "experience", "kill", "fishing", "travel", "treasure", "time", "tenacity", "hero");

    /** 成长型手札 类型:参数 -> 物品 NBT 键（扁平键直接写入手札 tag）。 */
    private static final Map<String, String> GROWTH_NBT_KEYS = Map.ofEntries(
            Map.entry("experience:time", "exp_per_level"),
            Map.entry("experience:damage", "growth_per_level"),
            Map.entry("experience:armor", "armor_growth_per_level"),
            Map.entry("experience:toughness", "toughness_growth_per_level"),
            Map.entry("experience:resistance", "resistance_growth_per_level"),
            Map.entry("kill:time", "kills_per_level"),
            Map.entry("kill:damage", "growth_per_level"),
            Map.entry("kill:armor", "armor_growth_per_level"),
            Map.entry("kill:toughness", "toughness_growth_per_level"),
            Map.entry("kill:resistance", "resistance_growth_per_level"),
            Map.entry("fishing:time", "fish_per_level"),
            Map.entry("fishing:damage", "growth_per_level"),
            Map.entry("fishing:armor", "armor_growth_per_level"),
            Map.entry("fishing:toughness", "toughness_growth_per_level"),
            Map.entry("fishing:resistance", "resistance_growth_per_level"),
            Map.entry("treasure:time", "opens_per_level"),
            Map.entry("treasure:damage", "growth_per_level"),
            Map.entry("treasure:armor", "armor_growth_per_level"),
            Map.entry("treasure:toughness", "toughness_growth_per_level"),
            Map.entry("treasure:resistance", "resistance_growth_per_level"),
            Map.entry("tenacity:time", "damage_per_level"),
            Map.entry("tenacity:damage", "growth_per_level"),
            Map.entry("tenacity:armor", "armor_growth_per_level"),
            Map.entry("tenacity:toughness", "toughness_growth_per_level"),
            Map.entry("tenacity:resistance", "resistance_growth_per_level"),
            Map.entry("time:time", "seconds_per_level"),
            Map.entry("time:damage", "growth_per_level"),
            Map.entry("time:armor", "armor_growth_per_level"),
            Map.entry("time:toughness", "toughness_growth_per_level"),
            Map.entry("time:resistance", "resistance_growth_per_level"),
            Map.entry("travel:time", "walk_distance_per_level"),
            Map.entry("travel:time2", "fly_distance_per_level"),
            Map.entry("travel:damage", "walk_growth_per_level"),
            Map.entry("travel:damage2", "fly_growth_per_level"),
            Map.entry("travel:armor", "armor_growth_per_level"),
            Map.entry("travel:armor2", "armor2_growth_per_level"),
            Map.entry("travel:toughness", "toughness_growth_per_level"),
            Map.entry("travel:toughness2", "toughness2_growth_per_level"),
            Map.entry("travel:resistance", "resistance_growth_per_level"),
            Map.entry("travel:resistance2", "resistance2_growth_per_level"),
            Map.entry("hero:time", "victories_per_level"),
            Map.entry("hero:time2", "high_level_start"),
            Map.entry("hero:damage", "growth_low_levels"),
            Map.entry("hero:damage2", "growth_high_levels"),
            Map.entry("hero:armor", "armor_growth_per_level"),
            Map.entry("hero:armor2", "armor2_growth_per_level"),
            Map.entry("hero:toughness", "toughness_growth_per_level"),
            Map.entry("hero:toughness2", "toughness2_growth_per_level"),
            Map.entry("hero:resistance", "resistance_growth_per_level"),
            Map.entry("hero:resistance2", "resistance2_growth_per_level"));

    private static final SuggestionProvider<CommandSourceStack> TYPE_SUGGESTIONS =
            (context, builder) -> {
                for (String s : LETTER_TYPES) builder.suggest(s);
                return builder.buildFuture();
            };

    private static final SuggestionProvider<CommandSourceStack> PARAM_SUGGESTIONS =
            (context, builder) -> {
                for (String s : LETTER_PARAMS) builder.suggest(s);
                return builder.buildFuture();
            };

    private static final SuggestionProvider<CommandSourceStack> BOOL_SUGGESTIONS =
            (context, builder) -> {
                builder.suggest("true");
                builder.suggest("false");
                return builder.buildFuture();
            };

    /** add 时列出注册表中所有伤害类型（排除已配置的）；delete 时列出当前已配置的。 */
    private static final SuggestionProvider<CommandSourceStack> DAMAGE_TYPE_ADD_SUGGESTIONS =
            (context, builder) -> {
                HashSet<String> existing = new HashSet<>();
                for (ResourceLocation id : ModConfig.getInstance().getDefaultBonusDamageTypes()) {
                    existing.add(id.toString().toLowerCase(Locale.ROOT));
                }
                try {
                    MinecraftServer server = context.getSource().getServer();
                    if (server != null) {
                        for (ResourceLocation id : server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).keySet()) {
                            if (!existing.contains(id.toString())) builder.suggest(id.toString());
                        }
                    }
                } catch (Exception ignored) {
                }
                return builder.buildFuture();
            };

    private static final SuggestionProvider<CommandSourceStack> DAMAGE_TYPE_DELETE_SUGGESTIONS =
            (context, builder) -> {
                for (ResourceLocation id : ModConfig.getInstance().getDefaultBonusDamageTypes()) {
                    builder.suggest(id.toString());
                }
                return builder.buildFuture();
            };

    /** add 时列出注册表中所有实体类型（排除已配置的）；delete 时列出当前已配置的。 */
    private static final SuggestionProvider<CommandSourceStack> ENTITY_TYPE_ADD_SUGGESTIONS =
            (context, builder) -> {
                HashSet<String> existing = new HashSet<>();
                for (ResourceLocation id : ModConfig.getInstance().getBlacklistEntityNames()) {
                    existing.add(id.toString().toLowerCase(Locale.ROOT));
                }
                try {
                    MinecraftServer server = context.getSource().getServer();
                    if (server != null) {
                        for (ResourceLocation id : server.registryAccess().registryOrThrow(Registries.ENTITY_TYPE).keySet()) {
                            if (!existing.contains(id.toString())) builder.suggest(id.toString());
                        }
                    }
                } catch (Exception ignored) {
                }
                return builder.buildFuture();
            };

    private static final SuggestionProvider<CommandSourceStack> ENTITY_TYPE_DELETE_SUGGESTIONS =
            (context, builder) -> {
                for (ResourceLocation id : ModConfig.getInstance().getBlacklistEntityNames()) {
                    builder.suggest(id.toString());
                }
                return builder.buildFuture();
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lettermulti")
                .requires(source -> source.hasPermission(2))
                .executes(LetterCommands::toggleMulti)
                .then(Commands.argument("value", StringArgumentType.word())
                        .suggests(BOOL_SUGGESTIONS)
                        .executes(LetterCommands::setMulti)));

        dispatcher.register(Commands.literal("lettersame")
                .requires(source -> source.hasPermission(2))
                .executes(LetterCommands::toggleSame)
                .then(Commands.argument("value", StringArgumentType.word())
                        .suggests(BOOL_SUGGESTIONS)
                        .executes(LetterCommands::setSame)));

        dispatcher.register(Commands.literal("letterinterval")
                .requires(source -> source.hasPermission(2))
                .executes(LetterCommands::showInterval)
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.001))
                        .executes(LetterCommands::setInterval)));

        // /letterdelay [value] —— 查询/设置重生返还与消失诅咒检查延迟（游戏刻，默认 20；0 = 立即执行）
        dispatcher.register(Commands.literal("letterdelay")
                .requires(source -> source.hasPermission(2))
                .executes(LetterCommands::showRespawnDelay)
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 6000))
                        .executes(LetterCommands::setRespawnDelay)));

        dispatcher.register(Commands.literal("letterset")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("type", StringArgumentType.word())
                        .suggests(TYPE_SUGGESTIONS)
                        .then(Commands.argument("param", StringArgumentType.word())
                                .suggests(PARAM_SUGGESTIONS)
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                        .executes(LetterCommands::setValue)))));

        dispatcher.register(Commands.literal("lettertype")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("damage_type", ResourceLocationArgument.id())
                                .suggests(DAMAGE_TYPE_ADD_SUGGESTIONS)
                                .executes(LetterCommands::addDamageType)))
                .then(Commands.literal("delete")
                        .then(Commands.argument("damage_type", ResourceLocationArgument.id())
                                .suggests(DAMAGE_TYPE_DELETE_SUGGESTIONS)
                                .executes(LetterCommands::deleteDamageType))));

        dispatcher.register(Commands.literal("letterentity")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("entity_type", ResourceLocationArgument.id())
                                .suggests(ENTITY_TYPE_ADD_SUGGESTIONS)
                                .executes(LetterCommands::addEntityType)))
                .then(Commands.literal("delete")
                        .then(Commands.argument("entity_type", ResourceLocationArgument.id())
                                .suggests(ENTITY_TYPE_DELETE_SUGGESTIONS)
                                .executes(LetterCommands::deleteEntityType))));

        dispatcher.register(Commands.literal("letterback")
                .requires(source -> source.hasPermission(2))
                .executes(LetterCommands::recallOwnLetters)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(LetterCommands::recallPlayerLetters)));

        // /lettercolor [<r> <g> <b>] —— 无参数查询手持物品的光灵发光颜色数据；带参数则设置（RGB 0-255）
        dispatcher.register(Commands.literal("lettercolor")
                .requires(source -> source.hasPermission(2))
                .executes(LetterCommands::queryLetterColor)
                .then(Commands.argument("red", IntegerArgumentType.integer(0, 255))
                        .then(Commands.argument("green", IntegerArgumentType.integer(0, 255))
                                .then(Commands.argument("blue", IntegerArgumentType.integer(0, 255))
                                        .executes(LetterCommands::setLetterColor)))));

        // /letterenchanted —— 附魔书战利品附加开关与概率（true/false/glowing/binding/conversion，权限 2/3/4）
        dispatcher.register(Commands.literal("letterenchanted")
                .requires(source -> source.hasPermission(2))
                .executes(LetterCommands::showEnchanted)
                .then(Commands.literal("true")
                        .executes(ctx -> setEnchantedEnabled(ctx, true)))
                .then(Commands.literal("false")
                        .executes(ctx -> setEnchantedEnabled(ctx, false)))
                .then(Commands.literal("glowing")
                        .then(Commands.argument("chance", DoubleArgumentType.doubleArg(0.0, 1.0))
                                .executes(ctx -> setEnchantedChance(ctx, "glowing"))))
                .then(Commands.literal("binding")
                        .then(Commands.argument("chance", DoubleArgumentType.doubleArg(0.0, 1.0))
                                .executes(ctx -> setEnchantedChance(ctx, "binding"))))
                .then(Commands.literal("conversion")
                        .then(Commands.argument("chance", DoubleArgumentType.doubleArg(0.0, 1.0))
                                .executes(ctx -> setEnchantedChance(ctx, "conversion")))));

        // /letterclean —— 定时清理绑定掉落物（on/off/time/add/delete/all/uuid，权限 2/3/4）
        dispatcher.register(Commands.literal("letterclean")
                .requires(source -> source.hasPermission(2))
                .executes(LetterCommands::showCleanup)
                .then(Commands.literal("on")
                        .executes(ctx -> setCleanupEnabled(ctx, true)))
                .then(Commands.literal("off")
                        .executes(ctx -> setCleanupEnabled(ctx, false)))
                .then(Commands.literal("time")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(1.0))
                                .executes(LetterCommands::setCleanupInterval)))
                .then(Commands.literal("add")
                        .then(Commands.literal("allbinding")
                                .executes(ctx -> setCleanupAllBinding(ctx, true)))
                        .then(Commands.literal("allnormal")
                                .executes(ctx -> setCleanupAllNormal(ctx, true)))
                        .then(Commands.argument("uuid", StringArgumentType.word())
                                .suggests(PLAYER_OR_UUID_SUGGESTIONS)
                                .executes(LetterCommands::addCleanupUuid)))
                .then(Commands.literal("delete")
                        .then(Commands.literal("allbinding")
                                .executes(ctx -> setCleanupAllBinding(ctx, false)))
                        .then(Commands.literal("allnormal")
                                .executes(ctx -> setCleanupAllNormal(ctx, false)))
                        .then(Commands.argument("uuid", StringArgumentType.word())
                                .suggests(PLAYER_OR_UUID_SUGGESTIONS)
                                .executes(LetterCommands::deleteCleanupUuid)))
                .then(Commands.literal("all")
                        .executes(ctx -> confirmCleanupAll(ctx, false))
                        .then(Commands.literal("confirm")
                                .executes(ctx -> confirmCleanupAll(ctx, true))))
                .then(Commands.literal("uuid")
                        .then(Commands.argument("uuid", StringArgumentType.word())
                                .suggests(PLAYER_OR_UUID_SUGGESTIONS)
                                .executes(ctx -> cleanupByUuid(ctx, false))
                                .then(Commands.literal("confirm")
                                        .executes(ctx -> cleanupByUuid(ctx, true))))));

        // /letterresistance —— 查询/开关服务器抗性提升功能、配置抗性上限（权限 2/3/4）
        dispatcher.register(Commands.literal("letterresistance")
                .requires(source -> source.hasPermission(2))
                .executes(LetterCommands::showResistance)
                .then(Commands.literal("on")
                        .executes(ctx -> setResistanceEnabled(ctx, true)))
                .then(Commands.literal("off")
                        .executes(ctx -> setResistanceEnabled(ctx, false)))
                .then(Commands.literal("limit")
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 1.0))
                                .executes(LetterCommands::setResistanceLimit))));

        // /lettervanish —— 强制消失开关（on/off，默认关闭；开启后死亡强制移除消失诅咒物品并阻止其形成掉落物）
        dispatcher.register(Commands.literal("lettervanish")
                .requires(source -> source.hasPermission(2))
                .executes(LetterCommands::showVanish)
                .then(Commands.literal("on")
                        .executes(ctx -> setVanishEnabled(ctx, true)))
                .then(Commands.literal("off")
                        .executes(ctx -> setVanishEnabled(ctx, false))));

        // /letterbinding —— 绑定物品弹出白名单 & 修改绑定 UUID（uuid/add/delete，权限 2/3/4）
        dispatcher.register(Commands.literal("letterbinding")
                .requires(source -> source.hasPermission(2))
                .executes(LetterCommands::showBinding)
                .then(Commands.literal("uuid")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(PLAYER_OR_UUID_SUGGESTIONS)
                                .executes(LetterCommands::setBindingUuid)))
                .then(Commands.literal("add")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(PLAYER_SUGGESTIONS)
                                .executes(LetterCommands::addBindingWhitelist)))
                .then(Commands.literal("delete")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(PLAYER_OR_UUID_SUGGESTIONS)
                                .executes(LetterCommands::deleteBindingWhitelist))));

    }

    // ==================== /lettercolor ====================

    private static int queryLetterColor(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof Player player)) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.letterback.player_only"));
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof MagicLetterItem || stack.getItem() instanceof LetterBinderItem)) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.lettercolor.require_letter"));
            return 0;
        }
        int color = ModDataComponents.getInt(stack, ModDataComponents.GLOW_COLOR, 0);
        int rr = (color >> 16) & 0xFF;
        int gg = (color >> 8) & 0xFF;
        int bb = color & 0xFF;
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.lettercolor.query", rr, gg, bb), true);
        return 1;
    }

    private static int setLetterColor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        if (!(context.getSource().getEntity() instanceof Player player)) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.letterback.player_only"));
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof MagicLetterItem || stack.getItem() instanceof LetterBinderItem)) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.lettercolor.require_letter"));
            return 0;
        }
        int r = context.getArgument("red", Integer.class);
        int g = context.getArgument("green", Integer.class);
        int b = context.getArgument("blue", Integer.class);
        int color = (r << 16) | (g << 8) | b;
        ModDataComponents.setInt(stack, ModDataComponents.GLOW_COLOR, color);
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.lettercolor.success", r, g, b), true);
        return 1;
    }

    // ==================== /letterenchanted ====================

    private static int showEnchanted(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterenchanted.status",
                stateComponent(ModConfig.isLetterEnchantedEnabled()),
                ModConfig.getLetterEnchantedGlowingChance(),
                ModConfig.getLetterEnchantedMagicBindingChance(),
                ModConfig.getLetterEnchantedMagicConversionChance()), true);
        return 1;
    }

    private static int setEnchantedEnabled(CommandContext<CommandSourceStack> context, boolean enabled) {
        ModConfig.setLetterEnchantedEnabled(enabled);
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterenchanted.enabled", stateComponent(enabled)), true);
        return 1;
    }

    private static int setEnchantedChance(CommandContext<CommandSourceStack> context, String type) {
        double chance = DoubleArgumentType.getDouble(context, "chance");
        String key;
        switch (type) {
            case "glowing":
                ModConfig.setLetterEnchantedGlowingChance(chance);
                key = "command.enchanter_letter.letterenchanted.glowing";
                break;
            case "binding":
                ModConfig.setLetterEnchantedMagicBindingChance(chance);
                key = "command.enchanter_letter.letterenchanted.binding";
                break;
            case "conversion":
                ModConfig.setLetterEnchantedMagicConversionChance(chance);
                key = "command.enchanter_letter.letterenchanted.conversion";
                break;
            default:
                return 0;
        }
        ModConfig.save();
        String finalKey = key;
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterenchanted.chance_set", Component.translatable(finalKey), chance), true);
        return 1;
    }

    // ==================== /letterclean ====================

    private static int showCleanup(CommandContext<CommandSourceStack> context) {
        ModConfig.LetterCleanupConfig cfg = ModConfig.getInstance().letterCleanup;
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterclean.status",
                stateComponent(cfg.enabled), cfg.intervalSeconds,
                String.join(", ", cfg.targetUuids),
                stateComponent(cfg.cleanAllBinding), stateComponent(cfg.cleanAllNormal)), true);
        return 1;
    }

    private static int setCleanupEnabled(CommandContext<CommandSourceStack> context, boolean enabled) {
        ModConfig.getInstance().letterCleanup.enabled = enabled;
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterclean.enabled", stateComponent(enabled)), true);
        return 1;
    }

    private static int setCleanupInterval(CommandContext<CommandSourceStack> context) {
        double value = context.getArgument("value", Double.class);
        ModConfig.getInstance().letterCleanup.intervalSeconds = value;
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterclean.time", value), true);
        return 1;
    }

    private static int addCleanupUuid(CommandContext<CommandSourceStack> context) {
        ModConfig.LetterCleanupConfig cfg = ModConfig.getInstance().letterCleanup;
        String s = resolveUuidOrPlayer(context, "uuid");
        if (s == null) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.letterplayer.not_found"));
            return 0;
        }
        if (cfg.targetUuids.contains(s)) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.letterclean.uuid_exists", s));
            return 0;
        }
        cfg.targetUuids.add(s);
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterclean.uuid_added", s), true);
        return 1;
    }

    private static int deleteCleanupUuid(CommandContext<CommandSourceStack> context) {
        ModConfig.LetterCleanupConfig cfg = ModConfig.getInstance().letterCleanup;
        String s = resolveUuidOrPlayer(context, "uuid");
        if (s == null) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.letterplayer.not_found"));
            return 0;
        }
        if (!cfg.targetUuids.remove(s)) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.letterclean.uuid_not_found", s));
            return 0;
        }
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterclean.uuid_deleted", s), true);
        return 1;
    }

    private static int setCleanupAllBinding(CommandContext<CommandSourceStack> context, boolean enabled) {
        ModConfig.getInstance().letterCleanup.cleanAllBinding = enabled;
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterclean.allbinding_set", stateComponent(enabled)), true);
        return 1;
    }

    private static int setCleanupAllNormal(CommandContext<CommandSourceStack> context, boolean enabled) {
        ModConfig.getInstance().letterCleanup.cleanAllNormal = enabled;
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterclean.allnormal_set", stateComponent(enabled)), true);
        return 1;
    }

    /** 清理所有手札/合订本掉落物（需二次确认）。 */
    private static int confirmCleanupAll(CommandContext<CommandSourceStack> context, boolean confirmed) {
        if (!confirmed) {
            context.getSource().sendSuccess(() -> Component.translatable(
                    "command.enchanter_letter.letterclean.all_confirm"), true);
            return 0;
        }
        int removed = removeLetterDrops(context.getSource().getServer(), null);
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterclean.all_done", removed), true);
        return 1;
    }

    /** 清理指定绑定 UUID 的手札/合订本掉落物（需二次确认）。 */
    private static int cleanupByUuid(CommandContext<CommandSourceStack> context, boolean confirmed) {
        String s = resolveUuidOrPlayer(context, "uuid");
        if (s == null) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.letterplayer.not_found"));
            return 0;
        }
        UUID uuid = UUID.fromString(s);
        if (!confirmed) {
            context.getSource().sendSuccess(() -> Component.translatable(
                    "command.enchanter_letter.letterclean.uuid_confirm", s), true);
            return 0;
        }
        int removed = removeLetterDrops(context.getSource().getServer(), uuid);
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterclean.uuid_done", s, removed), true);
        return 1;
    }

    private static int removeLetterDrops(MinecraftServer server, UUID boundUuid) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof ItemEntity itemEntity)) continue;
                ItemStack stack = itemEntity.getItem();
                if (stack.isEmpty() || !ModCommonEvents.isOurModItem(stack)) continue;
                if (boundUuid == null) {
                    itemEntity.discard();
                    count++;
                } else {
                    Optional<UUID> bound = ModDataComponents.getBoundPlayer(stack);
                    if (bound != null && bound.isPresent() && bound.get().equals(boundUuid)) {
                        itemEntity.discard();
                        count++;
                    }
                }
            }
        }
        return count;
    }

    // ==================== /letterresistance ====================

    private static int showResistance(CommandContext<CommandSourceStack> context) {
        ModConfig config = ModConfig.getInstance();
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterresistance.status",
                stateComponent(config.letterResistance.enabled), config.letterResistance.limit), true);
        return 1;
    }

    private static int setResistanceEnabled(CommandContext<CommandSourceStack> context, boolean enabled) {
        ModConfig config = ModConfig.getInstance();
        config.letterResistance.enabled = enabled;
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterresistance.enabled", stateComponent(enabled)), true);
        return 1;
    }

    /** “开启/关闭”状态组件（替代 true/false 直显）。 */
    private static Component stateComponent(boolean enabled) {
        return Component.translatable(enabled
                ? "command.enchanter_letter.state.on"
                : "command.enchanter_letter.state.off");
    }

    private static int setResistanceLimit(CommandContext<CommandSourceStack> context) {
        double value = context.getArgument("value", Double.class);
        ModConfig config = ModConfig.getInstance();
        config.letterResistance.limit = Math.max(0.0, Math.min(1.0, value));
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterresistance.limit", config.letterResistance.limit), true);
        return 1;
    }

    private static int toggleMulti(CommandContext<CommandSourceStack> context) {
        ModConfig config = ModConfig.getInstance();
        config.stackingRules.allowMultipleLetters = !config.stackingRules.allowMultipleLetters;
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.lettermulti.success", stateComponent(config.stackingRules.allowMultipleLetters)), true);
        return 1;
    }

    private static int setMulti(CommandContext<CommandSourceStack> context) {
        boolean value = parseBool(context, "value");
        ModConfig config = ModConfig.getInstance();
        config.stackingRules.allowMultipleLetters = value;
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.lettermulti.success", stateComponent(value)), true);
        return 1;
    }

    private static int toggleSame(CommandContext<CommandSourceStack> context) {
        ModConfig config = ModConfig.getInstance();
        config.stackingRules.allowSameLetters = !config.stackingRules.allowSameLetters;
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.lettersame.success", stateComponent(config.stackingRules.allowSameLetters)), true);
        return 1;
    }

    private static int setSame(CommandContext<CommandSourceStack> context) {
        boolean value = parseBool(context, "value");
        ModConfig config = ModConfig.getInstance();
        config.stackingRules.allowSameLetters = value;
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.lettersame.success", stateComponent(value)), true);
        return 1;
    }

    private static boolean parseBool(CommandContext<CommandSourceStack> context, String arg) {
        String s = context.getArgument(arg, String.class);
        return "true".equalsIgnoreCase(s);
    }

    private static int setValue(CommandContext<CommandSourceStack> context) {
        String type = context.getArgument("type", String.class);
        String param = context.getArgument("param", String.class);
        double value = context.getArgument("value", Double.class);
        if ("custom".equalsIgnoreCase(type)) {
            String result = applyCustomValue(context, param, value);
            if (result == null) {
                context.getSource().sendFailure(Component.translatable("command.enchanter_letter.letterset.invalid", type, param));
                return 0;
            }
            context.getSource().sendSuccess(() -> Component.translatable(
                    "command.enchanter_letter.letterset.success", type, param, value), true);
            return 1;
        }
        if (GROWTH_TYPES.contains(type.toLowerCase(Locale.ROOT))) {
            // 成长型手札：空手（或非玩家）→ 修改配置文件（旧行为）；手持对应类型手札 → 修改物品 NBT；
            // 手持其他物品 → 不执行（applyGrowthValue 返回 null）
            if (!(context.getSource().getEntity() instanceof Player growthPlayer) || growthPlayer.getMainHandItem().isEmpty()) {
                String result = applyValue(type, param, value);
                if (result == null) {
                    context.getSource().sendFailure(Component.translatable("command.enchanter_letter.letterset.invalid", type, param));
                    return 0;
                }
                ModConfig.save();
                context.getSource().sendSuccess(() -> Component.translatable(
                        "command.enchanter_letter.letterset.success", type, param, value), true);
                return 1;
            }
            String key = applyGrowthValue(context, type, param, value);
            if (key == null) {
                context.getSource().sendFailure(Component.translatable("command.enchanter_letter.letterset.invalid", type, param));
                return 0;
            }
            context.getSource().sendSuccess(() -> Component.translatable(
                    "command.enchanter_letter.letterset.success", type, param, value), true);
            return 1;
        }
        String result = applyValue(type, param, value);
        if (result == null) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.letterset.invalid", type, param));
            return 0;
        }
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterset.success", type, param, value), true);
        return 1;
    }

    /** 定制手札：数值直接写入物品 NBT，不写入配置文件，也不按计数/等级成长计算。 */
    private static String applyCustomValue(CommandContext<CommandSourceStack> context, String param, double value) {
        if (!(context.getSource().getEntity() instanceof Player player)) return null;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof CustomMagicLetterItem)) return null;
        switch (param) {
            case "damage":
                ModDataComponents.setDouble(stack, ModDataComponents.CUSTOM_DAMAGE, value);
                return "custom_damage";
            case "armor":
                ModDataComponents.setDouble(stack, ModDataComponents.CUSTOM_ARMOR, value);
                return "custom_armor";
            case "toughness":
                ModDataComponents.setDouble(stack, ModDataComponents.CUSTOM_TOUGHNESS, value);
                return "custom_toughness";
            case "resistance":
                ModDataComponents.setDouble(stack, ModDataComponents.CUSTOM_RESISTANCE, value);
                return "custom_resistance";
            default:
                return null;
        }
    }


    /**
     * 成长型手札：把 /letterset 的数值直接写入主手持物品的 NBT（不修改配置文件）。
     * 需要玩家执行；主手持物品须为对应类型的成长手札。
     * 返回写入的 NBT 键（或类型名）作为成功标记，失败返回 null。
     */
    private static String applyGrowthValue(CommandContext<CommandSourceStack> context, String type, String param, double value) {
        if (!(context.getSource().getEntity() instanceof Player player)) return null;
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return null;
        String t = type.toLowerCase(Locale.ROOT);
        boolean match;
        switch (t) {
            case "experience": match = stack.getItem() instanceof ExperienceMagicLetterItem; break;
            case "kill": match = stack.getItem() instanceof KillMagicLetterItem; break;
            case "fishing": match = stack.getItem() instanceof FishingMagicLetterItem; break;
            case "travel": match = stack.getItem() instanceof TravelMagicLetterItem; break;
            case "treasure": match = stack.getItem() instanceof TreasureMagicLetterItem; break;
            case "time": match = stack.getItem() instanceof TimeMagicLetterItem; break;
            case "tenacity": match = stack.getItem() instanceof TenacityMagicLetterItem; break;
            case "hero": match = stack.getItem() instanceof HeroMagicLetterItem; break;
            default: match = false;
        }
        if (!match) return null;
        String key = GROWTH_NBT_KEYS.get(t + ":" + param.toLowerCase(Locale.ROOT));
        if (key == null) return null;
        if ("high_level_start".equals(key) || "victories_per_level".equals(key)) {
            ModDataComponents.setGrowthInt(stack, key, (int) value);
        } else {
            ModDataComponents.setGrowthDouble(stack, key, value);
        }
        return key;
    }

    /** 阶段属性数组写入（不足则先补 0 扩容），返回配置键名。 */
    private static String setStageArrayValue(List<Double> values, int index, double value) {
        while (values.size() < index) values.add(0.0);
        values.set(index - 1, value);
        return "stage_" + index;
    }

    /**
     * 应用配置修改，返回 null 表示类型/参数无效。
     */
    private static String applyValue(String type, String param, double value) {
        ModConfig config = ModConfig.getInstance();
        switch (type.toLowerCase()) {
            case "experience":
                if ("time".equals(param)) { config.experienceLetter.expPerLevel = value; return "exp_per_level"; }
                if ("damage".equals(param)) { config.experienceLetter.growthPerLevel = value; return "growth_per_level"; }
                if ("armor".equals(param)) { config.experienceLetter.armorGrowthPerLevel = value; return "armor_growth_per_level"; }
                if ("toughness".equals(param)) { config.experienceLetter.toughnessGrowthPerLevel = value; return "toughness_growth_per_level"; }
                if ("resistance".equals(param)) { config.experienceLetter.resistanceGrowthPerLevel = value; return "resistance_growth_per_level"; }
                return null;
            case "kill":
                if ("time".equals(param)) { config.killLetter.killsPerLevel = (int) value; return "kills_per_level"; }
                if ("damage".equals(param)) { config.killLetter.growthPerLevel = value; return "growth_per_level"; }
                if ("armor".equals(param)) { config.killLetter.armorGrowthPerLevel = value; return "armor_growth_per_level"; }
                if ("toughness".equals(param)) { config.killLetter.toughnessGrowthPerLevel = value; return "toughness_growth_per_level"; }
                if ("resistance".equals(param)) { config.killLetter.resistanceGrowthPerLevel = value; return "resistance_growth_per_level"; }
                return null;
            case "fishing":
                if ("time".equals(param)) { config.fishingLetter.fishPerLevel = (int) value; return "fish_per_level"; }
                if ("damage".equals(param)) { config.fishingLetter.growthPerLevel = value; return "growth_per_level"; }
                if ("armor".equals(param)) { config.fishingLetter.armorGrowthPerLevel = value; return "armor_growth_per_level"; }
                if ("toughness".equals(param)) { config.fishingLetter.toughnessGrowthPerLevel = value; return "toughness_growth_per_level"; }
                if ("resistance".equals(param)) { config.fishingLetter.resistanceGrowthPerLevel = value; return "resistance_growth_per_level"; }
                return null;
            case "travel":
                if ("time".equals(param)) { config.travelLetter.walkDistancePerLevel = value; return "walk_distance_per_level"; }
                if ("time2".equals(param)) { config.travelLetter.flyDistancePerLevel = value; return "fly_distance_per_level"; }
                if ("damage".equals(param)) { config.travelLetter.walkGrowthPerLevel = value; return "walk_growth_per_level"; }
                if ("damage2".equals(param)) { config.travelLetter.flyGrowthPerLevel = value; return "fly_growth_per_level"; }
                if ("armor".equals(param)) { config.travelLetter.armorGrowthPerLevel = value; return "armor_growth_per_level"; }
                if ("armor2".equals(param)) { config.travelLetter.armor2GrowthPerLevel = value; return "armor2_growth_per_level"; }
                if ("toughness".equals(param)) { config.travelLetter.toughnessGrowthPerLevel = value; return "toughness_growth_per_level"; }
                if ("toughness2".equals(param)) { config.travelLetter.toughness2GrowthPerLevel = value; return "toughness2_growth_per_level"; }
                if ("resistance".equals(param)) { config.travelLetter.resistanceGrowthPerLevel = value; return "resistance_growth_per_level"; }
                if ("resistance2".equals(param)) { config.travelLetter.resistance2GrowthPerLevel = value; return "resistance2_growth_per_level"; }
                return null;
            case "treasure":
                if ("time".equals(param)) { config.treasureLetter.opensPerLevel = (int) value; return "opens_per_level"; }
                if ("damage".equals(param)) { config.treasureLetter.growthPerLevel = value; return "growth_per_level"; }
                if ("armor".equals(param)) { config.treasureLetter.armorGrowthPerLevel = value; return "armor_growth_per_level"; }
                if ("toughness".equals(param)) { config.treasureLetter.toughnessGrowthPerLevel = value; return "toughness_growth_per_level"; }
                if ("resistance".equals(param)) { config.treasureLetter.resistanceGrowthPerLevel = value; return "resistance_growth_per_level"; }
                return null;
            case "time":
                if ("time".equals(param)) { config.timeLetter.secondsPerLevel = value; return "seconds_per_level"; }
                if ("damage".equals(param)) { config.timeLetter.growthPerLevel = value; return "growth_per_level"; }
                if ("armor".equals(param)) { config.timeLetter.armorGrowthPerLevel = value; return "armor_growth_per_level"; }
                if ("toughness".equals(param)) { config.timeLetter.toughnessGrowthPerLevel = value; return "toughness_growth_per_level"; }
                if ("resistance".equals(param)) { config.timeLetter.resistanceGrowthPerLevel = value; return "resistance_growth_per_level"; }
                return null;
            case "tenacity":
                if ("time".equals(param)) { config.tenacityLetter.damagePerLevel = value; return "damage_per_level"; }
                if ("damage".equals(param)) { config.tenacityLetter.growthPerLevel = value; return "growth_per_level"; }
                if ("armor".equals(param)) { config.tenacityLetter.armorGrowthPerLevel = value; return "armor_growth_per_level"; }
                if ("toughness".equals(param)) { config.tenacityLetter.toughnessGrowthPerLevel = value; return "toughness_growth_per_level"; }
                if ("resistance".equals(param)) { config.tenacityLetter.resistanceGrowthPerLevel = value; return "resistance_growth_per_level"; }
                return null;
            case "hero":
                if ("time".equals(param)) { config.heroLetter.victoriesPerLevel = (int) value; return "victories_per_level"; }
                if ("time2".equals(param)) { config.heroLetter.highLevelStart = (int) value; return "high_level_start"; }
                if ("damage".equals(param)) { config.heroLetter.growthLowLevels = value; return "growth_low_levels"; }
                if ("damage2".equals(param)) { config.heroLetter.growthHighLevels = value; return "growth_high_levels"; }
                if ("armor".equals(param)) { config.heroLetter.armorGrowthPerLevel = value; return "armor_growth_per_level"; }
                if ("armor2".equals(param)) { config.heroLetter.armor2GrowthPerLevel = value; return "armor2_growth_per_level"; }
                if ("toughness".equals(param)) { config.heroLetter.toughnessGrowthPerLevel = value; return "toughness_growth_per_level"; }
                if ("toughness2".equals(param)) { config.heroLetter.toughness2GrowthPerLevel = value; return "toughness2_growth_per_level"; }
                if ("resistance".equals(param)) { config.heroLetter.resistanceGrowthPerLevel = value; return "resistance_growth_per_level"; }
                if ("resistance2".equals(param)) { config.heroLetter.resistance2GrowthPerLevel = value; return "resistance2_growth_per_level"; }
                return null;
            case "stage":
                // 阶段手札请使用具体阶段类型（stage_1 ~ stage_10）直接配置
                return null;
            case "stage_1": return setStageDamage(config, 1, param, value);
            case "stage_2": return setStageDamage(config, 2, param, value);
            case "stage_3": return setStageDamage(config, 3, param, value);
            case "stage_4": return setStageDamage(config, 4, param, value);
            case "stage_5": return setStageDamage(config, 5, param, value);
            case "stage_6": return setStageDamage(config, 6, param, value);
            case "stage_7": return setStageDamage(config, 7, param, value);
            case "stage_8": return setStageDamage(config, 8, param, value);
            case "stage_9": return setStageDamage(config, 9, param, value);
            case "stage_10": return setStageDamage(config, 10, param, value);
            default:
                return null;
        }
    }

    /** 直接设置某一阶阶段手札的数值：damage 修改倍率，armor/toughness/resistance 修改对应防御属性（各自独立）。 */
    private static String setStageDamage(ModConfig config, int stage, String param, double value) {
        if (stage < 1 || stage > 10) return null;
        if ("damage".equals(param)) {
            setStageArrayValue(config.stageLetters.multipliers, stage, value);
            return "stage_" + stage + "_multiplier";
        }
        if ("armor".equals(param)) {
            setStageArrayValue(config.stageLetters.armorValues, stage, value);
            return "stage_" + stage + "_armor";
        }
        if ("toughness".equals(param)) {
            setStageArrayValue(config.stageLetters.toughnessValues, stage, value);
            return "stage_" + stage + "_toughness";
        }
        if ("resistance".equals(param)) {
            setStageArrayValue(config.stageLetters.resistanceValues, stage, value);
            return "stage_" + stage + "_resistance";
        }
        return null;
    }

    // ==================== /lettertype 与 /letterentity ====================

    private static int addDamageType(CommandContext<CommandSourceStack> context) {
        ResourceLocation id = ResourceLocationArgument.getId(context, "damage_type");
        String normalized = id.toString().toLowerCase(Locale.ROOT);
        ModConfig config = ModConfig.getInstance();
        if (containsId(new ArrayList<>(config.getDefaultBonusDamageTypes()), normalized)) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.lettertype.exists", normalized));
            return 0;
        }
        String old = config.letterBonus.defaultBonusDamageTypes;
        config.letterBonus.defaultBonusDamageTypes = (old == null || old.isEmpty()) ? normalized : old + "," + normalized;
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable("command.enchanter_letter.lettertype.added", normalized), true);
        return 1;
    }

    private static int deleteDamageType(CommandContext<CommandSourceStack> context) {
        ResourceLocation id = ResourceLocationArgument.getId(context, "damage_type");
        String normalized = id.toString().toLowerCase(Locale.ROOT);
        ModConfig config = ModConfig.getInstance();
        List<ResourceLocation> current = new ArrayList<>(config.getDefaultBonusDamageTypes());
        boolean removed = current.removeIf(rl -> rl.toString().toLowerCase(Locale.ROOT).equals(normalized));
        if (!removed) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.lettertype.not_found", normalized));
            return 0;
        }
        config.letterBonus.defaultBonusDamageTypes = joinIds(current);
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable("command.enchanter_letter.lettertype.deleted", normalized), true);
        return 1;
    }

    private static int addEntityType(CommandContext<CommandSourceStack> context) {
        ResourceLocation id = ResourceLocationArgument.getId(context, "entity_type");
        String normalized = id.toString().toLowerCase(Locale.ROOT);
        ModConfig config = ModConfig.getInstance();
        if (containsId(new ArrayList<>(config.getBlacklistEntityNames()), normalized)) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.letterentity.exists", normalized));
            return 0;
        }
        String old = config.magicConversion.blacklistEntityNames;
        config.magicConversion.blacklistEntityNames = (old == null || old.isEmpty()) ? normalized : old + "," + normalized;
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable("command.enchanter_letter.letterentity.added", normalized), true);
        return 1;
    }

    private static int deleteEntityType(CommandContext<CommandSourceStack> context) {
        ResourceLocation id = ResourceLocationArgument.getId(context, "entity_type");
        String normalized = id.toString().toLowerCase(Locale.ROOT);
        ModConfig config = ModConfig.getInstance();
        List<ResourceLocation> current = new ArrayList<>(config.getBlacklistEntityNames());
        boolean removed = current.removeIf(rl -> rl.toString().toLowerCase(Locale.ROOT).equals(normalized));
        if (!removed) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.letterentity.not_found", normalized));
            return 0;
        }
        config.magicConversion.blacklistEntityNames = joinIds(current);
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable("command.enchanter_letter.letterentity.deleted", normalized), true);
        return 1;
    }

    private static boolean containsId(List<ResourceLocation> list, String normalized) {
        for (ResourceLocation id : list) {
            if (id.toString().toLowerCase(Locale.ROOT).equals(normalized)) return true;
        }
        return false;
    }

    private static String joinIds(List<ResourceLocation> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(ids.get(i));
        }
        return sb.toString();
    }

    // ==================== /letterback ====================

    private static int recallOwnLetters(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.translatable("command.enchanter_letter.letterback.player_only"));
            return 0;
        }
        return recallFor(source, player);
    }

    private static int recallPlayerLetters(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Player target = EntityArgument.getPlayer(context, "player");
        return recallFor(source, target);
    }

    /**
     * 检索所有已加载的掉落物，把绑定到 target 的手札/合订本传送到 target 所在坐标（速度归零）。
     */
    private static int recallFor(CommandSourceStack source, Player target) {
        MinecraftServer server = source.getServer();
        UUID targetId = target.getUUID();
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof ItemEntity itemEntity)) continue;
                ItemStack stack = itemEntity.getItem();
                if (stack.isEmpty() || !ModCommonEvents.isOurModItem(stack)) continue;
                Optional<UUID> bound = ModDataComponents.getBoundPlayer(stack);
                if (bound == null || bound.isEmpty() || !bound.get().equals(targetId)) continue;
                itemEntity.teleportTo(target.getX(), target.getY(), target.getZ());
                itemEntity.setDeltaMovement(Vec3.ZERO);
                itemEntity.setPickUpDelay(0);
                count++;
            }
        }
        final int n = count;
        if (n > 0) {
            source.sendSuccess(() -> Component.translatable("command.enchanter_letter.letterback.success", n, target.getDisplayName().getString()), true);
        } else {
            source.sendFailure(Component.translatable("command.enchanter_letter.letterback.none"));
        }
        return n > 0 ? 1 : 0;
    }

    // ==================== /letterinterval ====================

    /** 不带参数：显示当前魔法转化结算间隔。 */
    private static int showInterval(CommandContext<CommandSourceStack> context) {
        double value = ModConfig.getInstance().magicConversion.intervalSeconds;
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterinterval.current", value), true);
        return 1;
    }

    /** 带参数：设置魔法转化结算间隔（秒）并写回配置。 */
    private static int setInterval(CommandContext<CommandSourceStack> context) {
        double value = context.getArgument("value", Double.class);
        ModConfig config = ModConfig.getInstance();
        config.magicConversion.intervalSeconds = value;
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterinterval.success", value), true);
        return 1;
    }

    // ==================== /letterdelay ====================

    /** 不带参数：显示当前重生返还延迟（游戏刻）。 */
    private static int showRespawnDelay(CommandContext<CommandSourceStack> context) {
        int value = ModConfig.getInstance().letterRespawn.restoreDelayTicks;
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterdelay.current", value), true);
        return 1;
    }

    /** 带参数：设置重生返还延迟（游戏刻）并写回配置。 */
    private static int setRespawnDelay(CommandContext<CommandSourceStack> context) {
        int value = context.getArgument("value", Integer.class);
        ModConfig.getInstance().letterRespawn.restoreDelayTicks = value;
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterdelay.success", value), true);
        return 1;
    }

    // ==================== 通用：在线玩家名 <-> UUID ====================

    /** 建议在线玩家名（/letterbinding add 等）。 */
    private static final SuggestionProvider<CommandSourceStack> PLAYER_SUGGESTIONS =
            (context, builder) -> {
                MinecraftServer server = context.getSource().getServer();
                if (server != null) {
                    for (net.minecraft.server.level.ServerPlayer p : server.getPlayerList().getPlayers()) {
                        builder.suggest(p.getGameProfile().getName());
                    }
                }
                return builder.buildFuture();
            };

    /** 建议在线玩家名 + 现有清理名单（add/delete/uuid 通用）。 */
    private static final SuggestionProvider<CommandSourceStack> PLAYER_OR_UUID_SUGGESTIONS =
            (context, builder) -> {
                MinecraftServer server = context.getSource().getServer();
                if (server != null) {
                    for (net.minecraft.server.level.ServerPlayer p : server.getPlayerList().getPlayers()) {
                        builder.suggest(p.getGameProfile().getName());
                    }
                }
                for (String s : ModConfig.getInstance().letterCleanup.targetUuids) {
                    builder.suggest(s);
                }
                return builder.buildFuture();
            };

    /**
     * 把命令参数解析为 UUID 字符串：直接填 UUID 则原样返回；填在线玩家名则解析为其 UUID。
     * 无法解析时返回 null。
     */
    private static String resolveUuidOrPlayer(CommandContext<CommandSourceStack> context, String argName) {
        String s = context.getArgument(argName, String.class);
        try {
            UUID.fromString(s);
            return s;
        } catch (IllegalArgumentException ignored) {
        }
        MinecraftServer server = context.getSource().getServer();
        if (server != null) {
            net.minecraft.server.level.ServerPlayer p = server.getPlayerList().getPlayerByName(s);
            if (p != null) {
                return p.getUUID().toString();
            }
        }
        return null;
    }

    // ==================== /lettervanish ====================

    /** 查询强制消失开关状态。 */
    private static int showVanish(CommandContext<CommandSourceStack> context) {
        boolean enabled = ModConfig.getInstance().letterVanish.enabled;
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.lettervanish.status", stateComponent(enabled)), true);
        return 1;
    }

    private static int setVanishEnabled(CommandContext<CommandSourceStack> context, boolean enabled) {
        ModConfig.getInstance().letterVanish.enabled = enabled;
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.lettervanish.enabled", stateComponent(enabled)), true);
        return 1;
    }

    // ==================== /letterbinding ====================

    /** 查询绑定白名单（uuid_whitelist）。 */
    private static int showBinding(CommandContext<CommandSourceStack> context) {
        List<String> whitelist = ModConfig.getInstance().letterBinding.uuidWhitelist;
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterbinding.status", String.join(", ", whitelist)), true);
        return 1;
    }

    /** 修改手持手札/合订本的绑定玩家 UUID（支持在线玩家名 / 原版 UUID）。 */
    private static int setBindingUuid(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof Player player)) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.letterback.player_only"));
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof MagicLetterItem || stack.getItem() instanceof LetterBinderItem)) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.lettercolor.require_letter"));
            return 0;
        }
        String s = resolveUuidOrPlayer(context, "player");
        if (s == null) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.letterplayer.not_found"));
            return 0;
        }
        ModDataComponents.setBoundPlayer(stack, UUID.fromString(s));
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterbinding.uuid_set", s), true);
        return 1;
    }

    private static int addBindingWhitelist(CommandContext<CommandSourceStack> context) {
        String s = resolveUuidOrPlayer(context, "player");
        if (s == null) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.letterplayer.not_found"));
            return 0;
        }
        List<String> whitelist = ModConfig.getInstance().letterBinding.uuidWhitelist;
        if (!whitelist.contains(s)) {
            whitelist.add(s);
        }
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterbinding.added", s), true);
        return 1;
    }

    private static int deleteBindingWhitelist(CommandContext<CommandSourceStack> context) {
        String s = resolveUuidOrPlayer(context, "player");
        if (s == null) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.letterplayer.not_found"));
            return 0;
        }
        List<String> whitelist = ModConfig.getInstance().letterBinding.uuidWhitelist;
        whitelist.remove(s);
        ModConfig.save();
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterbinding.deleted", s), true);
        return 1;
    }
}