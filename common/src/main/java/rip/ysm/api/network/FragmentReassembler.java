package rip.ysm.api.network;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/** 分片存储和配额统一加锁，避免多连接并发时绕过全局预算。 */
public final class FragmentReassembler<K> {
    public static final int MAX_FRAGMENT_COUNT = 128;
    public static final int MAX_FRAGMENT_SIZE = 30_000;
    public static final int MAX_PACKET_SIZE = 2 * 1024 * 1024;
    private static final int MAX_CONNECTION_TRANSFERS = 4;
    private static final int MAX_GLOBAL_TRANSFERS = 128;
    private static final int MAX_CONNECTION_BYTES = 8 * 1024 * 1024;
    private static final int MAX_GLOBAL_BYTES = 64 * 1024 * 1024;
    private static final long BYTES_PER_SECOND = 16 * 1024 * 1024;
    private static final long IDLE_TIMEOUT = 30_000_000_000L;
    private static final long MAX_LIFETIME = 60_000_000_000L;

    private final Map<K, ConnectionTransfers> connections = new HashMap<>();
    // 空传输不保留连接；速率桶使用弱键，完成一个传输不能重置速率配额。
    private final Map<K, TokenBucket> rates = new WeakHashMap<>();
    private final int connectionByteLimit;
    private final int globalByteLimit;
    private int transferCount;
    private int bufferedBytes;

    public FragmentReassembler() {
        this(MAX_CONNECTION_BYTES, MAX_GLOBAL_BYTES);
    }

    // 允许测试用几个字节验证累计配额，不构造大负载。
    FragmentReassembler(int connectionByteLimit, int globalByteLimit) {
        if (connectionByteLimit <= 0 || globalByteLimit <= 0
                || connectionByteLimit > MAX_CONNECTION_BYTES || globalByteLimit > MAX_GLOBAL_BYTES) {
            throw new IllegalArgumentException("Invalid fragment byte budget");
        }
        this.connectionByteLimit = connectionByteLimit;
        this.globalByteLimit = globalByteLimit;
    }

    public synchronized byte[] accept(K connection, int transferId, int index, int count, byte[] data, long now) {
        ConnectionTransfers state = connections.get(connection);
        try {
            if (count <= 0 || count > MAX_FRAGMENT_COUNT || index < 0 || index >= count
                    || data.length == 0 || data.length > MAX_FRAGMENT_SIZE) {
                throw new IllegalArgumentException("Invalid YSM fragment metadata");
            }
            TokenBucket rate = rates.computeIfAbsent(connection, ignored -> new TokenBucket(BYTES_PER_SECOND, BYTES_PER_SECOND, now));
            if (!rate.tryConsume(data.length, now)) {
                throw new IllegalArgumentException("YSM fragment rate budget exceeded");
            }
            if (state == null) {
                state = new ConnectionTransfers();
            }
            Accumulator accumulator = state.transfers.get(transferId);
            if (accumulator == null) {
                if (state.transfers.size() >= MAX_CONNECTION_TRANSFERS || transferCount >= MAX_GLOBAL_TRANSFERS) {
                    throw new IllegalArgumentException("Too many incomplete YSM transfers");
                }
                accumulator = new Accumulator(count, now);
                state.transfers.put(transferId, accumulator);
                connections.put(connection, state);
                transferCount++;
            }
            if (count != accumulator.fragments.length) {
                throw new IllegalArgumentException("Inconsistent YSM fragment count");
            }
            if (now - accumulator.updated >= IDLE_TIMEOUT || now - accumulator.created >= MAX_LIFETIME) {
                throw new IllegalArgumentException("Expired YSM transfer");
            }
            // 重复分片不延长超时；不允许用不同内容覆盖已经确认的分片。
            if (accumulator.fragments[index] != null) {
                if (!java.util.Arrays.equals(accumulator.fragments[index], data)) {
                    throw new IllegalArgumentException("Conflicting YSM fragment");
                }
                return null;
            }
            if (data.length > MAX_PACKET_SIZE - accumulator.bytes
                    || data.length > connectionByteLimit - state.bytes
                    || data.length > globalByteLimit - bufferedBytes) {
                throw new IllegalArgumentException("YSM fragment memory budget exceeded");
            }
            accumulator.fragments[index] = data;
            accumulator.received++;
            accumulator.bytes += data.length;
            accumulator.updated = now;
            state.bytes += data.length;
            bufferedBytes += data.length;
            if (accumulator.received != count) {
                return null;
            }
            byte[] complete = new byte[accumulator.bytes];
            int offset = 0;
            for (byte[] fragment : accumulator.fragments) {
                System.arraycopy(fragment, 0, complete, offset, fragment.length);
                offset += fragment.length;
            }
            removeTransfer(connection, state, transferId);
            return complete;
        } catch (RuntimeException e) {
            if (state != null) {
                removeTransfer(connection, state, transferId);
            }
            throw e;
        }
    }

    public synchronized void expire(long now) {
        Iterator<Map.Entry<K, ConnectionTransfers>> connectionsIterator = connections.entrySet().iterator();
        while (connectionsIterator.hasNext()) {
            ConnectionTransfers state = connectionsIterator.next().getValue();
            Iterator<Accumulator> transfers = state.transfers.values().iterator();
            while (transfers.hasNext()) {
                Accumulator accumulator = transfers.next();
                if (now - accumulator.updated >= IDLE_TIMEOUT || now - accumulator.created >= MAX_LIFETIME) {
                    state.bytes -= accumulator.bytes;
                    bufferedBytes -= accumulator.bytes;
                    transferCount--;
                    transfers.remove();
                }
            }
            if (state.transfers.isEmpty()) {
                connectionsIterator.remove();
            }
        }
    }

    public synchronized void removeConnection(K connection) {
        ConnectionTransfers state = connections.remove(connection);
        if (state != null) {
            transferCount -= state.transfers.size();
            bufferedBytes -= state.bytes;
        }
        rates.remove(connection);
    }

    private void removeTransfer(K connection, ConnectionTransfers state, int transferId) {
        Accumulator accumulator = state.transfers.remove(transferId);
        if (accumulator != null) {
            state.bytes -= accumulator.bytes;
            bufferedBytes -= accumulator.bytes;
            transferCount--;
        }
        if (state.transfers.isEmpty()) {
            connections.remove(connection);
        }
    }

    synchronized int bufferedBytes() {
        return bufferedBytes;
    }

    synchronized int transferCount() {
        return transferCount;
    }

    private static final class ConnectionTransfers {
        final Map<Integer, Accumulator> transfers = new HashMap<>();
        int bytes;
    }

    private static final class Accumulator {
        final byte[][] fragments;
        final long created;
        long updated;
        int received;
        int bytes;

        Accumulator(int count, long now) {
            fragments = new byte[count][];
            created = updated = now;
        }
    }
}
