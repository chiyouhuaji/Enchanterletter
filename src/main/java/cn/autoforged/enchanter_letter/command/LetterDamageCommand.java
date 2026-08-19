package cn.autoforged.enchanter_letter.command;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.enchantment.ModEnchantments;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LetterDamageCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("letterdamage")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("damage_type", ResourceKeyArgument.key(Registries.DAMAGE_TYPE))
                                .executes(LetterDamageCommand::execute))
        );
    }

    @SuppressWarnings("unchecked")
    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("This command can only be executed by a player."));
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        if (!ModEnchantments.hasMagicConversion(stack)) {
            source.sendFailure(Component.translatable("command.enchanter_letter.letterdamage.no_enchantment"));
            return 0;
        }
        ResourceKey<?> rawKey = context.getArgument("damage_type", ResourceKey.class);
        var keyOpt = rawKey.cast(Registries.DAMAGE_TYPE);
        if (keyOpt.isEmpty()) {
            source.sendFailure(Component.translatable("command.enchanter_letter.letterdamage.invalid_type", rawKey.location().toString()));
            return 0;
        }
        ResourceKey<DamageType> damageTypeKey = keyOpt.get();
        String damageTypeStr = damageTypeKey.location().toString();

        if (!ModEnchantments.isValidDamageType(source.registryAccess(), damageTypeKey.location())) {
            source.sendFailure(Component.translatable("command.enchanter_letter.letterdamage.invalid_type", damageTypeStr));
            return 0;
        }
        stack.set(ModDataComponents.CONVERSION_DAMAGE_TYPE, damageTypeStr);
        source.sendSuccess(() -> Component.translatable("command.enchanter_letter.letterdamage.success", damageTypeStr), true);
        return 1;
    }
}