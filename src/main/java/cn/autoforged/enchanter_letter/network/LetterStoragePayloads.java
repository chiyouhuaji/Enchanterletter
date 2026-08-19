package cn.autoforged.enchanter_letter.network;

import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import cn.autoforged.enchanter_letter.storage.LetterStorageClient;
import cn.autoforged.enchanter_letter.storage.LetterStorageManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * /letterstorage 相关网络包（Fabric 1.20.1，旧式 PacketByteBuf + 标识符）：
 * - Capture 包（C2S）：客户端在交互模式下空手左键实体时上报该实体 UUID；
 * - Mode 包（S2C）：服务端同步交互模式开关给客户端（用于客户端拦截空手左键）。
 */
public class LetterStoragePayloads {
    public static final ResourceLocation CAPTURE_PACKET_ID =
            new ResourceLocation(UsefulMagicEnchanterLetterMod.MOD_ID, "letterstorage_capture");
    public static final ResourceLocation MODE_PACKET_ID =
            new ResourceLocation(UsefulMagicEnchanterLetterMod.MOD_ID, "letterstorage_mode");

    public static void register() {
        // 服务端接收截取
        ServerPlayNetworking.registerGlobalReceiver(CAPTURE_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            UUID entityId = buf.readBoolean() ? buf.readUUID() : null;
            server.execute(() -> {
                if (!LetterStorageManager.isCapturing(player)) return;
                if (entityId == null || entityId.equals(player.getUUID())) return;
                Entity target = findEntity(server, entityId);
                if (target == null) return;
                LetterStorageManager.setCaptured(player, entityId);
                String name = target.getDisplayName().getString();
                player.sendSystemMessage(Component.translatable(
                        "command.enchanter_letter.letterstorage.captured", name, entityId.toString()));
            });
        });
    }

    /** 客户端注册接收交互模式开关（在 ModClientEvents.register 中调用）。 */
    public static void registerClientReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(MODE_PACKET_ID, (client, handler, buf, responseSender) -> {
            boolean active = buf.readBoolean();
            client.execute(() -> LetterStorageClient.setActive(active));
        });
    }

    /** 客户端发送截取包。 */
    public static void sendCaptureToServer(UUID entityId) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(entityId != null);
        if (entityId != null) buf.writeUUID(entityId);
        ClientPlayNetworking.send(CAPTURE_PACKET_ID, buf);
    }

    /** 服务端同步交互模式给指定玩家。 */
    public static void sendModeToClient(ServerPlayer player, boolean active) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(active);
        ServerPlayNetworking.send(player, MODE_PACKET_ID, buf);
    }

    private static Entity findEntity(MinecraftServer server, UUID uuid) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(uuid);
            if (e != null) return e;
        }
        return null;
    }
}
