package rip.ysm.api.network;

/** 超额时只保留最新请求，空闲后的 tick 会补发最终状态；不保存无界请求队列。 */
public final class LatestRequestLimiter<T> {
    private final TokenBucket budget;
    private T pending;

    public LatestRequestLimiter(long burst, long perSecond, long now) {
        budget = new TokenBucket(burst, perSecond, now);
    }

    public synchronized T submit(T request, long now) {
        pending = request;
        return poll(now);
    }

    public synchronized T poll(long now) {
        if (pending == null || !budget.tryConsume(1, now)) {
            return null;
        }
        T result = pending;
        pending = null;
        return result;
    }
}
