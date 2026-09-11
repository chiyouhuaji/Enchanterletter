package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import cn.autoforged.enchanter_letter.effect.ModMagicActivation;
import cn.autoforged.enchanter_letter.effect.ModMagicObstruction;
import cn.autoforged.enchanter_letter.integration.CuriosIntegration;
import cn.autoforged.enchanter_letter.item.TemporaryLetterBinderItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 手札药水效果（/lettereffect）：以物品 NBT 保存状态（1.20.1 移植，无 DataComponents）。
 * 每个效果词条为独立 NBT 条目：
 *   id            (int)    词条自增 id（delete 用）
 *   effect        (String) 药水效果注册 id，如 minecraft:speed
 *   seconds       (int)    持续时间（秒）
 *   amplifier     (int)    效果等级（0=1 级）
 *   hideParticles (byte)   1=隐藏粒子
 *   loopMode      (String) daytime | gametime
 *   loopTimes     (long[]) 循环时间点：daytime 每项为每日 0~24000 游戏时刻（可多个）；
 *                          gametime 第 1 项为起始总游戏刻，第 2 项为间隔（tick），其余忽略
 * 生效统一使用世界时间（等价 /time query daytime / /time query gametime），不另建计时器：
 *   daytime：当前日时刻落在任一设定时间点的 1 秒窗口内触发；
 *   gametime：当前总游戏刻与起始点同余于间隔时触发（经判断是否在当前 1 秒窗口内）。
 * 补发：玩家上线/生物被加载时，只要距最近一次本应触发的时刻尚未超过 NBT 记录的持续秒数，
 *   按“已生效时长扣减后”的剩余时长补上对应效果。
 * 所有手札默认此 NBT 为空（新词条需命令添加），无需配置文件。
 */
public class LetterPotions {
    public static final String KEY = "letter_potions";
    public static final String SEQ_KEY = "letter_potion_seq";

    private LetterPotions() {
    }

    /** 读取物品的全部药水词条（不存在返回空列表）。 */
    public static List<CompoundTag> getAll(ItemStack stack) {
        List<CompoundTag> result = new ArrayList<>();
        if (stack.isEmpty()) return result;
        CompoundTag data = ModDataComponents.getLetterData(stack);
        ListTag list = data.getList(KEY, Tag.TAG_COMPOUND);
        for (Tag t : list) {
            if (t instanceof CompoundTag c) result.add(c);
        }
        return result;
    }

    /** 词条是否为空（无任何药水词条）。 */
    public static boolean isEmpty(ItemStack stack) {
        return getAll(stack).isEmpty();
    }

    /** 新增一个词条，返回其 id；写入失败返回 -1。 */
    public static int add(ItemStack stack, int slot, String effect, int seconds, int amplifier,
                          boolean hideParticles, String loopMode, List<Long> loopTimes) {
        if (stack.isEmpty() || seconds <= 0 || slot < 1) return -1;
        CompoundTag data = ModDataComponents.getLetterData(stack);
        ListTag list = data.getList(KEY, Tag.TAG_COMPOUND);
        long[] times = new long[loopTimes.size()];
        for (int i = 0; i < loopTimes.size(); i++) times[i] = loopTimes.get(i);
        // slot occupied: overwrite in place (keep id/slot)
        for (Tag t : list) {
            if (t instanceof CompoundTag c && c.getInt("slot") == slot) {
                c.putString("effect", effect);
                c.putInt("seconds", seconds);
                c.putInt("amplifier", Math.max(0, amplifier));
                c.putBoolean("hideParticles", hideParticles);
                c.putString("loopMode", "gametime".equals(loopMode) ? "gametime" : "daytime");
                c.putLongArray("loopTimes", times);
                ModDataComponents.setLetterData(stack, data);
                return c.getInt("id");
            }
        }
        // slot free: create new
        int seq = data.getInt(SEQ_KEY) + 1;
        CompoundTag entry = new CompoundTag();
        entry.putInt("id", seq);
        entry.putInt("slot", slot);
        entry.putString("effect", effect);
        entry.putInt("seconds", seconds);
        entry.putInt("amplifier", Math.max(0, amplifier));
        entry.putBoolean("hideParticles", hideParticles);
        entry.putString("loopMode", "gametime".equals(loopMode) ? "gametime" : "daytime");
        entry.putLongArray("loopTimes", times);
        list.add(entry);
        data.put(KEY, list);
        data.putInt(SEQ_KEY, seq);
        ModDataComponents.setLetterData(stack, data);
        return seq;
    }

