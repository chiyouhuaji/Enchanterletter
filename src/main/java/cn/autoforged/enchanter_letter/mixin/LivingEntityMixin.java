package cn.autoforged.enchanter_letter.mixin;

import cn.autoforged.enchanter_letter.event.ModCommonEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    /**
     * 在 hurt() 头部统一处理：
     * 1) 实体（玩家或生物）承受伤害累计（坚韧手札；/kill 指令伤害不计入）；
     * 2) 以“初始（未增益）”伤害把附魔手札转化伤害加入待结算队列；
     * 3) 未附魔手札对默认伤害的增益在此处放大（生物持有手札时对所有伤害生效）。
     */
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float modifyHurtDamage(float originalDamage, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (ModCommonEvents.isConversionInProgress(self)) return originalDamage;

        // 实体承受伤害累计（坚韧手札）——/kill 指令伤害不计入
        if (!self.level().isClientSide && !source.is(DamageTypes.GENERIC_KILL)) {
            ModCommonEvents.onEntityDamageTaken(self, originalDamage);
        }

        if (!ModCommonEvents.shouldApplyLetterBoost(source)) return originalDamage;

        ModCommonEvents.onUsefulMagicIncomingDamage(self, source, originalDamage);
        // 未附魔手札增益后应用受害者的抗性提升减免（护甲结算之前）
        float amplified = ModCommonEvents.onUsefulMagicHurtHealing(self, source, originalDamage);
        return ModCommonEvents.applyResistanceReduction(self, amplified);
    }
}
