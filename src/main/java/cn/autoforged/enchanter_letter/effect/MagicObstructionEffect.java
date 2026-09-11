package cn.autoforged.enchanter_letter.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 魔法阻碍：禁止携带者身上生效的手札功能。
 * 等级 1：禁用伤害增幅/转化；
 * 等级 2：额外禁用护甲值/护甲韧性；
 * 等级 3：额外禁用抗性提升/药水附加/光灵附魔。
 */
public class MagicObstructionEffect extends MobEffect {
    public MagicObstructionEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
