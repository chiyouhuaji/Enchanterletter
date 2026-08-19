package cn.autoforged.enchanter_letter.network;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import cn.autoforged.enchanter_letter.enchantment.ModEnchantments;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

public record ConversionDamageTypePayload(String damageType) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConversionDamageTypePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    UsefulMagicEnchanterLetterMod.MOD_ID, "conversion_damage_type"));
    public static final StreamCodec<FriendlyByteBuf, ConversionDamageTypePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, ConversionDamageTypePayload::damageType,
                    ConversionDamageTypePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(TYPE, STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
            context.server().execute(() -> {
                var player = context.player();
                var stack = player.getMainHandItem();
                if (stack.getItem() != Items.ENCHANTED_BOOK) return;
                if (!ModEnchantments.hasMagicConversion(stack)) return;
                if (!player.isCreative()) return;
                String damageType = payload.damageType();
                if (ModEnchantments.isValidDamageTypeString(damageType)) {
                    stack.set(ModDataComponents.CONVERSION_DAMAGE_TYPE, damageType);
                }
            });
        });
    }

    /** 客户端发送（仅在客户端调用）。 */
    public static void sendToServer(String damageType) {
        ClientPlayNetworking.send(new ConversionDamageTypePayload(damageType));
    }
}
