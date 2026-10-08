package fr.moussax.blightedSMP.engine.entities.state;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.attribute.CraftAttribute;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;
import org.jspecify.annotations.NonNull;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Utility for dynamically injecting attributes into entities that do not register them by default.
 */
public final class EntityAttributeInjector {

    private static final VarHandle ATTRIBUTES_MAP_HANDLE;
    private static final Constructor<AttributeInstance> INSTANCE_CONSTRUCTOR;

    static {
        VarHandle attributesHandle = null;
        Constructor<AttributeInstance> ctor = null;
        try {
            Field attributesField = AttributeMap.class.getDeclaredField("attributes");
            attributesField.setAccessible(true);
            attributesHandle = MethodHandles.privateLookupIn(AttributeMap.class, MethodHandles.lookup())
                    .unreflectVarHandle(attributesField);

            ctor = AttributeInstance.class.getDeclaredConstructor(Holder.class, Consumer.class);
            ctor.setAccessible(true);
        } catch (ReflectiveOperationException _) {
        }
        ATTRIBUTES_MAP_HANDLE = attributesHandle;
        INSTANCE_CONSTRUCTOR = ctor;
    }

    private EntityAttributeInjector() {
    }

    /**
     * Injects an attribute into a living entity if it does not already possess it,
     * and sets its base value.
     *
     * @param entity    target entity
     * @param attribute attribute to inject
     * @param baseValue initial base value
     * @return true if injected or already present, false if injection failed
     */
    public static boolean injectAttribute(@NonNull LivingEntity entity, @NonNull Attribute attribute, double baseValue) {
        Objects.requireNonNull(entity, "entity cannot be null");
        Objects.requireNonNull(attribute, "attribute cannot be null");

        if (entity.getAttribute(attribute) != null) {
            return true;
        }

        if (!(entity instanceof CraftLivingEntity craftEntity)) {
            return false;
        }

        net.minecraft.world.entity.LivingEntity nmsEntity = craftEntity.getHandle();
        AttributeMap attributeMap = nmsEntity.getAttributes();
        Holder<net.minecraft.world.entity.ai.attributes.Attribute> holder =
                CraftAttribute.bukkitToMinecraftHolder(attribute);

        if (ATTRIBUTES_MAP_HANDLE != null && INSTANCE_CONSTRUCTOR != null) {
            try {
                @SuppressWarnings("unchecked")
                var map = (Map<Holder<net.minecraft.world.entity.ai.attributes.Attribute>, AttributeInstance>) ATTRIBUTES_MAP_HANDLE.get(attributeMap);

                if (map != null) {
                    Consumer<AttributeInstance> onDirty = instance -> {
                        attributeMap.getAttributesToUpdate().add(instance);
                        if (instance.getAttribute().value().isClientSyncable()) {
                            attributeMap.getAttributesToSync().add(instance);
                        }
                    };
                    AttributeInstance newInstance = INSTANCE_CONSTRUCTOR.newInstance(holder, onDirty);
                    newInstance.setBaseValue(baseValue);
                    map.put(holder, newInstance);
                    return true;
                }
            } catch (Throwable _) {
            }
        }
        return false;
    }
}
