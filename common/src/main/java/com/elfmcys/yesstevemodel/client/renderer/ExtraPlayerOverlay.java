package com.elfmcys.yesstevemodel.client.renderer;

import com.elfmcys.yesstevemodel.client.gui.ExtraPlayerRenderScreen;
import com.elfmcys.yesstevemodel.config.ExtraPlayerRenderConfig;
import net.minecraft.client.DeltaTracker;
import rip.ysm.api.client.HudRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

public class ExtraPlayerOverlay implements HudRenderer {
    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;
        if (ExtraPlayerRenderConfig.DISABLE_PLAYER_RENDER.get() || localPlayer == null || (minecraft.gui.screen() instanceof ExtraPlayerRenderScreen)) {
            return;
        }
        if (ExtraPlayerRenderConfig.DISABLE_PLAYER_RENDER_THIRD_PERSON.get() && minecraft.options != null && !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        float partialTick = deltaTracker == null ? 0.0f : deltaTracker.getGameTimeDeltaPartialTick(false);
        ModelPreviewRenderer.renderPlayerOverlay(guiGraphics, localPlayer, ExtraPlayerRenderConfig.PLAYER_POS_X.get(), ExtraPlayerRenderConfig.PLAYER_POS_Y.get(), ExtraPlayerRenderConfig.PLAYER_SCALE.get().floatValue(), ExtraPlayerRenderConfig.PLAYER_YAW_OFFSET.get().floatValue(), -500, partialTick, ModelPreviewRenderer.PIP_KEY_PAPERDOLL_HUD);
    }
}
