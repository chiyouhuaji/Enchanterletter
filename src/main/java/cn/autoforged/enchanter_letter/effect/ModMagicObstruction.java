package cn.autoforged.enchanter_letter.effect;

import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * 魔法阻碍状态查询。
 * 该效果只压制“手札的主动/附加效果”，不影响魔法绑定附魔。
 */
public class ModMagicObstruction {
    public static final ResourceKey<MobEffect> KEY = ResourceKey.create(
            Registries.MOB_EFFECT,
            ResourceLocation.fromNamespaceAndPath(UsefulMagicEnchanterLetterMod.MOD_ID, "magic_obstruction"));

    private ModMagicObstruction() {
    }

    public static Holder<MobEffect> getHolder(Level level) {
        if (level == null) return null;
        return level.registryAccess().registryOrThrow(Registries.MOB_EFFECT).getHolder(KEY).orElse(null);
    }

    /** 当前效果等级：0=未生效，1=一级，2=二级，3=三级。 */
    public static int getLevel(LivingEntity entity) {
        if (entity == null) return 0;
        Holder<MobEffect> effect = getHolder(entity.level());
        if (effect == null) return 0;
        MobEffectInstance instance = entity.getEffect(effect);
        return instance == null ? 0 : instance.getAmplifier() + 1;
    }

    /** 是否禁用伤害增幅/转化（1 级及以上）。 */
    public static boolean blocksDamage(LivingEntity entity) {
        return getLevel(entity) >= 1;
    }

    /** 是否额外禁用护甲/护甲韧性（2 级及以上）。 */
    public static boolean blocksArmor(LivingEntity entity) {
        return getLevel(entity) >= 2;
    }

    /** 是否禁用全部手札效果（3 级）。 */
    public static boolean blocksAll(LivingEntity entity) {
        return getLevel(entity) >= 3;
    }
}
