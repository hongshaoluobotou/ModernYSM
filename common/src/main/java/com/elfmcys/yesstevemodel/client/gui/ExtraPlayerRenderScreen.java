package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.config.ExtraPlayerRenderConfig;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;


public class ExtraPlayerRenderScreen extends Screen {

    private static final char RESET_KEY = 'r';

    private int mouseStartX;

    private int mouseStartY;

    private float rotationX;

    private float rotationY;

    private boolean isDragging;

    private boolean isRightDragging;

    private int offsetX;

    private int offsetY;

    public ExtraPlayerRenderScreen() {
        super(Component.literal("YSM Extra Player Render Config GUI"));
        this.isDragging = false;
        this.isRightDragging = false;
        this.offsetX = 5;
        this.offsetY = 1;
        this.mouseStartX = ExtraPlayerRenderConfig.PLAYER_POS_X.get().intValue();
        this.mouseStartY = ExtraPlayerRenderConfig.PLAYER_POS_Y.get().intValue();
        this.rotationX = ExtraPlayerRenderConfig.PLAYER_SCALE.get().floatValue();
        this.rotationY = ExtraPlayerRenderConfig.PLAYER_YAW_OFFSET.get().floatValue();
        if (PauseScreenButtonBuilder.isAndroid()) {
            this.offsetX = 16;
            this.offsetY = 0;
        }
    }

