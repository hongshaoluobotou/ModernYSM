package com.elfmcys.yesstevemodel.client.gui.button;

import com.elfmcys.yesstevemodel.capability.PlayerCapability;
import com.elfmcys.yesstevemodel.client.ClientOnlyMode;
import com.elfmcys.yesstevemodel.client.ClientOnlySelection;
import com.elfmcys.yesstevemodel.client.entity.PlayerPreviewEntity;
import com.elfmcys.yesstevemodel.client.gui.ModelMetadataPresenter;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.client.renderer.ModelPreviewRenderer;
import com.elfmcys.yesstevemodel.client.renderer.RendererManager;
import com.elfmcys.yesstevemodel.network.NetworkHandler;
import com.elfmcys.yesstevemodel.network.message.C2SRequestSwitchModelPacket;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class TextureButton extends Button {

    public final PlayerPreviewEntity previewEntity;

    public final ModelAssembly modelAssembly;

    public TextureButton(int x, int y, PlayerPreviewEntity previewEntity, ModelAssembly modelAssembly) {
        super(x, y, 54, 102, Component.empty(), button -> {
        }, DEFAULT_NARRATION);
        this.previewEntity = previewEntity;
        this.modelAssembly = modelAssembly;
    }

    public void onPress(InputWithModifiers input) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null) {
            PlayerCapability.get(localPlayer).ifPresent(cap -> {
                cap.setCurrentTexture(this.previewEntity.getCurrentTextureName());
                if (NetworkHandler.isClientConnected() && !ClientOnlyMode.isForced()) {
                    NetworkHandler.sendToServer(new C2SRequestSwitchModelPacket(this.previewEntity.getModelId(), this.previewEntity.getCurrentTextureName()));
                    return;
                }
                cap.initModelWithTexture(this.previewEntity.getModelId(), this.previewEntity.getCurrentTextureName());
                ClientOnlySelection.save(this.previewEntity.getModelId(), this.previewEntity.getCurrentTextureName());
            });
        }
    }

    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        guiGraphics.fillGradient(getX(), getY(), getX() + this.width, getY() + this.height, -12369342, -12369342);
        renderPlayerPreview(guiGraphics, partialTick);
        String str = this.previewEntity.getCurrentTextureName();
        MutableComponent mutableComponentLiteral = Component.literal(ModelMetadataPresenter.getLocalizedModelString(this.modelAssembly, "files.player.texture.%s".formatted(str), str));
        List listSplit = font.split(mutableComponentLiteral, 50);
        if (listSplit.size() > 1) {
            guiGraphics.centeredText(font, (FormattedCharSequence) listSplit.get(0), getX() + (this.width / 2), (getY() + this.height) - 19, 15986656);
            guiGraphics.centeredText(font, (FormattedCharSequence) listSplit.get(1), getX() + (this.width / 2), (getY() + this.height) - 10, 15986656);
        } else {
            guiGraphics.centeredText(font, mutableComponentLiteral, getX() + (this.width / 2), (getY() + this.height) - 15, 15986656);
        }
        if (isHoveredOrFocused()) {
            guiGraphics.fillGradient(getX(), getY() + 1, getX() + 1, (getY() + this.height) - 1, -790560, -790560);
            guiGraphics.fillGradient(getX(), getY(), getX() + this.width, getY() + 1, -790560, -790560);
            guiGraphics.fillGradient((getX() + this.width) - 1, getY() + 1, getX() + this.width, (getY() + this.height) - 1, -790560, -790560);
            guiGraphics.fillGradient(getX(), (getY() + this.height) - 1, getX() + this.width, getY() + this.height, -790560, -790560);
        }
    }

    public void renderPlayerPreview(GuiGraphicsExtractor guiGraphics, float partialTick) {
        // 26.3 port: 与 1.20.1 TextureButton 逐值对照 ——
        //   scissor (x, y, width, height-20)；
        //   renderLivingEntityPreview(x + width/2, y + height/2 + 24, 35, ...,
        //                             disablePreviewRotation=false, hideEquipment=true)
        // → pitch -10、yaw 20、feet 锚定 (x + w/2, y + h/2 + 24)。PiP 自带区域裁剪（region 即纹理）。
        ModelPreviewRenderer.renderFixed(guiGraphics, getX(), getY(), getX() + this.width, getY() + this.height - 20,
                35.0f, -10.0f, 20.0f,
                getX() + this.width / 2.0f, getY() + this.height / 2.0f + 24.0f, this.previewEntity, partialTick);
    }
}
