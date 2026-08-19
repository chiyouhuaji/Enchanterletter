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

/**
 * /letterstorage 截取实体并交换槽位物品命令（权限 2/3/4）：
 * /letterstorage                    进入/退出“截取实体”交互模式（退出时清空截取数据）
 * /letterstorage hand              交换截取实体主手 与 玩家手持物品
 * /letterstorage offhand           交换截取实体副手 与 玩家手持物品
 * /letterstorage armor <slot>      交换截取实体盔甲槽(head/chest/legs/feet) 与 玩家手持物品
 * /letterstorage nbt               输出截取实体的完整 NBT（消息栏点击复制）
 */
public class LetterStorageCommand {
    private static final String[] ARMOR_SLOTS = {"head", "chest", "legs", "feet"};

    private static final SuggestionProvider<CommandSourceStack> ARMOR_SUGGESTIONS =
            (context, builder) -> {
                for (String s : ARMOR_SLOTS) builder.suggest(s);
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
                        .executes(LetterStorageCommand::nbtCommand)));
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
