package com.elfmcys.yesstevemodel.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 1.20.1 → 26.3 移植：文字颜色 alpha 兼容。
 *
 * <p>1.20.1 的 {@code GuiGraphics.drawString/drawCenteredString} 在颜色的 alpha 位为 0 时
 * 自动补不透明 alpha（{@code if ((color & 0xFC000000) == 0) color |= 0xFF000000}），
 * 因此 YSM 全部 GUI 代码沿用 1.20.1 习惯直接传 24 位颜色常量（如 15986656 = 0xF3EFE0、
 * 0x404040、0x777777）。26.3 的 {@code GuiGraphicsExtractor.text(...)} 语义改为
 * {@code if (ARGB.alpha(color) == 0) return;}——alpha 为 0 的文字被<b>静默丢弃</b>，
 * 导致模型选择界面所有卡片文字/占位符/页码"消失"（真机 2026.09.26 定位）。</p>
 *
 * <p>本 mixin 在 26.3 文字入口复刻 1.20.1 的自动补 alpha 语义，一次性修复全部调用点
 * （包括 vanilla EditBox 等经 GuiGraphicsExtractor 画字的路径）。</p>
 */
@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsExtractorMixin {

    @ModifyVariable(
            method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 2
    )
    private int ysm$fixTextColorAlpha(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }

    @ModifyVariable(
            method = "textWithWordWrap(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/FormattedText;IIII)I",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 3
    )
    private int ysm$fixWordWrapColorAlpha(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }

    @ModifyVariable(
            method = "textWithWordWrap(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/FormattedText;IIIIZ)I",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 3
    )
    private int ysm$fixWordWrapColorAlpha7(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }

    @ModifyVariable(
            method = "textWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 3
    )
    private int ysm$fixBackdropColorAlpha(int color) {
        return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
    }
}
