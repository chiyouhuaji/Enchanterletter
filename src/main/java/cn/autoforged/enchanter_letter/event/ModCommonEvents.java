package cn.autoforged.enchanter_letter.event;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import cn.autoforged.enchanter_letter.command.LetterCommands;
import cn.autoforged.enchanter_letter.command.LetterDamageCommand;
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
import cn.autoforged.enchanter_letter.item.LetterStats;
import cn.autoforged.enchanter_letter.item.MagicLetterItem;
import cn.autoforged.enchanter_letter.item.ModItems;
import cn.autoforged.enchanter_letter.item.TenacityMagicLetterItem;
import cn.autoforged.enchanter_letter.item.TimeMagicLetterItem;
import cn.autoforged.enchanter_letter.item.TravelMagicLetterItem;
import cn.autoforged.enchanter_letter.item.TreasureMagicLetterItem;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

@EventBusSubscriber(modid = UsefulMagicEnchanterLetterMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ModCommonEvents {
    static final ResourceKey<DamageType> USEFULMAGIC_MAGIC =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse("usefulmagic:magic"));

    /** /kill 指令使用的伤害类型（不再计入坚韧手札承受伤害计数）。 */
    private static final ResourceKey<DamageType> GENERIC_KILL =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.withDefaultNamespace("generic_kill"));

    /** 每个受害者允许排队的最大转化伤害类型数，超出部分丢弃（防止队列无界增长）。 */
    private static final int MAX_PENDING_TYPES_PER_VICTIM = 16;
    /** 全局待结算受害者上限，超出时不再新建队列（防止过多受害者堆积拖慢 tick）。 */
    private static final int MAX_PENDING_VICTIMS = 200;
    /** 受害者找不到时保留的 tick 数（约 5 秒），超时清理残留队列。 */
    private static final int MAX_STALE_TICKS = 100;
    /** 背包内他人绑定手札的弹出检测间隔（tick，默认每 1 秒）。 */
    private static final int BIND_CHECK_INTERVAL = 20;
    /** 每 tick 允许累计的最大移动距离（方块），防止传送/位移刷距离。 */
    private static final double MAX_TRAVEL_PER_TICK = 100.0;
    /** 旅行距离采样间隔（tick）：每 tick 全槽轮询会持续扫描在线玩家槽位，
     *  最低加入 1 tick（0.05 秒）延迟，这里取 2 tick 采样一次。 */
    private static final int TRAVEL_POLL_INTERVAL = 2;
    private static int travelPollCooldown = 0;
    /** 饰品模组（Curios）槽位兼容注入间隔（tick，默认每 5 秒，作为事件触发的兜底）。 */
    private static final int ACCESSORY_COMPAT_INTERVAL = 100;
    /** 手札护甲/韧性属性常驻同步间隔（tick，每 0.5 秒）。 */
    private static final int ARMOR_SYNC_INTERVAL = 10;

    private static int bindCheckCooldown = BIND_CHECK_INTERVAL;
    private static int accessoryCompatCooldown = ACCESSORY_COMPAT_INTERVAL;
    private static int armorSyncCooldown = ARMOR_SYNC_INTERVAL;

    private static final ThreadLocal<UUID> CONVERSION_IN_PROGRESS = new ThreadLocal<>();

    /** 记录每个玩家上一 tick 的位置，用于累计行走/飞行距离。 */
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
        /** 按伤害类型合并待结算伤害，同类型累加，避免同一受害者短时间多次受伤导致队列无限堆积。 */
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
        if (event.getEntity() instanceof LivingEntity living) {
            pendingDamages.remove(living.getUUID());
        }
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
            Optional<UUID> boundOpt = stack.get(ModDataComponents.BOUND_PLAYER.get());
            if (boundOpt != null && boundOpt.isPresent()) {
                itemEntity.setTarget(boundOpt.get());
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        // 定期监测：背包内他人绑定的手札自动弹出为掉落物
        if (--bindCheckCooldown <= 0) {
            bindCheckCooldown = BIND_CHECK_INTERVAL;
            ejectForeignBoundLetters(server);
        }

        // Curios 兼容：枚举所有槽位并允许我们的手札/合订本放入任意栏位（覆盖重载）
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

        // 光灵发光（玩家/生物）、生物消失诅咒零 UUID 绑定、定时清理绑定掉落物
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

    /** 重生：返还死亡保留池（魔法绑定物品 / 溢出的非诅咒手札）。 */
    @SubscribeEvent
    public static void onPlayerRespawn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
        LetterEntityEffects.onPlayerRespawn(event.getEntity());
    }

    /** 登出：清空保留池引用。 */
    @SubscribeEvent
    public static void onPlayerLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        LetterEntityEffects.onPlayerLogout(event.getEntity().getUUID());
        LetterStorageManager.onLogout(event.getEntity().getUUID());
    }

    /** 生物死亡（掉落生成前）：移除装备上的消失诅咒手札/合订本（合订本内非诅咒手札正常掉落）。 */
    @SubscribeEvent
    public static void onLivingDeathVanishing(LivingDeathEvent event) {
        LetterEntityEffects.handleMobDeath(event.getEntity());
    }

    /**
     * 死亡掉落兜底（SoulBound 思路）：从实际生成的掉落物中拦截魔法绑定物品并归还玩家，
     * 强制消失开启时同步拦截消失诅咒物品（原版消失诅咒会在掉落生成前删除）。
     * 遍历 event.getDrops() 可覆盖物品栏/盔甲/副手/饰品（含 Curios）。
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
                    // 神奇绑定：移除掉落物，重生归还
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

    /** 标签/槽位数据重载后，重新遍历 Curios 所有栏位注入兼容谓词。 */
    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        CuriosIntegration.ensureAllSlotCompat();
    }

    /** 服务器启动完成后（槽位数据已就绪）遍历所有 Curios 栏位注入兼容谓词。 */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        CuriosIntegration.ensureAllSlotCompat();
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (isConversionInProgress(victim)) return;

        // 坚韧手札：实体（玩家或生物）承受伤害累计（使用初始未增益伤害）
        // /kill 指令伤害（generic_kill）不计入，避免 /kill 刷坚韧计数
        if (!victim.level().isClientSide && !event.getSource().is(GENERIC_KILL)) {
            onEntityDamageTaken(victim, event.getOriginalAmount());
        }

        if (!shouldApplyLetterBoost(event.getSource()) || event.getAmount() <= 0) return;
        // 转化以“初始（未增益）”伤害为基数，避免被未附魔增益二次放大
        onUsefulMagicIncomingDamage(victim, event.getSource(), event.getOriginalAmount());
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        if (isConversionInProgress(victim)) return;
        float damage = event.getNewDamage();
        // 伤害增益（攻击者手札）
        if (shouldApplyLetterBoost(event.getSource())) {
            float amplified = onUsefulMagicHurtHealing(victim, event.getSource(), damage);
            if (amplified != damage) {
                event.setNewDamage(amplified);
                damage = amplified;
            }
        }
        // 抗性提升（受害者手札，参照原版抗性药水按比例减免，护甲结算之前）
        float afterResist = applyResistanceReduction(victim, damage);
        if (afterResist != damage) {
            event.setNewDamage(afterResist);
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
    }

    public static boolean isConversionInProgress(LivingEntity entity) {
        UUID current = CONVERSION_IN_PROGRESS.get();
        return current != null && current.equals(entity.getUUID());
    }

    /**
     * 判断该伤害源是否为手札可增益的伤害类型。
     */
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

    /**
     * 判断目标实体是否在黑名单内。
     */
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
            // 首次转化在“默认间隔”之后才结算
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

    /** 垂钓魔法手札：捕获一条鱼 +1。 */
    public static void onFishCaught(Player player) {
        if (player.level().isClientSide) return;
        scanAllSlots(player, stack -> {
            if (stack.is(ModItems.FISHING_MAGIC_LETTER.get())) {
                FishingMagicLetterItem.addFish(stack);
            }
        });
    }

    /** 宝藏魔法手札：开启一个未开过（仍有战利品表）的箱子 +1。 */
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

    /** 英雄手札：村庄袭击胜利 +1。 */
    public static void onRaidVictory(Player player) {
        if (player.level().isClientSide) return;
        scanAllSlots(player, stack -> {
            if (stack.is(ModItems.HERO_MAGIC_LETTER.get())) {
                HeroMagicLetterItem.addVictory(stack);
            }
        });
    }

    /** 旅行魔法手札：按每 tick 位移累计行走/飞行距离（米=方块）。 */
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
        // 手札掉落物无物理效果：免疫爆炸/仙人掌等所有伤害，配合 noGravity+归零速度保持原地不动
        return isOurLetter(itemEntity.getItem());
    }

    public static boolean isOurLetter(ItemStack stack) {
        return isOurModItem(stack);
    }

    /** 是否为模组自有物品（手札或手札合订本）。 */
    public static boolean isOurModItem(ItemStack stack) {
        return stack.getItem() instanceof MagicLetterItem || stack.getItem() instanceof LetterBinderItem;
    }

    /** 物品是否已绑定给其他玩家（白名单中的玩家可持有任意绑定手札而不弹出）。 */
    private static boolean isForeignBound(Player player, ItemStack stack) {
        Optional<UUID> bound = stack.get(ModDataComponents.BOUND_PLAYER.get());
        if (bound == null || bound.isEmpty()) return false;
        if (bound.get().equals(player.getUUID())) return false;
        // 弹出白名单：该玩家可拾取/持有任意玩家 ID 绑定的手札/合订本
        if (ModConfig.getInstance().letterBinding.uuidWhitelist.contains(player.getUUID().toString())) {
            return false;
        }
        return true;
    }

    /**
     * 定期监测所有在线玩家的背包与 Curios 饰品装备栏：若发现不属于该玩家的绑定物品
     * （手札或手札合订本，含合订本内容物中的他人绑定物品），自动弹出为掉落物。
     */
    private static void ejectForeignBoundLetters(MinecraftServer server) {
        for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
            Level level = player.level();
            if (level.isClientSide) continue;
            var inv = player.getInventory();
            // 1) 背包顶层槽位：不属于该玩家的绑定物品直接弹出
            for (int i = 0; i < inv.items.size(); i++) tryEjectForeign(level, inv.items, i, player);
            for (int i = 0; i < inv.armor.size(); i++) tryEjectForeign(level, inv.armor, i, player);
            for (int i = 0; i < inv.offhand.size(); i++) tryEjectForeign(level, inv.offhand, i, player);
            // 2) 背包中合订本内容物内的他人绑定物品
            List<ItemStack> invStacks = new ArrayList<>();
            invStacks.addAll(inv.items);
            invStacks.addAll(inv.armor);
            invStacks.addAll(inv.offhand);
            for (ItemStack stack : invStacks) {
                ejectForeignInner(level, player, stack);
                // 合订本图标同步（内容物是否为空 ↔ custom_model_data），覆盖旧存档与外部修改
                if (stack.getItem() instanceof LetterBinderItem) LetterBinderItem.ensureIcon(stack);
            }
            // 3) Curios 装备栏：顶层他人绑定物品弹出 + 合订本内容物内的他人绑定物品弹出
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
        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        if (contents.isEmpty()) return;
        List<ItemStack> keep = new ArrayList<>();
        boolean changed = false;
        for (ItemStack inner : contents.itemsCopy()) {
            if (inner.isEmpty()) continue;
            if (isOurModItem(inner) && isForeignBound(player, inner)) {
                dropProtected(level, player, inner.copy());
                changed = true;
            } else {
                keep.add(inner);
            }
        }
        if (changed) {
            stack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(keep));
        }
    }

    /** 弹出为掉落物（无重力、静止，由掉落物处理逻辑接管）。 */
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

    /**
     * 扫描单个物品槽位；若为手札合订本，则将其收纳袋内的所有物品视为一并扫描。
     */
    private static void scanStack(LivingEntity entity, ItemStack stack, java.util.function.Consumer<ItemStack> consumer) {
        if (stack.isEmpty()) return;
        consumer.accept(stack);
        if (stack.getItem() instanceof LetterBinderItem) {
            BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
            for (ItemStack inner : contents.itemsCopy()) {
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
            // 时间手札直接按服务器世界开启时间计算
            return TimeMagicLetterItem.getMultiplier(level);
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
            Optional<UUID> bound = stack.get(ModDataComponents.BOUND_PLAYER.get());
            if (bound != null && bound.isPresent() && !bound.get().equals(holder.getUUID())) {
                // 存在不属于持有者的绑定手札：不生效（玩家背包的交给 tick 定期弹出）
                return;
            }
            ResourceLocation type = ModEnchantments.getEffectiveDamageType(stack);
            boolean enchanted = ModEnchantments.hasMagicConversion(stack);
            if (dedup) {
                // 两个除转化状态外完全相同（同物品/倍率/伤害类型/附魔状态）的手札只生效一张
                String key = stack.getItem() + "|" + mult + "|" + type + "|" + enchanted;
                if (!seen.add(key)) return;
            }
            result.add(new LetterInfo(mult, type, enchanted));
        });
        return result;
    }

    /**
     * 实际生效的手札集合。
     * 当 stackingRules.allowMultipleLetters 为 false 时（默认），无论是否附魔，
     * 只判断所有手札中倍率最高的一张生效。
     */
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

    /**
     * 命中默认类型伤害时立即结算的增益：只累计生效集合中“未附魔”手札的加成。
     */
    private static double computeDirectMultiplier(LivingEntity player, Level level) {
        double total = 0;
        for (LetterInfo info : collectEffectiveLetters(player, level)) {
            if (info.enchanted) continue; // 附魔手札对默认伤害不生效
            total += info.multiplier;
        }
        return total;
    }

    /**
     * 参与延迟转化结算的手札组：只累计生效集合中“附魔”手札（按 NBT 标注的伤害类型分组并累加）。
     */
    private static List<TypeGroup> buildConversionGroups(LivingEntity player, Level level) {
        Map<ResourceLocation, List<Double>> byType = new LinkedHashMap<>();
        for (LetterInfo info : collectEffectiveLetters(player, level)) {
            if (!info.enchanted) continue; // 仅附魔手札产生延迟转化伤害
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
