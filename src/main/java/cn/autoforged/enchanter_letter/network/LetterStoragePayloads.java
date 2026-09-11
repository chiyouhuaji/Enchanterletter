package cn.autoforged.enchanter_letter.network;

import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import cn.autoforged.enchanter_letter.storage.LetterStorageClient;
import cn.autoforged.enchanter_letter.storage.LetterStorageManager;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.UUID;

/**
 * /letterstorage 相关网络包：
 * - CapturePayload（C2S）：客户端在交互模式下空手左键实体时上报该实体 UUID；
 * - ModePayload（S2C）：服务端同步交互模式开关给客户端（用于客户端拦截空手左键）。
 */
public class LetterStoragePayloads {
    public static final CustomPacketPayload.Type<CapturePayload> CAPTURE_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(UsefulMagicEnchanterLetterMod.MOD_ID, "letterstorage_capture"));

    public static final CustomPacketPayload.Type<ModePayload> MODE_TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(UsefulMagicEnchanterLetterMod.MOD_ID, "letterstorage_mode"));

    public record CapturePayload(UUID entityId) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, CapturePayload> CODEC =
                StreamCodec.composite(UUIDUtil.STREAM_CODEC, CapturePayload::entityId, CapturePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return CAPTURE_TYPE;
        }
    }

    public record ModePayload(boolean active) implements CustomPacketPayload {
        public static final StreamCodec<FriendlyByteBuf, ModePayload> CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, ModePayload::active, ModePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return MODE_TYPE;
        }
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(LetterStoragePayloads::onRegisterPayloadHandlers);
    }

    private static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(CAPTURE_TYPE, CapturePayload.CODEC, LetterStoragePayloads::handleCapture);
        registrar.playToClient(MODE_TYPE, ModePayload.CODEC, LetterStoragePayloads::handleMode);
    }

    /** 服务端：客户端上报截取了某个实体。 */
    private static void handleCapture(CapturePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!LetterStorageManager.isCapturing(player)) return;
            if (payload.entityId() == null) return;
            // 允许截取任意已加载实体（含其他玩家），但不允许截取自己
            if (payload.entityId().equals(player.getUUID())) return;
            Entity target = findEntity(player.server, payload.entityId());
            if (target == null) return;
            LetterStorageManager.setCaptured(player, payload.entityId());
            String name = target.getDisplayName().getString();
            String uuidText = payload.entityId().toString();
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
    }

    /** 客户端：接收交互模式开关。 */
    private static void handleMode(ModePayload payload, IPayloadContext context) {
        LetterStorageClient.setActive(payload.active());
    }

    private static Entity findEntity(MinecraftServer server, UUID uuid) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(uuid);
            if (e != null) return e;
        }
        return null;
    }

    private static MutableComponent clickableCopy(String text, String hoverKey) {
        return Component.literal(text).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable(hoverKey)))
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text)));
    }

    /** 客户端发送截取包。 */
    public static void sendCaptureToServer(UUID entityId) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new CapturePayload(entityId));
    }

    /** 服务端同步交互模式给指定玩家。 */
    public static void sendModeToClient(ServerPlayer player, boolean active) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new ModePayload(active));
    }
}
