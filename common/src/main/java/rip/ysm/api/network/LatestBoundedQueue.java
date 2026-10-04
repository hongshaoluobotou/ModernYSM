package rip.ysm.api.network;

import java.util.ArrayDeque;

/** 保留既有顺序，满队列只合并最后一项，确保最终状态不会因超额而丢失。 */
public final class LatestBoundedQueue<T> {
    private final int capacity;
    private final ArrayDeque<T> queue = new ArrayDeque<>();

    public LatestBoundedQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Queue capacity must be positive");
        this.capacity = capacity;
    }

    public synchronized void offer(T value) {
        if (queue.size() == capacity) queue.removeLast();
        queue.addLast(value);
    }

    public synchronized T poll() {
        return queue.pollFirst();
    }

    public synchronized void clear() {
        queue.clear();
    }
}
