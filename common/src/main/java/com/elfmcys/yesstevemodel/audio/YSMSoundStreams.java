package com.elfmcys.yesstevemodel.audio;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;

/**
 * 自定义音频流（模型包里的语音/音乐）的注入点：只有 NeoForge / Forge 用得上。
 *
 * <p>fabric 有 {@code FabricSoundInstance} 接口 + fabric-api 自带的 {@code SoundEngineMixin}
 * 把 {@code SoundEngine#play} 里的 {@code soundBuffers.getStream(...)} 改道到
 * {@code SoundInstance#getAudioStream(...)}；另外两个加载器没有等价物，只能自己拦：</p>
 *
 * <p>各加载器模块里的 {@code SoundEngineMixin} 用 {@code @WrapOperation} 包住那唯一一处
 * {@code SoundBufferLibrary#getStream(Identifier, boolean)} 调用，然后调到这里。</p>
 *
 * <p>为什么要拦：{@code ModSoundEvents.CUSTOM_SOUND} 在 {@code sounds.json} 里声明了
 * {@code "stream": true}，所以原版一定会走流式分支去读 {@code yes_steve_model:empty}
 * 这个空占位 ogg；真正的音频数据由 {@link YSMSoundInstance} 现场解码提供
 * （{@link IAudioStreamFactory}），不拦截就只会放出静音。</p>
 */
public final class YSMSoundStreams {

    private YSMSoundStreams() {
    }

    /**
     * {@code SoundEngine#play} 里 {@code getStream} 调用的替代品。
     *
     * @param library  原版的声音缓冲库（未拦截时照原样调用）
     * @param path     原版要读的路径（我们不使用，但必须传给原调用以保持语义）
     * @param looping  原版算出来的自动循环标志
     * @param instance 发起播放的声音实例
     */
    public static CompletableFuture<AudioStream> getStream(SoundBufferLibrary library, Identifier path, boolean looping, SoundInstance instance) {
        if (instance instanceof YSMSoundInstance ysm) {
            // 与 fabric 侧 YSMSoundInstanceFabricMixin 完全一致：Sound 参数不用，传 null
            return ysm.getStream(library, null, looping);
        }
        return library.getStream(path, looping);
    }
}
