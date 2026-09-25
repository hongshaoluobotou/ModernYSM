package com.elfmcys.yesstevemodel.client.texture;

import rip.ysm.compat.oculus.ShadersTextureType;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import com.mojang.renderpearl.api.textures.GpuTexture;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * 从外部文件（YSM 模型包）加载的纹理。
 * 26.3 渲染体系：AbstractTexture 仅持有 GpuTexture/GpuTextureView/GpuSampler，
 * 需通过 GpuDevice.createTexture + CommandEncoder.writeToTexture 上传像素。
 */
public class OuterFileTexture extends AbstractTexture implements ITextureMap {
    private final byte[] data;

    private Map<ShadersTextureType, OuterFileTexture> suffixTextures = Reference2ReferenceMaps.emptyMap();

    public OuterFileTexture(byte[] data) {
        this.data = data;
    }

    /**
     * 26.3：AbstractTexture 已无 load(ResourceManager) 钩子。
     * 外部文件纹理的 GPU 上传在 doLoad() 中进行，由需要时显式调用。
     */
    public void load(ResourceManager resourceManager) {
        // 26.3：RenderSystem.recordRenderCall / isOnRenderThreadOrInit 已移除
        if (!RenderSystem.isOnRenderThread()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null) {
                minecraft.execute(this::doLoad);
            } else {
                doLoad();
            }
        } else {
            doLoad();
        }
    }

    public void doLoad() {
        try (NativeImage imageIn = NativeImage.read(new ByteArrayInputStream(data))) {
            var device = RenderSystem.getDevice();
            String label = "OuterFileTexture";
            this.texture = device.createTexture(label, GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST, GpuFormat.RGBA8_UNORM,
                    imageIn.getWidth(), imageIn.getHeight(), 1, 1);
            this.sampler = RenderSystem.getSamplerCache().getRepeat(com.mojang.renderpearl.api.textures.FilterMode.NEAREST);
            this.textureView = device.createTextureView(this.texture);
            device.createCommandEncoder().writeToTexture(this.texture, imageIn);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 26.3：确保 GPU 上传已完成（幂等）。必须在渲染线程调用（如 UploadManager.registerTexture），
     * 若在非渲染线程调用则调度到渲染线程执行。
     */
    public void ensureLoaded() {
        if (this.texture != null && this.textureView != null) {
            return;
        }
        if (RenderSystem.isOnRenderThread()) {
            doLoad();
        } else {
            Minecraft.getInstance().execute(this::doLoad);
        }
    }

    public void setSuffixTextures(Map<ShadersTextureType, OuterFileTexture> map) {
        this.suffixTextures = Reference2ReferenceMaps.unmodifiable(new Reference2ReferenceOpenHashMap<>(map));
    }

    public Map<ShadersTextureType, ? extends AbstractTexture> getSuffixTextures() {
        return this.suffixTextures;
    }
}
