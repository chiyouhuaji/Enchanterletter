package cn.autoforged.enchanter_letter.event;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import cn.autoforged.enchanter_letter.command.LetterCommands;
import cn.autoforged.enchanter_letter.command.LetterDamageCommand;
import cn.autoforged.enchanter_letter.command.LetterEffectCommand;
import cn.autoforged.enchanter_letter.command.LetterStorageCommand;
import cn.autoforged.enchanter_letter.config.ModConfig;
import cn.autoforged.enchanter_letter.enchantment.ModEnchantments;
import cn.autoforged.enchanter_letter.integration.CuriosIntegration;
import cn.autoforged.enchanter_letter.storage.LetterStorageManager;
import cn.autoforged.enchanter_letter.item.ExperienceMagicLetterItem;
import cn.autoforged.enchanter_letter.item.FishingMagicLetterItem;
import cn.autoforged.enchanter_letter.item.HeroMagicLetterItem;
import cn.autoforged.enchanter_letter.item.KillMagicLetterItem;
import cn.autoforged.enchanter_letter.item.LetterBinderItem;
import cn.autoforged.enchanter_letter.item.LetterPotions;
import cn.autoforged.enchanter_letter.item.LetterStats;
import cn.autoforged.enchanter_letter.item.MagicLetterItem;
import cn.autoforged.enchanter_letter.item.ModItems;
import cn.autoforged.enchanter_letter.item.TenacityMagicLetterItem;
import cn.autoforged.enchanter_letter.item.TimeMagicLetterItem;
import cn.autoforged.enchanter_letter.item.TravelMagicLetterItem;
import cn.autoforged.enchanter_letter.item.TreasureMagicLetterItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * 1.20.1 移植说明：
 * - 伤害事件使用 LivingHurtEvent（可修改伤害值）完成：坚韧累计、转化排队、默认伤害增益；
 * - 转化伤害结算时以 ThreadLocal 标记避免递归；
 * - 服务器 tick 使用 TickEvent.ServerTickEvent（Phase.END）。
 */
