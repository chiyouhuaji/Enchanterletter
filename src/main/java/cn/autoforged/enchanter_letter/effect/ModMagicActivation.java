package cn.autoforged.enchanter_letter.effect;

import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import cn.autoforged.enchanter_letter.integration.CuriosIntegration;
import cn.autoforged.enchanter_letter.item.TemporaryLetterBinderItem;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 魔法激活状态查询与临时手札合订本生命周期管理。
 *
 * 规则：
 * 1. 临时手札合订本默认不生效；持有者具有“魔法激活”效果时，其内容手札才参与手札扫描。
 * 2. 持有临时手札合订本但没有该效果时，视为魔法激活已失效，清除临时手札合订本。
 * 3. 效果消失时，立即清除该实体身上所有临时手札合订本；清除时做两次检测确保兜底。
 */
public class ModMagicActivation {
    public static final ResourceKey<MobEffect> KEY = ResourceKey.create(
            Registries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(UsefulMagicEnchanterLetterMod.MOD_ID, "magic_activation"));

    private ModMagicActivation() {
    }

    public static Holder<MobEffect> getHolder(Level level) {
        if (level == null) return null;
        return level.registryAccess().registryOrThrow(Registries.MOB_EFFECT).getHolder(KEY).orElse(null);
    }

    /** 当前是否处于魔法激活状态。 */
    public static boolean isActive(LivingEntity entity) {
        if (entity == null) return false;
        Holder<MobEffect> effect = getHolder(entity.level());
        return effect != null && entity.getEffect(effect) != null;
    }

    /** 为实体附加一次魔法激活效果（会重置剩余时长）。 */
    public static void apply(LivingEntity entity, int durationTicks) {
        if (entity == null || entity.level() == null || durationTicks <= 0) return;
        Holder<MobEffect> effect = getHolder(entity.level());
        if (effect == null) return;
        entity.removeEffect(effect);
        entity.addEffect(new MobEffectInstance(effect, durationTicks, 0, false, false));
    }

    /** 每 tick 由服务器调用：保证临时合订本持有者获得效果，并在效果消失时清理。 */
    public static void tick(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ensureAndCleanup(player);
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (LivingEntity mob : level.getEntities(
                    net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),
                    e -> !(e instanceof Player))) {
                ensureAndCleanup(mob);
            }
        }
    }

    /**
     * 每 20 tick 由服务器调用：为刚获得（granted 记录为 0）的临时手札合订本发放魔法激活效果。
     * 时长按物品 NBT 的计时秒数换算（秒×20），发放后立即把 granted 翻转（防止重复发放）。
     */
    public static void grantCheck(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            grantIfNeeded(player);
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (LivingEntity mob : level.getEntities(
                    net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),
                    e -> !(e instanceof Player))) {
                grantIfNeeded(mob);
            }
        }
    }

    /** 实体持有未发放记录的临时手札合订本时，按其计时秒数发放魔法激活并标记已发放。 */
    private static void grantIfNeeded(LivingEntity entity) {
        if (entity == null || entity.isRemoved() || !entity.isAlive()) return;
        // 收集实体上所有临时合订本；没有则返回
        List<ItemStack> binders = new ArrayList<>();
        for (ItemStack stack : allSlots(entity)) {
            if (TemporaryLetterBinderItem.isTemporaryBinder(stack)) binders.add(stack);
        }
        if (binders.isEmpty()) return;
        if (isActive(entity)) return;
        // 先捕获第一个全新（count==0 且未永久失效）的临时合订本，用于本次发放
        ItemStack fresh = null;
        for (ItemStack stack : binders) {
            if (TemporaryLetterBinderItem.getActivationCount(stack) >= 1) continue;
            if (TemporaryLetterBinderItem.isPermanentlyDisabled(stack)) continue;
            fresh = stack;
            break;
        }
        // 触发本次激活：对实体上所有未永久失效的临时合订本计数 +1（0->1 或 1->2；count>=2 的跳过）
        // 规则：只要在同一个实体上执行了由临时手札合订本触发的效果逻辑就 +1
        for (ItemStack stack : binders) {
            if (TemporaryLetterBinderItem.isPermanentlyDisabled(stack)) continue;
            TemporaryLetterBinderItem.incrementActivation(stack);
        }
        // 若捕获到全新合订本：发放魔法激活（时长按 NBT 秒数换算）并标记已发放；不再重复计数
        if (fresh != null) {
            int seconds = TemporaryLetterBinderItem.getRemainingSeconds(fresh);
            apply(entity, seconds * 20);
            TemporaryLetterBinderItem.setGranted(fresh);
        }
    }

    private static void ensureAndCleanup(LivingEntity entity) {
        if (entity == null || entity.isRemoved() || !entity.isAlive()) return;
        if (hasTempBinder(entity)) {
            if (!isActive(entity)) {
                // 优先为未发放记录的临时合订本发放效果；若仍无效才清除（双次检查防缺漏）
                grantIfNeeded(entity);
                if (!isActive(entity)) {
                    removeTempBinders(entity);
                    removeTempBinders(entity);
                }
            }
        } else {
            // 没有临时手札合订本：清理旧临时合订本（双次检查防缺漏）
            removeTempBinders(entity);
            removeTempBinders(entity);
        }
    }

    private static boolean hasTempBinder(LivingEntity entity) {
        return firstTempBinder(entity) != null;
    }

    private static ItemStack firstTempBinder(LivingEntity entity) {
        for (ItemStack stack : allSlots(entity)) {
            if (TemporaryLetterBinderItem.isTemporaryBinder(stack)) return stack;
        }
        return null;
    }

    /** 双次检测清除：第一遍删除所有临时合订本，第二遍确认没有残留。 */
    public static void removeTempBinders(LivingEntity entity) {
        if (entity == null) return;
        for (int pass = 0; pass < 2; pass++) {
            if (entity instanceof Player player) {
                var inv = player.getInventory();
                clearContainer(inv.items);
                clearContainer(inv.armor);
                clearContainer(inv.offhand);
                CuriosIntegration.ejectStacksWhere(player,
                        TemporaryLetterBinderItem::isTemporaryBinder, s -> { });
            } else {
                for (ItemStack stack : entity.getArmorSlots()) clearStack(stack);
                for (ItemStack stack : entity.getHandSlots()) clearStack(stack);
                for (ItemStack stack : CuriosIntegration.getCuriosStacks(entity)) {
                    if (TemporaryLetterBinderItem.isTemporaryBinder(stack)) stack.setCount(0);
                }
            }
        }
    }

    private static void clearContainer(List<ItemStack> container) {
        for (int i = 0; i < container.size(); i++) {
            ItemStack stack = container.get(i);
            if (TemporaryLetterBinderItem.isTemporaryBinder(stack)) {
                container.set(i, ItemStack.EMPTY);
            }
        }
    }

    private static void clearStack(ItemStack stack) {
        if (TemporaryLetterBinderItem.isTemporaryBinder(stack)) stack.setCount(0);
    }

    private static List<ItemStack> allSlots(LivingEntity entity) {
        List<ItemStack> result = new ArrayList<>();
        if (entity instanceof Player player) {
            var inv = player.getInventory();
            result.addAll(inv.items);
            result.addAll(inv.armor);
            result.addAll(inv.offhand);
        } else {
            for (ItemStack stack : entity.getArmorSlots()) result.add(stack);
            for (ItemStack stack : entity.getHandSlots()) result.add(stack);
        }
        result.addAll(CuriosIntegration.getCuriosStacks(entity));
        return result;
    }
}
