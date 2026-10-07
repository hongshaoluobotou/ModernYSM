package rip.ysm.api.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FragmentReassemblerTest {
    @Test
    void incompleteTransfersAreLimitedAndIdleCleanupDoesNotNeedMorePackets() {
        FragmentReassembler<Object> reassembler = new FragmentReassembler<>();
        Object connection = new Object();
        for (int id = 0; id < 4; id++) {
            assertNull(reassembler.accept(connection, id, 0, 2, new byte[]{1}, 0));
        }
        assertThrows(IllegalArgumentException.class, () -> reassembler.accept(connection, 4, 0, 2, new byte[]{1}, 0));
        assertEquals(4, reassembler.transferCount());
        reassembler.expire(30_000_000_000L);
        assertEquals(0, reassembler.transferCount());
        assertEquals(0, reassembler.bufferedBytes());
    }


    @Test
    void globalTransferLimitAppliesAcrossConnections() {
        FragmentReassembler<Object> reassembler = new FragmentReassembler<>();
        for (int i = 0; i < 128; i++) {
            reassembler.accept(new Object(), 0, 0, 2, new byte[]{1}, 0);
        }
        assertThrows(IllegalArgumentException.class, () -> reassembler.accept(new Object(), 0, 0, 2, new byte[]{1}, 0));
        assertEquals(128, reassembler.bufferedBytes());
        reassembler.expire(30_000_000_000L);
        assertEquals(0, reassembler.bufferedBytes());
    }


    @Test
    void outOfOrderAndDuplicateFragmentsWorkAndCompletionReleasesBudget() {
        FragmentReassembler<String> reassembler = new FragmentReassembler<>();
        assertNull(reassembler.accept("one", 1, 1, 2, new byte[]{2}, 0));
        assertNull(reassembler.accept("one", 1, 1, 2, new byte[]{2}, 1));
        assertArrayEquals(new byte[]{1, 2}, reassembler.accept("one", 1, 0, 2, new byte[]{1}, 2));
        assertEquals(0, reassembler.bufferedBytes());
        assertEquals(0, reassembler.transferCount());
    }


    @Test
    void invalidMetadataDropsOnlyItsOwnTransferAndDisconnectReleasesTheRest() {
        FragmentReassembler<String> reassembler = new FragmentReassembler<>();
        reassembler.accept("one", 1, 0, 2, new byte[]{1}, 0);
        reassembler.accept("one", 2, 0, 2, new byte[]{2}, 0);
        assertThrows(IllegalArgumentException.class, () -> reassembler.accept("one", 1, 1, 3, new byte[]{1}, 0));
        assertEquals(1, reassembler.bufferedBytes());
        assertThrows(IllegalArgumentException.class, () -> reassembler.accept("one", 2, 0, 2, new byte[]{3}, 0));
        assertEquals(0, reassembler.transferCount());
        reassembler.accept("one", 3, 0, 2, new byte[]{1}, 0);
        reassembler.removeConnection("one");
        assertEquals(0, reassembler.bufferedBytes());
    }


    @Test
    void duplicateFragmentsDoNotExtendExpiryAndActiveTransfersHaveAbsoluteLifetime() {
        FragmentReassembler<String> reassembler = new FragmentReassembler<>();
        reassembler.accept("one", 1, 0, 4, new byte[]{1}, 0);
        reassembler.accept("one", 1, 0, 4, new byte[]{1}, 29_000_000_000L);
        reassembler.expire(30_000_000_000L);
        assertEquals(0, reassembler.transferCount());
        reassembler.accept("one", 2, 0, 4, new byte[]{1}, 0);
        reassembler.accept("one", 2, 1, 4, new byte[]{2}, 25_000_000_000L);
        reassembler.accept("one", 2, 2, 4, new byte[]{3}, 50_000_000_000L);
        reassembler.expire(60_000_000_000L);
        assertEquals(0, reassembler.transferCount());
    }


    @Test
    void perPacketSizeLimitReleasesAccumulatedBytes() {
        FragmentReassembler<String> reassembler = new FragmentReassembler<>();
        // 约 2 MiB 的有界本地 fixture，不建立网络连接。
        for (int i = 0; i < 69; i++) {
            reassembler.accept("one", 1, i, 71, new byte[30_000], 0);
        }
        assertThrows(IllegalArgumentException.class, () -> reassembler.accept("one", 1, 69, 71, new byte[30_000], 0));
        assertEquals(0, reassembler.bufferedBytes());
    }


    @Test
    void cumulativeConnectionAndGlobalByteLimitsApplyBeforeRetainingData() {
        FragmentReassembler<String> reassembler = new FragmentReassembler<>(4, 6);
        reassembler.accept("one", 1, 0, 2, new byte[3], 0);
        assertThrows(IllegalArgumentException.class, () -> reassembler.accept("one", 2, 0, 2, new byte[2], 0));
        assertEquals(3, reassembler.bufferedBytes());
        reassembler.accept("two", 1, 0, 2, new byte[3], 0);
        assertThrows(IllegalArgumentException.class, () -> reassembler.accept("three", 1, 0, 2, new byte[1], 0));
        assertEquals(6, reassembler.bufferedBytes());
        reassembler.removeConnection("one");
        reassembler.accept("three", 1, 0, 2, new byte[1], 0);
        assertEquals(4, reassembler.bufferedBytes());
    }


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

}
