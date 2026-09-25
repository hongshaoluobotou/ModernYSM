package rip.ysm.api.config;

import com.elfmcys.yesstevemodel.config.ConfigSpec;

public final class ConfigRegistration {

    private ConfigRegistration() {
    }
    public static void register(String modId, ConfigSpec spec) {
        rip.ysm.api.config.fabric.ConfigRegistrationImpl.register(modId, spec);
    }
}