    /** 按下标删除词条（0-based），成功返回 true。 */
    public static boolean removeAt(ItemStack stack, int index) {
        if (stack.isEmpty() || index < 0) return false;
        CompoundTag data = ModDataComponents.getLetterData(stack);
        ListTag list = data.getList(KEY, Tag.TAG_COMPOUND);
        if (index >= list.size()) return false;
        list.remove(index);
        data.put(KEY, list);
        ModDataComponents.setLetterData(stack, data);
        return true;
    }

    /** 按词条 id 删除，成功返回 true。 */
    public static boolean removeById(ItemStack stack, int id) {
        if (stack.isEmpty()) return false;
        CompoundTag data = ModDataComponents.getLetterData(stack);
        ListTag list = data.getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            if (list.getCompound(i).getInt("id") == id) {
                return removeAt(stack, i);
            }
        }
        return false;
    }

    public static boolean removeBySlot(ItemStack stack, int slot) {
        if (stack.isEmpty() || slot < 1) return false;
        CompoundTag data = ModDataComponents.getLetterData(stack);
        ListTag list = data.getList(KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            if (list.getCompound(i).getInt("slot") == slot) {
                return removeAt(stack, i);
            }
        }
        return false;
    }

    /**
     * 对一个持有者（玩家/生物）扫描其所有手札（背包/盔甲/副手/饰品/合订本内容物），
     * 汇总全部药水词条，按循环模式计算“最近一次本应触发的世界时刻”，
     * 若当前仍在该次触发的持续窗口内，则按剩余时长施加（兼容未加载/离线补发）。
     */
    public static void tick(LivingEntity holder, ServerLevel level) {
        if (ModMagicObstruction.blocksAll(holder)) return;
        if (holder == null || level == null || holder.isRemoved() || !holder.isAlive()) return;
        List<CompoundTag> entries = new ArrayList<>();
        scanAllSlots(holder, stack -> entries.addAll(getAll(stack)));
        if (entries.isEmpty()) return;
        long gameTime = level.getGameTime();  // /time query gametime：单调，不回拨
        long dayTime = level.getDayTime();    // /time query daytime：受 /time set 影响
        for (CompoundTag entry : entries) {
            String effectId = entry.getString("effect");
            if (effectId.isEmpty()) continue;
            int seconds = entry.getInt("seconds");
            if (seconds <= 0) continue;
            int amplifier = entry.getInt("amplifier");
            boolean hide = entry.getBoolean("hideParticles");
            long remaining = remainingEffectTicks(entry, gameTime, dayTime, seconds * 20L);
            if (remaining <= 0) continue;
            apply(holder, level, effectId, (int) remaining, amplifier, hide);
        }
    }

    /**
     * 计算该词条当前还应生效的剩余时长（tick）——兼容未加载/离线补发，并在每次
     * 调用时基于当前世界时间重新校准。
     * 时间来源与游戏原版一致：
     *   gametime 模式：用 /time query gametime（level.getGameTime()，单调不回拨，准确）；
     *   daytime 模式：用 /time query daytime（level.getDayTime()，受 /time set 影响），按当前
     *                 日时刻 t 的窗口起点判定“最近一次本应触发”，并以其日时刻差作为已过时长。
     * 若当前仍在该次触发的持续窗口 [触发点, 触发点+duration) 内，返回剩余时长；否则返回 0。
     */
    private static long remainingEffectTicks(CompoundTag entry, long gameTime, long dayTime, long durationTicks) {
        boolean gametime = "gametime".equals(entry.getString("loopMode"));
        long[] times = entry.getLongArray("loopTimes");
        if (gametime) {
            if (times.length < 2 || times[1] <= 0) return 0L;
            long start = times[0];
            long interval = times[1];
            if (gameTime < start) return 0L;
            long lastTrigger = start + ((gameTime - start) / interval) * interval;
            long elapsed = gameTime - lastTrigger;
            long remaining = durationTicks - elapsed;
            return remaining > 0 ? remaining : 0L;
        }
        // daytime：用 getDayTime() 判定“当前日时刻”，与 /time query daytime 一致；
        // 已过时长 = 当前日时刻到最近一次窗口起点的日时刻差（游戏刻数，正常同速）。
        long currentDay = Math.floorMod(dayTime, 24000L);
        long bestDayElapsed = Long.MAX_VALUE;
        for (long t : times) {
            if (t < 0 || t >= 24000L) continue;
            long dayElapsed = (currentDay >= t) ? (currentDay - t) : (currentDay - t + 24000L);
            if (dayElapsed < bestDayElapsed) bestDayElapsed = dayElapsed;
        }
        if (bestDayElapsed == Long.MAX_VALUE) return 0L;
        long remaining = durationTicks - bestDayElapsed;
        return remaining > 0 ? remaining : 0L;
    }

    /** 施加效果（1.20.1：Registry.get 直接取值解析，无 Holder；durationTicks 为剩余时长（tick），不在此再乘 20）。 */
    private static void apply(LivingEntity holder, ServerLevel level, String effectId,
                              int durationTicks, int amplifier, boolean hide) {
        ResourceLocation id = ResourceLocation.tryParse(effectId);
        if (id == null) return;
        MobEffect effect = level.registryAccess()
                .registryOrThrow(Registries.MOB_EFFECT)
                .get(id);
        if (effect == null) return;
        holder.removeEffect(effect);
        holder.addEffect(new MobEffectInstance(effect, durationTicks, Math.max(0, amplifier), false, !hide));
    }

    /** 扫描持有者全部槽位（含手札合订本内容物、Curios 饰品）。 */
    private static void scanAllSlots(LivingEntity entity, java.util.function.Consumer<ItemStack> consumer) {
        if (entity instanceof Player player) {
            var inv = player.getInventory();
            for (var stack : inv.items) scanStack(entity, stack, consumer);
            for (var stack : inv.armor) scanStack(entity, stack, consumer);
            for (var stack : inv.offhand) scanStack(entity, stack, consumer);
        } else {
            for (var stack : entity.getArmorSlots()) scanStack(entity, stack, consumer);
            for (var stack : entity.getHandSlots()) scanStack(entity, stack, consumer);
        }
        for (var stack : CuriosIntegration.getCuriosStacks(entity)) scanStack(entity, stack, consumer);
    }

    private static void scanStack(LivingEntity entity, ItemStack stack, java.util.function.Consumer<ItemStack> consumer) {
        if (stack.isEmpty()) return;
        consumer.accept(stack);
        if (stack.getItem() instanceof LetterBinderItem) {
            NonNullList<ItemStack> contents = LetterBinderItem.readContents(stack);
            for (ItemStack inner : contents) {
                if (!inner.isEmpty()) consumer.accept(inner);
            }
        } else if (stack.getItem() instanceof TemporaryLetterBinderItem && ModMagicActivation.isActive(entity)) {
            NonNullList<ItemStack> contents = LetterBinderItem.readContents(stack);
            for (ItemStack inner : contents) {
                if (!inner.isEmpty()) consumer.accept(inner);
            }
        }
    }

    /** 命令/回显用的模组 id。 */
    public static String modId() {
        return UsefulMagicEnchanterLetterMod.MOD_ID;
    }
}