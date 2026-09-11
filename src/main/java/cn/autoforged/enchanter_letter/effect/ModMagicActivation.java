package cn.autoforged.enchanter_letter.effect;

import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import cn.autoforged.enchanter_letter.integration.AccessoriesIntegration;
import cn.autoforged.enchanter_letter.integration.CuriosIntegration;
import cn.autoforged.enchanter_letter.item.TemporaryLetterBinderItem;
import net.minecraft.core.registries.Registries;
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
 * 魔法激活状态查询与临时手札合订本生命周期管理（Fabric 1.20.1）。
 */
public class ModMagicActivation {
    public static final ResourceLocation ID = new ResourceLocation(UsefulMagicEnchanterLetterMod.MOD_ID, "magic_activation");

    private ModMagicActivation() {
    }

    public static MobEffect getEffect(Level level) {
        if (level == null) return null;
        return level.registryAccess().registryOrThrow(Registries.MOB_EFFECT).get(ID);
    }

    public static boolean isActive(LivingEntity entity) {
        if (entity == null) return false;
        MobEffect effect = getEffect(entity.level());
        return effect != null && entity.getEffect(effect) != null;
    }

    public static void apply(LivingEntity entity, int durationTicks) {
        if (entity == null || entity.level() == null || durationTicks <= 0) return;
        MobEffect effect = getEffect(entity.level());
        if (effect == null) return;
        entity.removeEffect(effect);
        entity.addEffect(new MobEffectInstance(effect, durationTicks, 0, false, false));
    }

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

    private static void ensureAndCleanup(LivingEntity entity) {
        if (entity == null || entity.isRemoved() || !entity.isAlive()) return;
        if (hasTempBinder(entity)) {
            if (!isActive(entity)) {
                // 优先为激活计数为 0 的全新临时合订本发放效果（时长按 NBT 秒数换算，忽略 /lettervanish 开关）
                grantIfNeeded(entity);
                if (!isActive(entity)) {
                    // 魔法激活效果已消失：清除身上所有临时手札合订本（双次检查防缺漏）
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

    /** 实体持有激活计数为 0 的全新临时合订本时，按其计时秒数发放魔法激活；触发时对整个实体所有临时合订本计数 +1。 */
    private static void grantIfNeeded(LivingEntity entity) {
        if (entity == null || entity.isRemoved() || !entity.isAlive()) return;
        if (isActive(entity)) return;
        ItemStack grantStack = null;
        for (ItemStack stack : allSlots(entity)) {
            if (!TemporaryLetterBinderItem.isTemporaryBinder(stack)) continue;
            // 已使用过一次（count>=1）：不再给予魔法激活（含永久失效 count>=2）
            if (TemporaryLetterBinderItem.getActivationCount(stack) >= 1) continue;
            grantStack = stack;
            break;
        }
        if (grantStack == null) return;
        int seconds = TemporaryLetterBinderItem.getRemainingSeconds(grantStack);
        apply(entity, seconds * 20);
        // 触发本次激活时：对实体上所有临时合订本计数 +1（已永久失效 count>=2 的跳过）
        // 0->1 首次、1->2 后进入永久失效，且 count>=1 的物品不再重复发放。
        for (ItemStack stack : allSlots(entity)) {
            if (!TemporaryLetterBinderItem.isTemporaryBinder(stack)) continue;
            if (TemporaryLetterBinderItem.isPermanentlyDisabled(stack)) continue;
            TemporaryLetterBinderItem.incrementActivation(stack);
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
            AccessoriesIntegration.ejectStacksWhere(entity, TemporaryLetterBinderItem::isTemporaryBinder, s -> { });
            for (ItemStack stack : AccessoriesIntegration.getAccessoriesStacks(entity)) {
                if (TemporaryLetterBinderItem.isTemporaryBinder(stack)) stack.setCount(0);
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
        result.addAll(AccessoriesIntegration.getAccessoriesStacks(entity));
        return result;
    }
}
