package com.elfmcys.yesstevemodel.client.renderer;

/** 将 GUI 预览的所有权绑定到等待 PiP 延迟提交的渲染状态。 */
public interface PreviewRenderStateAccess {
    // Object 只表达所有权；渲染器仍通过现有路径获取具体类型的能力对象。
    void ysm$retainPreviewOwner(Object previewOwner);
}
