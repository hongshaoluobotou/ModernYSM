package rip.ysm.api.config.fabric;

import com.elfmcys.yesstevemodel.config.ConfigSpec;

public final class ConfigRegistrationImpl {

    private ConfigRegistrationImpl() {
    }

    public static void register(String modId, ConfigSpec spec) {
        // TOML 文件在 ConfigSpec.builder(path) 时已加载并开启 autosave，此处仅保留注册语义。
    }
}
