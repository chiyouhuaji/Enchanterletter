package cn.autoforged.enchanter_letter.network;

import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import cn.autoforged.enchanter_letter.storage.LetterStorageClient;
import cn.autoforged.enchanter_letter.storage.LetterStorageManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.UUID;

/**
 * /letterstorage 相关网络包（Fabric 1.21.1 CustomPacketPayload）：
 * - Capture 包（C2S）：客户端在交互模式下空手左键实体时上报该实体 UUID；
 * - Mode 包（S2C）：服务端同步交互模式开关给客户端。
 */
public class LetterStoragePayloads {
    public record CapturePayload(Optional<UUID> entityId) implements CustomPacketPayload {
        public static final Type<CapturePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(UsefulMagicEnchanterLetterMod.MOD_ID, "letterstorage_capture"));
        public static final StreamCodec<FriendlyByteBuf, CapturePayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), CapturePayload::entityId,
                        CapturePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record ModePayload(boolean active) implements CustomPacketPayload {
        public static final Type<ModePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(UsefulMagicEnchanterLetterMod.MOD_ID, "letterstorage_mode"));
        public static final StreamCodec<FriendlyByteBuf, ModePayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, ModePayload::active,
                        ModePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(CapturePayload.TYPE, CapturePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ModePayload.TYPE, ModePayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CapturePayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                Optional<UUID> entityId = payload.entityId();
                if (!LetterStorageManager.isCapturing(player)) return;
                if (entityId.isEmpty() || entityId.get().equals(player.getUUID())) return;
                UUID id = entityId.get();
                Entity target = findEntity(context.server(), id);
                if (target == null) return;
                // 已在截取同一实体时跳过：避免单次点击由于客户端重复派发 startAttack
                // （handleKeybinds 的 while(consumeClick()) 循环 + 一次点击多次键击）导致
                // 同一实体被“截取两次”、提示出现两次。重新截取同一实体本就是幂等操作。
                if (id.equals(LetterStorageManager.getCapturedId(player))) return;
                LetterStorageManager.setCaptured(player, id);
                String name = target.getDisplayName().getString();
                String uuidText = id.toString();
                String entityIdText = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
                // 生物名 / UUID / 实体ID 均可点击复制。
                MutableComponent msg = Component.translatable(
                        "command.enchanter_letter.letterstorage.captured_header");
                msg.append(clickableCopy(name, "command.enchanter_letter.letterstorage.copy_name_hint"))
                        .append("  ")
                        .append(clickableCopy(uuidText, "command.enchanter_letter.letterstorage.copy_uuid_hint"))
                        .append("  ")
                        .append(clickableCopy(entityIdText, "command.enchanter_letter.letterstorage.copy_entity_id_hint"));
                player.sendSystemMessage(msg);
            });
        });
    }

    /** 客户端注册接收交互模式开关（在 ModClientEvents.register 中调用）。 */
    public static void registerClientReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(ModePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> LetterStorageClient.setActive(payload.active()));
        });
    }

    /** 客户端发送截取包。 */
    public static void sendCaptureToServer(UUID entityId) {
        ClientPlayNetworking.send(new CapturePayload(Optional.ofNullable(entityId)));
    }

    /** 服务端同步交互模式给指定玩家。 */
    public static void sendModeToClient(ServerPlayer player, boolean active) {
        ServerPlayNetworking.send(player, new ModePayload(active));
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
