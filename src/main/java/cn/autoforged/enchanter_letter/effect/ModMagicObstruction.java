package cn.autoforged.enchanter_letter.effect;

import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * 魔法阻碍状态查询（Fabric 1.20.1）。
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

    public static int getLevel(LivingEntity entity) {
        if (entity == null) return 0;
        MobEffect effect = getEffect(entity.level());
        if (effect == null) return 0;
        MobEffectInstance instance = entity.getEffect(effect);
        return instance == null ? 0 : instance.getAmplifier() + 1;
    }

    public static boolean blocksDamage(LivingEntity entity) {
        return getLevel(entity) >= 1;
    }

    public static boolean blocksArmor(LivingEntity entity) {
        return getLevel(entity) >= 2;
    }

    public static boolean blocksAll(LivingEntity entity) {
        return getLevel(entity) >= 3;
    }
}
