package rip.ysm.zstd;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ZstdBudgetTest {
    @Test
    void compressionRoundTripsOrdinaryAndOffsetInputs() {
        byte[] input = new byte[4096];
        for (int i = 0; i < input.length; i++) input[i] = (byte) (i % 31);
        assertArrayEquals(input, ZstdUtil.decompress(ZstdUtil.compress(input, 3)));
        assertArrayEquals(Arrays.copyOfRange(input, 37, 1037), ZstdUtil.decompress(ZstdUtil.compress(input, 37, 1000, 4)));
    }

    @Test
    void knownAndUnknownFramesRespectTheSameOutputLimit() {
        byte[] expected = new byte[128];
        Arrays.fill(expected, (byte) 42);
        for (boolean known : new boolean[]{true, false}) {
            byte[] frame = rawFrame(expected, known);
            assertArrayEquals(expected, ZstdUtil.decompress(frame, 0, frame.length, expected.length));
            assertThrows(MalformedInputException.class, () -> ZstdUtil.decompress(frame, 0, frame.length, expected.length - 1));
        }
    }

    @Test
    void unknownSizeConcatenatedFramesHaveOneCumulativeOutputLimit() {
        byte[] first = rawFrame(new byte[80], false);
        byte[] second = rawFrame(new byte[80], true);
        byte[] input = concat(first, second);
        assertEquals(160, ZstdUtil.decompress(input, 0, input.length, 160).length);
        assertThrows(MalformedInputException.class, () -> ZstdUtil.decompress(input, 0, input.length, 159));
    }

    @Test
    void oversizedDeclarationsAndUnsignedLengthsAreRejectedBeforeAllocation() {
        for (long declared : new long[]{(long) ZstdUtil.MAX_DECOMPRESSED_SIZE + 1, 1L << 32, Long.MAX_VALUE, Long.MIN_VALUE}) {
            byte[] input = singleSegmentHeader(declared);
            assertThrows(MalformedInputException.class, () -> ZstdUtil.decompress(input));
            byte[] stream = concat(rawFrame(new byte[1], false), input);
            assertThrows(MalformedInputException.class, () -> ZstdUtil.decompress(stream));
        }
    }

    @Test
    void windowDescriptorAndInvalidSliceAreCheckedBeforeUnsafeReads() {
        // Unknown content size, deliberately unsupported window; no compressed body or large allocation.
        byte[] frame = new byte[]{0x28, (byte) 0xb5, 0x2f, (byte) 0xfd, 0, (byte) 0xf8};
        assertThrows(MalformedInputException.class, () -> ZstdUtil.decompress(frame));
        assertThrows(IndexOutOfBoundsException.class, () -> ZstdUtil.decompress(frame, Integer.MAX_VALUE, 1));
        assertThrows(IllegalArgumentException.class, () -> new ZstdDecompressor().getDecompressedSize(frame, 1, Integer.MAX_VALUE));
    }

    @Test
    void validLargeSingleSegmentHeaderIsNotConfusedWithStreamingWindowLimit() {
        long size = 16L * 1024 * 1024;
        byte[] header = singleSegmentHeader(size);
        // 只读取帧头，不实际分配大样本；单帧已知输出仍允许高于流式 8 MiB 窗口的声明。
        assertEquals(size, new ZstdDecompressor().getDecompressedSize(header, 0, header.length));
    }

    private static byte[] rawFrame(byte[] data, boolean known) {
        if (data.length > 255) throw new IllegalArgumentException();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.writeBytes(new byte[]{0x28, (byte) 0xb5, 0x2f, (byte) 0xfd});
        output.write(known ? 0x20 : 0x00);
        output.write(known ? data.length : 0x00); // 未知长度使用 1 KiB 窗口。
        int block = (data.length << 3) | 1;
        output.write(block & 255);
        output.write((block >>> 8) & 255);
        output.write((block >>> 16) & 255);
        output.writeBytes(data);
        return output.toByteArray();
    }

    private static byte[] singleSegmentHeader(long declared) {
        return ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(0xfd2fb528).put((byte) 0xe0).putLong(declared).array();
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
