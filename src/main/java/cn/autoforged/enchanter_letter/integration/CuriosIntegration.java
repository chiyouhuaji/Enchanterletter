package cn.autoforged.enchanter_letter.integration;

import cn.autoforged.enchanter_letter.item.LetterBinderItem;
import cn.autoforged.enchanter_letter.item.MagicLetterItem;
import cn.autoforged.enchanter_letter.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
/**
 * Curios API 兼容（可选，非前置，全部反射调用，对 Curios 版本尽量兼容）。
 * 流程：先注册（registerCurio 注册所有手札与合订本的行为 + registerCurioPredicate 注册允许谓词），
 * 再遍历所有槽位表（全局槽位表 / 玩家槽位表 / 实体槽位表，服务端与客户端）把谓词注入每个槽位的 validators。
 * 注意：Curios 的服务端装备校验走的是“实体槽位表”（isStackValid 调用 getEntitySlots），
 * 与全局槽位表是不同实例，必须全部注入，否则装备会被服务端拒绝而弹回。
 */
public class CuriosIntegration {
    private static final boolean LOADED;
    // 扫描用
    private static Method getInventoryMethod;
    private static Method getCuriosMethod;
    private static Method getSlotsMethod;
    private static Method getStackInSlotMethod;
    // 槽位兼容用（逐个解析，缺失不影响整体加载）
    private static Method registerPredicateMethod;
    private static Method getSlotsAllMethod;
    private static Method getSlotsNoArgMethod;
    private static Method getPlayerSlotsSideMethod;
    private static Method getPlayerSlotsNoArgMethod;
    private static Method getEntitySlotsSideMethod;
    private static Method getEntitySlotsNoArgMethod;
    private static Method getValidatorsMethod;
    private static Method slotResultStackMethod;
    private static Method getSlotHelperMethod;
    private static Method getSlotTypeIdsMethod;
    private static Method getSlotTypeMethod;
    private static boolean slotCompatUsable = false;

    private static final ResourceLocation ALL_LETTERS_PREDICATE =
            ResourceLocation.fromNamespaceAndPath("enchanter_letter", "all_letters");
    private static boolean predicateRegistered = false;
    // 是否已改写 Curios 全局 "curios:tag" 谓词（见 wrapTagPredicate，幂等）
    private static boolean tagPredicateWrapped = false;

    static {
        boolean flag = false;
        try {
            Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            getInventoryMethod = curiosApi.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class);

            // 槽位兼容方法逐个解析（Curios 不同小版本 API 略有差异，失败不影响整体）
            try { registerPredicateMethod = curiosApi.getMethod("registerCurioPredicate", ResourceLocation.class, Predicate.class); } catch (Exception ignored) {}
            try { getSlotsAllMethod = curiosApi.getMethod("getSlots", boolean.class); } catch (Exception ignored) {}
            try { getSlotsNoArgMethod = curiosApi.getMethod("getSlots"); } catch (Exception ignored) {}
            try { getPlayerSlotsSideMethod = curiosApi.getMethod("getPlayerSlots", boolean.class); } catch (Exception ignored) {}
            try { getPlayerSlotsNoArgMethod = curiosApi.getMethod("getPlayerSlots"); } catch (Exception ignored) {}
            try { getEntitySlotsSideMethod = curiosApi.getMethod("getEntitySlots", EntityType.class, boolean.class); } catch (Exception ignored) {}
            try { getEntitySlotsNoArgMethod = curiosApi.getMethod("getEntitySlots", EntityType.class); } catch (Exception ignored) {}
            try {
                Class<?> islotType = Class.forName("top.theillusivec4.curios.api.type.ISlotType");
                getValidatorsMethod = islotType.getMethod("getValidators");
            } catch (Exception ignored) {}
            try {
                Class<?> slotResult = Class.forName("top.theillusivec4.curios.api.SlotResult");
                slotResultStackMethod = slotResult.getMethod("stack");
            } catch (Exception ignored) {}
            try {
                Class<?> slotHelperI = Class.forName("top.theillusivec4.curios.api.type.util.ISlotHelper");
                getSlotTypeIdsMethod = slotHelperI.getMethod("getSlotTypeIds");
                getSlotTypeMethod = slotHelperI.getMethod("getSlotType", String.class);
            } catch (Exception ignored) {}
            try { getSlotHelperMethod = curiosApi.getMethod("getSlotHelper"); } catch (Exception ignored) {}

            slotCompatUsable = registerPredicateMethod != null
                    && getValidatorsMethod != null
                    && slotResultStackMethod != null
                    && (getSlotsAllMethod != null || getSlotsNoArgMethod != null
                        || getPlayerSlotsSideMethod != null || getEntitySlotsSideMethod != null
                        || (getSlotHelperMethod != null && getSlotTypeIdsMethod != null));
            flag = true;
        } catch (Exception ignored) {
        }
        LOADED = flag;
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    public static boolean isOurAccessoryItem(ItemStack stack) {
        return stack.getItem() instanceof MagicLetterItem || stack.getItem() instanceof LetterBinderItem;
    }

