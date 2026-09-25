package rip.ysm.api.client.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class KeyMappingFactoryImpl {

    // 26.3：KeyMapping 分类从 String 变为 KeyMapping.Category record，必须经 Category.register(Identifier)
    // 注册（会加入 Category.SORT_ORDER，fabric 的 KeyMappingCategoryMixin 注册后自动重排序），
    // 否则按键绑定界面按分类分组时无法正确显示。显示语言键：key.category.yes_steve_model.main。
    private static final KeyMapping.Category YSM_CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("yes_steve_model", "main"));

    private KeyMappingFactoryImpl() {
    }

    public static KeyMapping createInGameAlt(String name, InputConstants.Type type, int keyCode, String category) {
        return new KeyMapping(name, type, keyCode, YSM_CATEGORY);
    }

    public static KeyMapping createInGameNone(String name, InputConstants.Type type, int keyCode, String category) {
        return new KeyMapping(name, type, keyCode, YSM_CATEGORY);
    }

    public static boolean isActiveAndMatches(KeyMapping keyMapping, int keyCode, int scanCode) {
        // TODO port: 26.3 KeyMapping.matches 接收 KeyEvent record
        return keyMapping.matches(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, 0));
    }
}
