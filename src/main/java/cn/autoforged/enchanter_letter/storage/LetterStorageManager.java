package cn.autoforged.enchanter_letter.storage;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * /letterstorage 服务端状态管理（纯内存，登出清理）：
 * - 交互模式（mode）：开启后客户端拦截“空手左键”，把点击到的实体 id 发到服务端；
 * - 截取实体（captured）：一个玩家同时只截取一个实体（按实体 UUID 记录）。
 * 用命令再次执行 /letterstorage 会关闭模式并清空截取数据。
 */
public class LetterStorageManager {
    /** 处于 /letterstorage 交互模式（客户端已开启空手左键拦截）的玩家。 */
    private static final Set<UUID> MODE_PLAYERS = new HashSet<>();
    /** 玩家 UUID -> 已截取实体的 UUID。 */
    private static final Map<UUID, UUID> CAPTURED_ENTITIES = new HashMap<>();

    private LetterStorageManager() {
    }

    /** 玩家是否处于 /letterstorage 交互模式。 */
    public static boolean isCapturing(ServerPlayer player) {
        return player != null && MODE_PLAYERS.contains(player.getUUID());
    }

    /** 进入/退出交互模式（退出时同时清空截取数据）。 */
    public static void setMode(ServerPlayer player, boolean on) {
        if (player == null) return;
        UUID id = player.getUUID();
        if (on) {
            MODE_PLAYERS.add(id);
        } else {
            MODE_PLAYERS.remove(id);
            CAPTURED_ENTITIES.remove(id);
        }
    }

    /** 切换交互模式，返回切换后是否处于开启状态。 */
    public static boolean toggle(ServerPlayer player) {
        boolean on = !isCapturing(player);
        setMode(player, on);
        return on;
    }

    /** 截取模式下记录玩家截取的实体。 */
    public static void setCaptured(ServerPlayer player, UUID entityId) {
        if (player == null || entityId == null) return;
        if (!isCapturing(player)) return;
        CAPTURED_ENTITIES.put(player.getUUID(), entityId);
    }

    /** 玩家当前截取的实体 UUID（未截取返回 null）。 */
    public static UUID getCapturedId(ServerPlayer player) {
        if (player == null || !isCapturing(player)) return null;
        return CAPTURED_ENTITIES.get(player.getUUID());
    }

    /** 清空当前截取数据（保留交互模式）。 */
    public static void clearCaptured(ServerPlayer player) {
        if (player != null) CAPTURED_ENTITIES.remove(player.getUUID());
    }

    /** 登出：释放所有状态。 */
    public static void onLogout(UUID playerUuid) {
        if (playerUuid == null) return;
        MODE_PLAYERS.remove(playerUuid);
        CAPTURED_ENTITIES.remove(playerUuid);
    }

    /**
     * 解析玩家当前截取的实体（按 UUID 在已加载世界查找）。
     * 实体不存在或已死亡时立即清空截取数据并返回 null。
     */
    public static Entity resolveCaptured(MinecraftServer server, ServerPlayer player) {
        UUID capturedId = getCapturedId(player);
        if (capturedId == null || server == null) return null;
        Entity entity = findEntity(server, capturedId);
        if (entity == null || (entity instanceof LivingEntity le && !le.isAlive())) {
            clearCaptured(player);
            return null;
        }
        return entity;
    }

    private static Entity findEntity(MinecraftServer server, UUID uuid) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(uuid);
            if (e != null) return e;
        }
        return null;
    }
}
