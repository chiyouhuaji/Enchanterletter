package cn.autoforged.enchanter_letter.integration;

import cn.autoforged.enchanter_letter.config.ModConfig;
import cn.autoforged.enchanter_letter.enchantment.ModEnchantments;
import cn.autoforged.enchanter_letter.item.LetterBinderItem;
import cn.autoforged.enchanter_letter.item.MagicLetterItem;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Accessories（饰品，Fabric）兼容 —— 可选，非前置，全部通过反射调用。
 * 枚举所有 Accessories 槽位（含其他模组注册的槽位），通过槽位 validator 谓词
 * 把模组所有手札与手札合订本允许放入任意栏位，并提供饰品槽位物品扫描。
 */
public class AccessoriesIntegration {
    private static final boolean LOADED;
    private static Method registerPredicateMethod;
    private static Method getSlotTypesMethod;
    private static Method validatorsMethod;
    private static Method capabilityGetMethod;
    private static Method getAllEquippedMethod;
    private static Method slotEntryStackMethod;
    private static Method slotEntryReferenceMethod;
    private static Method slotRefGetStackMethod;
    private static Method slotRefSetStackMethod;
    private static Method slotRefSlotNameMethod;
    private static Method slotRefSlotMethod;
    private static Method rendererRegistryRegisterMethod;
    private static Method rendererRegistryReloadMethod;
    private static boolean rendererAvailable = false;
    private static Method registerAccessoryMethod;
    private static boolean noEquipFromUseRegistered = false;
    private static Class<?> dropRuleClass;
    private static Class<?> onDropCallbackClass;
    private static Object onDropEvent;
    private static Method onDropEventRegisterMethod;
    private static boolean deathDropRulesRegistered = false;
    private static final ResourceLocation ALL_LETTERS_PREDICATE =
            ResourceLocation.fromNamespaceAndPath("enchanter_letter", "all_letters");
    private static boolean predicateRegistered = false;

