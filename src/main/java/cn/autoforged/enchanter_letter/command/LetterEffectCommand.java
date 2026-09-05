package cn.autoforged.enchanter_letter.command;

import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import cn.autoforged.enchanter_letter.item.LetterPotions;
import cn.autoforged.enchanter_letter.item.MagicLetterItem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * /lettereffect —— 手札药水效果配置（权限 2/3/4），状态存于手持手札 NBT，无配置文件。
 *   /lettereffect add <slot> <effect> <seconds> <amplifier> <hideParticles> <loopMode> <looptime> [<looptime2> ...]
 *   /lettereffect delete <slot>
 *   /lettereffect ls
 */
public class LetterEffectCommand {
    private static final SuggestionProvider<CommandSourceStack> EFFECT_SUGGESTIONS = (context, builder) -> {
        try {
            for (var key : context.getSource().getServer().registryAccess()
                    .registryOrThrow(Registries.MOB_EFFECT).keySet()) {
                builder.suggest(key.toString());
            }
        } catch (Exception ignored) {
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> LOOP_MODE_SUGGESTIONS =
            (context, builder) -> {
                builder.suggest("daytime");
                builder.suggest("gametime");
                return builder.buildFuture();
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /lettereffect add <slot> <effect> <seconds> <amplifier> <hideParticles> <loopMode> <looptime> [<looptime2>...]
        ArgumentBuilder<CommandSourceStack, ?> base = Commands.argument("slot", IntegerArgumentType.integer(1))
                .then(Commands.argument("effect", ResourceLocationArgument.id())
                        .suggests(EFFECT_SUGGESTIONS)
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                                .then(Commands.argument("amplifier", IntegerArgumentType.integer(0))
                                        .then(Commands.argument("hideParticles", BoolArgumentType.bool())
                                                .then(Commands.argument("loopMode", StringArgumentType.word())
                                                        .suggests(LOOP_MODE_SUGGESTIONS)
                                                        .then(loopTimes(1)))))));
        dispatcher.register(Commands.literal("lettereffect")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add").then(base))
                .then(Commands.literal("delete")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1))
                                .executes(LetterEffectCommand::delete)))
                .then(Commands.literal("ls")
                        .executes(LetterEffectCommand::list)));
    }

    /** 递归构建 looptime / looptime2 / ... 参数链，最多 8 个时间点。 */
    private static ArgumentBuilder<CommandSourceStack, ?> loopTimes(int n) {
        ArgumentBuilder<CommandSourceStack, ?> node = Commands.argument("looptime" + n, IntegerArgumentType.integer(0, 24000))
                .executes(ctx -> add(ctx, n));
        if (n < 8) {
            return node.then(loopTimes(n + 1));
        }
        return node;
    }

    private static int add(CommandContext<CommandSourceStack> context, int useN) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.letterback.player_only"));
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof MagicLetterItem)) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.lettereffect.require_letter"));
            return 0;
        }
        int slot = context.getArgument("slot", Integer.class);
        ResourceLocation effectId = ResourceLocationArgument.getId(context, "effect");
        String effect = effectId.toString();
        int seconds = context.getArgument("seconds", Integer.class);
        int amplifier = context.getArgument("amplifier", Integer.class);
        boolean hide = context.getArgument("hideParticles", Boolean.class);
        String loopMode = context.getArgument("loopMode", String.class);
        List<Long> times = new ArrayList<>();
        for (int i = 1; i <= useN; i++) {
            try {
                times.add((long) (int) context.getArgument("looptime" + i, Integer.class));
            } catch (Exception e) {
                break;
            }
        }
        if (times.isEmpty()) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.lettereffect.invalid_times"));
            return 0;
        }
        int id = LetterPotions.add(stack, slot, effect, seconds, amplifier, hide, loopMode, times);
        if (id < 0) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.lettereffect.add_fail"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.lettereffect.added", effect, seconds, amplifier, slot), true);
        return 1;
    }

    private static int delete(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.letterback.player_only"));
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof MagicLetterItem)) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.lettereffect.require_letter"));
            return 0;
        }
        int slot = context.getArgument("slot", Integer.class);
        if (!LetterPotions.removeBySlot(stack, slot)) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.lettereffect.not_found", slot));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.translatable(
                "command.enchanter_letter.lettereffect.deleted", slot), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.letterback.player_only"));
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof MagicLetterItem)) {
            context.getSource().sendFailure(Component.translatable("command.enchanter_letter.lettereffect.require_letter"));
            return 0;
        }
        List<CompoundTag> entries = LetterPotions.getAll(stack);
        if (entries.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable("command.enchanter_letter.lettereffect.empty"), true);
            return 1;
        }
        context.getSource().sendSuccess(() -> Component.translatable("command.enchanter_letter.lettereffect.list_header", entries.size()), true);
        for (CompoundTag e : entries) {
            int id = e.getInt("slot");
            String fx = e.getString("effect");
            int sec = e.getInt("seconds");
            int amp = e.getInt("amplifier");
            boolean hide = e.getBoolean("hideParticles");
            String mode = e.getString("loopMode");
            long[] times = e.getLongArray("loopTimes");
            StringBuilder sb = new StringBuilder("#").append(id).append(" ").append(fx)
                    .append(" sec=").append(sec)
                    .append(" level=").append(amp + 1)
                    .append(" hideParticles=").append(hide ? "yes" : "no")
                    .append(" mode=").append(mode)
                    .append(" times=");
            for (long t : times) sb.append(t).append(" ");
            context.getSource().getEntity().sendSystemMessage(Component.literal(sb.toString().trim())
                    .withStyle(ChatFormatting.GRAY));
        }
        return 1;
    }
}
