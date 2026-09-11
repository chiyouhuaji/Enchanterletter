package cn.autoforged.enchanter_letter.event;

import cn.autoforged.enchanter_letter.ModDataComponents;
import cn.autoforged.enchanter_letter.config.ModConfig;
import cn.autoforged.enchanter_letter.effect.ModMagicActivation;
import cn.autoforged.enchanter_letter.effect.ModMagicObstruction;
import cn.autoforged.enchanter_letter.enchantment.ModEnchantments;
import cn.autoforged.enchanter_letter.integration.AccessoriesIntegration;
import cn.autoforged.enchanter_letter.integration.CuriosIntegration;
import cn.autoforged.enchanter_letter.item.LetterStats;
import cn.autoforged.enchanter_letter.item.ExperienceMagicLetterItem;
import cn.autoforged.enchanter_letter.item.FishingMagicLetterItem;
import cn.autoforged.enchanter_letter.item.HeroMagicLetterItem;
import cn.autoforged.enchanter_letter.item.KillMagicLetterItem;
import cn.autoforged.enchanter_letter.item.LetterBinderItem;
import cn.autoforged.enchanter_letter.item.LetterPotions;
import cn.autoforged.enchanter_letter.item.MagicLetterItem;
import cn.autoforged.enchanter_letter.item.ModItems;
import cn.autoforged.enchanter_letter.item.TenacityMagicLetterItem;
import cn.autoforged.enchanter_letter.item.TemporaryLetterBinderItem;
import cn.autoforged.enchanter_letter.item.TimeMagicLetterItem;
import cn.autoforged.enchanter_letter.item.TravelMagicLetterItem;
import cn.autoforged.enchanter_letter.item.TreasureMagicLetterItem;
import cn.autoforged.enchanter_letter.storage.LetterStorageManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class ModCommonEvents {
    static final ResourceKey<DamageType> USEFULMAGIC_MAGIC =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("usefulmagic:magic"));

    /** /kill 鎸囦护浣跨敤鐨勪激瀹崇被鍨嬶紙涓嶅啀璁″叆鍧氶煣鎵嬫湱鎵垮彈浼ゅ璁℃暟锛夈€?*/
    private static final ResourceKey<DamageType> GENERIC_KILL =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("minecraft", "generic_kill"));

    private static final int MAX_PENDING_TYPES_PER_VICTIM = 16;
    private static final int MAX_PENDING_VICTIMS = 200;
    private static final int MAX_STALE_TICKS = 100;
    private static final int BIND_CHECK_INTERVAL = 20;
    private static final double MAX_TRAVEL_PER_TICK = 100.0;
    /** 鏃呰璺濈閲囨牱闂撮殧锛坱ick锛夛細姣?tick 鍏ㄦЫ杞浼氭寔缁壂鎻忓湪绾跨帺瀹舵Ы浣嶏紝
     *  鏈€浣庡姞鍏?1 tick锛?.05 绉掞級寤惰繜锛岃繖閲屽彇 2 tick 閲囨牱涓€娆°€?*/
    private static final int TRAVEL_POLL_INTERVAL = 5;
    private static final int ACCESSORY_COMPAT_INTERVAL = 100;
    private static final int ARMOR_SYNC_INTERVAL = 10;
    /** 鎵嬫湱鑽按鏁堟灉杞闂撮殧锛坱ick锛屾瘡 1 绉掞紱鐢ㄤ笘鐣屾椂闂村垽瀹氬惊鐜紝鏃犻渶姣?tick锛夈€?*/
    private static final int POTION_POLL_INTERVAL = 20;
    private static final int MAGIC_ACTIVATION_INTERVAL = 100;

    private static int accessoryCompatCooldown = ACCESSORY_COMPAT_INTERVAL;
    private static int bindCheckCooldown = BIND_CHECK_INTERVAL;
    private static int armorSyncCooldown = ARMOR_SYNC_INTERVAL;
    private static int travelPollCooldown = 0;
    private static int potionPollCooldown = POTION_POLL_INTERVAL;
    private static int magicActivationCooldown = MAGIC_ACTIVATION_INTERVAL;

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

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            // 鏉€鎴墜鏈細鎸佹湁鑰咃紙鐜╁鎴栫敓鐗╋級鍑绘潃浠绘剰鐢熺墿/鐜╁ +1
            if (source.getEntity() instanceof LivingEntity killer) {
                scanEntitySlots(killer, stack -> {
                    if (stack.is(ModItems.KILL_MAGIC_LETTER.get())) {
                        KillMagicLetterItem.addKill(stack);
                    }
                });
            }
            pendingDamages.remove(entity.getUUID());
        });

        // 閲嶇敓锛氳繑杩樻浜′繚鐣欐睜锛堥瓟娉曠粦瀹氱墿鍝?/ 婧㈠嚭鐨勯潪璇呭拻鎵嬫湱锛?
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> LetterEntityEffects.onPlayerRespawn(newPlayer));

        // 鐧诲嚭锛氭竻绌轰繚鐣欐睜寮曠敤 + /letterstorage 鎴彇鐘舵€?
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> {
                    LetterEntityEffects.onPlayerLogout(handler.player.getUUID());
                    LetterStorageManager.onLogout(handler.player.getUUID());
                });

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof ItemEntity itemEntity)) return;
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) return;
            // 涓存椂鎵嬫湱鍚堣鏈笉鍏佽鎴愪负鎺夎惤鐗╋細鐩存帴閿€姣?
            if (TemporaryLetterBinderItem.isTemporaryBinder(stack)) {
                entity.discard();
                return;
            }
            // 寮哄埗娑堝け寮€鍚椂锛氬甫鏈夋秷澶辫瘏鍜掔殑鐗╁搧涓€寰嬮樆姝㈠舰鎴愭帀钀界墿锛堝寘鎷敓鐗╃殑 NBT 瑁呭杞垚鎺夎惤鐗╋級
            // 鏈ā缁勯€氳繃 addVanishing 鏍囪鐨勬秷澶辩墿鍝侊細蹇界暐 /lettervanish 寮€鍏筹紝涓€寰嬮攢姣併€?
            if ((ModConfig.getInstance().letterVanish.enabled && ModEnchantments.hasVanishing(stack))
                    || ModEnchantments.isModAppliedVanishing(stack)) {
                entity.discard();
                return;
            }
            // 榄旀硶缁戝畾鐗╁搧锛氭寔鏈変汉姝ｆ浜℃椂鍏滃簳閿€姣佸苟鍔犲叆淇濈暀姹狅紙閬垮厤鎺夎惤涓㈠け锛涢噸鐢熸椂缁熶竴褰掕繕锛?
            if (ModEnchantments.hasEffectiveMagicBinding(stack)) {
                Optional<UUID> boundOpt = ModDataComponents.getBoundPlayer(stack);
                if (boundOpt.isPresent() && world instanceof ServerLevel) {
                    ServerLevel serverLevel = (ServerLevel) world;
                    Player owner = serverLevel.getPlayerByUUID(boundOpt.get());
                    if (owner != null && owner.isDeadOrDying()) {
                        LetterEntityEffects.addToKeepPool(owner, stack);
                        entity.discard();
                        return;
                    }
                }
            }
            if (isOurLetter(stack)) {
                // 鎵嬫湱鎺夎惤鐗╋細鐧借壊鍙戝厜锛堟柟渚挎壘鍒扮墿鍝侊級+ 鏃犻噸鍔涖€侀潤姝笉鍔?
                entity.setGlowingTag(true);
                entity.setNoGravity(true);
                entity.setDeltaMovement(0.0, 0.0, 0.0);
                Optional<UUID> boundOpt = ModDataComponents.getBoundPlayer(stack);
                if (boundOpt.isPresent()) {
                    itemEntity.setTarget(boundOpt.get());
                }
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (--bindCheckCooldown <= 0) {
                bindCheckCooldown = BIND_CHECK_INTERVAL;
                ejectForeignBoundLetters(server);
            }

            if (--accessoryCompatCooldown <= 0) {
                accessoryCompatCooldown = ACCESSORY_COMPAT_INTERVAL;
                CuriosIntegration.ensureAllSlotCompat();
                AccessoriesIntegration.ensureAllSlotCompat(server.overworld());
            }

            // 绱鎵€鏈夊湪绾跨帺瀹剁殑琛岃蛋/椋炶璺濈锛堟梾琛岄瓟娉曟墜鏈級锛氬甫鍐峰嵈锛岄伩鍏嶆瘡 tick 鍏ㄦЫ杞
            if (--travelPollCooldown <= 0) {
                travelPollCooldown = TRAVEL_POLL_INTERVAL;
                tickTravelDistances(server);
            }

            // 鎵嬫湱鎶ょ敳鍊?鎶ょ敳闊ф€э細浠ュ師鐗堝睘鎬?modifier 褰㈠紡甯搁┗鍙犲姞锛堟姢鐢叉潯/Overloaded Armor Bar 鍙锛涚敓鐗╁悓鏍风敓鏁堬級
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

            // 鎵嬫湱鑽按鏁堟灉锛氬鍦ㄧ嚎鐜╁锛堝強鎼哄甫鎵嬫湱鐨勭敓鐗╋級鎸夎瘝鏉″惊鐜ā寮忔柦鍔狅紙涓栫晫鏃堕棿鍒ゅ畾锛?
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

            // 鍏夌伒鍙戝厜鍚屾 / 鐢熺墿娑堝け璇呭拻闆?UUID 缁戝畾 / 鎺夎惤鐗╁畾鏃舵竻鐞?
            LetterEntityEffects.tick(server);

            // 榄旀硶婵€娲伙細淇濊瘉涓存椂鎵嬫湱鍚堣鏈寔鏈夎€呰幏寰楁晥鏋滐紱鏁堟灉娑堝け鏃舵竻鐞嗕复鏃跺悎璁㈡湰锛堟瘡 5 绉掞級
            if (--magicActivationCooldown <= 0) {
                magicActivationCooldown = MAGIC_ACTIVATION_INTERVAL;
                ModMagicActivation.tick(server);
            }

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
        });
    }

    /**
     * 鎶楁€ф彁鍗囷細鍙楀鑰呮惡甯︾殑鎵嬫湱鎸夆€滃師鐗堟姉鎬ф彁鍗囪嵂姘粹€濋€昏緫鍑忓厤浼ゅ
     * 锛堝噺鍏嶆瘮渚?= 绛夌骇 脳 姣忕骇澧為暱锛屾渶缁堝彇 min(鍑忓厤, 鏈嶅姟鍣ㄩ厤缃笂闄?锛夈€?
     * 鍔熻兘寮€鍏虫垨鍑忓厤涓?0 鏃跺師鏍疯繑鍥炪€?
     */
    public static float applyResistanceReduction(LivingEntity victim, float amount) {
        if (ModMagicObstruction.blocksAll(victim)) return amount;
        if (amount <= 0) return amount;
        ModConfig config = ModConfig.getInstance();
        if (!config.letterResistance.enabled) return amount;
        double reduction = LetterStats.effectiveResistance(victim, victim.level());
        if (reduction <= 0) return amount;
        reduction = Math.min(reduction, config.letterResistance.limit);
        return (float) (amount * (1.0 - reduction));
    }

    public static boolean isConversionInProgress(LivingEntity entity) {
        UUID current = CONVERSION_IN_PROGRESS.get();
        return current != null && current.equals(entity.getUUID());
    }

    public static boolean isLetterBoosted(DamageSource source) {
        if (source.is(USEFULMAGIC_MAGIC)) return true;
        Optional<ResourceKey<DamageType>> keyOpt = source.typeHolder().unwrapKey();
        return ModConfig.getInstance().getDefaultBonusDamageTypes().contains(keyOpt.get().location());
    }

    /**
     * 鏄惁搴斿鏈浼ゅ杩涜鎵嬫湱澧炵泭锛?
     * 1) 鍛戒腑鍙鐩婁激瀹崇被鍨嬶紙鐜╁琛屼负锛夛紝鎴?
     * 2) 鏀诲嚮鑰呬负鈥滅敓鐗┾€濓紙闈炵帺瀹?LivingEntity锛変笖鍏朵富鍓墜/瑁呭鎸佹湁鐢熸晥鎵嬫湱鏃跺氨瀵瑰叾鎵€鏈変激瀹冲鐩婏紝
     *    涓嶅啀瑕佹眰浼ゅ绫诲瀷鍖归厤锛堥渶姹傦細鐢熺墿鎸佹湁鎵嬫湱鏃跺鎵€鏈変骇鐢熺殑浼ゅ杩涜澧炵泭锛夈€?
     */
    public static boolean shouldApplyLetterBoost(DamageSource source) {
        Entity attacker = source.getEntity();
        if (isLetterBoosted(source)) return true;
        if (attacker instanceof LivingEntity holder && !(holder instanceof Player)) {
            return hasEffectiveLetter(holder);
        }
        return false;
    }

    private static boolean hasEffectiveLetter(LivingEntity holder) {
        return !collectEffectiveLetters(holder, holder.level()).isEmpty();
    }

    public static boolean isBlacklistedEntity(LivingEntity victim) {
        ResourceLocation entityKey = EntityType.getKey(victim.getType());
        return ModConfig.getInstance().getBlacklistEntityNames().contains(entityKey);
    }

    public static float onUsefulMagicHurtHealing(LivingEntity victim, DamageSource source, float originalDamage) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity holder)) return originalDamage;

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

        var groups = buildConversionGroups(holder, victim.level());

        ResourceLocation sourceTypeLoc = source.typeHolder().unwrapKey().map(key -> key.location()).orElse(null);

        ConversionDamageState state = pendingDamages.get(victim.getUUID());
        if (state == null) {
            if (pendingDamages.size() >= MAX_PENDING_VICTIMS) {
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
        scanEntitySlots(player, stack -> {
            if (stack.is(ModItems.EXPERIENCE_MAGIC_LETTER.get())) {
                ExperienceMagicLetterItem.addExperience(stack, amount);
            }
        });
    }

    public static void onFishCaught(Player player) {
        if (player.level().isClientSide) return;
        scanEntitySlots(player, stack -> {
            if (stack.is(ModItems.FISHING_MAGIC_LETTER.get())) {
                FishingMagicLetterItem.addFish(stack);
            }
        });
    }

    public static void onTreasureOpened(Player player) {
        if (player.level().isClientSide) return;
        scanEntitySlots(player, stack -> {
            if (stack.is(ModItems.TREASURE_MAGIC_LETTER.get())) {
                TreasureMagicLetterItem.addOpen(stack);
            }
        });
    }

    /** 鍧氶煣榄旀硶鎵嬫湱锛氬疄浣擄紙鐜╁鎴栫敓鐗╋級鎵垮彈鐨勪激瀹崇疮璁°€?*/
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
        scanEntitySlots(player, stack -> {
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
                    scanEntitySlots(player, stack -> {
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
        return stack.getItem() instanceof MagicLetterItem || stack.getItem() instanceof LetterBinderItem
                || stack.getItem() instanceof TemporaryLetterBinderItem;
    }

    /**
     * 鏄惁搴斿己鍒舵竻闄よ鐗╁搧锛堜綔涓烘帀钀界墿涓€寰嬮攢姣侊紱鐜╁涓诲姩涓㈠嚭鐨勪复鏃跺悎璁㈡湰璧?onItemToss 鏀捐涓烘甯告帀钀界墿锛夈€?
     * - 涓存椂鎵嬫湱鍚堣鏈細娌℃湁浠讳綍缁戝畾閫昏緫锛屾棤璁洪檮榄斿姛鑳藉紑鍏充笌鍚﹂兘鐩存帴娓呴櫎锛?lettervanish 鍊欒ˉ鎴愬憳锛夈€?
     * - 寮哄埗娑堝け寮€鍚椂锛氬甫娑堝け璇呭拻鐨勭墿鍝佺洿鎺ユ竻闄ゃ€?
     */
    public static boolean shouldVanishClear(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // 1) 鐧藉悕鍗曟垚鍛橈紙鍚?addVanishing 鏍囪锛夛細鏃犺寮€鍏筹紝濮嬬粓寮哄埗娑堝け銆?
        if (ModEnchantments.isModAppliedVanishing(stack) || isForceVanishWhitelisted(stack)) return true;
        // 2) 寮€鍏冲紑鍚細鏀捐鎵€鏈夊甫娑堝け璇呭拻鐨勭墿鍝併€?
        if (ModConfig.getInstance().letterVanish.enabled) return ModEnchantments.hasVanishing(stack);
        // 3) 寮€鍏冲叧闂細鍙湁鐧藉悕鍗曠墿鍝佽Е鍙戯紝鍏朵綑鎷︽埅锛堜笉娑堝け锛夈€?
        return false;
    }

    /** 鏄惁涓哄己鍒舵秷澶辩櫧鍚嶅崟鎴愬憳锛堟寜鐗╁搧娉ㄥ唽鍚嶏紝濡?enchanter_letter:temporary_letter_binder锛夈€?*/
    private static boolean isForceVanishWhitelisted(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        List<String> whitelist = ModConfig.getInstance().letterVanish.forceVanishWhitelist;
        if (whitelist == null || whitelist.isEmpty()) return false;
        String itemKey = ModEnchantments.getItemKeyString(stack);
        if (itemKey.isEmpty()) return false;
        return whitelist.contains(itemKey);
    }

    private static boolean isForeignBound(Player player, ItemStack stack) {
        if (ModConfig.getInstance().letterBinding.uuidWhitelist.contains(player.getUUID().toString())) {
            return false; // 寮瑰嚭鐧藉悕鍗曪細璇ョ帺瀹跺彲鎸佹湁浠绘剰鐜╁ ID 缁戝畾鐨勬墜鏈?鍚堣鏈€屼笉寮瑰嚭
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
            AccessoriesIntegration.ejectStacksWhere(player,
                    stack -> isOurModItem(stack) && isForeignBound(player, stack),
                    copy -> dropProtected(level, player, copy));
            AccessoriesIntegration.mutateStacksWhere(player,
                    stack -> stack.getItem() instanceof LetterBinderItem,
                    stack -> {
                        ejectForeignInner(level, player, stack);
                        LetterBinderItem.ensureIcon(stack);
                    });
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

    /**
     * 鎵弿瀹炰綋鎼哄甫鐨勬墜鏈Ы浣嶏細鐜╁鍚儗鍖?鐩旂敳/鍓墜/楗板搧锛圕urios + Accessories锛夛紝
     * 鐢熺墿鍚墜鎸?鐩旂敳/楗板搧銆傜敤浜庤鐢熺墿瑁呭鐨勬墜鏈悓鏍峰鎶ょ敳/闊ф€?鎶楁€?浼ゅ/鍏夌伒鐢熸晥銆?
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
        for (var stack : AccessoriesIntegration.getAccessoriesStacks(entity)) scanStack(entity, stack, consumer);
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
        } else if (stack.getItem() instanceof TemporaryLetterBinderItem && ModMagicActivation.isActive(entity)) {
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

    private static List<LetterInfo> collectEffectiveLetters(LivingEntity holder, Level level) {
        List<LetterInfo> letters = collectLetters(holder, level);
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

    private static double computeDirectMultiplier(LivingEntity holder, Level level) {
        if (ModMagicObstruction.blocksDamage(holder)) return 0;
        double total = 0;
        for (LetterInfo info : collectEffectiveLetters(holder, level)) {
            if (info.enchanted) continue;
            total += info.multiplier;
        }
        return total;
    }

    private static List<TypeGroup> buildConversionGroups(LivingEntity holder, Level level) {
        if (ModMagicObstruction.blocksDamage(holder)) return List.of();
        Map<ResourceLocation, List<Double>> byType = new LinkedHashMap<>();
        for (LetterInfo info : collectEffectiveLetters(holder, level)) {
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
