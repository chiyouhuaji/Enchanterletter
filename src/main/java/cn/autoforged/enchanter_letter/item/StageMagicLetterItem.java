package cn.autoforged.enchanter_letter.item;

import cn.autoforged.enchanter_letter.config.ModConfig;
import net.minecraft.world.item.ItemStack;

public class StageMagicLetterItem extends MagicLetterItem {
    private final int stage;

    public StageMagicLetterItem(Properties properties, int stage) {
        super(properties);
        this.stage = stage;
    }

    @Override
    public int getLevel(ItemStack stack) {
        return stage;
    }

    @Override
    public double getMultiplier(ItemStack stack) {
        var multipliers = ModConfig.getInstance().stageLetters.multipliers;
        if (stage >= 1 && stage <= multipliers.size()) {
            Double val = multipliers.get(stage - 1);
            if (val != null) return val;
        }
        return stage;
    }
}
