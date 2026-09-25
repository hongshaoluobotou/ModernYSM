package rip.ysm.compat.gun.tacz;

// TODO port: stub, 原实现依赖 1.20.1 的 mod jar，恢复 compat 时替换（真实现见 common/src/main/java/rip/ysm/compat，被构建排除）

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import rip.ysm.compat.gun.swarfare.SWarfareCompat;

public class ConditionTAC {

    private static final String EMPTY = "";

    private final ObjectOpenHashSet<String> nameTest = new ObjectOpenHashSet<>();

    private final ObjectOpenHashSet<Identifier> idTest = new ObjectOpenHashSet<>();

    public void addTest(String name) {
        if (!name.startsWith("tac:") || !name.contains("$")) {
            return;
        }
        String[] strArrSplit = StringUtils.split(name, "$", 2);
        if (strArrSplit.length < 2) {
            return;
        }
        String str2 = strArrSplit[1];
        if (Identifier.tryParse(str2) != null) {
            this.nameTest.add(name);
            this.idTest.add(Identifier.tryParse(str2));
        }
    }

    public String doTest(ItemStack itemStack, String str) {
        if (itemStack.isEmpty()) {
            return EMPTY;
        }
        Identifier gunId = TacCompat.getGunTexture(itemStack);
        if (gunId == null) {
            gunId = SWarfareCompat.getGunTexture(itemStack);
            if (gunId == null) {
                return EMPTY;
            }
        }
        if (this.idTest.contains(gunId)) {
            String str2 = str.substring(0, str.length() - 1) + "$" + gunId;
            if (this.nameTest.contains(str2)) {
                return str2;
            }
            return EMPTY;
        }
        return EMPTY;
    }
}
