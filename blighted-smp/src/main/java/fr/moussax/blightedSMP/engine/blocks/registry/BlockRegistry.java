package fr.moussax.blightedSMP.engine.blocks.registry;

import fr.moussax.bedrock.utils.debug.Log;
import fr.moussax.blightedSMP.engine.blocks.BlightedBlock;
import fr.moussax.blightedSMP.registry.EngineRegistry;
import fr.moussax.blightedSMP.registry.RegistryModule;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Central registry for all {@link BlightedBlock} definitions.
 */
public final class BlockRegistry {

    private static final EngineRegistry<BlightedBlock> REGISTRY = new EngineRegistry<>("BlockRegistry", BlightedBlock::getId);

    private BlockRegistry() {
    }

    public static void initialize(List<RegistryModule<Consumer<BlightedBlock>>> modules) {
        REGISTRY.initialize(modules);
    }

    public static void register(@NonNull BlightedBlock block) {
        if (block.getBlightedItem() == null) {
            Log.warn("BlockRegistry", "Attempted to register block with null item. Skipping.");
            return;
        }
        REGISTRY.register(block);
    }

    @Nullable
    public static BlightedBlock get(String id) {
        return REGISTRY.get(id);
    }

    public static Collection<BlightedBlock> getAll() {
        return REGISTRY.getAll();
    }

    public static void clear() {
        REGISTRY.clear();
    }
}
