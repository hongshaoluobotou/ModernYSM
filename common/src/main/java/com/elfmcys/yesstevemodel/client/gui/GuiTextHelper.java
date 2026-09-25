package com.elfmcys.yesstevemodel.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 26.3 port: 旧 AbstractWidget.renderScrollingString 系列方法已删除，集中到此类复刻实现。
 * 语义：宽度足够时居中绘制；不足时按时间循环滚动。
 */
public final class GuiTextHelper {

    private static final int FADE_RADIUS = 4;
    private static final int SCROLL_SPEED_MS_PER_CHAR = 32;

    private GuiTextHelper() {
    }

    /**
     * 26.3 port: I18n.exists 已移除；缺失 key 时 I18n.get 会返回 key 本身，据此回退到字面文本。
     */
    public static net.minecraft.network.chat.MutableComponent localized(String key, String fallback) {
        String translated = net.minecraft.client.resources.language.I18n.get(key);
        return translated.equals(key) ? Component.literal(fallback) : Component.translatable(key);
    }

    /**
     * 旧 AbstractWidget#renderScrollingString(GuiGraphics, Font, int, int)
     */
    public static void renderScrollingString(GuiGraphicsExtractor guiGraphics, Font font, net.minecraft.client.gui.components.AbstractWidget widget, int margin, int color) {
        int minX = widget.getX() + margin;
        int maxX = widget.getX() + widget.getWidth() - margin;
        renderScrollingString(guiGraphics, font, widget.getMessage(), minX, widget.getY(), maxX, widget.getY() + widget.getHeight(), color);
    }

    /**
     * 旧 GuiGraphics#renderScrollingString(Font, Component, int, int, int, int, int)
     */
    public static void renderScrollingString(GuiGraphicsExtractor guiGraphics, Font font, Component message, int minX, int minY, int maxX, int maxY, int color) {
        int textWidth = font.width(message);
        int centerY = (minY + maxY - 9) / 2 + 1;
        int availWidth = maxX - minX;
        if (textWidth > availWidth) {
            int extra = textWidth - availWidth;
            double period = (double) (SCROLL_SPEED_MS_PER_CHAR * extra + 2000L);
            double t = (net.minecraft.util.Util.getMillis() % period) / period;
            double phase = t < 0.5 ? t * 2.0 : (1.0 - t) * 2.0;
            int offset = Mth.clamp((int) (phase * extra), 0, extra);
            int width = Math.min(textWidth - offset, availWidth);
            guiGraphics.enableScissor(minX, minY, maxX, maxY);
            guiGraphics.text(font, message, minX - offset, centerY, color);
            guiGraphics.disableScissor();
        } else {
            int x = (minX + maxX - textWidth) / 2;
            guiGraphics.text(font, message, x, centerY, color);
        }
    }
}
