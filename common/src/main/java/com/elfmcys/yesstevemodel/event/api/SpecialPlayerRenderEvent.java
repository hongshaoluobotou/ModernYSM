package com.elfmcys.yesstevemodel.event.api;

import com.elfmcys.yesstevemodel.client.entity.CustomPlayerEntity;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class SpecialPlayerRenderEvent {

    public static final Event<RenderHandler> EVENT = EventFactory.createArrayBacked(RenderHandler.class,
            listeners -> event -> {
                for (RenderHandler handler : listeners) {
                    if (!handler.onRender(event)) {
                        return false;
                    }
                }
                return true;
            });

    /**
     * @return true 表示继续渲染（原 EventResult.pass），false 表示取消。
     */
    @FunctionalInterface
    public interface RenderHandler {
        boolean onRender(SpecialPlayerRenderEvent event);
    }

    /**
     * @return true 表示继续渲染，false 表示被监听器取消（原 EventResult.isFalse()）。
     */
    public static boolean post(SpecialPlayerRenderEvent event) {
        return EVENT.invoker().onRender(event);
    }

    private final Player player;

    private final CustomPlayerEntity customPlayer;

    private final String modelId;

    @Nullable
    private ResourceLocation textureLocation;

    public SpecialPlayerRenderEvent() {
        this.player = null;
        this.customPlayer = null;
        this.modelId = null;
    }

    public SpecialPlayerRenderEvent(Player player, CustomPlayerEntity customPlayer, String str) {
        this.player = player;
        this.customPlayer = customPlayer;
        this.modelId = str;
    }

    public Player getPlayer() {
        return this.player;
    }

    public CustomPlayerEntity getCustomPlayer() {
        return this.customPlayer;
    }

    public String getModelId() {
        return this.modelId;
    }

    @Nullable
    public ResourceLocation getTextureLocation() {
        return this.textureLocation;
    }

    public void setTextureLocation(@Nullable ResourceLocation resourceLocation) {
        this.textureLocation = resourceLocation;
    }
}