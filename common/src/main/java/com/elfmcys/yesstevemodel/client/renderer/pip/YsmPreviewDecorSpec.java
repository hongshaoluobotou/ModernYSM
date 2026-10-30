package com.elfmcys.yesstevemodel.client.renderer.pip;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 动画测试界面（{@code PlayerTextureScreen} / {@code ModernPlayerTextureScreen}）的 3D 预览装饰内容。
 *
 * <p><b>1.20.1 语义</b>（{@code ModelPreviewRenderer#renderEntityPreview}）：除了模型本体，还会在
 * 同一套 model-view 变换下额外渲染</p>
 * <ul>
 *   <li>{@code renderBedPreview}：sleep 动画时模型身下的红床；</li>
 *   <li>{@code renderGroundPreview}：{@code showGround} 打开时脚下的 3×3 草方块 + 草丛 + 郁金香；</li>
 *   <li>{@code renderVehicleForAnimation}：ride / ride_pig / boat 动画时脚下的马 / 猪 / 船。</li>
 * </ul>
 * <p>它们的变换链都从"模型基座旋转之后"开始（1.20.1 里各自 {@code new PoseStack()}，但渲染时仍乘上
 * 当时的 model-view，故与模型共享坐标系），<b>且不受模型自身姿态预旋转影响</b>（sleep 的
 * {@code Ry(yaw-90)} 只乘在模型自己的 PoseStack 上）。因此最终提交时用
 * {@link YsmPreviewDecor}（= 本记录 + 基座变换）区分两条变换链。</p>
 *
 * @param bed        床的方块状态（仅 sleep 动画非空）
 * @param ground     是否画草地平台（对应 1.20.1 {@code renderGroundPreview} 的 renderGround 参数）
 * @param vehicle    载具实体（马 / 猪 / 船，仅 ride 系动画非空；已由 {@code AnimatableCacheUtil} 缓存）
 * @param vehicleY   载具相对脚底锚点的竖直偏移（1.20.1：{@code -(vehicle.getPassengersRidingOffset()
 *                   + rider.getMyRidingOffset())}；26.3 用
 *                   {@code -(vehicle.getPassengerRidingPosition(rider).y - vehicle.getY())}）
 * @param yawDeg     1.20.1 {@code renderEntityPreview} 的 yaw 参数（= {@code -bodyYawDeg - 180}）
 */
public record YsmPreviewDecorSpec(
        @Nullable BlockState bed,
        boolean ground,
        @Nullable Entity vehicle,
        float vehicleY,
        float yawDeg
) {
}
