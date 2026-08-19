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
import net.neoforged.neoforge.network.PacketDistributor;

public class ConversionDamageTypeScreen extends Screen {
    private static final int INPUT_WIDTH = 200;
    private final ItemStack stack;
    private EditBox input;
    private Button doneButton;

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

        doneButton = Button.builder(CommonComponents.GUI_DONE, btn -> {
            String value = input.getValue();
            if (!ModEnchantments.isValidDamageTypeString(value)) return;
            PacketDistributor.sendToServer(new ConversionDamageTypePayload(value));
            this.onClose();
        }).pos(centerX - 50, centerY + 20).width(100).build();
        addRenderableWidget(doneButton);

        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, btn -> this.onClose())
                .pos(centerX - 50, centerY + 50).width(100).build());
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
