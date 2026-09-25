package rip.ysm.api.client.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

public final class KeyMappingFactoryImpl {

    private KeyMappingFactoryImpl() {
    }

    // TODO port: 26.3 KeyMapping 的 category 参数从 String 变为 KeyMapping.Category record；
    // 旧资源键 "key.category.yes_steve_model" 暂映射到 MISC，运行期如需自定义分类需再适配。
    public static KeyMapping createInGameAlt(String name, InputConstants.Type type, int keyCode, String category) {
        return new KeyMapping(name, type, keyCode, KeyMapping.Category.MISC);
    }

    public static KeyMapping createInGameNone(String name, InputConstants.Type type, int keyCode, String category) {
        return new KeyMapping(name, type, keyCode, KeyMapping.Category.MISC);
    }

    public static boolean isActiveAndMatches(KeyMapping keyMapping, int keyCode, int scanCode) {
        // TODO port: 26.3 KeyMapping.matches 接收 KeyEvent record
        return keyMapping.matches(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, 0));
    }
}