    /**
     * 注册所有手札与手札合订本为 Curio（行为注册，幂等）。
     * 必须在模组初始化阶段调用。
     */
    public static void registerOurItems(List<java.util.function.Supplier<? extends Item>> suppliers) {
        if (!LOADED) return;
        try {
            Class<?> iCurioItem = Class.forName("top.theillusivec4.curios.api.type.capability.ICurioItem");
            Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method registerMethod;
            try {
                registerMethod = curiosApi.getMethod("registerCurio", Item.class, iCurioItem);
            } catch (NoSuchMethodException e) {
                return;
            }

            Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                    CuriosIntegration.class.getClassLoader(),
                    new Class<?>[]{iCurioItem},
                    (obj, method, args) -> {
                        String name = method.getName();
                        Class<?> ret = method.getReturnType();
                        if ("curioTick".equals(name)) return null;
                        // 装备/卸下/手持装备判断：一律允许
                        if ("canEquip".equals(name) || "canUnequip".equals(name)) return true;
                        // 右键装备（canEquipFromUse）：玩家 shift（潜行）时返回 false，放行本模组绑定逻辑
                        if ("canEquipFromUse".equals(name)) {
                            if (args != null && args.length >= 1) {
                                Object slotCtx = args[0];
                                try {
                                    Method ent = slotCtx.getClass().getMethod("entity");
                                    Object e = ent.invoke(slotCtx);
                                    if (e instanceof net.minecraft.world.entity.player.Player p && p.isShiftKeyDown()) {
                                        return false;
                                    }
                                } catch (Exception ignored2) {
                                }
                            }
                            return true;
                        }
                        // 死亡掉落规则（与 Enigmatic Legacy cursed_ring 同方案，由 Curios 原生管理槽位）：
                        // - 魔法绑定物品：ALWAYS_KEEP（死亡时保留在原槽位，重生天然原位，无需手动装回）；
                        // - 强制消失开启时的消失诅咒物品：DESTROY（死亡时销毁）；
                        // - 其余：DEFAULT（原版规则）。
                        if ("getDropRule".equals(name)) {
                            try {
                                ItemStack stack = args != null && args.length >= 1
                                        ? (ItemStack) args[args.length - 1] : null;
                                if (stack != null && !stack.isEmpty()) {
                                    if (cn.autoforged.enchanter_letter.enchantment.ModEnchantments.hasEffectiveMagicBinding(stack)) {
                                        return dropRule("ALWAYS_KEEP");
                                    }
                                    if (cn.autoforged.enchanter_letter.config.ModConfig.getInstance().letterVanish.enabled
                                            && cn.autoforged.enchanter_letter.enchantment.ModEnchantments.hasVanishing(stack)) {
                                        return dropRule("DESTROY");
                                    }
                                }
                            } catch (Exception ignored3) {
                            }
                            return dropRule("DEFAULT");
                        }
                        // 属性修饰符必须返回 Guava Multimap（Curios 会强转），否则悬停物品时 ClassCastException 崩溃
                        if ("getAttributeModifiers".equals(name)) return emptyMultimap();
                        // 槽位/属性提示返回 List<Component>
                        if ("getSlotsTooltip".equals(name) || "getAttributesTooltip".equals(name)) return List.of();
                        if (ret == boolean.class) return true;
                        if (ret == int.class) return 0;
                        if (ret == void.class) return null;
                        return null;
                    });

            for (var supplier : suppliers) {
                registerMethod.invoke(null, supplier.get(), proxy);
            }
            // 手札合订本也注册为 Curio
            registerMethod.invoke(null, ModItems.LETTER_BINDER.get(), proxy);
        } catch (Exception ignored) {
        }
    }

    /** 反射获取 Curios 的 DropRule 枚举值（ICurio$DropRule.ALWAYS_KEEP 等）。 */
    private static Object dropRule(String name) {
        try {
            Class<?> dropRule = Class.forName("top.theillusivec4.curios.api.type.capability.ICurio$DropRule");
            return Enum.valueOf((Class) dropRule, name);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 遍历 Curios 的所有槽位表（全局 / 玩家 / 实体，服务端与客户端）并把“允许我们的手札/合订本”
     * 的谓词注入每个槽位的 validators。幂等，可在任意时机调用以覆盖槽位重载。
     */
    public static void ensureAllSlotCompat() {
        if (!LOADED || !slotCompatUsable) return;
        try {
            // 1) 先注册允许谓词（Curios 内部 putIfAbsent，幂等）
            if (!predicateRegistered) {
                registerPredicateMethod.invoke(null, ALL_LETTERS_PREDICATE, (Predicate<Object>) result -> {
                    try {
                        ItemStack stack = (ItemStack) slotResultStackMethod.invoke(result);
                        return isOurAccessoryItem(stack);
                    } catch (Exception e) {
                        return false;
                    }
                });
                predicateRegistered = true;
            }

            // 1.5) 改写全局 "curios:tag" 谓词（越过第三方槽位的标签限制，如 Terra Curio）
            wrapTagPredicate();

            // 2) 收集所有槽位表并注入
            List<Map<?, ?>> allMaps = new ArrayList<>();
            for (boolean side : new boolean[]{false, true}) {
                addMap(allMaps, getSlotsAllMethod, new Object[]{side});
                addMap(allMaps, getPlayerSlotsSideMethod, new Object[]{side});
                addMap(allMaps, getEntitySlotsSideMethod, new Object[]{EntityType.PLAYER, side});
            }
            addMap(allMaps, getSlotsNoArgMethod, new Object[0]);
            addMap(allMaps, getPlayerSlotsNoArgMethod, new Object[0]);
            addMap(allMaps, getEntitySlotsNoArgMethod, new Object[]{EntityType.PLAYER});

            // 3) ISlotHelper 兜底：枚举所有槽位 id
            if (getSlotHelperMethod != null && getSlotTypeIdsMethod != null && getSlotTypeMethod != null) {
                try {
                    Object helper = getSlotHelperMethod.invoke(null);
                    Set<?> ids = (Set<?>) getSlotTypeIdsMethod.invoke(helper);
                    Map<Object, Object> m = new HashMap<>();
                    for (Object id : ids) {
                        Object st = getSlotTypeMethod.invoke(helper, id);
                        if (st instanceof Optional<?> opt) {
                            st = opt.isPresent() ? opt.get() : null;
                        }
                        if (st != null) m.put(id, st);
                    }
                    allMaps.add(m);
                } catch (Exception ignored) {}
            }

            for (Map<?, ?> slots : allMaps) {
                if (slots == null) continue;
                for (Object slotType : slots.values()) {
                    addPredicateToSlot(slotType);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void addMap(List<Map<?, ?>> out, Method method, Object[] args) {
        if (method == null) return;
        try {
            Object result = method.invoke(null, args);
            if (result instanceof Map<?, ?>) {
                out.add((Map<?, ?>) result);
            }
        } catch (Exception ignored) {
        }
    }

    private static void addPredicateToSlot(Object slotType) {
        if (slotType == null) return;
        try {
            Set<ResourceLocation> validators = (Set<ResourceLocation>) getValidatorsMethod.invoke(slotType);
            if (validators == null) return;
            try {
                validators.add(ALL_LETTERS_PREDICATE);
                return;
            } catch (UnsupportedOperationException ignored) {
                // 槽位 json 没有显式声明 validators 时，Curios 的 SlotType.Builder 使用
                // 不可变的 Set.of("curios:tag")（Terra Curio 的 accessory 槽就是这种），
                // 无法 add，改为整体替换 validators 字段。
            }
        } catch (Exception ignored) {
            return;
        }
        replaceValidatorsField(slotType);
    }

    /**
     * 当槽位的 validators 集合不可变时，直接替换 SlotType 实例的 validators 字段：
     * 保留原有校验器并追加我们的允许谓词。
     */
    private static void replaceValidatorsField(Object slotType) {
        Class<?> clazz = slotType.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                Field field = clazz.getDeclaredField("validators");
                field.setAccessible(true);
                Set<ResourceLocation> merged = new HashSet<>();
                Object current = field.get(slotType);
                if (current instanceof Set<?> existing) {
                    for (Object o : existing) {
                        if (o instanceof ResourceLocation rl) merged.add(rl);
                    }
                }
                merged.add(ALL_LETTERS_PREDICATE);
                field.set(slotType, merged);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                return;
            }
        }
    }

    /**
     * 越过第三方槽位的标签限制：Curios 内置的 "curios:tag" 谓词要求物品位于
     * #curios:&lt;槽位名&gt; 或 #curios:curio 标签中。Terra Curio 为它的 accessory 槽
     * 提供了只包含自己物品的 #curios:accessory 标签，其他模组的物品（包括我们的
     * 手札）都会被该谓词拒绝。这里用反射拿到 Curios 内部的全局谓词表，用
     * “我们的物品一律放行，其余物品保持原逻辑”的包装谓词替换 "curios:tag"。
     * 幂等，且不改变任何非本模组物品的行为。
     */
    private static void wrapTagPredicate() {
        if (tagPredicateWrapped) return;
        try {
            Class<?> hooks = Class.forName("top.theillusivec4.curios.mixin.CuriosImplMixinHooks");
            Field mapField = hooks.getDeclaredField("SLOT_RESULT_PREDICATES");
            mapField.setAccessible(true);
            Object mapObj = mapField.get(null);
            if (!(mapObj instanceof Map<?, ?> map)) return;
            ResourceLocation tagId = ResourceLocation.fromNamespaceAndPath("curios", "tag");
            Object original = map.get(tagId);
            if (!(original instanceof Predicate<?> originalPredicate)) return;
            Predicate<Object> wrapped = result -> {
                try {
                    ItemStack stack = (ItemStack) slotResultStackMethod.invoke(result);
                    if (isOurAccessoryItem(stack)) return true;
                } catch (Exception ignored) {
                }
                try {
                    return ((Predicate<Object>) originalPredicate).test(result);
                } catch (Exception e) {
                    return false;
                }
            };
            ((Map<Object, Object>) mapObj).put(tagId, wrapped);
            tagPredicateWrapped = true;
        } catch (Exception ignored) {
        }
    }

    /** 惰性构造一个空的 Guava Multimap（Curios 对 getAttributeModifiers 的返回类型强转所需）。 */
    private static Object emptyMultimapInstance;
    private static synchronized Object emptyMultimap() {
        if (emptyMultimapInstance == null) {
            try {
                Class<?> hm = Class.forName("com.google.common.collect.HashMultimap");
                emptyMultimapInstance = hm.getMethod("create").invoke(null);
            } catch (Exception e) {
                emptyMultimapInstance = List.of();
            }
        }
        return emptyMultimapInstance;
    }

    public static List<ItemStack> getCuriosStacks(net.minecraft.world.entity.LivingEntity player) {
        if (!LOADED) return List.of();
        List<ItemStack> result = new ArrayList<>();
        try {
            Optional<?> optional = (Optional<?>) getInventoryMethod.invoke(null, player);
            if (optional.isEmpty()) return List.of();
            Object handler = optional.get();

            if (getCuriosMethod == null) {
                getCuriosMethod = findMethod(handler.getClass(), "getCurios");
            }
            if (getCuriosMethod == null) return List.of();

            Object curios = getCuriosMethod.invoke(handler);

            if (curios instanceof java.util.Map<?, ?> map) {
                for (Object entry : map.values()) {
                    addStacksFromHandler(entry, result);
                }
            } else {
                addStacksFromHandler(curios, result);
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private static void addStacksFromHandler(Object handlerObj, List<ItemStack> result) throws Exception {
        Method getStacksMethod = findMethod(handlerObj.getClass(), "getStacks");
        if (getStacksMethod == null) return;

        Object dynamicHandler = getStacksMethod.invoke(handlerObj);
        if (dynamicHandler == null) return;

        if (getSlotsMethod == null) {
            getSlotsMethod = findMethod(dynamicHandler.getClass(), "getSlots");
        }
        if (getSlotsMethod == null) return;

        if (getStackInSlotMethod == null) {
            getStackInSlotMethod = findMethod(dynamicHandler.getClass(), "getStackInSlot");
        }
        if (getStackInSlotMethod == null) return;

        int slots = (int) getSlotsMethod.invoke(dynamicHandler);
        for (int i = 0; i < slots; i++) {
            ItemStack stack = (ItemStack) getStackInSlotMethod.invoke(dynamicHandler, i);
            if (!stack.isEmpty()) {
                result.add(stack);
            }
        }
    }

    private static Method findMethod(Class<?> clazz, String name) {
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(name)) return m;
        }
        return null;
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * 把玩家某个 Curios 槽位的物品放回原槽位（用于魔法绑定物品死亡后重生还原位置）。
     * @return true 表示已成功放回原槽位；false 表示找不到原槽位（调用方应回退到背包）。
     */
    public static boolean restoreStack(Player player, String slotType, int slotIndex, ItemStack stack) {
        if (!LOADED) return false;
        try {
            Optional<?> optional = (Optional<?>) getInventoryMethod.invoke(null, player);
            if (optional.isEmpty()) return false;
            Object handler = optional.get();
            // 首选官方 API：ICuriosItemHandler.setEquippedCurio(slotType, slot, stack)
            // （官方实现自带槽位类型/索引校验，跨版本最通用；无效槽位时静默 no-op，需读回验证）
            boolean setOk = false;
            Method setEquippedM = findMethod(handler.getClass(), "setEquippedCurio", String.class, int.class, ItemStack.class);
            if (setEquippedM != null) {
                try {
                    setEquippedM.invoke(handler, slotType, slotIndex, stack.copy());
                    setOk = true;
                } catch (Throwable ignored) {
                }
            }
            if (!setOk) {
                // 旧反射链兜底（与 setEquippedCurio 相同逻辑：map.get(slotType) -> getStacks() -> setStackInSlot）
                if (getCuriosMethod == null) getCuriosMethod = findMethod(handler.getClass(), "getCurios");
                if (getCuriosMethod == null) return false;
                Object curios = getCuriosMethod.invoke(handler);
                Object stacksHandler = null;
                if (curios instanceof Map<?, ?> map) {
                    stacksHandler = map.get(slotType);
                }
                if (stacksHandler == null) return false;
                Method getStacksMethod = findMethod(stacksHandler.getClass(), "getStacks");
                if (getStacksMethod == null) return false;
                Object dynamicHandler = getStacksMethod.invoke(stacksHandler);
                if (dynamicHandler == null) return false;
                Method setStackInSlotM = findMethod(dynamicHandler.getClass(), "setStackInSlot", int.class, ItemStack.class);
                if (setStackInSlotM == null) return false;
                try {
                    setStackInSlotM.invoke(dynamicHandler, slotIndex, stack);
                    setOk = true;
                } catch (Exception ignored) {
                }
            }
            if (!setOk) return false;
            // 读回验证：确认物品确实装上了该槽位（防止静默失败导致物品丢失后无从回退）
            return verifySlotHasStack(handler, slotType, slotIndex);
        } catch (Exception ignored) {
        }
        return false;
    }

    /** 读回指定 Curios 槽位的物品（用于装回后验证）。 */
    private static ItemStack getStackFromSlot(Object handler, String slotType, int slotIndex) {
        try {
            if (getCuriosMethod == null) getCuriosMethod = findMethod(handler.getClass(), "getCurios");
            if (getCuriosMethod == null) return null;
            Object curios = getCuriosMethod.invoke(handler);
            if (!(curios instanceof Map<?, ?> map)) return null;
            Object stacksHandler = map.get(slotType);
            if (stacksHandler == null) return null;
            Method getStacksMethod = findMethod(stacksHandler.getClass(), "getStacks");
            if (getStacksMethod == null) return null;
            Object dynamicHandler = getStacksMethod.invoke(stacksHandler);
            if (dynamicHandler == null) return null;
            Method getStackM = findMethod(dynamicHandler.getClass(), "getStackInSlot");
            if (getStackM == null) return null;
            return (ItemStack) getStackM.invoke(dynamicHandler, slotIndex);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean verifySlotHasStack(Object handler, String slotType, int slotIndex) {
        ItemStack current = getStackFromSlot(handler, slotType, slotIndex);
        return current != null && !current.isEmpty();
    }

    /**
     * 玩家指定 Curios 槽位当前是否仍装着期望的物品（用于魔法绑定重生校验：
     * Curios DropRule(ALWAYS_KEEP) 原生保留时槽位已有同物品则无需手动装回）。
     */
    public static boolean slotContains(Player player, String slotType, int slotIndex, ItemStack expected) {
        Object handler = resolveCuriosHandler(player);
        if (handler == null) return false;
        ItemStack current = getStackFromSlot(handler, slotType, slotIndex);
        return current != null && !current.isEmpty() && ItemStack.isSameItemSameComponents(current, expected);
    }

    /** 解析玩家的 Curios handler（能力未就绪返回 null）。 */
    private static Object resolveCuriosHandler(Player player) {
        try {
            Optional<?> optional = (Optional<?>) getInventoryMethod.invoke(null, player);
            if (optional.isEmpty()) return null;
            return optional.get();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 死亡事件后、掉落机制前：纯读取玩家所有 Curios 槽位的完整快照（槽位类型/索引/物品副本），
     * 不删除任何物品。用于暂存死亡瞬间饰品栏全部数据，重生时按快照把魔法绑定物品装回原槽位。
     */
    public static List<CurioSocket> snapshotAllSlots(Player player) {
        List<CurioSocket> out = new ArrayList<>();
        if (!LOADED) return out;
        try {
            Optional<?> optional = (Optional<?>) getInventoryMethod.invoke(null, player);
            if (optional.isEmpty()) return out;
            Object handler = optional.get();
            if (getCuriosMethod == null) getCuriosMethod = findMethod(handler.getClass(), "getCurios");
            if (getCuriosMethod == null) return out;
            Object curios = getCuriosMethod.invoke(handler);
            if (curios instanceof Map<?, ?> map) {
                for (Object entry : map.entrySet()) {
                    String slotType = String.valueOf(((Map.Entry<?, ?>) entry).getKey());
                    Object stacksHandler = ((Map.Entry<?, ?>) entry).getValue();
                    if (stacksHandler == null) continue;
                    Method getStacksMethod = findMethod(stacksHandler.getClass(), "getStacks");
                    if (getStacksMethod == null) continue;
                    Object dynamicHandler = getStacksMethod.invoke(stacksHandler);
                    if (dynamicHandler == null) continue;
                    Method getSlotsM = findMethod(dynamicHandler.getClass(), "getSlots");
                    Method getStackM = findMethod(dynamicHandler.getClass(), "getStackInSlot");
                    if (getSlotsM == null || getStackM == null) continue;
                    int slots = (int) getSlotsM.invoke(dynamicHandler);
                    for (int i = 0; i < slots; i++) {
                        ItemStack stack = (ItemStack) getStackM.invoke(dynamicHandler, i);
                        if (stack == null || stack.isEmpty()) continue;
                        out.add(new CurioSocket(slotType, i, stack.copy()));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    /**
     * 从玩家指定 Curios 槽位取出物品（防止其进入死亡掉落列表）。
     * @return 取出的物品；取出失败（槽位不存在/为空/拒绝取出）返回 EMPTY。
     */
    public static ItemStack removeStackFromSlot(Player player, String slotType, int slotIndex) {
        if (!LOADED) return ItemStack.EMPTY;
        try {
            Optional<?> optional = (Optional<?>) getInventoryMethod.invoke(null, player);
            if (optional.isEmpty()) return ItemStack.EMPTY;
            Object handler = optional.get();
            if (getCuriosMethod == null) getCuriosMethod = findMethod(handler.getClass(), "getCurios");
            if (getCuriosMethod == null) return ItemStack.EMPTY;
            Object curios = getCuriosMethod.invoke(handler);
            if (!(curios instanceof Map<?, ?> map)) return ItemStack.EMPTY;
            Object stacksHandler = map.get(slotType);
            if (stacksHandler == null) return ItemStack.EMPTY;
            Method getStacksMethod = findMethod(stacksHandler.getClass(), "getStacks");
            if (getStacksMethod == null) return ItemStack.EMPTY;
            Object dynamicHandler = getStacksMethod.invoke(stacksHandler);
            if (dynamicHandler == null) return ItemStack.EMPTY;
            Method getStackM = findMethod(dynamicHandler.getClass(), "getStackInSlot");
            if (getStackM == null) return ItemStack.EMPTY;
            ItemStack current = (ItemStack) getStackM.invoke(dynamicHandler, slotIndex);
            if (current == null || current.isEmpty()) return ItemStack.EMPTY;
            ItemStack copy = current.copy();
            Method extractM = findMethod(dynamicHandler.getClass(), "extractItem", int.class, int.class, boolean.class);
            if (extractM != null) {
                try {
                    Object out = extractM.invoke(dynamicHandler, slotIndex, copy.getCount(), false);
                    if (out instanceof ItemStack rs && !rs.isEmpty()) return rs;
                } catch (Exception ignored) {
                }
            }
            Method setStackM = findMethod(dynamicHandler.getClass(), "setStackInSlot", int.class, ItemStack.class);
            if (setStackM != null) {
                try {
                    setStackM.invoke(dynamicHandler, slotIndex, ItemStack.EMPTY);
                    return copy;
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return ItemStack.EMPTY;
    }

    /**
     * 遍历玩家所有 Curios 槽位：移除满足条件的物品，并把（槽位类型、槽位索引、物品）交给 onEjected 回调。
     * 用于魔法绑定物品死亡后记录原位置以重生还原。全部反射调用。
     */
    /** 单个 Curios 槽位信息（槽位类型 + 槽位索引 + 物品）。 */
    public static class CurioSocket {
        public final String slotType;
        public final int slot;
        public final ItemStack stack;

        public CurioSocket(String slotType, int slot, ItemStack stack) {
            this.slotType = slotType;
            this.slot = slot;
            this.stack = stack;
        }
    }

    public static void ejectStacksWhereWithSlot(Player player, Predicate<ItemStack> predicate,
                                                java.util.function.Consumer<CurioSocket> onEjected) {
        if (!LOADED) return;
        try {
            Optional<?> optional = (Optional<?>) getInventoryMethod.invoke(null, player);
            if (optional.isEmpty()) return;
            Object handler = optional.get();
            if (getCuriosMethod == null) getCuriosMethod = findMethod(handler.getClass(), "getCurios");
            if (getCuriosMethod == null) return;
            Object curios = getCuriosMethod.invoke(handler);
            if (curios instanceof Map<?, ?> map) {
                for (Object entry : map.entrySet()) {
                    String slotType = String.valueOf(((Map.Entry<?, ?>) entry).getKey());
                    Object stacksHandler = ((Map.Entry<?, ?>) entry).getValue();
                    if (stacksHandler == null) continue;
                    Method getStacksMethod = findMethod(stacksHandler.getClass(), "getStacks");
                    if (getStacksMethod == null) continue;
                    Object dynamicHandler = getStacksMethod.invoke(stacksHandler);
                    if (dynamicHandler == null) continue;
                    Method getSlotsM = findMethod(dynamicHandler.getClass(), "getSlots");
                    Method getStackM = findMethod(dynamicHandler.getClass(), "getStackInSlot");
                    if (getSlotsM == null || getStackM == null) continue;
                    int slots = (int) getSlotsM.invoke(dynamicHandler);
                    for (int i = 0; i < slots; i++) {
                        ItemStack stack = (ItemStack) getStackM.invoke(dynamicHandler, i);
                        if (stack == null || stack.isEmpty() || !predicate.test(stack)) continue;
                        ItemStack copy = stack.copy();
                        boolean removed = false;
                        Method extractM = findMethod(dynamicHandler.getClass(), "extractItem", int.class, int.class, boolean.class);
                        if (extractM != null) {
                            try {
                                Object out = extractM.invoke(dynamicHandler, i, copy.getCount(), false);
                                if (out instanceof ItemStack rs && !rs.isEmpty()) { onEjected.accept(new CurioSocket(slotType, i, rs)); removed = true; }
                            } catch (Exception ignored) {}
                        }
                        if (!removed) {
                            Method setStackM = findMethod(dynamicHandler.getClass(), "setStackInSlot", int.class, ItemStack.class);
                            if (setStackM != null) {
                                try { setStackM.invoke(dynamicHandler, i, ItemStack.EMPTY); onEjected.accept(new CurioSocket(slotType, i, copy)); } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 遍历玩家所有 Curios 槽位：移除满足条件的物品，并把被移除的物品交给 onEjected 回调
     * （用于把不属于当前玩家的绑定物品从饰品槽弹出）。全部反射调用。
     */
    public static void ejectStacksWhere(Player player, Predicate<ItemStack> predicate, Consumer<ItemStack> onEjected) {
        if (!LOADED) return;
        try {
            Optional<?> optional = (Optional<?>) getInventoryMethod.invoke(null, player);
            if (optional.isEmpty()) return;
            Object handler = optional.get();
            if (getCuriosMethod == null) {
                getCuriosMethod = findMethod(handler.getClass(), "getCurios");
            }
            if (getCuriosMethod == null) return;
            Object curios = getCuriosMethod.invoke(handler);
            List<Object> stackHandlers = new ArrayList<>();
            if (curios instanceof Map<?, ?> map) {
                for (Object v : map.values()) stackHandlers.add(v);
            } else if (curios != null) {
                stackHandlers.add(curios);
            }
            for (Object stacksHandler : stackHandlers) {
                Method getStacksMethod = findMethod(stacksHandler.getClass(), "getStacks");
                if (getStacksMethod == null) continue;
                Object dynamicHandler = getStacksMethod.invoke(stacksHandler);
                if (dynamicHandler == null) continue;
                Method getSlotsM = findMethod(dynamicHandler.getClass(), "getSlots");
                Method getStackM = findMethod(dynamicHandler.getClass(), "getStackInSlot");
                if (getSlotsM == null || getStackM == null) continue;
                int slots = (int) getSlotsM.invoke(dynamicHandler);
                for (int i = 0; i < slots; i++) {
                    ItemStack stack = (ItemStack) getStackM.invoke(dynamicHandler, i);
                    if (stack == null || stack.isEmpty() || !predicate.test(stack)) continue;
                    ItemStack copy = stack.copy();
                    boolean removed = false;
                    Method extractM = findMethod(dynamicHandler.getClass(), "extractItem", int.class, int.class, boolean.class);
                    if (extractM != null) {
                        try {
                            Object out = extractM.invoke(dynamicHandler, i, copy.getCount(), false);
                            if (out instanceof ItemStack rs && !rs.isEmpty()) {
                                onEjected.accept(rs);
                                removed = true;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    if (!removed) {
                        Method setStackM = findMethod(dynamicHandler.getClass(), "setStackInSlot", int.class, ItemStack.class);
                        if (setStackM != null) {
                            try {
                                setStackM.invoke(dynamicHandler, i, ItemStack.EMPTY);
                                onEjected.accept(copy);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
