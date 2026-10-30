package com.elfmcys.yesstevemodel.client.renderer.pip;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jspecify.annotations.Nullable;

/**
 * 取 {@link GuiGraphicsExtractor} 当前的 scissor 区域。
 *
 * <p>26.3 三个加载器对 {@code GuiGraphicsExtractor#scissorStack} 的可见性不一致：
 * fabric（原版）是 {@code public final} 字段 + public 内部类；NeoForge patch 改成 private 字段 +
 * {@code public ScreenRectangle peekScissorStack()}；Forge patch 改成 private 字段 +
 * {@code public ScissorStack getScissorStack()}（返回的仍是包私有类型，源码层无法调用 {@code peek()}）。
 * 因此这里用反射统一读取，失败时退化为 {@code null}（= 不额外裁剪）。</p>
 *
 * <p>开销：每次预览提交一次 {@code Field.get + Method.invoke}（每帧十几个预览），可忽略；
 * Field/Method 只解析一次。</p>
 */
public final class PipScissors {

    private static final @Nullable Field SCISSOR_STACK_FIELD;
    private static final @Nullable Method PEEK_METHOD;

    static {
        Field field = null;
        Method peek = null;
        try {
            field = GuiGraphicsExtractor.class.getDeclaredField("scissorStack");
            field.setAccessible(true);
            Class<?> stackClass = field.getType();
            peek = stackClass.getMethod("peek");
            peek.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException e) {
            field = null;
            peek = null;
        }
        SCISSOR_STACK_FIELD = field;
        PEEK_METHOD = peek;
    }

    private PipScissors() {
    }

    public static @Nullable ScreenRectangle peek(GuiGraphicsExtractor guiGraphics) {
        if (SCISSOR_STACK_FIELD == null || PEEK_METHOD == null) {
            return null;
        }
        try {
            Object stack = SCISSOR_STACK_FIELD.get(guiGraphics);
            return stack == null ? null : (ScreenRectangle) PEEK_METHOD.invoke(stack);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
