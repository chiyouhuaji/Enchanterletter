package cn.autoforged.enchanter_letter.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 魔法激活：携带临时手札合订本时激活，允许合订本内的临时手札生效。
 */
public class MagicActivationEffect extends MobEffect {
    public MagicActivationEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}