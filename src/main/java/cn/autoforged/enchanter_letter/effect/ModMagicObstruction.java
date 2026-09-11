package cn.autoforged.enchanter_letter.effect;

import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * 魔法阻碍状态查询（Forge 1.20.1，直接 Registry 取值）。
 * 该效果只压制“手札的主动/附加效果”，不影响魔法绑定附魔。
 */
public class ModMagicObstruction {
    public static final ResourceLocation ID = new ResourceLocation(UsefulMagicEnchanterLetterMod.MOD_ID, "magic_obstruction");

    private ModMagicObstruction() {
    }

    public static MobEffect getEffect(Level level) {
        if (level == null) return null;
        return level.registryAccess().registryOrThrow(Registries.MOB_EFFECT).get(ID);
    }

    /** 当前效果等级：0=未生效，1=一级，2=二级，3=三级。 */
    public static int getLevel(LivingEntity entity) {
        if (entity == null) return 0;
        MobEffect effect = getEffect(entity.level());
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
