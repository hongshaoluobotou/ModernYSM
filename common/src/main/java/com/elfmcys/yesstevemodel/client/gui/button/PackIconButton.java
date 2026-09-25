package com.elfmcys.yesstevemodel.client.gui.button;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.resource.models.ModelPackData;
import com.elfmcys.yesstevemodel.client.gui.ModelMetadataPresenter;
import com.elfmcys.yesstevemodel.util.FileTypeUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

public class PackIconButton extends Button {

    private static final Identifier default_pack_icon = Identifier.fromNamespaceAndPath(YesSteveModel.MOD_ID, "texture/default_pack_icon.png");

    private final ModelPackData packData;

    public PackIconButton(int x, int y, int width, int height, ModelPackData packData, OnPress onPress) {
        super(x, y, width, height, Component.literal(ModelMetadataPresenter.getLocalizedString(packData, "name", packData.getName())), onPress, DEFAULT_NARRATION);
        this.packData = packData;
    }

    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        guiGraphics.fillGradient(getX(), getY(), getX() + this.width, getY() + this.height, -6598176, -6598176);
        Identifier location = FileTypeUtil.getPackIconLocation(this.packData.getPath());
        // 26.3 port: getTexture(Identifier, AbstractTexture) 双参签名已移除，改用注册表缺省纹理判断
        AbstractTexture texture = minecraft.getTextureManager().getTexture(location);
                if (texture == minecraft.getTextureManager().getTexture(MissingTextureAtlasSprite.getLocation())) {
            guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, default_pack_icon, getX(), getY(), 0.0f, 0.0f, this.width, this.height, this.width, this.height);
        } else {
            guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, location, getX(), getY(), 0.0f, 0.0f, this.width, this.height, this.width, this.height);
        }
                List listSplit = font.split(getMessage(), 45);
        if (listSplit.size() > 1) {
            drawCenteredString(guiGraphics, font, (FormattedCharSequence) listSplit.get(0), getX() + (this.width / 2), (getY() + this.height) - 19, 5592405);
            drawCenteredString(guiGraphics, font, (FormattedCharSequence) listSplit.get(1), getX() + (this.width / 2), (getY() + this.height) - 10, 5592405);
        } else {
            drawCenteredString(guiGraphics, font, getMessage(), getX() + (this.width / 2), (getY() + this.height) - 15, 5592405);
        }
        if (isHoveredOrFocused()) {
            guiGraphics.fillGradient(getX(), getY() + 1, getX() + 1, (getY() + this.height) - 1, -1982745, -1982745);
            guiGraphics.fillGradient(getX(), getY(), getX() + this.width, getY() + 1, -1982745, -1982745);
            guiGraphics.fillGradient((getX() + this.width) - 1, getY() + 1, getX() + this.width, (getY() + this.height) - 1, -1982745, -1982745);
            guiGraphics.fillGradient(getX(), (getY() + this.height) - 1, getX() + this.width, getY() + this.height, -1982745, -1982745);
        }
    }

    public void renderDescription(GuiGraphicsExtractor guiGraphics, Screen screen, int mouseX, int mouseY) {
        String str = ModelMetadataPresenter.getLocalizedString(this.packData, "description", this.packData.getDescription());
        if (StringUtils.isBlank(str)) {
            return;
        }
        List<Component> listSingletonList = Collections.singletonList(Component.literal(str));
        if (isHovered()) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(0.0f, 0.0f);
            guiGraphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font, listSingletonList, mouseX, mouseY);
            guiGraphics.pose().popMatrix();
        }
    }

    private static void drawCenteredString(GuiGraphicsExtractor guiGraphics, Font font, Component component, int centerX, int y, int color) {
        guiGraphics.text(font, component, centerX - (font.width(component) / 2), y, color, false);
    }

    private static void drawCenteredString(GuiGraphicsExtractor guiGraphics, Font font, FormattedCharSequence formattedCharSequence, int centerX, int y, int color) {
        guiGraphics.text(font, formattedCharSequence, centerX - (font.width(formattedCharSequence) / 2), y, color, false);
    }
}