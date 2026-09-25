package com.elfmcys.yesstevemodel.client.gui.button;

import com.mojang.blaze3d.Blaze3D;
import com.elfmcys.yesstevemodel.resource.models.AuthorInfo;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.client.gui.ModelMetadataPresenter;
import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import com.elfmcys.yesstevemodel.client.gui.GuiTextHelper;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.net.URI;

import java.util.List;

public class AuthorButton extends Button {

    private final AuthorInfo authorInfo;

    private final ModelAssembly modelAssembly;

    private final Identifier resourceLocation;

    private final int authorIndex;

    private final List<Component> componentList;

    private int selectedContactIndex;

    private final Screen parentScreen;

    public AuthorButton(int x, int y, AuthorInfo authorInfo, ModelAssembly modelAssembly, Identifier resourceLocation, int authorIndex, Screen screen) {
        super(x, y, 70, 130, Component.empty(), button -> {
        }, DEFAULT_NARRATION);
        this.selectedContactIndex = -1;
        this.authorInfo = authorInfo;
        this.modelAssembly = modelAssembly;
        this.resourceLocation = resourceLocation;
        this.authorIndex = authorIndex;
        this.componentList = Lists.newArrayList();
        if (this.authorInfo != null) {
            renderTooltip(false);
        }
        this.parentScreen = screen;
    }

    public static AuthorButton createAuthorButton(int x, int y, Screen screen) {
        return new AuthorButton(x, y, null, null, null, -1, screen);
    }

    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        if (this.authorInfo == null || this.modelAssembly == null || this.resourceLocation == null) {
            guiGraphics.fillGradient(getX(), getY(), getX() + this.width, getY() + this.height, -1891417534, -1891417534);
            guiGraphics.centeredText(font, Component.literal("......"), getX() + (this.width / 2), getY() + (this.height / 2), 0xA0A0A0);
            return;
        }
        if (isHoveredOrFocused()) {
            guiGraphics.fillGradient(getX(), getY(), getX() + this.width, getY() + this.height, -1892652116, -1892652116);
        } else {
            guiGraphics.fillGradient(getX(), getY(), getX() + this.width, getY() + this.height, -1891417534, -1891417534);
        }
        guiGraphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, this.resourceLocation, getX() + 3, getY() + 3, 0.0f, 0.0f, 64, 64, 64, 64);
        String str = ModelMetadataPresenter.getLocalizedModelString(this.modelAssembly, "metadata.authors.%d.name".formatted(this.authorIndex), this.authorInfo.getName());
        String str2 = ModelMetadataPresenter.getLocalizedModelString(this.modelAssembly, "metadata.authors.%d.role".formatted(this.authorIndex), this.authorInfo.getRole());
        String str3 = ModelMetadataPresenter.getLocalizedModelString(this.modelAssembly, "metadata.authors.%d.comment".formatted(this.authorIndex), this.authorInfo.getComment());
        GuiTextHelper.renderScrollingString(guiGraphics, font, Component.literal(str), getX() + 2, getY() + 72, (getX() + this.width) - 2, getY() + 82, 0xFFAA00);
        guiGraphics.centeredText(font, str2, getX() + 35, getY() + 82, 0x55FF55);
        drawWrappedText(guiGraphics, Component.literal(str3), getX() + 3, getY() + 95, 64, -1);
    }

    public void drawWrappedText(GuiGraphicsExtractor guiGraphics, FormattedText formattedText, int x, int y, int wrapWidth, int color) {
        Font font = Minecraft.getInstance().font;
        for (FormattedCharSequence formattedCharSequence : font.split(formattedText, wrapWidth)) {
            guiGraphics.text(font, formattedCharSequence, x, y, color, false);
            y += 9;
            if (y > getY() + this.height) {
                return;
            }
        }
    }

    public void refreshContactComponents(GuiGraphicsExtractor guiGraphics, Screen screen, int mouseX, int mouseY) {
        if (this.isHovered && !this.componentList.isEmpty()) {
            guiGraphics.setComponentTooltipForNextFrame(Minecraft.getInstance().font, this.componentList, mouseX, mouseY);
        } else if (this.selectedContactIndex != -1) {
            this.selectedContactIndex = -1;
            renderTooltip(false);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double delta = scrollY;
        if (delta > 0.0d) {
            if (this.selectedContactIndex > 0) {
                this.selectedContactIndex--;
                renderTooltip(false);
                return true;
            }
            return true;
        }
        if (delta < 0.0d) {
            if (this.selectedContactIndex < this.componentList.size() - 2) {
                this.selectedContactIndex++;
                renderTooltip(false);
                return true;
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void renderTooltip(boolean copied) {
        if (this.authorInfo == null) {
            return;
        }
        this.componentList.clear();
        for (int i = 0; i < this.authorInfo.getContact().size(); i++) {
            MutableComponent componentLiteral = Component.literal(this.authorInfo.getContact().getKeyAt(i) + ": " + this.authorInfo.getContact().getValueAt(i));
            if (i == this.selectedContactIndex) {
                componentLiteral.append(Component.literal(copied ? " ✓" : " ◀").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
            }
            this.componentList.add(componentLiteral);
        }
        if (!this.componentList.isEmpty()) {
            this.componentList.add(Component.translatable("gui.yes_steve_model.model.info.contact.click_hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public void onPress(InputWithModifiers input) {
        String link;
        if (this.authorInfo == null) {
            return;
        }
        int i = this.selectedContactIndex;
        if (i == -1) {
            i = 0;
        }
        if (i < 0 || i >= this.authorInfo.getContact().size() || (link = this.authorInfo.getContact().getValueAt(i)) == null) {
            return;
        }
        if (link.startsWith("http://") || link.startsWith("https://")) {
            Minecraft.getInstance().setScreenAndShow(new ConfirmLinkScreen(confirmed -> {
                if (confirmed) {
                    Blaze3D.openUri(URI.create(link));
                }
                Minecraft.getInstance().setScreenAndShow(this.parentScreen);
            }, URI.create(link), true));
            return;
        }
        Minecraft.getInstance().keyboardHandler.setClipboard(link);
        if (this.selectedContactIndex == -1) {
            this.selectedContactIndex = 0;
        }
        renderTooltip(true);
    }
}
