package cn.autoforged.enchanter_letter.event;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.config.ModConfig;
import cn.autoforged.enchanter_letter.enchantment.ModEnchantments;
import cn.autoforged.enchanter_letter.integration.CuriosIntegration;
import cn.autoforged.enchanter_letter.item.LetterBinderItem;
import cn.autoforged.enchanter_letter.item.MagicLetterItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.PlayerTeam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 手札对实体（玩家/生物）的附加效果：
 * - 光灵附魔：持有者发光（RGB 平均混合，经 /lettercolor 或 NBT 调色；1.20.1/1.21.1 统一用队伍颜色近似）；
 * - 消失诅咒（生物）：装备有该附魔的手札/合订本立即绑定零 UUID 防止窃取；
 * - 定时清理：每 N 秒清理绑定到配置名单 UUID 的手札/合订本掉落物；
 * - 死亡处理：玩家魔法绑定物品保留重生返还；消失诅咒物品死亡移除，
 *   合订本内的非诅咒手札返还物品栏（溢出在重生点生成掉落物）。
 */
public class LetterEntityEffects {
    /** 发光队伍名前缀（队伍按实体 id 命名，长度受 16 字符限制）。 */
    private static final String TEAM_PREFIX = "elg_";

    /** 死亡保留物品：物品 + 原饰品槽位信息（curioType/index；非饰品槽为 null/-1）。 */
    private static class KeptItem {
        final ItemStack stack;
        final String curioType;
        final int curioIndex;
        /** 是否已处理完成（成功装回槽位或已放入背包），用于重试时避免重复归还。 */
        boolean restored;

        KeptItem(ItemStack stack, String curioType, int curioIndex) {
            this.stack = stack;
            this.curioType = curioType;
            this.curioIndex = curioIndex;
        }
    }

    /** 死亡保留池：UUID -> 需在重生时返还的物品（魔法绑定物品，含饰品槽原位）。 */
    private static final Map<UUID, List<KeptItem>> DEATH_KEEP_POOL = new HashMap<>();

    /** 饰品装回失败后的额外重试时长（tick）：重生槽位延迟就绪时逐 tick 重试，超时后回退背包。 */
    private static final int MAX_RESTORE_RETRY_TICKS = 60;

    /** 待处理的重生任务：经 /letterdelay 延迟（游戏刻）后执行饰品原位装回与消失诅咒检查。 */
    private static class PendingRespawn {
        final List<KeptItem> keep;
        int countdown;
        int retriesLeft;

        PendingRespawn(List<KeptItem> keep, int countdown) {
            this.keep = keep;
            this.countdown = countdown;
            this.retriesLeft = MAX_RESTORE_RETRY_TICKS;
        }
    }

    /** 重生待处理队列：UUID -> 延迟任务（延迟结束后装回保留物品并扫描删除消失诅咒物品）。 */
    private static final Map<UUID, PendingRespawn> PENDING_RESPAWNS = new HashMap<>();

    private static int glowCooldown = 10;
    private static int cleanCooldown = 20;
    private static int teamSweepCooldown = 600;

    /** 已施加的队伍颜色缓存（实体 UUID -> 上次使用的颜色），用于避免重复改队伍颜色省包。 */
    private static final Map<UUID, ChatFormatting> appliedGlowColor = new HashMap<>();

    private LetterEntityEffects() {
    }

    /** 服务器 tick 调用：发光同步、零 UUID 绑定、掉落物定时清理、重生延迟任务。 */
    public static void tick(MinecraftServer server) {
        if (--glowCooldown <= 0) {
            glowCooldown = 10;
            syncGlowAndZeroBinding(server);
        }
        if (--teamSweepCooldown <= 0) {
            teamSweepCooldown = 600;
            sweepTeams(server);
        }
        if (--cleanCooldown <= 0) {
            cleanCooldown = Math.max(20, (int) (ModConfig.getInstance().letterCleanup.intervalSeconds * 20));
            cleanupBoundDrops(server);
        }
        tickPendingRespawns(server);
    }

    // ==================== 光灵发光 ====================

