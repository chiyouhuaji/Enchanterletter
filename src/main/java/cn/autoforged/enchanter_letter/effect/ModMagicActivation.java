package cn.autoforged.enchanter_letter.effect;

import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import cn.autoforged.enchanter_letter.integration.AccessoriesIntegration;
import cn.autoforged.enchanter_letter.integration.CuriosIntegration;
import cn.autoforged.enchanter_letter.item.TemporaryLetterBinderItem;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 魔法激活状态管理（Fabric 1.21.1）。
 * 获得临时手札合订本时附加魔法激活；效果失效时清除所有临时手札合订本。
 */
public class ModMagicActivation {
    public static final ResourceKey<MobEffect> KEY = ResourceKey.create(
            Registries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(UsefulMagicEnchanterLetterMod.MOD_ID, "magic_activation"));

    private ModMagicActivation() {
    }

    public static boolean isActive(LivingEntity entity) {
        if (entity == null) return false;
        Holder<MobEffect> effect = getHolder(entity.level());
        if (effect == null) return false;
        return entity.hasEffect(effect);
    }

    public static void apply(LivingEntity entity, int durationTicks) {
        if (entity == null || entity.level().isClientSide || durationTicks <= 0) return;
        Holder<MobEffect> effect = getHolder(entity.level());
        if (effect == null) return;
        entity.removeEffect(effect);
        entity.addEffect(new MobEffectInstance(effect, durationTicks, 0, false, true, true));
    }

    private static Holder<MobEffect> getHolder(Level level) {
        if (level == null) return null;
        return level.registryAccess().registryOrThrow(Registries.MOB_EFFECT).getHolder(KEY).orElse(null);
    }

    /** 服务端每 tick 调用：扫描玩家和生物，施加/移除魔法激活效果。 */
    public static void tick(MinecraftServer server) {
        for (Player player : server.getPlayerList().getPlayers()) {
            tickEntity(player);
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (LivingEntity entity : level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),
                    ent -> !(ent instanceof Player))) {
                tickEntity(entity);
            }
        }
    }

    private static void tickEntity(LivingEntity entity) {
        if (entity == null || entity.isRemoved() || !entity.isAlive()) return;
        ItemStack binder = findFirstTemporaryBinder(entity);
        if (binder == null || binder.isEmpty()) {
            // 没有临时手札合订本：移除效果并清理旧临时合订本（内部保留的内容物也一并清理）
            removeEffect(entity);
            removeTempBinders(entity);
            removeTempBinders(entity);
        } else {
            if (!isActive(entity)) {
                // 优先为激活计数为 0 的全新临时合订本发放效果（时长按 NBT 秒数换算，忽略 /lettervanish 开关）
                grantIfNeeded(entity);
                if (!isActive(entity)) {
                    // 魔法激活效果已消失：清除身上所有临时手札合订本（双次检查防缺漏）
                    removeTempBinders(entity);
                    removeTempBinders(entity);
                }
            }
        }
    }

    /** 实体持有激活计数为 0 的全新临时合订本时，按其计时秒数发放魔法激活；触发时对整个实体所有临时合订本计数 +1。 */
    private static void grantIfNeeded(LivingEntity entity) {
        if (entity == null || entity.isRemoved() || !entity.isAlive()) return;
        if (isActive(entity)) return;
        ItemStack grantStack = null;
        for (ItemStack stack : collectTempBinders(entity)) {
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
        for (ItemStack stack : collectTempBinders(entity)) {
            if (TemporaryLetterBinderItem.isPermanentlyDisabled(stack)) continue;
            TemporaryLetterBinderItem.incrementActivation(stack);
        }
    }

    /** 收集实体身上所有临时合订本（背包/盔甲/副手/饰品/生物槽）。 */
    private static List<ItemStack> collectTempBinders(LivingEntity entity) {
        List<ItemStack> result = new ArrayList<>();
        if (entity instanceof Player player) {
            var inv = player.getInventory();
            for (ItemStack stack : inv.items) if (TemporaryLetterBinderItem.isTemporaryBinder(stack)) result.add(stack);
            for (ItemStack stack : inv.armor) if (TemporaryLetterBinderItem.isTemporaryBinder(stack)) result.add(stack);
            for (ItemStack stack : inv.offhand) if (TemporaryLetterBinderItem.isTemporaryBinder(stack)) result.add(stack);
        } else {
            for (ItemStack stack : entity.getArmorSlots()) if (TemporaryLetterBinderItem.isTemporaryBinder(stack)) result.add(stack);
            for (ItemStack stack : entity.getHandSlots()) if (TemporaryLetterBinderItem.isTemporaryBinder(stack)) result.add(stack);
        }
        for (ItemStack stack : CuriosIntegration.getCuriosStacks(entity)) if (TemporaryLetterBinderItem.isTemporaryBinder(stack)) result.add(stack);
        for (ItemStack stack : AccessoriesIntegration.getAccessoriesStacks(entity)) if (TemporaryLetterBinderItem.isTemporaryBinder(stack)) result.add(stack);
        return result;
    }

    public static ItemStack findFirstTemporaryBinder(LivingEntity entity) {
        if (entity instanceof Player player) {
            var inv = player.getInventory();
            for (ItemStack stack : inv.items) {
                if (TemporaryLetterBinderItem.isTemporaryBinder(stack)) return stack;
            }
            for (ItemStack stack : inv.armor) {
                if (TemporaryLetterBinderItem.isTemporaryBinder(stack)) return stack;
            }
            for (ItemStack stack : inv.offhand) {
                if (TemporaryLetterBinderItem.isTemporaryBinder(stack)) return stack;
            }
        } else {
            for (ItemStack stack : entity.getArmorSlots()) {
                if (TemporaryLetterBinderItem.isTemporaryBinder(stack)) return stack;
            }
            for (ItemStack stack : entity.getHandSlots()) {
                if (TemporaryLetterBinderItem.isTemporaryBinder(stack)) return stack;
            }
        }
        for (ItemStack stack : CuriosIntegration.getCuriosStacks(entity)) {
            if (TemporaryLetterBinderItem.isTemporaryBinder(stack)) return stack;
        }
        for (ItemStack stack : AccessoriesIntegration.getAccessoriesStacks(entity)) {
            if (TemporaryLetterBinderItem.isTemporaryBinder(stack)) return stack;
        }
        return null;
    }

    /** 清理实体身上所有临时手札合订本（含内容物），背包/盔甲/副手/饰品/生物槽。 */
    public static void removeTempBinders(LivingEntity entity) {
        if (entity == null) return;
        for (int pass = 0; pass < 2; pass++) {
            if (entity instanceof Player player) {
                var inv = player.getInventory();
                clearContainer(inv.items);
                clearContainer(inv.armor);
                clearContainer(inv.offhand);
                CuriosIntegration.ejectStacksWhere(player, TemporaryLetterBinderItem::isTemporaryBinder, s -> { });
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

    private static void removeEffect(LivingEntity entity) {
        Holder<MobEffect> holder = getHolder(entity.level());
        if (holder != null) {
            entity.removeEffect(holder);
        }
    }
}
