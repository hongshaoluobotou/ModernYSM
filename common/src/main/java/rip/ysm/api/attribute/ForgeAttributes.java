package rip.ysm.api.attribute;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.jetbrains.annotations.Nullable;

public final class ForgeAttributes {

    private ForgeAttributes() {
    }

    @Nullable
    public static Attribute blockReach() {
        return rip.ysm.api.attribute.impl.ForgeAttributesImpl.blockReach();
    }

    @Nullable
    public static Attribute entityReach() {
        return rip.ysm.api.attribute.impl.ForgeAttributesImpl.entityReach();
    }

    @Nullable
    public static Attribute swimSpeed() {
        return rip.ysm.api.attribute.impl.ForgeAttributesImpl.swimSpeed();
    }

    @Nullable
    public static Attribute entityGravity() {
        return rip.ysm.api.attribute.impl.ForgeAttributesImpl.entityGravity();
    }

    @Nullable
    public static Attribute stepHeightAddition() {
        return rip.ysm.api.attribute.impl.ForgeAttributesImpl.stepHeightAddition();
    }

    @Nullable
    public static Attribute nametagDistance() {
        return rip.ysm.api.attribute.impl.ForgeAttributesImpl.nametagDistance();
    }

    public static double getValue(LivingEntity entity, @Nullable Attribute attribute, double defaultValue) {
        if (attribute == null) {
            return defaultValue;
        }
        return entity.getAttributeValue(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute)); // TODO port: 26.3 属性走 Holder
    }
}
