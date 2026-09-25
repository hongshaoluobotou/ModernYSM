package com.elfmcys.yesstevemodel.client.gui.button;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.gui.ISpecialWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;


import java.util.function.Consumer;

// 26.3 port: net.minecraft.client.gui.components.StateSwitchingButton 已删除，
// 改为继承 AbstractButton，用 blit 手动绘制选中/未选中两态纹理。
@Environment(EnvType.CLIENT)
public class ConfigCheckBox extends AbstractButton implements ISpecialWidget {

    private static final Identifier location = Identifier.fromNamespaceAndPath(YesSteveModel.MOD_ID, "texture/roulette.png");

    private final Consumer<Boolean> consumer2;

    private final Component component2;

    private boolean isStateTriggered;

    public ConfigCheckBox(int x, int y, int width, Component component, Consumer<Boolean> consumer) {
        super(x, y, width, 12, Component.empty());
        this.component2 = component;
        this.consumer2 = consumer;
    }

    public ConfigCheckBox(int x, int y, Component component, Consumer<Boolean> consumer) {
        this(x, y, 115, component, consumer);
    }

    public void setStateTriggered(boolean triggered) {
        this.isStateTriggered = triggered;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        // roulette.png 中 128x12 为一态纹理，纵向偏移 12 表示选中态（与旧 StateSwitchingButton initTextureValues(0,0,128,12) 对应）
        int texY = this.isStateTriggered ? 12 : 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, location, getX(), getY(), 0.0f, texY, getWidth(), getHeight(), 128, 24);
        guiGraphics.text(Minecraft.getInstance().font, this.component2, getX() + 14, getY() + 2, -1, false);
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.isStateTriggered = !this.isStateTriggered;
        this.consumer2.accept(Boolean.valueOf(this.isStateTriggered));
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
