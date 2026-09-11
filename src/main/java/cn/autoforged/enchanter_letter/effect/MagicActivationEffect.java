package cn.autoforged.enchanter_letter.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 魔法激活：临时手札合订本的“生效开关”。
 * 持有该效果时，携带的临时手札合订本内手札视为生效；
 * 效果以任意形式消失时，清除玩家/生物身上所有临时手札合订本。
 */
public class MagicActivationEffect extends MobEffect {
    public MagicActivationEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
