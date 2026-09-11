package cn.autoforged.enchanter_letter.command;

import cn.autoforged.enchanter_letter.network.LetterStoragePayloads;
import cn.autoforged.enchanter_letter.storage.LetterStorageManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * /letterstorage 截取实体并交换槽位物品命令（权限 2/3/4）：
 * /letterstorage                    进入/退出“截取实体”交互模式（退出时清空截取数据）
 * /letterstorage hand              交换截取实体主手 与 玩家手持物品
 * /letterstorage offhand           交换截取实体副手 与 玩家手持物品
 * /letterstorage armor <slot>      交换截取实体盔甲槽(head/chest/legs/feet) 与 玩家手持物品
 * /letterstorage nbt               输出截取实体的完整 NBT（消息栏点击复制）
 * /letterstorage nbt <uuid|玩家>    直接按 UUID 锁定玩家/实体（必须存在）并输出其 NBT（tab 可检索在线玩家）
 */
public class LetterStorageCommand {
    private static final String[] ARMOR_SLOTS = {"head", "chest", "legs", "feet"};

    private static final SuggestionProvider<CommandSourceStack> ARMOR_SUGGESTIONS =
            (context, builder) -> {
                for (String s : ARMOR_SLOTS) builder.suggest(s);
                return builder.buildFuture();
            };

