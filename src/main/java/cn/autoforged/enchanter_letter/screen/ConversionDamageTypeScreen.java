package cn.autoforged.enchanter_letter.screen;

import cn.autoforged.enchanter_letter.UsefulMagicEnchanterLetterMod;
import cn.autoforged.enchanter_letter.enchantment.ModEnchantments;
import cn.autoforged.enchanter_letter.network.ConversionDamageTypePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ConversionDamageTypeScreen extends Screen {
    private static final int INPUT_WIDTH = 200;
    private final ItemStack stack;
    private EditBox input;

    public ConversionDamageTypeScreen(ItemStack stack) {
        super(Component.translatable("screen." + UsefulMagicEnchanterLetterMod.MOD_ID + ".conversion_damage_type"));
        this.stack = stack;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        input = new EditBox(this.font, centerX - INPUT_WIDTH / 2, centerY - 10, INPUT_WIDTH, 20,
                Component.translatable("screen." + UsefulMagicEnchanterLetterMod.MOD_ID + ".damage_type_input"));
        input.setMaxLength(128);
        input.setFilter(s -> s.isEmpty() || s.codePoints().allMatch(c -> {
            if (c == ':') return true;
            if (Character.isLetterOrDigit(c)) return true;
            return c == '_' || c == '-';
        }));
        input.setValue(ModEnchantments.getConversionDamageTypeOrDefault(stack));
        addRenderableWidget(input);

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, btn -> {
            String value = input.getValue();
            if (!ModEnchantments.isValidDamageTypeString(value)) return;
            ConversionDamageTypePayload.sendToServer(value);
            this.onClose();
        }).bounds(centerX - 50, centerY + 20, 100, 20).build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, btn -> this.onClose())
                .bounds(centerX - 50, centerY + 50, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 30, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("screen." + UsefulMagicEnchanterLetterMod.MOD_ID + ".damage_type_hint"),
                this.width / 2, 50, 0xAAAAAA);
    }
}