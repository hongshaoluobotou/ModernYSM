package com.elfmcys.yesstevemodel.client.renderer.pip;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

/**
 * 预览装饰的最终提交形态：装饰内容（{@link YsmPreviewDecorSpec}）+ <b>装饰自己的基座变换</b>。
 *
 * <p>基座变换 = 1.20.1 里模型 PoseStack 处于 {@code ... · S(scale) · T(0, 0.8, 0) · Rz(180) · Rx(pitch)}
 * 之后、模型自身姿态（sleep 的 {@code Ry(yaw-90)}、sit/ride 的平移）<b>之前</b>的状态——因为
 * 1.20.1 的三处装饰都是各起一个 PoseStack，不吃模型自身的姿态变换。</p>
 *
 * @param spec        装饰内容
 * @param translation 基座平移（脚底锚点，不含模型姿态偏移）
 * @param rotation    基座旋转（{@code Rz(180)·Rx(pitch)}，不含模型姿态预旋转）
 */
public record YsmPreviewDecor(
        YsmPreviewDecorSpec spec,
        Vector3fc translation,
        Quaternionfc rotation
) {

    public @Nullable BlockState bed() {
        return this.spec.bed();
    }

    public boolean ground() {
        return this.spec.ground();
    }

    public @Nullable Entity vehicle() {
        return this.spec.vehicle();
    }

    public float vehicleY() {
        return this.spec.vehicleY();
    }

    public float yawDeg() {
        return this.spec.yawDeg();
    }
}
