package cn.autoforged.enchanter_letter.network;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import cn.autoforged.enchanter_letter.enchantment.ModEnchantments;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

/** 1.20.1 移植：Fabric 旧式包（PacketByteBuf + 标识符）。 */
public class ConversionDamageTypePayload {
    public static final ResourceLocation PACKET_ID =
            new ResourceLocation(UsefulMagicEnchanterLetterMod.MOD_ID, "conversion_damage_type");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(PACKET_ID, (server, player, handler, buf, responseSender) -> {
            String damageType = buf.readUtf(128);
            server.execute(() -> {
                var stack = player.getMainHandItem();
                if (stack.getItem() != Items.ENCHANTED_BOOK) return;
                if (!ModEnchantments.hasMagicConversion(stack)) return;
                if (!player.isCreative()) return;
                if (ModEnchantments.isValidDamageTypeString(damageType)) {
                    ModDataComponents.setString(stack, ModDataComponents.CONVERSION_DAMAGE_TYPE, damageType);
                }
            });
        });
    }

    /** 客户端发送（仅在客户端调用）。 */
    public static void sendToServer(String damageType) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(damageType, 128);
        ClientPlayNetworking.send(PACKET_ID, buf);
    }
}