    /** tab 检索当前存在的在线玩家（按游戏名建议，uuid 子命令用）。 */
    private static final SuggestionProvider<CommandSourceStack> TARGET_SUGGESTIONS =
            (context, builder) -> {
                try {
                    for (var p : context.getSource().getServer().getPlayerList().getPlayers()) {
                        builder.suggest(p.getGameProfile().getName());
                    }
                } catch (Exception ignored) {
                }
                return builder.buildFuture();
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("letterstorage")
                .requires(source -> source.hasPermission(2))
                .executes(LetterStorageCommand::toggleCommand)
                .then(Commands.literal("hand")
                        .executes(ctx -> swapSlot(ctx, EquipmentSlot.MAINHAND)))
                .then(Commands.literal("offhand")
                        .executes(ctx -> swapSlot(ctx, EquipmentSlot.OFFHAND)))
                .then(Commands.literal("armor")
                        .then(Commands.argument("slot", StringArgumentType.word())
                                .suggests(ARMOR_SUGGESTIONS)
                                .executes(LetterStorageCommand::swapArmor)))
                .then(Commands.literal("nbt")
                        .executes(LetterStorageCommand::nbtCommand)
                        .then(Commands.argument("target", StringArgumentType.string())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(LetterStorageCommand::nbtTargetCommand))));
    }

    /** /letterstorage：切换交互模式（开启 / 关闭并清空截取）。 */
    private static int toggleCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.enchanter_letter.letterstorage.player_only"));
            return 0;
        }
        boolean now = LetterStorageManager.toggle(player);
        LetterStoragePayloads.sendModeToClient(player, now);
        if (now) {
            source.sendSuccess(() -> Component.translatable("command.enchanter_letter.letterstorage.toggle_on"), true);
        } else {
            source.sendSuccess(() -> Component.translatable("command.enchanter_letter.letterstorage.toggle_off"), true);
        }
        return 1;
    }

    /** /letterstorage armor <head|chest|legs|feet>。 */
    private static int swapArmor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String slot = context.getArgument("slot", String.class);
        EquipmentSlot target;
        switch (slot.toLowerCase()) {
            case "head": target = EquipmentSlot.HEAD; break;
            case "chest": target = EquipmentSlot.CHEST; break;
            case "legs": target = EquipmentSlot.LEGS; break;
            case "feet": target = EquipmentSlot.FEET; break;
            default:
                context.getSource().sendFailure(Component.translatable(
                        "command.enchanter_letter.letterstorage.invalid_slot", slot));
                return 0;
        }
        return swapSlot(context, target);
    }

    /** 交换截取实体指定槽位物品 与 玩家手持物品。 */
    private static int swapSlot(CommandContext<CommandSourceStack> context, EquipmentSlot target) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.enchanter_letter.letterstorage.player_only"));
            return 0;
        }
        if (!LetterStorageManager.isCapturing(player)) {
            source.sendFailure(Component.translatable("command.enchanter_letter.letterstorage.not_in_mode"));
            return 0;
        }
        Entity captured = LetterStorageManager.resolveCaptured(source.getServer(), player);
        if (captured == null) {
            source.sendFailure(Component.translatable("command.enchanter_letter.letterstorage.no_capture"));
            return 0;
        }
        if (!(captured instanceof LivingEntity living)) {
            source.sendFailure(Component.translatable("command.enchanter_letter.letterstorage.no_slots"));
            return 0;
        }
        ItemStack entityItem = living.getItemBySlot(target).copy();
        ItemStack playerItem = player.getMainHandItem().copy();
        living.setItemSlot(target, playerItem);
        // 实体原槽位物品回到玩家主手
        player.getInventory().setItem(player.getInventory().selected, entityItem);
        String name = captured.getDisplayName().getString();
        source.sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.letterstorage.swap_success", name, slotLabel(target)), true);
        return 1;
    }

    /** /letterstorage nbt：实时获取截取实体的完整 NBT 并输出（可点击复制）。 */
    private static int nbtCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.enchanter_letter.letterstorage.player_only"));
            return 0;
        }
        // 再次获取实体，保持数据最新；实体不存在则立即清空截取数据
        Entity captured = LetterStorageManager.resolveCaptured(source.getServer(), player);
        if (captured == null) {
            source.sendFailure(Component.translatable("command.enchanter_letter.letterstorage.no_capture"));
            return 0;
        }
        CompoundTag tag = new CompoundTag();
        captured.saveWithoutId(tag);
        String nbt = tag.toString();
        MutableComponent header = Component.translatable(
                "command.enchanter_letter.letterstorage.nbt_header", captured.getDisplayName().getString());
        Component clickable = Component.literal(nbt).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("command.enchanter_letter.letterstorage.nbt_copy_hint")))
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, nbt)));
        player.sendSystemMessage(header.append(" ").append(clickable));
        return 1;
    }

    /**
     * /letterstorage nbt <uuid|玩家>：按 UUID 锁定玩家/实体（必须存在）并输出其 NBT。
     * 目标可为 UUID 或在线玩家名（tab 检索）；实体必须存在于已加载世界，否则报错且不锁定。
     * 成功时若执行者为玩家则将该目标设为截取实体（后续 hand/armor 等可直接使用），并输出完整 NBT。
     */
    private static int nbtTargetCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String target = context.getArgument("target", String.class);
        Entity entity = null;
        // 1) 尝试按 UUID 解析
        try {
            UUID uuid = UUID.fromString(target.trim());
            entity = findEntity(source.getServer(), uuid);
        } catch (IllegalArgumentException ignored) {
            // 2) 尝试按在线玩家名解析
            ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(target);
            if (online != null) entity = online;
        }
        if (entity == null) {
            source.sendFailure(Component.translatable("command.enchanter_letter.letterstorage.target_not_found", target));
            return 0;
        }
        if (entity instanceof LivingEntity le && !le.isAlive()) {
            source.sendFailure(Component.translatable("command.enchanter_letter.letterstorage.target_not_found", target));
            return 0;
        }
        // 玩家执行者：锁定为截取实体
        if (source.getEntity() instanceof ServerPlayer player) {
            LetterStorageManager.setCaptured(player, entity.getUUID());
        }
        CompoundTag tag = new CompoundTag();
        entity.saveWithoutId(tag);
        String nbt = tag.toString();
        MutableComponent header = Component.translatable(
                "command.enchanter_letter.letterstorage.nbt_header", entity.getDisplayName().getString());
        // 生物名 / UUID / 实体ID 均可点击复制。
        MutableComponent info = Component.translatable(
                "command.enchanter_letter.letterstorage.captured_header");
        info.append(clickableCopy(entity.getDisplayName().getString(), "command.enchanter_letter.letterstorage.copy_name_hint"))
                .append("  ")
                .append(clickableCopy(entity.getUUID().toString(), "command.enchanter_letter.letterstorage.copy_uuid_hint"))
                .append("  ")
                .append(clickableCopy(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(), "command.enchanter_letter.letterstorage.copy_entity_id_hint"));
        Component clickable = Component.literal(nbt).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("command.enchanter_letter.letterstorage.nbt_copy_hint")))
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, nbt)));
        source.sendSuccess(() -> header.append(" ").append(clickable).append("\n").append(info), true);
        return 1;
    }

    private static MutableComponent clickableCopy(String text, String hoverKey) {
        return Component.literal(text).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable(hoverKey)))
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text)));
    }

    private static Entity findEntity(net.minecraft.server.MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) return null;
        for (var level : server.getAllLevels()) {
            Entity e = level.getEntity(uuid);
            if (e != null) return e;
        }
        return null;
    }

    private static String slotLabel(EquipmentSlot slot) {
        switch (slot) {
            case HEAD: return "头部";
            case CHEST: return "胸部";
            case LEGS: return "腿部";
            case FEET: return "脚部";
            case OFFHAND: return "副手";
            default: return "主手";
        }
    }
}
