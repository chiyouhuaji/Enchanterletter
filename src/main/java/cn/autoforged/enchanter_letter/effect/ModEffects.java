package cn.autoforged.enchanter_letter.effect;

import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, UsefulMagicEnchanterLetterMod.MOD_ID);

    public static final DeferredHolder<MobEffect, MagicObstructionEffect> MAGIC_OBSTRUCTION =
            EFFECTS.register("magic_obstruction",
                    () -> new MagicObstructionEffect(MobEffectCategory.HARMFUL, 0x7B2D9A));

    public static final DeferredHolder<MobEffect, MagicActivationEffect> MAGIC_ACTIVATION =
            EFFECTS.register("magic_activation",
                    () -> new MagicActivationEffect(MobEffectCategory.BENEFICIAL, 0xD93E74));

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }
}