    private static void syncGlowAndZeroBinding(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            applyGlow(player, collectGlowColors(player));
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (LivingEntity entity : level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(net.minecraft.world.entity.LivingEntity.class),
                    ent -> !(ent instanceof Player))) {
                applyGlow(entity, collectGlowColors(entity));
            }
        }
    }

    /** 收集实体携带的全部光灵颜色（玩家含背包/饰品/合订本内容；生物为装备槽）。 */
    private static List<Integer> collectGlowColors(Player player) {
        List<Integer> colors = new ArrayList<>();
        scanPlayerSlots(player, stack -> {
            if (ModEnchantments.hasGlowing(stack)) {
                colors.add(ModEnchantments.getGlowColorOrDefault(stack));
            }
        });
        return colors;
    }

    private static List<Integer> collectGlowColors(LivingEntity entity) {
        List<Integer> colors = new ArrayList<>();
        for (ItemStack stack : entity.getArmorSlots()) collectGlowColor(stack, colors);
        for (ItemStack stack : entity.getHandSlots()) collectGlowColor(stack, colors);
        // 饰品槽（生物也可能佩戴饰品）；玩家路径在 scanPlayerSlots 已含 Curios
        for (ItemStack stack : CuriosIntegration.getCuriosStacks(entity)) collectGlowColor(stack, colors);
        return colors;
    }

    private static void collectGlowColor(ItemStack stack, List<Integer> colors) {
        if (stack.isEmpty() || !ModEnchantments.hasGlowing(stack)) return;
        colors.add(ModEnchantments.getGlowColorOrDefault(stack));
    }

    /** RGB 平均混合：各通道取均值；全 0 视为白色。 */
    private static int averageColor(List<Integer> colors) {
        if (colors.isEmpty()) return 0xFFFFFF;
        int r = 0, g = 0, b = 0;
        for (int c : colors) {
            r += (c >> 16) & 0xFF;
            g += (c >> 8) & 0xFF;
            b += c & 0xFF;
        }
        int n = colors.size();
        r = r / n;
        g = g / n;
        b = b / n;
        return (r << 16) | (g << 8) | b;
    }

    /** 发光：设置队伍颜色 + 发光标记；无光灵时清除。 */
    private static void applyGlow(LivingEntity entity, List<Integer> colors) {
        if (colors.isEmpty()) {
            clearGlow(entity);
            return;
        }
        if (entity.level().isClientSide) return;
        int color = averageColor(colors);
        ChatFormatting fmt = nearestFormatting(color);
        UUID uuid = entity.getUUID();
        String teamName = TEAM_PREFIX + entity.getId();
        Scoreboard scoreboard = entity.level().getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
        }
        // 颜色变化时才更新队伍颜色（避免每 tick 刷队色更新包）
        ChatFormatting prev = appliedGlowColor.get(uuid);
        if (prev != fmt) {
            team.setColor(fmt);
            appliedGlowColor.put(uuid, fmt);
        }
        // 幂等：把实体加入队伍（addPlayerToTeam 会自动从原队伍移除）
        scoreboard.addPlayerToTeam(entity.getScoreboardName(), team);
        entity.setGlowingTag(true);
        if (appliedGlowColor.size() > 5000) {
            // 兜底防无限增长（正常会在 clearGlow/sweepTeams 中移除）
            appliedGlowColor.clear();
        }
    }

    private static void clearGlow(LivingEntity entity) {
        if (entity.level().isClientSide) return;
        boolean wasApplied = appliedGlowColor.remove(entity.getUUID()) != null;
        if (entity.hasGlowingTag()) {
            entity.setGlowingTag(false);
        }
        Scoreboard scoreboard = entity.level().getScoreboard();
        String teamName = TEAM_PREFIX + entity.getId();
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) return;
        // 仅当本模组曾把该实体加入 elg_ 队伍时才移除，避免误拆其他模组/原版的队伍关系
        if (wasApplied) {
            scoreboard.removePlayerFromTeam(entity.getScoreboardName(), team);
        }
        if (team.getPlayers().isEmpty()) {
            scoreboard.removePlayerTeam(team);
        }
    }

    /** 周期清理残留队伍（实体销毁后遗留）。 */
    private static void sweepTeams(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            Scoreboard scoreboard = level.getScoreboard();
            List<PlayerTeam> toRemove = new ArrayList<>();
            for (PlayerTeam team : scoreboard.getPlayerTeams()) {
                if (team.getName().startsWith(TEAM_PREFIX) && team.getPlayers().isEmpty()) {
                    toRemove.add(team);
                }
            }
            for (PlayerTeam team : toRemove) {
                scoreboard.removePlayerTeam(team);
            }
        }
    }

    private static ChatFormatting nearestFormatting(int color) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        ChatFormatting best = ChatFormatting.WHITE;
        int bestDist = Integer.MAX_VALUE;
        for (ChatFormatting fmt : ChatFormatting.values()) {
            if (!fmt.isColor()) continue;
            Integer c = fmt.getColor();
            if (c == null) continue;
            int fr = (c >> 16) & 0xFF, fg = (c >> 8) & 0xFF, fb = c & 0xFF;
            int dist = (fr - r) * (fr - r) + (fg - g) * (fg - g) + (fb - b) * (fb - b);
            if (dist < bestDist) {
                bestDist = dist;
                best = fmt;
            }
        }
        return best;
    }

    // ==================== 掉落物定时清理 ====================

    private static void cleanupBoundDrops(MinecraftServer server) {
        ModConfig.LetterCleanupConfig cfg = ModConfig.getInstance().letterCleanup;
        if (!cfg.enabled || cfg.targetUuids.isEmpty()) return;
        Set<String> targets = new HashSet<>(cfg.targetUuids);
        for (ServerLevel level : server.getAllLevels()) {
            for (ItemEntity itemEntity : level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(net.minecraft.world.entity.item.ItemEntity.class), e -> true)) {
                ItemStack stack = itemEntity.getItem();
                if (stack.isEmpty() || !ModCommonEvents.isOurModItem(stack)) continue;
                Optional<UUID> bound = stack.get(ModDataComponents.BOUND_PLAYER.get());
                if (bound != null && bound.isPresent() && targets.contains(bound.get().toString())) {
                    itemEntity.discard();
                }
            }
        }
    }

    // ==================== 玩家死亡处理（魔法绑定保留 / 消失诅咒移除） ====================

    /** 玩家死亡（Player.die HEAD，掉落生成之前）：处理背包/盔甲/副手/饰品槽。 */
    public static void handlePlayerDeath(Player player) {
        if (player.level().isClientSide) return;
        boolean vanish = ModConfig.getInstance().letterVanish.enabled;
        List<KeptItem> keep = new ArrayList<>();
        handleContainer(player, player.getInventory().items, keep, vanish);
        handleContainer(player, player.getInventory().armor, keep, vanish);
        handleContainer(player, player.getInventory().offhand, keep, vanish);
        // 饰品栏（Curios）：死亡事件后、掉落机制前，先截取死亡瞬间饰品栏全部数据（纯读取快照）。
        // 魔法绑定物品：由 Curios DropRule(ALWAYS_KEEP) 原生保留在原槽位（死亡不掉落、重生天然原位，
        //   与 Enigmatic Legacy cursed_ring 同方案），此处只记录快照供重生校验兜底，不再手动取出；
        // 消失诅咒物品（强制消失开启时）：取出销毁。
        List<CuriosIntegration.CurioSocket> curiosSnapshot = CuriosIntegration.snapshotAllSlots(player);
        cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod.LOGGER.info(
                "[EL-diag] death: player={} vanish={} curiosSlots={}",
                player.getName().getString(), vanish, curiosSnapshot.size());
        for (CuriosIntegration.CurioSocket cs : curiosSnapshot) {
            if (cs.stack.isEmpty()) continue;
            boolean vanishItem = vanish && ModEnchantments.hasVanishing(cs.stack);
            if (vanishItem) {
                // 消失诅咒物品：取出销毁
                CuriosIntegration.removeStackFromSlot(player, cs.slotType, cs.slot);
            } else if (ModEnchantments.hasEffectiveMagicBinding(cs.stack)) {
                // 魔法绑定：不取出（Curios DropRule 保留原位），记录快照供重生校验
                keep.add(new KeptItem(cs.stack.copy(), cs.slotType, cs.slot));
            }
        }
        if (!keep.isEmpty()) {
            DEATH_KEEP_POOL.put(player.getUUID(), keep);
            cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod.LOGGER.info(
                    "[EL-diag] death keep: player={} kept={}", player.getName().getString(), keep.size());
        }
    }

    private static void handleContainer(Player player, List<ItemStack> container, List<KeptItem> keep, boolean vanish) {
        for (int i = 0; i < container.size(); i++) {
            ItemStack stack = container.get(i);
            if (stack.isEmpty()) continue;
            if (vanish && ModEnchantments.hasVanishing(stack)) {
                container.set(i, ItemStack.EMPTY); // 强制消失：销毁（即使开启原版死亡不掉落）
            } else if (ModEnchantments.hasEffectiveMagicBinding(stack)) {
                keep.add(new KeptItem(stack.copy(), null, -1));
                container.set(i, ItemStack.EMPTY);
            }
        }
    }

    /**
     * 重生：登记延迟返还任务。等待 /letterdelay（游戏刻，默认 20）后：
     * 1) 返还死亡保留池（魔法绑定物品；饰品槽物品强制装回原栏位并覆盖，否则回退背包/掉落）；
     * 2) 扫描玩家全部槽位并立即删除消失诅咒物品（强制消失开启时）。
     * 延迟等待是为了让饰品槽位/背包在重生后加载完成，避免有槽位却装回失败。
     */
    public static void onPlayerRespawn(Player player) {
        if (player.level().isClientSide) return;
        List<KeptItem> keep = DEATH_KEEP_POOL.remove(player.getUUID());
        int delay = Math.max(0, ModConfig.getInstance().letterRespawn.restoreDelayTicks);
        PENDING_RESPAWNS.put(player.getUUID(), new PendingRespawn(keep == null ? List.of() : keep, delay));
    }

    /** 每 tick 处理重生延迟任务：倒计时归零后执行饰品原位装回 + 消失诅咒物品扫描删除。 */
    private static void tickPendingRespawns(MinecraftServer server) {
        if (PENDING_RESPAWNS.isEmpty()) return;
        List<UUID> finished = new ArrayList<>();
        for (Map.Entry<UUID, PendingRespawn> entry : new ArrayList<>(PENDING_RESPAWNS.entrySet())) {
            PendingRespawn pending = entry.getValue();
            if (pending.countdown > 0) {
                pending.countdown--;
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                boolean allRestored = restoreKeptItems(player, pending.keep);
                sweepVanishingItems(player);
                if (!allRestored && pending.retriesLeft > 0) {
                    // 饰品槽位可能尚未就绪（延迟加载/其他模组未初始化）：下一 tick 重试
                    pending.retriesLeft--;
                    pending.countdown = 1;
                    continue;
                }
                if (!allRestored) {
                    // 重试超时：仍未装回的饰品物品回退到背包（背包满则重生点掉落），避免滞留内存丢失
                    forceReturnKeptItems(player, pending.keep);
                }
            }
            finished.add(entry.getKey());
        }
        for (UUID uuid : finished) {
            PENDING_RESPAWNS.remove(uuid);
        }
    }

    /** 归还死亡保留物品：饰品槽物品强制装回原槽位并覆盖。返回是否全部装回成功（失败可稍后重试）。 */
    private static boolean restoreKeptItems(Player player, List<KeptItem> keep) {
        boolean allDone = true;
        for (KeptItem ki : keep) {
            if (ki.restored) continue; // 已处理（成功装回槽位或已放入背包），避免重试重复归还
            if (ki.curioType != null) {
                // 魔法绑定饰品：Curios DropRule(ALWAYS_KEEP) 已原生保留时槽位仍有同物品，无需再装回；
                // 槽位缺失（其他模组清槽/保留失败）时用官方 setEquippedCurio 补回原槽位并覆盖。
                if (CuriosIntegration.slotContains(player, ki.curioType, ki.curioIndex, ki.stack)) {
                    ki.restored = true;
                    continue;
                }
                if (inventoryContains(player, ki.stack)) {
                    // Curios 保留失败（物品曾掉落并被 LivingDrops 兜底收回背包）：避免复制，不再装回槽位
                    ki.restored = true;
                    continue;
                }
                boolean restored = CuriosIntegration.restoreStack(player, ki.curioType, ki.curioIndex, ki.stack);
                cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod.LOGGER.info(
                        "[EL-diag] restoreStack: player={} slot={}[{}] item={} result={}",
                        player.getName().getString(), ki.curioType, ki.curioIndex,
                        ki.stack.getHoverName().getString(), restored);
                if (restored) {
                    ki.restored = true;
                    continue;
                }
                allDone = false; // 槽位暂不可用：保留待重试
                continue;
            }
            if (!player.getInventory().add(ki.stack)) {
                spawnDrop(player, ki.stack);
            }
            ki.restored = true;
        }
        return allDone;
    }

    /** 重试超时后兜底：把仍未装回的饰品物品放入背包（背包满则重生点掉落）。 */
    private static void forceReturnKeptItems(Player player, List<KeptItem> keep) {
        for (KeptItem ki : keep) {
            if (ki.restored) continue;
            if (!player.getInventory().add(ki.stack)) {
                spawnDrop(player, ki.stack);
            }
            ki.restored = true;
        }
    }

    /** 玩家背包是否已有相同物品（用于防复制：掉落兜底已收回背包时不再装回槽位）。 */
    private static boolean inventoryContains(Player player, ItemStack expected) {
        for (ItemStack s : player.getInventory().items) {
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, expected)) return true;
        }
        return false;
    }

    /**
     * 消失诅咒延迟检查：强制消失开启时，扫描玩家全部槽位（背包/盔甲/副手/饰品栏，
     * 含合订本内容物），检测到带消失诅咒附魔的物品立即删除。
     */
    private static void sweepVanishingItems(Player player) {
        if (!ModConfig.getInstance().letterVanish.enabled) return;
        var inv = player.getInventory();
        sweepContainer(player, inv.items);
        sweepContainer(player, inv.armor);
        sweepContainer(player, inv.offhand);
        // 饰品栏：带消失诅咒的物品直接销毁
        CuriosIntegration.ejectStacksWhere(player, ModEnchantments::hasVanishing, stack -> { });
        // 饰品栏内的合订本：扫描并删除内容物中的消失诅咒物品
        for (ItemStack stack : CuriosIntegration.getCuriosStacks(player)) {
            sweepBinderContents(player, stack);
        }
    }

    private static void sweepContainer(Player player, List<ItemStack> container) {
        for (int i = 0; i < container.size(); i++) {
            ItemStack stack = container.get(i);
            if (stack.isEmpty()) continue;
            if (ModEnchantments.hasVanishing(stack)) {
                container.set(i, ItemStack.EMPTY); // 检测到消失诅咒物品：立即删除
                continue;
            }
            sweepBinderContents(player, stack);
        }
    }

    /** 扫描合订本内容物：删除带消失诅咒的内部物品（合订本本身带消失诅咒时整体销毁）。 */
    private static void sweepBinderContents(Player player, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof LetterBinderItem)) return;
        if (ModEnchantments.hasVanishing(stack)) {
            // 合订本本身带消失诅咒：整体销毁（含内容物）
            stack.setCount(0);
            return;
        }
        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        if (contents.isEmpty()) return;
        List<ItemStack> keep = new ArrayList<>();
        boolean changed = false;
        for (ItemStack inner : contents.itemsCopy()) {
            if (inner.isEmpty()) continue;
            if (ModEnchantments.hasVanishing(inner)) {
                changed = true; // 内容物中的消失诅咒物品：删除
            } else {
                keep.add(inner);
            }
        }
        if (changed) {
            stack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(keep));
        }
    }

    /** 玩家登出：清空保留池与待处理的重生任务（物品已随存档保留，直接丢弃引用）。 */
    public static void onPlayerLogout(UUID playerUuid) {
        DEATH_KEEP_POOL.remove(playerUuid);
        PENDING_RESPAWNS.remove(playerUuid);
    }

    /** 把掉落中的魔法绑定物品加入保留池（LivingDropsEvent 兜底）；重生时归还。 */
    public static void addToKeepPool(Player player, ItemStack stack) {
        List<KeptItem> list = DEATH_KEEP_POOL.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        list.add(new KeptItem(stack.copy(), null, -1));
    }

    private static void spawnDrop(Player player, ItemStack stack) {
        ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack);
        drop.setDeltaMovement(0, 0, 0);
        player.level().addFreshEntity(drop);
    }

    // ==================== 生物死亡处理（消失诅咒移除） ====================

    /** 生物死亡（LivingDeathEvent，掉落生成前）：强制消失开启时移除装备上的消失诅咒物品。 */
    public static void handleMobDeath(LivingEntity entity) {
        if (entity.level().isClientSide || entity instanceof Player) return;
        if (!ModConfig.getInstance().letterVanish.enabled) return;
        for (ItemStack stack : entity.getArmorSlots()) handleMobVanishing(stack);
        for (ItemStack stack : entity.getHandSlots()) handleMobVanishing(stack);
    }

    private static void handleMobVanishing(ItemStack stack) {
        if (stack.isEmpty() || !ModEnchantments.hasVanishing(stack)) return;
        stack.setCount(0);
    }

    /** 玩家槽位扫描（含饰品栏/合订本内容物）。 */
    private static void scanPlayerSlots(Player player, java.util.function.Consumer<ItemStack> consumer) {
        var inv = player.getInventory();
        for (var stack : inv.items) scanStack(stack, consumer);
        for (var stack : inv.armor) scanStack(stack, consumer);
        for (var stack : inv.offhand) scanStack(stack, consumer);
        for (var stack : CuriosIntegration.getCuriosStacks(player)) scanStack(stack, consumer);
    }

    private static void scanStack(ItemStack stack, java.util.function.Consumer<ItemStack> consumer) {
        if (stack.isEmpty()) return;
        consumer.accept(stack);
        if (stack.getItem() instanceof LetterBinderItem) {
            BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
            for (ItemStack inner : contents.itemsCopy()) {
                if (!inner.isEmpty()) consumer.accept(inner);
            }
        }
    }
}
