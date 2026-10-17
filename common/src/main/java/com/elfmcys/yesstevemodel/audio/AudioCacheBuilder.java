package com.elfmcys.yesstevemodel.audio;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

public class AudioCacheBuilder {

    private final AudioStreamCache.CachedAudioStreamProvider cacheProvider;

    private final AudioTrackData trackData;

    private final ByteBuf audioBuffer;

    private final IntArrayList chunkSizes = new IntArrayList(5);

    private boolean isClosed = false;

    public AudioCacheBuilder(AudioStreamCache.CachedAudioStreamProvider cacheProvider, AudioTrackData trackData) {
        this.cacheProvider = cacheProvider;
        this.trackData = trackData;
        this.audioBuffer = PooledByteBufAllocator.DEFAULT.directBuffer(((int) trackData.getDuration()) * 2);
    }

    public void appendAudio(ByteBuffer byteBuffer) {
        if (!this.isClosed && this.audioBuffer.writableBytes() > 0) {
            int iMin = Math.min(this.audioBuffer.writableBytes(), byteBuffer.remaining());
            this.audioBuffer.writeBytes(byteBuffer.limit(iMin));
            this.chunkSizes.add(iMin);
        }
    }

    public void flushToCache() {
        if (!this.isClosed) {
            this.isClosed = true;
            ByteBuffer byteBuffer = BufferUtils.createByteBuffer(this.audioBuffer.readableBytes());
            this.audioBuffer.readBytes(byteBuffer.duplicate());
            this.audioBuffer.release();
            this.cacheProvider.cacheAudioData(this.trackData, byteBuffer, this.chunkSizes);
        }
    }

    /**
     * 流在到达 EOF 前被 stopAll/状态切换关闭时调用：释放池化缓冲，并撤销 pendingTracks 占位。
     * 否则该音轨会永久停在 pendingTracks 里（只走实时解码、永不缓存），且这份 pooled direct buffer 永不归还。
     */
    public void discard() {
        if (!this.isClosed) {
            this.isClosed = true;
            this.audioBuffer.release();
            this.cacheProvider.cancelCaching(this.trackData);
        }
    }
}