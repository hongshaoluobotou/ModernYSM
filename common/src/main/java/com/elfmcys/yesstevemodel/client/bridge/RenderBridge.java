package com.elfmcys.yesstevemodel.client.bridge;

/**
 * 渲染层（client.renderer / rip.ysm.gpu，26.3 移植期间被排除编译）与
 * 动画/命令/数据层之间的状态桥。
 *
 * <p>26.3 移植说明：原代码直接静态调用 {@code AnimationDebugOverlay.isDebugActive()} 与
 * {@code ModelPreviewRenderer.isFirstPerson()/isExtraPlayer()/isPreview()/isFirstPersonOnRenderThread()}。
 * 渲染层恢复后，应在初始化时把这些真实状态回写到本桥（setter），保持数据层零渲染依赖。</p>
 */
public final class RenderBridge {
    private RenderBridge() {
    }

    /** 动画调试（molang watch）是否激活。由 AnimationDebugOverlay 回写。 */
    public static volatile boolean debugActive = false;

    /** 第一人称自定义模型渲染中。由 ModelPreviewRenderer 回写。 */
    public static volatile boolean firstPerson = false;

    /** 额外玩家（纸娃娃）预览渲染中。由 ModelPreviewRenderer 回写。 */
    public static volatile boolean extraPlayer = false;

    /** 模型预览界面渲染中。由 ModelPreviewRenderer 回写。 */
    public static volatile boolean preview = false;

    /** 渲染线程上的第一人称渲染。由 ModelPreviewRenderer 回写。 */
    public static volatile boolean firstPersonOnRenderThread = false;
}