    public void init() {
        clearWidgets();
        int i = -30;
        if (PauseScreenButtonBuilder.isAndroid()) {
            addRenderableWidget(Button.builder(Component.translatable("controls.reset"), button -> {
                resetTransform();
            }).bounds((this.width / 2) - 50, this.height - 35, 100, 30).build());
            i = -60;
        }
        MutableComponent mutableComponentTranslatable = Component.translatable("gui.yes_steve_model.hide_or_show");
        int iWidth = this.font.width(mutableComponentTranslatable) + 24;
        // 26.3 port: Checkbox 改用 Builder
        addRenderableWidget(Checkbox.builder(mutableComponentTranslatable, this.font)
                .pos((this.width - iWidth) / 2, this.height + i).maxWidth(iWidth)
                .selected(ExtraPlayerRenderConfig.DISABLE_PLAYER_RENDER.get().booleanValue())
                .onValueChange((checkbox, sel) -> ExtraPlayerRenderConfig.DISABLE_PLAYER_RENDER.set(sel)).build());
    }

    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        int boxLeft = this.mouseStartX;
        int boxTop = this.mouseStartY;
        int boxRight = (int) (boxLeft + (this.rotationX));
        int boxBottom = (int) (boxTop + (this.rotationX * 2.0f));
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(0.0f, 0.0f);
        guiGraphics.verticalLine((this.width / 2) - 1, -2, this.height + 2, -1610612737);
        guiGraphics.horizontalLine(-2, this.width + 2, (this.height / 2) - 1, -1610612737);
        guiGraphics.verticalLine(10, -2, this.height + 2, -1610612737);
        guiGraphics.verticalLine(this.width - 10, -2, this.height + 2, -1610612737);
        guiGraphics.horizontalLine(-2, this.width + 2, 10, -1610612737);
        guiGraphics.horizontalLine(-2, this.width + 2, this.height - 10, -1610612737);
        guiGraphics.verticalLine(boxLeft, boxTop, boxBottom, -65536);
        guiGraphics.verticalLine(boxRight, boxTop, boxBottom, -65536);
        guiGraphics.horizontalLine(boxLeft, boxRight, boxTop, -65536);
        guiGraphics.horizontalLine(boxLeft, boxRight, boxBottom, -65536);
        guiGraphics.fillGradient(boxLeft, boxTop, boxRight, boxBottom, 1342177279, 1342177279);
        guiGraphics.fillGradient(boxLeft - this.offsetX, boxTop - this.offsetX, boxLeft + this.offsetX, boxTop + this.offsetX, -16711777, -16711777);
        guiGraphics.fillGradient(boxRight - this.offsetX, boxBottom - this.offsetX, boxRight + this.offsetX, boxBottom + this.offsetX, -16777057, -16777057);
        int tipY = 15;
        for (FormattedCharSequence formattedCharSequence : this.font.split(Component.translatable("gui.yes_steve_model.extra_player_render.tips"), 500)) {
            guiGraphics.text(this.font, formattedCharSequence, (this.width - 15) - this.font.width(formattedCharSequence), tipY, 16777215);
            tipY += 10;
        }
        guiGraphics.pose().popMatrix();
        if (Minecraft.getInstance().player != null && !ExtraPlayerRenderConfig.DISABLE_PLAYER_RENDER.get().booleanValue()) {
            // 26.3 port: 经 ModelPreviewRenderer.renderPlayerOverlay 走 GuiEntityRenderState(PiP) 体系恢复纸娃娃渲染
            ModelPreviewRenderer.renderPlayerOverlay(guiGraphics, Minecraft.getInstance().player, this.mouseStartX, this.mouseStartY, this.rotationX, this.rotationY, -500, partialTick);
        }
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        boolean inLeftHandleX = ((double) (this.mouseStartX - this.offsetX)) < mouseX && mouseX < ((double) (this.mouseStartX + this.offsetX));
        boolean inLeftHandleY = ((double) (this.mouseStartY - this.offsetX)) < mouseY && mouseY < ((double) (this.mouseStartY + this.offsetX));
        if (button == 0 && inLeftHandleX && inLeftHandleY) {
            this.isDragging = true;
        }
        int rightHandleX = (int) (this.mouseStartX + (this.rotationX));
        int rightHandleY = (int) (this.mouseStartY + (this.rotationX * 2.0f));
        boolean inRightHandleX = ((double) (rightHandleX - this.offsetX)) < mouseX && mouseX < ((double) (rightHandleX + this.offsetX));
        boolean inRightHandleY = ((double) (rightHandleY - this.offsetX)) < mouseY && mouseY < ((double) (rightHandleY + this.offsetX));
        if (button == 0 && inRightHandleX && inRightHandleY) {
            this.isRightDragging = true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        this.isDragging = false;
        this.isRightDragging = false;
        return super.mouseReleased(event);
    }

    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (this.isRightDragging) {
            this.rotationX = (float) Math.min(mouseX - this.mouseStartX, (mouseY - this.mouseStartY) / 2.0d);
            return true;
        }
        if (this.isDragging) {
            this.mouseStartX = (int) mouseX;
            this.mouseStartY = (int) mouseY;
            return true;
        }
        if (button == this.offsetY) {
            this.rotationY += (float) (dragX * 2.0d);
            return true;
        }
        return false;
    }

    public boolean keyPressed(KeyEvent event) {
        // 26.3 port: Alt+R 重置的 keyPressed 兜底。1.20.1 走 charTyped（GLFW 字符回调
        // 按住 Alt 仍触发）；26.3 换 SDL 后字符事件走 SDL 文本输入路径，按住 Alt 时
        // 不保证产生（且 CharacterEvent 不携带修饰符），故在 keyPressed 按键域直接判定
        // Alt+R（R 的 SDL scancode = 21），charTyped 路径保留兜底语义，二者幂等。
        if (event.key() == InputConstants.KEY_R && event.hasAltDown()) {
            resetTransform();
            return true;
        }
        return super.keyPressed(event);
    }

    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        if (Character.toLowerCase((char) event.codepoint()) == RESET_KEY && this.minecraft.hasAltDown()) {
            resetTransform();
        }
        return super.charTyped(event);
    }

    private void resetTransform() {
        this.mouseStartX = 10;
        this.mouseStartY = 10;
        this.rotationX = 40.0f;
        this.rotationY = 5.0f;
    }

    public void onClose() {
        ExtraPlayerRenderConfig.PLAYER_POS_X.set(Integer.valueOf(this.mouseStartX));
        ExtraPlayerRenderConfig.PLAYER_POS_Y.set(Integer.valueOf(this.mouseStartY));
        ExtraPlayerRenderConfig.PLAYER_SCALE.set(Double.valueOf(this.rotationX));
        ExtraPlayerRenderConfig.PLAYER_YAW_OFFSET.set(Double.valueOf(this.rotationY));
        super.onClose();
    }
}
