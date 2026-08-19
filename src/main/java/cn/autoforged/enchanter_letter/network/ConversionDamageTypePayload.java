package cn.autoforged.enchanter_letter.network;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import cn.autoforged.enchanter_letter.enchantment.ModEnchantments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public record ConversionDamageTypePayload(String damageType) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConversionDamageTypePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(UsefulMagicEnchanterLetterMod.MOD_ID, "conversion_damage_type"));

    public static final StreamCodec<FriendlyByteBuf, ConversionDamageTypePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, ConversionDamageTypePayload::damageType,
                    ConversionDamageTypePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ConversionDamageTypePayload::onRegisterPayloadHandlers);
    }

    private static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(TYPE, CODEC, ConversionDamageTypePayload::handle);
    }

    private static void handle(ConversionDamageTypePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var stack = player.getMainHandItem();
            if (stack.getItem() != Items.ENCHANTED_BOOK) return;
            if (!ModEnchantments.hasMagicConversion(stack)) return;
            if (!player.isCreative()) return;
            String input = payload.damageType();
            if (ModEnchantments.isValidDamageTypeString(input)) {
                stack.set(ModDataComponents.CONVERSION_DAMAGE_TYPE.get(), input);
            }
        });
    }
}
