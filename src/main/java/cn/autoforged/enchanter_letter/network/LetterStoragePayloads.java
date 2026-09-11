package cn.autoforged.enchanter_letter.network;

import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import cn.autoforged.enchanter_letter.storage.LetterStorageClient;
import cn.autoforged.enchanter_letter.storage.LetterStorageManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * /letterstorage 相关网络包（Forge 1.20.1，SimpleChannel）：
 * - CaptureMessage（C2S）：客户端在交互模式下空手左键实体时上报该实体 UUID；
 * - ModeMessage（S2C）：服务端同步交互模式开关给客户端（用于客户端拦截空手左键）。
 */
public class LetterStoragePayloads {
    private static final String PROTOCOL_VERSION = "1";
    private static SimpleChannel CHANNEL;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(UsefulMagicEnchanterLetterMod.MOD_ID, "letterstorage"),
                () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
        CHANNEL.registerMessage(0, CaptureMessage.class,
                CaptureMessage::encode, CaptureMessage::decode, CaptureMessage::handle);
        CHANNEL.registerMessage(1, ModeMessage.class,
                ModeMessage::encode, ModeMessage::decode, ModeMessage::handle);
    }

    /** 客户端发送截取包。 */
    public static void sendCaptureToServer(UUID entityId) {
        if (CHANNEL != null) {
            CHANNEL.sendToServer(new CaptureMessage(entityId));
        }
    }

    /** 服务端同步交互模式给指定玩家。 */
    public static void sendModeToClient(ServerPlayer player, boolean active) {
        if (CHANNEL != null) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ModeMessage(active));
        }
    }

    public static class CaptureMessage {
        private final UUID entityId;

        public CaptureMessage() {
            this.entityId = null;
        }

        public CaptureMessage(UUID entityId) {
            this.entityId = entityId;
        }

        public static void encode(CaptureMessage msg, FriendlyByteBuf buf) {
            buf.writeBoolean(msg.entityId != null);
            if (msg.entityId != null) buf.writeUUID(msg.entityId);
        }

        public static CaptureMessage decode(FriendlyByteBuf buf) {
            UUID id = buf.readBoolean() ? buf.readUUID() : null;
            return new CaptureMessage(id);
        }

        public static void handle(CaptureMessage msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player == null || player.server == null) return;
                if (!LetterStorageManager.isCapturing(player)) return;
                if (msg.entityId == null || msg.entityId.equals(player.getUUID())) return;
                Entity target = findEntity(player.server, msg.entityId);
                if (target == null) return;
                LetterStorageManager.setCaptured(player, msg.entityId);
                String name = target.getDisplayName().getString();
                String uuidText = msg.entityId.toString();
                String entityIdText = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
                // 生物名 / UUID / 实体ID 均可点击复制。
                MutableComponent msgComponent = Component.translatable(
                        "command.enchanter_letter.letterstorage.captured_header");
                msgComponent.append(clickableCopy(name, "command.enchanter_letter.letterstorage.copy_name_hint"))
                        .append("  ")
                        .append(clickableCopy(uuidText, "command.enchanter_letter.letterstorage.copy_uuid_hint"))
                        .append("  ")
                        .append(clickableCopy(entityIdText, "command.enchanter_letter.letterstorage.copy_entity_id_hint"));
                player.sendSystemMessage(msgComponent);
            });
            ctx.setPacketHandled(true);
        }
    }

    public static class ModeMessage {
        private final boolean active;

        public ModeMessage() {
            this.active = false;
        }

        public ModeMessage(boolean active) {
            this.active = active;
        }

        public static void encode(ModeMessage msg, FriendlyByteBuf buf) {
            buf.writeBoolean(msg.active);
        }

        public static ModeMessage decode(FriendlyByteBuf buf) {
            return new ModeMessage(buf.readBoolean());
        }

        public static void handle(ModeMessage msg, Supplier<NetworkEvent.Context> ctxSupplier) {
            NetworkEvent.Context ctx = ctxSupplier.get();
            // 客户端：设置交互模式标志（volatile，网络线程写入安全）
            LetterStorageClient.setActive(msg.active);
            ctx.setPacketHandled(true);
        }
    }

    private static MutableComponent clickableCopy(String text, String hoverKey) {
        return Component.literal(text).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable(hoverKey)))
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text)));
    }

    private static Entity findEntity(MinecraftServer server, UUID uuid) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(uuid);
            if (e != null) return e;
        }
        return null;
    }
}