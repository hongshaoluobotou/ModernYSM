package com.elfmcys.yesstevemodel.client.gui.button;

import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import com.elfmcys.yesstevemodel.config.ConfigSpec.BooleanValue;

public class ConfigCheckBoxForge extends Checkbox {

    private final BooleanValue booleanValue;

    public ConfigCheckBoxForge(int x, int y, String str, BooleanValue booleanValue) {
        super(x, y, 400, 20, Component.translatable("gui.yes_steve_model.config." + str), booleanValue.get().booleanValue());
        this.booleanValue = booleanValue;
    }

    public void onPress() {
        super.onPress();
        this.booleanValue.set(!this.booleanValue.get());
    }
}