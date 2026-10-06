package fr.moussax.blightedSMP.engine.items.registry;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.registry.EngineRegistry;
import fr.moussax.blightedSMP.registry.RegistryModule;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Central registry for all {@link BlightedItem} definitions available to the
 * BlightedMC item system.
 */
public final class ItemRegistry {

    private static final EngineRegistry<BlightedItem> REGISTRY =
            new EngineRegistry<>("ItemDirectory", BlightedItem::getItemId);

    private ItemRegistry() {
    }

    public static void initialize(List<RegistryModule<Consumer<BlightedItem>>> modules) {
        REGISTRY.initialize(modules);
    }

    @Nullable
    public static BlightedItem fromItemStack(@NonNull ItemStack itemStack) {
        return BlightedItem.fromItemStack(itemStack);
    }

    public static void register(@NonNull BlightedItem blightedItem) {
        REGISTRY.register(blightedItem);
    }

    public static void register(BlightedItem... items) {
        for (BlightedItem item : items) {
            register(item);
        }
    }

    @Nullable
    public static BlightedItem get(@NonNull String itemId) {
        return REGISTRY.get(itemId);
    }

    @NonNull
    public static BlightedItem getOrThrow(@NonNull String itemId) {
        BlightedItem item = get(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Unknown item ID: " + itemId);
        }
        return item;
    }

    public static Collection<BlightedItem> getAll() {
        return REGISTRY.getAll();
    }

    public static List<BlightedItem> getAllItems() {
        return List.copyOf(REGISTRY.getAll());
    }

    public static void clear() {
        REGISTRY.clear();
    }
}
