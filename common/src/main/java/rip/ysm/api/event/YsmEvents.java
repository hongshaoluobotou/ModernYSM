package rip.ysm.api.event;

/**
 * {@link YsmEvent} 的构造入口（替代 Fabric API 的 {@code EventFactory}）。
 */
public final class YsmEvents {

    private YsmEvents() {
    }

    /**
     * 创建一个数组回调事件。与 Fabric 的 {@code EventFactory.createArrayBacked} 语义相同：
     * 有监听器时按注册顺序依次调用，监听器可为回调返回值做聚合（由工厂决定）。
     *
     * @param type    回调接口类型
     * @param factory 监听器数组 → 派发器
     */
    public static <T> YsmEvent<T> createArrayBacked(Class<T> type, YsmEvent.InvokerFactory<T> factory) {
        return new YsmEvent<>(type, factory);
    }
}
