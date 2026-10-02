package com.elfmcys.yesstevemodel.client.texture;

import rip.ysm.compat.oculus.ShadersTextureType;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.elfmcys.yesstevemodel.resource.YSMFolderDeserializer;
import rip.ysm.imagestream.avif.AvifDecoder;
import rip.ysm.imagestream.webp.WebpDecoder;
import com.mojang.renderpearl.api.GpuFormat;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import com.mojang.renderpearl.api.textures.GpuTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
        // 26.3 port 修复（作者头像卡死）：1.20.1 的 doLoad 同样只支持 PNG，但 1.20.1 blit 一个未加载的
        // AbstractTexture 只会画空白，而 26.3 AbstractTexture.getTextureView 直接抛
        // "Texture view does not exist" → GUI 渲染每帧崩（真机表现为界面卡死）。
        // YSMClientMapper.toTexture 对 BMP(1)/JPEG(3) 直接透传原始字节，这里补 ImageIO/WebP/AVIF 解码，
        // 失败则保持 texture==null，由 UploadManager 侧降级为不注册（调用点回退默认头像）。
        if (this.texture != null && this.textureView != null) {
            return;
        }
        try (NativeImage imageIn = readImage(data)) {
            if (imageIn == null) {
                return;
            }
            var device = RenderSystem.getDevice();
            String label = "OuterFileTexture";
            this.texture = device.createTexture(label, GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST, GpuFormat.RGBA8_UNORM,
                    imageIn.getWidth(), imageIn.getHeight(), 1, 1);
            this.sampler = RenderSystem.getSamplerCache().getRepeat(com.mojang.renderpearl.api.textures.FilterMode.NEAREST);
            this.textureView = device.createTextureView(this.texture);
            device.createCommandEncoder().writeToTexture(this.texture, imageIn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isLoaded() {
        return this.texture != null && this.textureView != null;
    }

    @Nullable
    private static NativeImage readImage(byte[] data) {
        try {
            return NativeImage.read(new ByteArrayInputStream(data));
        } catch (IOException ignored) {
            // 非 PNG：按模型包资源同款解码链转 NativeImage
        }
        int format = YSMFolderDeserializer.detectFormat(data);
        BufferedImage img = null;
        try {
            switch (format) {
                case 1, 3 -> img = ImageIO.read(new ByteArrayInputStream(data));
                case 4 -> img = new WebpDecoder().read(data);
                case 5 -> img = new AvifDecoder().read(data);
                default -> {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        if (img == null) {
            return null;
        }
        NativeImage image = new NativeImage(img.getWidth(), img.getHeight(), false);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                // BufferedImage.getRGB 返回 ARGB；setPixel 内部转换为 NativeImage 的 ABGR 存储。
                image.setPixel(x, y, img.getRGB(x, y));
            }
        }
        return image;
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
