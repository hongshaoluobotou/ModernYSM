package rip.ysm.api.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MolangNetworkBudgetTest {
    @Test
    void tokenRefillIsBoundedAndRejectsClockReversal() {
        TokenBucket bucket = new TokenBucket(2, 2, 0);
        assertTrue(bucket.tryConsume(2, 0));
        assertFalse(bucket.tryConsume(1, -1));
        assertFalse(bucket.tryConsume(1, 499_000_000));
        assertTrue(bucket.tryConsume(1, 500_000_000));
        assertFalse(bucket.tryConsume(3, 100_000_000_000L));
        assertTrue(bucket.tryConsume(2, 100_000_000_000L));
    }


    @Test
    void latestThrottledRequestIsEventuallyFlushedWithoutReplayingStaleValues() {
        LatestRequestLimiter<String> limiter = new LatestRequestLimiter<>(1, 20, 0);
        assertEquals("first", limiter.submit("first", 0));
        assertNull(limiter.submit("old", 0));
        assertNull(limiter.submit("final", 0));
        assertEquals("final", limiter.poll(50_000_000));
        assertNull(limiter.poll(100_000_000));
        assertEquals("newer", limiter.submit("newer", 100_000_000));
        assertNull(limiter.poll(150_000_000));
    }


    @Test
    void networkQueueKeepsFinalStateAndClearDiscardsOldWorldWork() {
        LatestBoundedQueue<Integer> queue = new LatestBoundedQueue<>(3);
        queue.offer(1);
        queue.offer(2);
        queue.offer(3);
        queue.offer(4);
        assertEquals(1, queue.poll());
        assertEquals(2, queue.poll());
        assertEquals(4, queue.poll());
        assertNull(queue.poll());
        queue.offer(5);
        queue.clear();
        assertNull(queue.poll());
    }
}