@Mod.EventBusSubscriber(modid = UsefulMagicEnchanterLetterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCommonEvents {
    static final ResourceKey<DamageType> USEFULMAGIC_MAGIC =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("usefulmagic:magic"));

    /** /kill 指令使用的伤害类型（不再计入坚韧手札承受伤害计数）。 */
    private static final ResourceKey<DamageType> GENERIC_KILL =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("minecraft", "generic_kill"));

    private static final int MAX_PENDING_TYPES_PER_VICTIM = 16;
    private static final int MAX_PENDING_VICTIMS = 200;
    private static final int MAX_STALE_TICKS = 100;
    private static final int BIND_CHECK_INTERVAL = 20;
    private static final double MAX_TRAVEL_PER_TICK = 100.0;
    /** 旅行距离采样间隔（tick）：每 tick 全槽轮询会持续扫描在线玩家槽位，
     *  最低加入 1 tick（0.05 秒）延迟，这里取 2 tick 采样一次。 */
    private static final int TRAVEL_POLL_INTERVAL = 2;
    private static final int ACCESSORY_COMPAT_INTERVAL = 100;
    private static final int ARMOR_SYNC_INTERVAL = 10;
    /** 手札药水效果轮询间隔（tick，每 1 秒；用世界时间判定循环，无需每 tick）。 */
    private static final int POTION_POLL_INTERVAL = 20;

    private static int bindCheckCooldown = BIND_CHECK_INTERVAL;
    private static int accessoryCompatCooldown = ACCESSORY_COMPAT_INTERVAL;
    private static int armorSyncCooldown = ARMOR_SYNC_INTERVAL;
    private static int potionPollCooldown = POTION_POLL_INTERVAL;
    private static int travelPollCooldown = 0;

    private static final ThreadLocal<UUID> CONVERSION_IN_PROGRESS = new ThreadLocal<>();

    private static final Map<UUID, Vec3> lastTravelPos = new HashMap<>();

    static class PendingEntry {
        final ResourceKey<DamageType> damageType;
        float amount;
        Entity directEntity;
        Entity sourceEntity;

        PendingEntry(ResourceKey<DamageType> damageType, float amount, Entity directEntity, Entity sourceEntity) {
            this.damageType = damageType;
            this.amount = amount;
            this.directEntity = directEntity;
            this.sourceEntity = sourceEntity;
        }
    }

    static class ConversionDamageState {
        final Map<ResourceKey<DamageType>, PendingEntry> pendingByType = new LinkedHashMap<>();
        int countdown;
        int staleTicks;
    }

    private static final Map<UUID, ConversionDamageState> pendingDamages = new HashMap<>();

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        // 杀戮手札：持有者（玩家或生物）击杀任意生物/玩家 +1
        if (event.getSource().getEntity() instanceof LivingEntity killer) {
            scanEntitySlots(killer, stack -> {
                if (stack.is(ModItems.KILL_MAGIC_LETTER.get())) {
                    KillMagicLetterItem.addKill(stack);
                }
            });
        }
        LivingEntity living = event.getEntity();
        if (living != null) {
            pendingDamages.remove(living.getUUID());
            // 消失诅咒 / 魔法绑定死亡处理：玩家移除/保留物品，生物移除装备上的消失诅咒物品
            if (living instanceof Player player) {
                LetterEntityEffects.handlePlayerDeath(player);
            } else {
                LetterEntityEffects.handleMobDeath(living);
            }
        }
    }

    /**
     * 玩家死亡掉落兜底：遍历已经生成的掉落物（可覆盖物品栏/盔甲/副手/饰品等所有来源），
     * 移除魔法绑定物品（加入保留池，重生归还）与强制消失开启时的消失诅咒物品。
     * 作为 die 预扫描之外的兜底，确保任何路径进入掉落列表的对应物品都被正确处理。
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getSource().getEntity() instanceof Player player) return;
        if (event.getEntity() instanceof Player player) {
            java.util.Collection<ItemEntity> drops = event.getDrops();
            if (drops == null || drops.isEmpty()) return;
            boolean vanish = ModConfig.getInstance().letterVanish.enabled;
            var it = drops.iterator();
            while (it.hasNext()) {
                ItemEntity de = it.next();
                ItemStack stack = de.getItem();
                if (stack.isEmpty()) continue;
                if (ModEnchantments.hasEffectiveMagicBinding(stack)) {
                    // 魔法绑定：移除掉落物，重生归还
                    LetterEntityEffects.addToKeepPool(player, stack);
                    it.remove();
                    de.discard();
                } else if (vanish && ModEnchantments.hasVanishing(stack)) {
                    // 强制消失：含有消失诅咒的掉落物直接销毁
                    it.remove();
                    de.discard();
                }
            }
        }
    }

    /** 重生：返还死亡保留池（魔法绑定物品与合订本溢出的非诅咒手札）。 */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        LetterEntityEffects.onPlayerRespawn(event.getEntity());
    }

    /** 登出：清空保留池引用。 */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LetterEntityEffects.onPlayerLogout(event.getEntity().getUUID());
        LetterStorageManager.onLogout(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) return;
        ItemStack stack = itemEntity.getItem();
        // 强制消失开启时：带有消失诅咒的物品一律阻止形成掉落物（包括生物的 NBT 装备转成掉落物）
        if (ModConfig.getInstance().letterVanish.enabled
                && !stack.isEmpty() && ModEnchantments.hasVanishing(stack)) {
            itemEntity.discard();
            return;
        }
        if (isOurLetter(stack)) {
            // 手札掉落物：白色发光（方便找到物品，不做颜色改色）+ 无重力、静止不动
            event.getEntity().setGlowingTag(true);
            event.getEntity().setNoGravity(true);
            event.getEntity().setDeltaMovement(0.0, 0.0, 0.0);
            Optional<UUID> boundOpt = ModDataComponents.getBoundPlayer(stack);
            if (boundOpt.isPresent()) {
                itemEntity.setTarget(boundOpt.get());
            }
        }
    }

    /**
     * 拦截 Curios 的 shift+右键抢先装备（EventPriority.HIGH，先于 Curios 默认优先级执行）。
     * 玩家潜行 + 手持本模组手札/合订本 = 绑定/解绑动作：置位绑定拦截窗口，
     * 使 Curios 槽位有效性判定（isItemValid，走本模组注入的谓词）在窗口内拒绝本模组物品，
     * 从而跳过 curioRightClick 的自动装备；不取消事件，物品自身的 use() 正常执行
     * （MagicLetterItem.use 检测潜行后 startUsingItem 进入长按绑定）。
     * 窗口在服务器 tick 末尾（END 相位）关闭。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (player == null || stack.isEmpty()) return;
        if (!(stack.getItem() instanceof MagicLetterItem) && !(stack.getItem() instanceof LetterBinderItem)) return;
        if (!player.isShiftKeyDown()) return;
        CuriosIntegration.beginBindIntercept();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();

        // 关闭上一 tick 的绑定拦截窗口（窗口在 PlayerInteractEvent 中置位，
        // 在 Curios 的 curioRightClick 处理完之后，最迟本 tick 结束前清除）
        CuriosIntegration.endBindIntercept();

        if (--bindCheckCooldown <= 0) {
            bindCheckCooldown = BIND_CHECK_INTERVAL;
            ejectForeignBoundLetters(server);
        }

        if (--accessoryCompatCooldown <= 0) {
            accessoryCompatCooldown = ACCESSORY_COMPAT_INTERVAL;
            CuriosIntegration.ensureAllSlotCompat();
        }

        // 累计所有在线玩家的行走/飞行距离（旅行魔法手札）：带冷却，避免每 tick 全槽轮询
        if (--travelPollCooldown <= 0) {
            travelPollCooldown = TRAVEL_POLL_INTERVAL;
            tickTravelDistances(server);
        }

        // 手札护甲值/护甲韧性：以原版属性 modifier 形式常驻叠加（护甲条/Overloaded Armor Bar 可见；生物同样生效）
        if (--armorSyncCooldown <= 0) {
            armorSyncCooldown = ARMOR_SYNC_INTERVAL;
            for (var player : server.getPlayerList().getPlayers()) {
                LetterStats.syncArmorModifiers(player);
            }
            for (ServerLevel level : server.getAllLevels()) {
                for (LivingEntity mob : level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),
                        ent -> !(ent instanceof Player))) {
                    LetterStats.syncArmorModifiers(mob);
                }
            }
        }

        // 手札药水效果：对在线玩家（及携带手札的生物）按词条循环模式施加（世界时间判定）
        if (--potionPollCooldown <= 0) {
            potionPollCooldown = POTION_POLL_INTERVAL;
            for (var player : server.getPlayerList().getPlayers()) {
                LetterPotions.tick(player, (ServerLevel) player.level());
            }
            for (ServerLevel level : server.getAllLevels()) {
                for (LivingEntity mob : level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),
                        ent -> !(ent instanceof Player))) {
                    LetterPotions.tick(mob, level);
                }
            }
        }

        // 光灵发光同步 / 生物消失诅咒零 UUID 绑定 / 掉落物定时清理
        LetterEntityEffects.tick(server);

        if (pendingDamages.isEmpty()) return;

        int intervalTicks = Math.max(1, (int) (ModConfig.getInstance().magicConversion.intervalSeconds * 20));

        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, ConversionDamageState> entry : new ArrayList<>(pendingDamages.entrySet())) {
            UUID victimId = entry.getKey();
            ConversionDamageState state = entry.getValue();
            if (state.pendingByType.isEmpty()) {
                toRemove.add(victimId);
                continue;
            }
            if (state.countdown > 0) {
                state.countdown--;
                continue;
            }
            LivingEntity living = findLivingEntity(server, victimId);
            if (living == null) {
                state.staleTicks++;
                if (state.staleTicks >= MAX_STALE_TICKS) {
                    toRemove.add(victimId);
                } else {
                    state.countdown = 1;
                }
                continue;
            }
            PendingEntry pending = pickLargest(state);
            if (pending == null) {
                toRemove.add(victimId);
                continue;
            }
            applyConversionDamage(living, pending);

            if (state.pendingByType.isEmpty()) {
                toRemove.add(victimId);
            } else {
                state.countdown = intervalTicks;
            }
        }
        for (UUID uuid : toRemove) {
            pendingDamages.remove(uuid);
        }
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        CuriosIntegration.ensureAllSlotCompat();
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        CuriosIntegration.ensureAllSlotCompat();
    }

    /**
     * 1.20.1 移植：LivingHurtEvent 可修改伤害值，统一在此完成：
     * 1) 玩家承受伤害累计（坚韧手札，使用初始未增益伤害）；
     * 2) 附魔手札转化伤害进入待结算队列（基于初始未增益伤害）；
     * 3) 未附魔手札对默认伤害的增益直接放大本次伤害。
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (isConversionInProgress(victim)) return;

        float original = event.getAmount();
        if (original <= 0) return;

        // 坚韧手札：实体（玩家或生物）承受伤害累计（使用初始未增益伤害）
        // /kill 指令伤害（generic_kill）不计入，避免 /kill 刷坚韧计数
        if (!victim.level().isClientSide && !event.getSource().is(GENERIC_KILL)) {
            onEntityDamageTaken(victim, original);
        }

        // 防御属性对所有来源伤害生效：
        // 1) 护甲值 / 护甲韧性：已由服务器 tick 以原版属性 modifier 常驻叠加（护甲条可见），
        //    此处直接参与后续原版护甲减伤计算；
        // 2) 抗性提升：参照原版抗性药水按比例减免（护甲结算之前）
        float afterResist = applyResistanceReduction(victim, event.getAmount());
        if (afterResist != event.getAmount()) {
            event.setAmount(afterResist);
        }

        boolean letterBoosted = shouldApplyLetterBoost(event.getSource());
        if (!letterBoosted) return;

        // 转化以“初始（未增益）”伤害为基数
        onUsefulMagicIncomingDamage(victim, event.getSource(), original);

        // 默认伤害增益（基于抗性减免后的当前值，与抗性同为乘法关系）
        float amplified = onUsefulMagicHurtHealing(victim, event.getSource(), event.getAmount());
        if (amplified != event.getAmount()) {
            event.setAmount(amplified);
        }
        if (event.getAmount() != original) {
            // MesdagPortLib 将最终伤害改为读取 PortDamageContainer 的值
            // （其 LivingEntityMixin 在 actuallyHurt 中用容器值覆盖本地伤害），
            // 必须把最终伤害同步进容器，否则会被还原为原始伤害。
            syncPortlibDamageContainer(victim, event.getAmount());
        }
    }

    /**
     * 抗性提升：受害者携带的手札按“原版抗性提升药水”逻辑减免伤害
     * （减免比例 = 等级 × 每级增长，最终取 min(减免, 服务器配置上限)）。
     * 功能开关或减免为 0 时原样返回。
     */
    public static float applyResistanceReduction(LivingEntity victim, float amount) {
        if (amount <= 0) return amount;
        ModConfig config = ModConfig.getInstance();
        if (!config.letterResistance.enabled) return amount;
        double reduction = LetterStats.effectiveResistance(victim, victim.level());
        if (reduction <= 0) return amount;
        reduction = Math.min(reduction, config.letterResistance.limit);
        return (float) (amount * (1.0 - reduction));
    }

    /**
     * 将放大后的伤害同步到 MesdagPortLib 的 PortDamageContainer（反射调用，无编译期依赖）。
     * portlib 未安装或版本不兼容时静默忽略，走原 Forge 事件流程。
     */
    private static void syncPortlibDamageContainer(LivingEntity entity, float newDamage) {
        try {
            Class<?> iface = Class.forName("org.mesdag.portlib.diff.IPortLivingEntity");
            Object holder = iface.getMethod("of", LivingEntity.class).invoke(null, entity);
            if (holder == null) return;
            Object stack = iface.getMethod("portlib$getDamageContainers").invoke(holder);
            if (!(stack instanceof Stack<?> containers) || containers.isEmpty()) return;
            Object container = containers.peek();
            container.getClass().getMethod("setNewDamage", float.class).invoke(container, newDamage);
        } catch (Throwable ignored) {
            // portlib 未安装或 API 变化：保持原 Forge 流程
        }
    }

    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        Player player = event.getEntity();
        if (player != null && !player.level().isClientSide) {
            onFishCaught(player);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LetterDamageCommand.register(event.getDispatcher());
        LetterCommands.register(event.getDispatcher());
        LetterStorageCommand.register(event.getDispatcher());
        LetterEffectCommand.register(event.getDispatcher());
    }

    public static boolean isConversionInProgress(LivingEntity entity) {
        UUID current = CONVERSION_IN_PROGRESS.get();
        return current != null && current.equals(entity.getUUID());
    }

    public static boolean isLetterBoosted(DamageSource source) {
        if (source.is(USEFULMAGIC_MAGIC)) return true;
        Optional<ResourceKey<DamageType>> keyOpt = source.typeHolder().unwrapKey();
        if (keyOpt.isEmpty()) return false;
        return ModConfig.getInstance().getDefaultBonusDamageTypes().contains(keyOpt.get().location());
    }

    /**
     * 是否应对本次伤害进行手札增益：
     * 1) 命中可增益伤害类型（玩家行为），或
     * 2) 攻击者为“生物”（非玩家 LivingEntity）且其主副手/装备持有生效手札时就对其所有伤害增益，
     *    不再要求伤害类型匹配（需求：生物持有手札时对所有产生的伤害进行增益）。
     */
    public static boolean shouldApplyLetterBoost(DamageSource source) {
        if (isLetterBoosted(source)) return true;
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity holder && !(holder instanceof Player)) {
            return hasEffectiveLetter(holder);
        }
        return false;
    }

    private static boolean hasEffectiveLetter(LivingEntity holder) {
        if (holder == null) return false;
        return !collectEffectiveLetters(holder, holder.level()).isEmpty();
    }

    public static boolean isBlacklistedEntity(LivingEntity victim) {
        ResourceLocation entityKey = EntityType.getKey(victim.getType());
        return ModConfig.getInstance().getBlacklistEntityNames().contains(entityKey);
    }

    public static float onUsefulMagicHurtHealing(LivingEntity victim, DamageSource source, float originalDamage) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity holder)) return originalDamage;

        // 默认伤害的增益只由“未附魔”的手札结算（附魔的手札对默认伤害不再生效）
        double multiplier = computeDirectMultiplier(holder, victim.level());
        if (multiplier > 0) {
            return originalDamage * (1.0f + (float) multiplier);
        }
        return originalDamage;
    }

    public static boolean onUsefulMagicIncomingDamage(LivingEntity victim, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity holder)) return false;

        if (amount <= 0.0F) return false;
        if (isBlacklistedEntity(victim)) return false;

        // 只有“附魔”的手札产生延迟转化伤害
        var groups = buildConversionGroups(holder, victim.level());
        if (groups.isEmpty()) return false;

        ResourceLocation sourceTypeLoc = source.typeHolder().unwrapKey().map(key -> key.location()).orElse(null);

        ConversionDamageState state = pendingDamages.get(victim.getUUID());
        if (state == null) {
            if (pendingDamages.size() >= MAX_PENDING_VICTIMS) {
                return false;
            }
            state = new ConversionDamageState();
            state.countdown = Math.max(1, (int) (ModConfig.getInstance().magicConversion.intervalSeconds * 20));
            pendingDamages.put(victim.getUUID(), state);
        }

        for (var g : groups) {
            if (g.multiplier <= 0) continue;
            if (USEFULMAGIC_MAGIC.location().equals(g.damageType)) continue;
            if (sourceTypeLoc != null && sourceTypeLoc.equals(g.damageType)) continue;
            float convAmount = amount * (float) g.multiplier;
            if (convAmount <= 0) continue;
            ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, g.damageType);
            PendingEntry existing = state.pendingByType.get(key);
            if (existing == null) {
                if (state.pendingByType.size() >= MAX_PENDING_TYPES_PER_VICTIM) {
                    continue;
                }
                state.pendingByType.put(key, new PendingEntry(key, convAmount, source.getDirectEntity(), source.getEntity()));
            } else {
                existing.amount += convAmount;
                existing.directEntity = source.getDirectEntity();
                existing.sourceEntity = source.getEntity();
            }
        }
        return false;
    }

    private static LivingEntity findLivingEntity(MinecraftServer server, UUID uuid) {
        for (ServerLevel serverLevel : server.getAllLevels()) {
            Entity e = serverLevel.getEntity(uuid);
            if (e instanceof LivingEntity le && le.isAlive()) {
                return le;
            }
        }
        return null;
    }

    private static PendingEntry pickLargest(ConversionDamageState state) {
        PendingEntry largest = null;
        for (PendingEntry entry : state.pendingByType.values()) {
            if (largest == null || entry.amount > largest.amount) {
                largest = entry;
            }
        }
        if (largest != null) {
            state.pendingByType.remove(largest.damageType);
        }
        return largest;
    }

    private static void applyConversionDamage(LivingEntity living, PendingEntry pending) {
        var holderOpt = living.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolder(pending.damageType);
        if (holderOpt.isEmpty()) return;
        DamageSource convSource = new DamageSource(holderOpt.get(), pending.directEntity, pending.sourceEntity);
        CONVERSION_IN_PROGRESS.set(living.getUUID());
        try {
            int savedInvuln = living.invulnerableTime;
            living.invulnerableTime = 0;
            living.hurt(convSource, pending.amount);
            living.invulnerableTime = savedInvuln;
        } finally {
            CONVERSION_IN_PROGRESS.remove();
        }
    }

    public static void onXpGained(Player player, int amount) {
        if (amount <= 0) return;
        scanAllSlots(player, stack -> {
            if (stack.is(ModItems.EXPERIENCE_MAGIC_LETTER.get())) {
                ExperienceMagicLetterItem.addExperience(stack, amount);
            }
        });
    }

    public static void onFishCaught(Player player) {
        if (player.level().isClientSide) return;
        scanAllSlots(player, stack -> {
            if (stack.is(ModItems.FISHING_MAGIC_LETTER.get())) {
                FishingMagicLetterItem.addFish(stack);
            }
        });
    }

    public static void onTreasureOpened(Player player) {
        if (player.level().isClientSide) return;
        scanAllSlots(player, stack -> {
            if (stack.is(ModItems.TREASURE_MAGIC_LETTER.get())) {
                TreasureMagicLetterItem.addOpen(stack);
            }
        });
    }

    /** 坚韧魔法手札：实体（玩家或生物）承受的伤害累计。 */
    public static void onEntityDamageTaken(LivingEntity entity, float amount) {
        if (entity.level().isClientSide || amount <= 0) return;
        scanEntitySlots(entity, stack -> {
            if (stack.is(ModItems.TENACITY_MAGIC_LETTER.get())) {
                TenacityMagicLetterItem.addDamage(stack, amount);
            }
        });
    }

    public static void onRaidVictory(Player player) {
        if (player.level().isClientSide) return;
        scanAllSlots(player, stack -> {
            if (stack.is(ModItems.HERO_MAGIC_LETTER.get())) {
                HeroMagicLetterItem.addVictory(stack);
            }
        });
    }

    private static void tickTravelDistances(MinecraftServer server) {
        for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
            Vec3 pos = player.position();
            Vec3 prev = lastTravelPos.get(player.getUUID());
            if (prev != null) {
                double dx = pos.x - prev.x;
                double dz = pos.z - prev.z;
                double dy = pos.y - prev.y;
                double walk = 0;
                double fly = 0;
                if (player.getAbilities().flying || player.isFallFlying()) {
                    fly = Math.sqrt(dx * dx + dy * dy + dz * dz);
                } else {
                    walk = Math.sqrt(dx * dx + dz * dz);
                }
                walk = Math.min(walk, MAX_TRAVEL_PER_TICK * TRAVEL_POLL_INTERVAL);
                fly = Math.min(fly, MAX_TRAVEL_PER_TICK * TRAVEL_POLL_INTERVAL);
                if (walk > 0.001 || fly > 0.001) {
                    final double w = walk;
                    final double f = fly;
                    scanAllSlots(player, stack -> {
                        if (stack.is(ModItems.TRAVEL_MAGIC_LETTER.get())) {
                            TravelMagicLetterItem.addDistance(stack, w, f);
                        }
                    });
                }
            }
            lastTravelPos.put(player.getUUID(), pos);
        }
        if (lastTravelPos.size() > 1000) {
            lastTravelPos.clear();
        }
    }

    public static boolean shouldProtectItemDrop(ItemEntity itemEntity, DamageSource source) {
        return isOurLetter(itemEntity.getItem());
    }

    public static boolean isOurLetter(ItemStack stack) {
        return isOurModItem(stack);
    }

    public static boolean isOurModItem(ItemStack stack) {
        return stack.getItem() instanceof MagicLetterItem || stack.getItem() instanceof LetterBinderItem;
    }

    private static boolean isForeignBound(Player player, ItemStack stack) {
        if (ModConfig.getInstance().letterBinding.uuidWhitelist.contains(player.getUUID().toString())) {
            return false; // 弹出白名单：该玩家可持有任意玩家 ID 绑定的手札/合订本而不弹出
        }
        Optional<UUID> bound = ModDataComponents.getBoundPlayer(stack);
        return bound.isPresent() && !bound.get().equals(player.getUUID());
    }

    private static void ejectForeignBoundLetters(MinecraftServer server) {
        for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
            Level level = player.level();
            if (level.isClientSide) continue;
            var inv = player.getInventory();
            for (int i = 0; i < inv.items.size(); i++) tryEjectForeign(level, inv.items, i, player);
            for (int i = 0; i < inv.armor.size(); i++) tryEjectForeign(level, inv.armor, i, player);
            for (int i = 0; i < inv.offhand.size(); i++) tryEjectForeign(level, inv.offhand, i, player);
            List<ItemStack> invStacks = new ArrayList<>();
            invStacks.addAll(inv.items);
            invStacks.addAll(inv.armor);
            invStacks.addAll(inv.offhand);
            for (ItemStack stack : invStacks) {
                ejectForeignInner(level, player, stack);
                if (stack.getItem() instanceof LetterBinderItem) LetterBinderItem.ensureIcon(stack);
            }
            CuriosIntegration.ejectStacksWhere(player,
                    stack -> isOurModItem(stack) && isForeignBound(player, stack),
                    copy -> dropProtected(level, player, copy));
            for (ItemStack stack : CuriosIntegration.getCuriosStacks(player)) {
                ejectForeignInner(level, player, stack);
                if (stack.getItem() instanceof LetterBinderItem) LetterBinderItem.ensureIcon(stack);
            }
        }
    }

    private static void tryEjectForeign(Level level, java.util.List<ItemStack> container, int index, Player player) {
        ItemStack stack = container.get(index);
        if (stack.isEmpty()) return;
        if (!isOurModItem(stack)) return;
        if (!isForeignBound(player, stack)) return;
        ItemStack dropStack = stack.copy();
        container.set(index, ItemStack.EMPTY);
        dropProtected(level, player, dropStack);
    }

    /** 把不属于该玩家的绑定物品从合订本内容物中移除并弹出（合订本本身留在原槽位）。 */
    private static void ejectForeignInner(Level level, Player player, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof LetterBinderItem)) return;
        NonNullList<ItemStack> contents = LetterBinderItem.readContents(stack);
        if (contents.isEmpty()) return;
        List<ItemStack> keep = new ArrayList<>();
        boolean changed = false;
        for (ItemStack inner : contents) {
            if (inner.isEmpty()) continue;
            if (isOurModItem(inner) && isForeignBound(player, inner)) {
                dropProtected(level, player, inner.copy());
                changed = true;
            } else {
                keep.add(inner.copy());
            }
        }
        if (changed) {
            LetterBinderItem.writeContents(stack, NonNullList.of(ItemStack.EMPTY, keep.toArray(new ItemStack[0])));
            LetterBinderItem.ensureIcon(stack);
        }
    }

    private static void dropProtected(Level level, Player player, ItemStack dropStack) {
        ItemEntity drop = new ItemEntity(level,
                player.getX(), player.getBoundingBox().getYsize() > 0 ? player.getY() + 1.0 : player.getY() + 0.5, player.getZ(),
                dropStack);
        level.addFreshEntity(drop);
    }

    private static void scanAllSlots(Player player, java.util.function.Consumer<ItemStack> consumer) {
        scanEntitySlots(player, consumer);
    }

    /**
     * 扫描实体携带的手札槽位：玩家含背包/盔甲/副手/饰品，生物含手持/盔甲/饰品。
     * 用于让生物装备的手札同样对护甲/韧性/抗性/伤害/光灵生效。
     */
    private static void scanEntitySlots(LivingEntity entity, java.util.function.Consumer<ItemStack> consumer) {
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
                if (!inner.isEmpty()) {
                    consumer.accept(inner);
                }
            }
        }
    }

    private static double getMultiplierFromStack(ItemStack stack, Level level) {
        if (stack.isEmpty()) return 0;
        var item = stack.getItem();
        if (item instanceof TimeMagicLetterItem) {
            return TimeMagicLetterItem.getMultiplier(level, stack);
        }
        if (item instanceof MagicLetterItem ml) {
            return ml.getMultiplier(stack);
        }
        return 0;
    }

    private static List<LetterInfo> collectLetters(LivingEntity holder, Level level) {
        List<LetterInfo> result = new ArrayList<>();
        boolean dedup = !ModConfig.getInstance().stackingRules.allowSameLetters;
        Set<String> seen = new HashSet<>();
        scanEntitySlots(holder, stack -> {
            double mult = getMultiplierFromStack(stack, level);
            if (mult <= 0) return;
            Optional<UUID> bound = ModDataComponents.getBoundPlayer(stack);
            if (bound.isPresent() && !bound.get().equals(holder.getUUID())) {
                return;
            }
            ResourceLocation type = ModEnchantments.getEffectiveDamageType(stack);
            boolean enchanted = ModEnchantments.hasMagicConversion(stack);
            if (dedup) {
                String key = stack.getItem() + "|" + mult + "|" + type + "|" + enchanted;
                if (!seen.add(key)) return;
            }
            result.add(new LetterInfo(mult, type, enchanted));
        });
        return result;
    }

    private static List<LetterInfo> collectEffectiveLetters(LivingEntity player, Level level) {
        List<LetterInfo> letters = collectLetters(player, level);
        if (letters.isEmpty()) return letters;
        if (!ModConfig.getInstance().stackingRules.allowMultipleLetters) {
            LetterInfo best = null;
            for (LetterInfo info : letters) {
                if (best == null || info.multiplier > best.multiplier) {
                    best = info;
                }
            }
            return best == null ? List.of() : List.of(best);
        }
        return letters;
    }

    private static class LetterInfo {
        final double multiplier;
        final ResourceLocation damageType;
        final boolean enchanted;

        LetterInfo(double multiplier, ResourceLocation damageType, boolean enchanted) {
            this.multiplier = multiplier;
            this.damageType = damageType;
            this.enchanted = enchanted;
        }
    }

    private static class TypeGroup {
        final ResourceLocation damageType;
        double multiplier;

        TypeGroup(ResourceLocation damageType, double multiplier) {
            this.damageType = damageType;
            this.multiplier = multiplier;
        }
    }

    private static double computeDirectMultiplier(LivingEntity player, Level level) {
        double total = 0;
        for (LetterInfo info : collectEffectiveLetters(player, level)) {
            if (info.enchanted) continue;
            total += info.multiplier;
        }
        return total;
    }

    private static List<TypeGroup> buildConversionGroups(LivingEntity player, Level level) {
        Map<ResourceLocation, List<Double>> byType = new LinkedHashMap<>();
        for (LetterInfo info : collectEffectiveLetters(player, level)) {
            if (!info.enchanted) continue;
            byType.computeIfAbsent(info.damageType, k -> new ArrayList<>()).add(info.multiplier);
        }
        List<TypeGroup> groups = new ArrayList<>();
        for (var entry : byType.entrySet()) {
            double typeTotal = entry.getValue().stream().mapToDouble(Double::doubleValue).sum();
            groups.add(new TypeGroup(entry.getKey(), typeTotal));
        }
        return groups;
    }
}