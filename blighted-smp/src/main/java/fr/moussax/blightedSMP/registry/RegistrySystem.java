package fr.moussax.blightedSMP.registry;

import fr.moussax.blightedSMP.content.ContentRegistrar;
import fr.moussax.blightedSMP.engine.entities.registry.EntitiesRegistry;
import fr.moussax.blightedSMP.engine.entities.rituals.registry.RitualRegistry;
import fr.moussax.blightedSMP.engine.fishing.registry.FishingLootRegistry;
import fr.moussax.blightedSMP.engine.blocks.registry.BlockRegistry;
import fr.moussax.blightedSMP.engine.recipes.crafting.registry.RecipeRegistry;
import fr.moussax.blightedSMP.engine.recipes.forging.registry.ForgeRegistry;
import fr.moussax.blightedSMP.engine.items.registry.ItemRegistry;

/**
 * Coordinates the initialization and cleanup of all BlightedMC registries.
 *
 * <p>The registry system provides a single entry point for loading and clearing
 * custom items, blocks, recipes, forge recipes, entities, rituals, and fishing
 * loot tables.</p>
 */
public final class RegistrySystem {

    private RegistrySystem() {
    }

    /**
     * Initializes all BlightedMC registries in dependency order.
     *
     * <p>Registries are initialized from their respective content providers
     * before dependent systems are loaded.</p>
     */
    public static void initialize() {
        ItemRegistry.initialize(ContentRegistrar.ITEM_MODULES);
        BlockRegistry.initialize(ContentRegistrar.BLOCK_MODULES);
        RecipeRegistry.initialize(ContentRegistrar.RECIPE_MODULES);
        ForgeRegistry.initialize(ContentRegistrar.FORGE_MODULES);
        EntitiesRegistry.initialize(ContentRegistrar.ENTITY_MODULES);
        RitualRegistry.initialize(ContentRegistrar.RITUAL_MODULES);
        FishingLootRegistry.initialize(ContentRegistrar.FISHING_MODULES);
    }

    /**
     * Clears all registered BlightedMC content.
     *
     * <p>This removes the contents of every managed registry without
     * reinitializing them.</p>
     */
    public static void clear() {
        ItemRegistry.clear();
        BlockRegistry.clear();
        RecipeRegistry.clear();
        ForgeRegistry.clear();
        EntitiesRegistry.clear();
        RitualRegistry.clear();
        FishingLootRegistry.clear();
    }
}
