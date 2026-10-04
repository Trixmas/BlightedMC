package fr.moussax.blightedSMP.engine.recipes.forging.registry;

import fr.moussax.blightedSMP.engine.recipes.forging.ForgeRecipe;
import fr.moussax.blightedSMP.registry.EngineRegistry;
import fr.moussax.blightedSMP.registry.RegistryModule;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Registry for {@link ForgeRecipe} definitions available to the forge system.
 */
public final class ForgeRegistry {

    private static final EngineRegistry<ForgeRecipe> REGISTRY =
            new EngineRegistry<>("ForgeRegistry", recipe -> recipe.getForgedItem() != null ? recipe.getForgedItem().getItemId() : "recipe_" + recipe.hashCode());

    private ForgeRegistry() {
    }

    public static void initialize(List<RegistryModule<Consumer<ForgeRecipe>>> modules) {
        REGISTRY.initialize(modules);
    }

    public static void register(@NonNull ForgeRecipe recipe) {
        REGISTRY.register(recipe);
    }

    public static Collection<ForgeRecipe> getAll() {
        return REGISTRY.getAll();
    }

    public static void clear() {
        REGISTRY.clear();
    }
}
