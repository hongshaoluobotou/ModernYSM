package rip.ysm.api.network;

/** 按单调时钟补充的配额；调用方负责同步以及连接生命周期。 */
public final class TokenBucket {
    private final long capacity;
    private final double tokensPerNano;
    private double available;
    private long lastUpdate;

    public TokenBucket(long capacity, long tokensPerSecond, long now) {
        if (capacity <= 0 || tokensPerSecond <= 0) {
            throw new IllegalArgumentException("Token budget must be positive");
        }
        this.capacity = capacity;
        this.tokensPerNano = tokensPerSecond / 1_000_000_000.0;
        this.available = capacity;
        this.lastUpdate = now;
    }

    public boolean tryConsume(long count, long now) {
        if (count < 0) {
            throw new IllegalArgumentException("Negative token count");
        }
        long elapsed = now - lastUpdate;
        if (elapsed > 0) {
            available = Math.min(capacity, available + elapsed * tokensPerNano);
            lastUpdate = now;
        }
        if (count > available) {
            return false;
        }
        available -= count;
        return true;
    }
}
