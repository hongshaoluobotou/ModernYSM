package com.elfmcys.yesstevemodel.forge.mixin.client;

import com.elfmcys.yesstevemodel.audio.YSMSoundStreams;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.concurrent.CompletableFuture;

/**
 * 模型包语音/音乐（自定义 {@code AudioStream}）在 Forge 上的接入点。
 *
 * <p>三个加载器在这一处的差异（26.3 实测）：fabric 用 {@code FabricSoundInstance} 接口；
 * NeoForge 直接把 {@code SoundEngine#play} 改成调 {@code SoundInstance#getStream}（覆写即可，
 * 无需 mixin）；<b>只有 Forge 的 {@code play} 仍然直接读 {@code SoundBufferLibrary#getStream}</b>，
 * 因此只有本模块需要这个 mixin，逻辑复用 {@link YSMSoundStreams}。</p>
 *
 * <p>注意：Forge 的 mixin 配置只能写进 JAR manifest（见 forge/build.gradle），
 * dev 运行（Slime Launcher）不读 manifest，所以这个 mixin 在 dev 下不生效，需要打包后验证。</p>
 */
@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    @WrapOperation(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundBufferLibrary;getStream(Lnet/minecraft/resources/Identifier;Z)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<AudioStream> ysm$customAudioStream(SoundBufferLibrary library, Identifier path, boolean looping, Operation<CompletableFuture<AudioStream>> original, @Local(argsOnly = true) SoundInstance instance) {
        if (instance instanceof com.elfmcys.yesstevemodel.audio.YSMSoundInstance) {
            return YSMSoundStreams.getStream(library, path, looping, instance);
        }
        return original.call(library, path, looping);
    }
}
