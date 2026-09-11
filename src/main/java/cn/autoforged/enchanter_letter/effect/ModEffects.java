package cn.autoforged.enchanter_letter.effect;

import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;

public class ModEffects {
    public static final MagicObstructionEffect MAGIC_OBSTRUCTION =
            new MagicObstructionEffect(MobEffectCategory.HARMFUL, 0x7B2D9A);

    public static final MagicActivationEffect MAGIC_ACTIVATION =
            new MagicActivationEffect(MobEffectCategory.BENEFICIAL, 0xD93E74);

    public static void register() {
        Registry.register(BuiltInRegistries.MOB_EFFECT,
                ResourceLocation.fromNamespaceAndPath(UsefulMagicEnchanterLetterMod.MOD_ID, "magic_obstruction"),
                MAGIC_OBSTRUCTION);
        Registry.register(BuiltInRegistries.MOB_EFFECT,
                ResourceLocation.fromNamespaceAndPath(UsefulMagicEnchanterLetterMod.MOD_ID, "magic_activation"),
                MAGIC_ACTIVATION);
    }

    private ModEffects() {
    }
}