    static {
        boolean flag = false;
        try {
            Class<?> api = Class.forName("io.wispforest.accessories.api.AccessoriesAPI");
            registerPredicateMethod = api.getMethod("registerPredicate", ResourceLocation.class,
                    Class.forName("io.wispforest.accessories.api.slot.SlotBasedPredicate"));
            Class<?> loader = Class.forName("io.wispforest.accessories.data.SlotTypeLoader");
            getSlotTypesMethod = loader.getMethod("getSlotTypes", Level.class);
            Class<?> slotType = Class.forName("io.wispforest.accessories.api.slot.SlotType");
            validatorsMethod = slotType.getMethod("validators");
            Class<?> capability = Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
            capabilityGetMethod = capability.getMethod("get", net.minecraft.world.entity.LivingEntity.class);
            getAllEquippedMethod = capability.getMethod("getAllEquipped");
            Class<?> entryRef = Class.forName("io.wispforest.accessories.api.slot.SlotEntryReference");
            slotEntryStackMethod = entryRef.getMethod("stack");
            slotEntryReferenceMethod = entryRef.getMethod("reference");
            Class<?> slotRef = Class.forName("io.wispforest.accessories.api.slot.SlotReference");
            slotRefGetStackMethod = slotRef.getMethod("getStack");
            slotRefSetStackMethod = slotRef.getMethod("setStack", ItemStack.class);
            try { slotRefSlotNameMethod = slotRef.getMethod("slotName"); } catch (Exception ignored) {}
            try { slotRefSlotMethod = slotRef.getMethod("slot"); } catch (Exception ignored) {}
            try {
                registerAccessoryMethod = api.getMethod("registerAccessory",
                        net.minecraft.world.item.Item.class,
                        Class.forName("io.wispforest.accessories.api.Accessory"));
            } catch (Exception ignored) {
            }
            try {
                dropRuleClass = Class.forName("io.wispforest.accessories.api.DropRule");
                onDropCallbackClass = Class.forName("io.wispforest.accessories.api.events.OnDropCallback");
                java.lang.reflect.Field eventField = onDropCallbackClass.getField("EVENT");
                onDropEvent = eventField.get(null);
                onDropEventRegisterMethod = onDropEvent.getClass().getMethod("register", Object.class);
            } catch (Exception ignored) {
            }
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
     * 枚举所有 Accessories 槽位，把“允许我们的手札/合订本”的谓词加入每个槽位的 validators。
     * 幂等，可在服务端/客户端周期调用以覆盖槽位重载。
     */
    public static void ensureAllSlotCompat(Level level) {
        if (!LOADED || level == null) return;
        try {
            if (!predicateRegistered) {
                Class<?> slotBasedPredicate = Class.forName("io.wispforest.accessories.api.slot.SlotBasedPredicate");
                Object predicate = Proxy.newProxyInstance(
                        AccessoriesIntegration.class.getClassLoader(),
                        new Class<?>[]{slotBasedPredicate},
                        (obj, method, args) -> {
                            if ("isValid".equals(method.getName())) {
                                ItemStack stack = (ItemStack) args[3];
                                return isOurAccessoryItem(stack) ? TriState.TRUE : TriState.DEFAULT;
                            }
                            if ("toString".equals(method.getName())) return "EnchanterLetterSlotPredicate";
                            if ("hashCode".equals(method.getName())) return System.identityHashCode(obj);
                            if ("equals".equals(method.getName())) return args != null && args.length > 0 && obj == args[0];
                            return invokeDefaultMethod(obj, method, args);
                        });
                registerPredicateMethod.invoke(null, ALL_LETTERS_PREDICATE, predicate);
                predicateRegistered = true;
            }
            Map<?, ?> slots = (Map<?, ?>) getSlotTypesMethod.invoke(null, level);
            if (slots == null) return;
            for (Object slotType : slots.values()) {
                Set<ResourceLocation> validators = (Set<ResourceLocation>) validatorsMethod.invoke(slotType);
                if (validators != null) {
                    validators.add(ALL_LETTERS_PREDICATE);
                }
            }
        } catch (Exception ignored) {
        }
    }

    /** 实体 Accessories 槽位中已装备的物品（玩家/生物共用）。 */
    public static List<ItemStack> getAccessoriesStacks(LivingEntity player) {
        if (!LOADED) return List.of();
        List<ItemStack> result = new ArrayList<>();
        try {
            Object capability = capabilityGetMethod.invoke(null, player);
            if (capability == null) return result;
            Object equipped = getAllEquippedMethod.invoke(capability);
            if (equipped instanceof Iterable<?> iterable) {
                for (Object entry : iterable) {
                    ItemStack stack = (ItemStack) slotEntryStackMethod.invoke(entry);
                    if (stack != null && !stack.isEmpty()) {
                        result.add(stack);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    /**
     * 遍历玩家所有 Accessories 装备槽位：移除满足条件的物品，并把被移除物品交给 onEjected 回调
     * （用于把不属于当前玩家的绑定物品从饰品槽弹出）。
     */
    public static void ejectStacksWhere(Player player, java.util.function.Predicate<ItemStack> predicate,
                                        java.util.function.Consumer<ItemStack> onEjected) {
        if (!LOADED || slotEntryReferenceMethod == null || slotRefGetStackMethod == null || slotRefSetStackMethod == null) return;
        try {
            Object capability = capabilityGetMethod.invoke(null, player);
            if (capability == null) return;
            Object equipped = getAllEquippedMethod.invoke(capability);
            if (equipped instanceof Iterable<?> iterable) {
                for (Object entry : iterable) {
                    Object reference = slotEntryReferenceMethod.invoke(entry);
                    ItemStack stack = (ItemStack) slotRefGetStackMethod.invoke(reference);
                    if (stack == null || stack.isEmpty() || !predicate.test(stack)) continue;
                    ItemStack copy = stack.copy();
                    slotRefSetStackMethod.invoke(reference, ItemStack.EMPTY);
                    onEjected.accept(copy);
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 遍历玩家所有 Accessories 装备槽位：对满足条件的物品就地执行 mutator 并写回槽位
     * （用于把合订本内容物中的他人绑定物品弹出）。
     */
    public static void mutateStacksWhere(Player player, java.util.function.Predicate<ItemStack> predicate,
                                         java.util.function.Consumer<ItemStack> mutator) {
        if (!LOADED || slotEntryReferenceMethod == null || slotRefGetStackMethod == null || slotRefSetStackMethod == null) return;
        try {
            Object capability = capabilityGetMethod.invoke(null, player);
            if (capability == null) return;
            Object equipped = getAllEquippedMethod.invoke(capability);
            if (equipped instanceof Iterable<?> iterable) {
                for (Object entry : iterable) {
                    Object reference = slotEntryReferenceMethod.invoke(entry);
                    ItemStack stack = (ItemStack) slotRefGetStackMethod.invoke(reference);
                    if (stack == null || stack.isEmpty() || !predicate.test(stack)) continue;
                    mutator.accept(stack);
                    slotRefSetStackMethod.invoke(reference, stack);
                }
            }
        } catch (Exception ignored) {
        }
    }

    /** 单个 Accessories 槽位信息（槽位类型 + 槽位索引 + 物品）。 */
    public static class AccessoriesSocket {
        public final String slotType;
        public final int slot;
        public final ItemStack stack;

        public AccessoriesSocket(String slotType, int slot, ItemStack stack) {
            this.slotType = slotType;
            this.slot = slot;
            this.stack = stack;
        }
    }

    /** 把玩家某个 Accessories 槽位的物品放回原槽位（用于魔法绑定物品死亡后重生还原位置）。 */
    public static boolean restoreStack(Player player, String slotType, int slotIndex, ItemStack stack) {
        if (!LOADED || slotRefSlotNameMethod == null || slotRefSlotMethod == null || slotRefSetStackMethod == null) return false;
        try {
            Object capability = capabilityGetMethod.invoke(null, player);
            if (capability == null) return false;
            Object equipped = getAllEquippedMethod.invoke(capability);
            if (equipped instanceof Iterable<?> iterable) {
                for (Object entry : iterable) {
                    Object reference = slotEntryReferenceMethod.invoke(entry);
                    String name = String.valueOf(slotRefSlotNameMethod.invoke(reference));
                    int slot = (int) slotRefSlotMethod.invoke(reference);
                    if (name.equals(slotType) && slot == slotIndex) {
                        slotRefSetStackMethod.invoke(reference, stack);
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /** 移除满足条件的 Accessories 物品，把（槽位类型、槽位索引、物品）交给 onEjected 回调。 */
    public static void ejectStacksWhereWithSlot(Player player, java.util.function.Predicate<ItemStack> predicate,
                                                java.util.function.Consumer<AccessoriesSocket> onEjected) {
        if (!LOADED || slotEntryReferenceMethod == null || slotRefGetStackMethod == null
                || slotRefSetStackMethod == null || slotRefSlotNameMethod == null || slotRefSlotMethod == null) return;
        try {
            Object capability = capabilityGetMethod.invoke(null, player);
            if (capability == null) return;
            Object equipped = getAllEquippedMethod.invoke(capability);
            if (equipped instanceof Iterable<?> iterable) {
                for (Object entry : iterable) {
                    Object reference = slotEntryReferenceMethod.invoke(entry);
                    ItemStack stack = (ItemStack) slotRefGetStackMethod.invoke(reference);
                    if (stack == null || stack.isEmpty() || !predicate.test(stack)) continue;
                    ItemStack copy = stack.copy();
                    String name = String.valueOf(slotRefSlotNameMethod.invoke(reference));
                    int slot = (int) slotRefSlotMethod.invoke(reference);
                    slotRefSetStackMethod.invoke(reference, ItemStack.EMPTY);
                    onEjected.accept(new AccessoriesSocket(name, slot, copy));
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 死亡事件后、掉落机制前：纯读取玩家所有 Accessories 槽位的完整快照（槽位类型/索引/物品副本），
     * 不删除任何物品。用于暂存死亡瞬间饰品栏全部数据，重生时按快照把魔法绑定物品装回原槽位。
     */
    public static List<AccessoriesSocket> snapshotAllSlots(Player player) {
        List<AccessoriesSocket> out = new ArrayList<>();
        if (!LOADED || slotEntryReferenceMethod == null || slotRefGetStackMethod == null
                || slotRefSlotNameMethod == null || slotRefSlotMethod == null) return out;
        try {
            Object capability = capabilityGetMethod.invoke(null, player);
            if (capability == null) return out;
            Object equipped = getAllEquippedMethod.invoke(capability);
            if (equipped instanceof Iterable<?> iterable) {
                for (Object entry : iterable) {
                    Object reference = slotEntryReferenceMethod.invoke(entry);
                    ItemStack stack = (ItemStack) slotRefGetStackMethod.invoke(reference);
                    if (stack == null || stack.isEmpty()) continue;
                    String name = String.valueOf(slotRefSlotNameMethod.invoke(reference));
                    int slot = (int) slotRefSlotMethod.invoke(reference);
                    out.add(new AccessoriesSocket(name, slot, stack.copy()));
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    /**
     * 从玩家指定 Accessories 槽位取出物品（防止其进入死亡掉落列表）。
     * @return 取出的物品；取出失败（槽位不存在/为空）返回 EMPTY。
     */
    public static ItemStack removeStackFromSlot(Player player, String slotType, int slotIndex) {
        if (!LOADED || slotEntryReferenceMethod == null || slotRefGetStackMethod == null
                || slotRefSetStackMethod == null || slotRefSlotNameMethod == null || slotRefSlotMethod == null) {
            return ItemStack.EMPTY;
        }
        try {
            Object capability = capabilityGetMethod.invoke(null, player);
            if (capability == null) return ItemStack.EMPTY;
            Object equipped = getAllEquippedMethod.invoke(capability);
            if (equipped instanceof Iterable<?> iterable) {
                for (Object entry : iterable) {
                    Object reference = slotEntryReferenceMethod.invoke(entry);
                    String name = String.valueOf(slotRefSlotNameMethod.invoke(reference));
                    int slot = (int) slotRefSlotMethod.invoke(reference);
                    if (!name.equals(slotType) || slot != slotIndex) continue;
                    ItemStack stack = (ItemStack) slotRefGetStackMethod.invoke(reference);
                    if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
                    ItemStack copy = stack.copy();
                    slotRefSetStackMethod.invoke(reference, ItemStack.EMPTY);
                    return copy;
                }
            }
        } catch (Exception ignored) {
        }
        return ItemStack.EMPTY;
    }

    /**
     * 调用 Java 动态代理中接口 default 方法的默认实现。
     * 代理会把所有方法调用路由到 InvocationHandler（包括 default 方法），
     * 不处理的方法返回 null 会在 boolean 等原始类型拆箱处 NPE。
     */
    private static Object invokeDefaultMethod(Object proxy, Method method, Object[] args) {
        if (!method.isDefault()) {
            Class<?> rt = method.getReturnType();
            if (rt == boolean.class) return Boolean.FALSE;
            if (rt == int.class) return 0;
            if (rt == long.class) return 0L;
            if (rt == double.class) return 0.0d;
            if (rt == float.class) return 0.0f;
            return null;
        }
        try {
            Class<?> declaring = method.getDeclaringClass();
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(declaring, MethodHandles.lookup());
            return lookup.unreflectSpecial(method, declaring)
                    .bindTo(proxy)
                    .invokeWithArguments(args == null ? new Object[0] : args);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 客户端初始化：解析渲染器注册 API（Accessories 未安装时静默跳过）。
     * 1.20.1 修复：Accessories beta.48 的 AccessoriesRenderLayer 无条件织入 Sodium API，
     * 未安装 Sodium 时渲染任何已装备饰品（不限于本模组物品）都会 NoClassDefFoundError 崩溃。
     */
    public static void initClient() {
        if (!LOADED) return;
        try {
            Class<?> registry = Class.forName("io.wispforest.accessories.api.client.AccessoriesRendererRegistry");
            rendererRegistryRegisterMethod = registry.getMethod("registerRenderer",
                    net.minecraft.world.item.Item.class, java.util.function.Supplier.class);
            rendererRegistryReloadMethod = registry.getMethod("onReload");
            rendererAvailable = true;
        } catch (Throwable ignored) {
            rendererAvailable = false;
        }
    }

    /**
     * 为本模组所有手札/合订本注册 Accessory（Accessory.canEquipFromUse → false），
     * 阻止 Accessories 的 attemptEquipFromUse（UseItemCallback）拦截右键抢先装备，
     * 使手札的 shift+右键绑定（长按 2 秒）正常触发。与 Forge 端 Curios 的
     * canEquipFromUse=false 修复方式一致；饰品仍可通过饰品界面拖放装备。
     */
    public static void registerNoEquipFromUse() {
        if (!LOADED || registerAccessoryMethod == null || noEquipFromUseRegistered) return;
        try {
            Class<?> accessoryCls = Class.forName("io.wispforest.accessories.api.Accessory");
            Object accessory = Proxy.newProxyInstance(
                    AccessoriesIntegration.class.getClassLoader(),
                    new Class<?>[]{accessoryCls},
                    (obj, method2, args) -> {
                        String name = method2.getName();
                        if ("getDropRule".equals(name)) {
                            if (args != null && args.length >= 1 && args[0] instanceof ItemStack stack) {
                                if (ModEnchantments.hasEffectiveMagicBinding(stack)) return dropRuleConstant("KEEP");
                                if (ModConfig.getInstance().letterVanish.enabled && ModEnchantments.hasVanishing(stack)) {
                                    return dropRuleConstant("DESTROY");
                                }
                            }
                            return dropRuleConstant("DEFAULT");
                        }
                        if ("canEquipFromUse".equals(name)) return Boolean.FALSE;
                        if ("canEquip".equals(name) || "canUnequip".equals(name)) return Boolean.TRUE;
                        if ("toString".equals(name)) return "EnchanterLetterNoEquipFromUseAccessory";
                        if ("hashCode".equals(name)) return System.identityHashCode(obj);
                        if ("equals".equals(name)) return args != null && args.length > 0 && obj == args[0];
                        // Java 动态代理不会自动调用接口 default 实现：
                        // 不处理的方法必须显式调用 default 实现，否则返回 null 会在
                        // boolean 拆箱处 NPE（Accessories 进服校验 canEquip 时崩溃）。
                        return invokeDefaultMethod(obj, method2, args);
                    });
            for (java.util.function.Supplier<? extends net.minecraft.world.item.Item> itemSupplier
                    : cn.autoforged.enchanter_letter.item.ModItems.ALL_LETTERS) {
                registerAccessoryMethod.invoke(null, itemSupplier.get(), accessory);
            }
            noEquipFromUseRegistered = true;
            registerDeathDropRules();
        } catch (Throwable ignored) {
        }
    }

    /**
     * 注册 Accessories 全局死亡掉落规则：
     * - 魔法绑定物品返回 KEEP，饰品不会进入掉落列表，死亡后仍保留在原饰品槽位；
     * - 强制消失开启时，消失诅咒物品返回 DESTROY。
     * 通过 OnDropCallback 事件实现，可覆盖任意 Accessories 槽位中的物品（不限于本模组物品）。
     */
    public static void registerDeathDropRules() {
        if (!LOADED || onDropEvent == null || onDropEventRegisterMethod == null || deathDropRulesRegistered) return;
        try {
            Object listener = Proxy.newProxyInstance(
                    AccessoriesIntegration.class.getClassLoader(),
                    new Class<?>[]{onDropCallbackClass},
                    (obj, method, args) -> {
                        String name = method.getName();
                        if ("getAlternativeRule".equals(name)) {
                            if (args != null && args.length >= 2 && args[1] instanceof ItemStack stack) {
                                if (ModEnchantments.hasEffectiveMagicBinding(stack)) {
                                    return dropRuleConstant("KEEP");
                                }
                                if (ModConfig.getInstance().letterVanish.enabled && ModEnchantments.hasVanishing(stack)) {
                                    return dropRuleConstant("DESTROY");
                                }
                            }
                            return args != null && args.length >= 1 ? args[0] : dropRuleConstant("DEFAULT");
                        }
                        if ("toString".equals(name)) return "EnchanterLetterAccessoriesDeathDropRules";
                        if ("hashCode".equals(name)) return System.identityHashCode(obj);
                        if ("equals".equals(name)) return args != null && args.length > 0 && obj == args[0];
                        return invokeDefaultMethod(obj, method, args);
                    });
            onDropEventRegisterMethod.invoke(onDropEvent, listener);
            deathDropRulesRegistered = true;
        } catch (Throwable ignored) {
        }
    }

    private static Object dropRuleConstant(String name) {
        if (dropRuleClass == null) return null;
        for (Object constant : dropRuleClass.getEnumConstants()) {
            if (constant.toString().equals(name)) return constant;
        }
        return null;
    }

    /** Accessories 原生 DropRule 死亡处理是否已注册（注册成功后可不再手动摘除饰品）。 */
    public static boolean isDeathDropRuleActive() {
        return LOADED && deathDropRulesRegistered;
    }

    /**
     * 客户端：为本模组所有手札/合订本注册“不渲染”渲染器（shouldRender → false）。
     * AccessoriesRenderLayer 会跳过该物品的模型渲染，从而不会创建依赖 Sodium API 的
     * MPOATVConstructingVertexConsumer —— 未装 Sodium 也不再崩溃；饰品槽位的效果
     * （手札增益、掉落保护等）完全不受影响。
     */
    public static void registerNoRenderClient() {
        if (!rendererAvailable || rendererRegistryRegisterMethod == null) return;
        try {
            Class<?> rendererCls = Class.forName("io.wispforest.accessories.api.client.AccessoryRenderer");
            Object noOpRenderer = Proxy.newProxyInstance(
                    AccessoriesIntegration.class.getClassLoader(),
                    new Class<?>[]{rendererCls},
                    (obj, method, args) -> {
                        String name = method.getName();
                        if ("shouldRender".equals(name) || "shouldRenderInFirstPerson".equals(name)) {
                            return Boolean.FALSE;
                        }
                        if ("toString".equals(name)) return "EnchanterLetterNoOpAccessoryRenderer";
                        if ("hashCode".equals(name)) return System.identityHashCode(obj);
                        if ("equals".equals(name)) return args != null && args.length > 0 && obj == args[0];
                        // render（abstract void）等非 default 方法返回 null 即可；
                        // default 方法调用接口默认实现，避免返回 null 导致拆箱/解引用崩溃。
                        return invokeDefaultMethod(obj, method, args);
                    });
            java.util.function.Supplier<Object> supplier = () -> noOpRenderer;
            for (java.util.function.Supplier<? extends net.minecraft.world.item.Item> itemSupplier
                    : cn.autoforged.enchanter_letter.item.ModItems.ALL_LETTERS) {
                rendererRegistryRegisterMethod.invoke(null, itemSupplier.get(), supplier);
            }
            // 刷新渲染器缓存，确保本次注册立即生效（幂等）
            if (rendererRegistryReloadMethod != null) {
                rendererRegistryReloadMethod.invoke(null);
            }
        } catch (Throwable ignored) {
        }
    }
}