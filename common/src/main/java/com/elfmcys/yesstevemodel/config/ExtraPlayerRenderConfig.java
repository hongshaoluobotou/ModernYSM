package com.elfmcys.yesstevemodel.config;

import com.elfmcys.yesstevemodel.config.ConfigSpec.BooleanValue;
import com.elfmcys.yesstevemodel.config.ConfigSpec.Builder;
import com.elfmcys.yesstevemodel.config.ConfigSpec.DoubleValue;
import com.elfmcys.yesstevemodel.config.ConfigSpec.IntValue;

public class ExtraPlayerRenderConfig {

    public static BooleanValue DISABLE_PLAYER_RENDER;

    public static BooleanValue DISABLE_PLAYER_RENDER_THIRD_PERSON;

    public static IntValue PLAYER_POS_X;

    public static IntValue PLAYER_POS_Y;

    public static DoubleValue PLAYER_SCALE;

    public static DoubleValue PLAYER_YAW_OFFSET;

    public static void define(Builder builder) {
        builder.push("extra_player_render");
        builder.comment("Whether to display player");
        DISABLE_PLAYER_RENDER = builder.define("DisablePlayerRender", false);
        builder.comment("Hide the HUD model preview while in third person view");
        DISABLE_PLAYER_RENDER_THIRD_PERSON = builder.define("DisablePlayerRenderThirdPerson", false);
        builder.comment("Player position x in screen");
        PLAYER_POS_X = builder.defineInRange("PlayerPosX", 10, 0, Integer.MAX_VALUE);
        builder.comment("Player position y in screen");
        PLAYER_POS_Y = builder.defineInRange("PlayerPosY", 10, 0, Integer.MAX_VALUE);
        builder.comment("Player scale in screen");
        PLAYER_SCALE = builder.defineInRange("PlayerScale", 40.0d, 8.0d, 360.0d);
        builder.comment("Player yaw offset in screen");
        PLAYER_YAW_OFFSET = builder.defineInRange("PlayerYawOffset", 5.0d, Double.MIN_VALUE, Double.MAX_VALUE);
        builder.pop();
    }
}
