package cn.autoforged.enchanter_letter.effect;

import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, UsefulMagicEnchanterLetterMod.MOD_ID);

    public static final RegistryObject<MagicObstructionEffect> MAGIC_OBSTRUCTION =
            EFFECTS.register("magic_obstruction",
                    () -> new MagicObstructionEffect(MobEffectCategory.HARMFUL, 0x7B2D9A));

    public static final RegistryObject<MagicActivationEffect> MAGIC_ACTIVATION =
            EFFECTS.register("magic_activation",
                    () -> new MagicActivationEffect(MobEffectCategory.BENEFICIAL, 0xD93E74));

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }
}
