package rip.ysm.api.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

public final class ConfigRegistration {

    private ConfigRegistration() {
    }
    public static void register(String modId, ModConfig.Type type, ForgeConfigSpec spec) {
        rip.ysm.api.config.fabric.ConfigRegistrationImpl.register(modId, type, spec);
    }
}
