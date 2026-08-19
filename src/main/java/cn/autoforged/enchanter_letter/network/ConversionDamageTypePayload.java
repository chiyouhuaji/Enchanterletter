package cn.autoforged.enchanter_letter.network;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import cn.autoforged.enchanter_letter.enchantment.ModEnchantments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/** 1.20.1 移植：使用 SimpleChannel 传输转化伤害类型设置消息。 */
public class ConversionDamageTypePayload {
    private static final String PROTOCOL_VERSION = "1";
    private static SimpleChannel CHANNEL;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(UsefulMagicEnchanterLetterMod.MOD_ID, "main"),
                () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
        CHANNEL.registerMessage(0, ConversionMessage.class,
                ConversionMessage::encode, ConversionMessage::decode, ConversionMessage::handle);
    }

    public static void sendToServer(String damageType) {
        if (CHANNEL != null) {
            CHANNEL.sendToServer(new ConversionMessage(damageType));
        }
    }

    public static class ConversionMessage {
        private String damageType;

        public ConversionMessage() {
        }

        public ConversionMessage(String damageType) {
            this.damageType = damageType;
        }

        public static void encode(ConversionMessage msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.damageType == null ? "" : msg.damageType, 128);
        }

        public static ConversionMessage decode(FriendlyByteBuf buf) {
            return new ConversionMessage(buf.readUtf(128));
        }

        public static void handle(ConversionMessage msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player == null) return;
                var stack = player.getMainHandItem();
                if (stack.getItem() != Items.ENCHANTED_BOOK) return;
                if (!ModEnchantments.hasMagicConversion(stack)) return;
                if (!player.isCreative()) return;
                String input = msg.damageType;
                if (ModEnchantments.isValidDamageTypeString(input)) {
                    ModDataComponents.setString(stack, ModDataComponents.CONVERSION_DAMAGE_TYPE, input);
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}
