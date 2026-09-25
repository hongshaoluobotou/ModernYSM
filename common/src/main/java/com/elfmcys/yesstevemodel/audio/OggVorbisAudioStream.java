package com.elfmcys.yesstevemodel.audio;

import net.minecraft.client.sounds.JOrbisAudioStream;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

// TODO port: 26.3 移除 com.mojang.blaze3d.audio.OggAudioStream，改用 JOrbisAudioStream（float 采样），这里手动转 16bit LE PCM 并做立体声→单声道混音
public class OggVorbisAudioStream implements IAudioStreamSupport {

    private static final ByteBuffer EMPTY_BUFFER = BufferUtils.createByteBuffer(0);

    private final JOrbisAudioStream oggStream;

    private final int sourceChannels;

    private final AudioFormat audioFormat;

    @Nullable
    private final AudioCacheBuilder cacheBuilder;

    private volatile boolean isClosed;

    private boolean isEndOfStream;

    public OggVorbisAudioStream(ByteBuffer byteBuffer, @Nullable AudioCacheBuilder cacheBuilder) throws UnsupportedAudioFileException, IOException {
        this.oggStream = new JOrbisAudioStream(new ByteBufInputStream(Unpooled.wrappedBuffer(byteBuffer)));
        AudioFormat sourceFormat = this.oggStream.getFormat();
        this.sourceChannels = sourceFormat.getChannels();
        if (this.sourceChannels != 1 && this.sourceChannels != 2) {
            throw new UnsupportedAudioFileException();
        }
        this.audioFormat = new AudioFormat(sourceFormat.getSampleRate(), 16, 1, true, false);
        this.cacheBuilder = cacheBuilder;
    }

    @NotNull
    public AudioFormat getFormat() {
        return this.audioFormat;
    }

    @NotNull
    public ByteBuffer read(int i) throws IOException {
        if (this.isEndOfStream || this.isClosed) {
            return EMPTY_BUFFER;
        }
        int needFloats = this.sourceChannels * Math.max(1, (i + 1) / 2);
        List<Float> samples = new ArrayList<>(needFloats);
        while (samples.size() < needFloats) {
            boolean chunk = this.oggStream.readChunk(samples::add);
            if (!chunk) {
                this.isEndOfStream = true;
                if (this.cacheBuilder != null) {
                    this.cacheBuilder.flushToCache();
                }
                break;
            }
        }
        if (samples.isEmpty()) {
            return EMPTY_BUFFER;
        }
        int monoSamples = samples.size() / this.sourceChannels;
        ByteBuffer buffer = BufferUtils.createByteBuffer(monoSamples * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (int idx = 0; idx < monoSamples; idx++) {
            float sample;
            if (this.sourceChannels == 2) {
                sample = (samples.get(idx * 2) + samples.get(idx * 2 + 1)) / 2.0f;
            } else {
                sample = samples.get(idx);
            }
            float clamped = Math.max(-1.0f, Math.min(1.0f, sample));
            buffer.putShort((short) Math.round(clamped * 32767.0f));
        }
        buffer.flip();
        if (this.cacheBuilder != null && !this.isEndOfStream) {
            this.cacheBuilder.appendAudio(buffer.duplicate());
        }
        return buffer;
    }

    public void close() throws IOException {
        if (!this.isClosed) {
            this.oggStream.close();
            this.isClosed = true;
        }
    }

    @Override
    public boolean isClosed() {
        return this.isClosed;
    }
}
