package rip.ysm.api.event;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 加载器无关的「数组回调」事件（替代 Fabric API 的 {@code Event}/{@code EventFactory}）。
 *
 * <p>用法与 Fabric 版一致：
 * <pre>{@code
 * public static final YsmEvent<RenderHandler> EVENT = YsmEvents.createArrayBacked(RenderHandler.class,
 *         listeners -> event -> { ... });
 * EVENT.register(handler);
 * EVENT.invoker().onRender(event);
 * }</pre>
 *
 * <p>invoker 在首次使用时构建，注册新监听器后自动失效重建。线程安全（写少读多）。
 *
 * @param <T> 回调接口类型（必须是只有一个抽象方法的接口）
 */
public final class YsmEvent<T> {

    /**
     * 由监听器数组构建 invoker 的工厂（对应 Fabric 的 {@code EventFactory.createArrayBacked} 第二参数）。
     */
    @FunctionalInterface
    public interface InvokerFactory<T> {
        T create(T[] listeners);
    }

    private final Class<T> type;
    private final InvokerFactory<T> factory;
    private final CopyOnWriteArrayList<T> listeners = new CopyOnWriteArrayList<>();
    private volatile T invoker;

    YsmEvent(Class<T> type, InvokerFactory<T> factory) {
        this.type = type;
        this.factory = factory;
    }

    public void register(T listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        listeners.add(listener);
        invoker = null;
    }

    public boolean unregister(T listener) {
        boolean removed = listeners.remove(listener);
        if (removed) {
            invoker = null;
        }
        return removed;
    }

    /**
     * @return 全部监听器的派发器；没有监听器时返回一个空实现（调用无副作用）。
     */
    public T invoker() {
        T result = invoker;
        if (result == null) {
            result = buildInvoker();
            invoker = result;
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private T buildInvoker() {
        T[] array = listeners.toArray((T[]) java.lang.reflect.Array.newInstance(type, 0));
        if (array.length == 0) {
            array = Arrays.copyOf(array, 0);
        }
        return factory.create(array);
    }
}
