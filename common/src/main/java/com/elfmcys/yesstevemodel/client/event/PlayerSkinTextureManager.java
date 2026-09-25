package com.elfmcys.yesstevemodel.client.event;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.event.api.SpecialPlayerRenderEvent;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import rip.ysm.api.PlatformAPI;
import java.util.Map;

public class PlayerSkinTextureManager {

    private static final Identifier STEVE_SKIN = Identifier.parse("textures/entity/player/wide/steve.png");

    private static final Identifier ALEX_SKIN = Identifier.parse("textures/entity/player/slim/alex.png");

    private static final String STEVE_TEXTURE_ID = "misc/2_steve";

    private static final String ALEX_TEXTURE_ID = "misc/1_alex";

    private PlayerSkinTextureManager() {
    }

    public static void register() {
        if (PlatformAPI.isServer()) {
            return;
        }
        SpecialPlayerRenderEvent.EVENT.register(PlayerSkinTextureManager::onRenderTexture);
    }

    private static boolean onRenderTexture(SpecialPlayerRenderEvent event) {
        Identifier location;
        if (!YesSteveModel.isAvailable()) {
            return true;
        }
        Player player = event.getPlayer();
        if (isDefaultSkin(event.getModelId()) && (player instanceof AbstractClientPlayer abstractClientPlayer)) {
            // 26.3 port: SkinManager.getInsecureSkinInformation/registerTexture 已删除，改用 AbstractClientPlayer#getSkin（PlayerSkin record）
            location = abstractClientPlayer.getSkin().body().texturePath();
            event.setTextureLocation(location);
        }
        return true;
    }

    private static boolean isDefaultSkin(String str) {
        return str.equals(STEVE_TEXTURE_ID) || str.equals(ALEX_TEXTURE_ID);
    }

    private static Identifier getSkinTexture(String str) {
        return str.equals(STEVE_TEXTURE_ID) ? STEVE_SKIN : ALEX_SKIN;
    }
}
