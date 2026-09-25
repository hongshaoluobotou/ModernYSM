package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.YesSteveModel;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import rip.ysm.api.PlatformAPI;
import rip.ysm.compat.sbackpack.SBackpackCompat;

public class RendererManager {

    private static CustomPlayerRenderer playerRenderer;

    private static ProjectileRenderer projectileRenderer;

    private static HandItemRenderer handRenderer;

    private static VehicleRenderer vehicleRenderer;

    private RendererManager() {
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        ResourceManagerReloadListener listener = resourceManager -> resetRenderers();
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Identifier.fromNamespaceAndPath(YesSteveModel.MOD_ID, "renderer_manager"), listener);
    }

    private static void resetRenderers() {
        playerRenderer = null;
        projectileRenderer = null;
        handRenderer = null;
        vehicleRenderer = null;
    }

    private static void initRenderers(ResourceManager resourceManager) {
        if (!YesSteveModel.isAvailable()) {
            return;
        }
        // 26.3 port: EntityRendererProvider.Context 构造参数已扩充（MapRenderer/EquipmentAssetManager/
        // AtlasManager/PlayerSkinRenderCache/PalettedTextureManager 等），按 Minecraft 单例逐个补齐。
        EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        EntityRendererProvider.Context context = new EntityRendererProvider.Context(
                entityRenderDispatcher,
                new net.minecraft.client.renderer.block.BlockModelResolver(Minecraft.getInstance().getModelManager()),
                Minecraft.getInstance().getItemModelResolver(),
                Minecraft.getInstance().getMapRenderer(),
                resourceManager,
                Minecraft.getInstance().getEntityModels(),
                // 26.3 port: Minecraft 未暴露 EquipmentAssetManager；本渲染器不使用装备资产渲染，传空实例即可。
                new net.minecraft.client.resources.model.EquipmentAssetManager(),
                Minecraft.getInstance().getAtlasManager(),
                Minecraft.getInstance().font,
                Minecraft.getInstance().playerSkinRenderCache(),
                Minecraft.getInstance().getPalettedTextureManager());
        playerRenderer = new CustomPlayerRenderer(context);
        projectileRenderer = new ProjectileRenderer(context);
        handRenderer = new HandItemRenderer();
        vehicleRenderer = new VehicleRenderer(context);
        SBackpackCompat.setupRenderLayers();
    }

    public static CustomPlayerRenderer getPlayerRenderer() {
        if (playerRenderer == null) {
            initRenderers(Minecraft.getInstance().getResourceManager());
        }
        return playerRenderer;
    }

    public static ProjectileRenderer getProjectileRenderer() {
        if (projectileRenderer == null) {
            initRenderers(Minecraft.getInstance().getResourceManager());
        }
        return projectileRenderer;
    }

    public static HandItemRenderer getHandRenderer() {
        if (handRenderer == null) {
            initRenderers(Minecraft.getInstance().getResourceManager());
        }
        return handRenderer;
    }

    public static VehicleRenderer getVehicleRenderer() {
        if (vehicleRenderer == null) {
            initRenderers(Minecraft.getInstance().getResourceManager());
        }
        return vehicleRenderer;
    }
}
