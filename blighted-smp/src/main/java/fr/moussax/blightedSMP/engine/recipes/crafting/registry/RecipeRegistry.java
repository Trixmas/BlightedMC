package fr.moussax.blightedSMP.engine.recipes.crafting.registry;

import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedRecipe;
import fr.moussax.blightedSMP.engine.recipes.crafting.builder.ShapedRecipeBuilder;
import fr.moussax.blightedSMP.engine.recipes.crafting.builder.ShapelessRecipeBuilder;
import fr.moussax.blightedSMP.registry.RegistryModule;
import fr.moussax.bedrock.utils.debug.Log;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.Consumer;

/**
 * Central registry for custom {@link BlightedRecipe} definitions.
 *
 * <p>Recipes are provided by registered {@link RegistryModule} implementations
 * and added to the global recipe collection when {@link #initialize(List)} is
 * called. The registry also provides factory methods for creating shaped and
 * shapeless recipe builders.</p>
 */
public final class RecipeRegistry {

    private RecipeRegistry() {
    }

    /**
     * Initializes the recipe registry using the provided list of modules.
     *
     * <p>Previously registered recipes are cleared before all configured
     * recipe providers are loaded.</p>
     *
     * @param modules the list of recipe modules to load
     */
    public static void initialize(List<RegistryModule<Consumer<BlightedRecipe>>> modules) {
        clear();
        modules.forEach(module -> module.register(RecipeRegistry::register));
        Log.success("RecipesRegistry", "Registered " + BlightedRecipe.REGISTERED_RECIPES.size() + " custom recipes.");
    }

    /**
     * Registers a custom recipe.
     *
     * @param recipe the recipe to register
     */
    public static void register(@NonNull BlightedRecipe recipe) {
        recipe.addRecipe();
    }

    /**
     * Registers multiple custom recipes.
     *
     * @param recipes the recipes to register
     */
    public static void register(BlightedRecipe... recipes) {
        for (BlightedRecipe recipe : recipes) {
            register(recipe);
        }
    }

    /**
     * Creates a builder for a shaped custom recipe.
     *
     * @param resultId the item ID of the recipe result
     * @param amount   the amount produced by the recipe
     * @return a shaped recipe builder
     */
    public static ShapedRecipeBuilder shapedRecipe(String resultId, int amount) {
        return ShapedRecipeBuilder.of(resultId, amount);
    }

    /**
     * Creates a builder for a shapeless custom recipe producing a single item.
     *
     * @param resultId the item ID of the recipe result
     * @return a shapeless recipe builder
     */
    public static ShapelessRecipeBuilder shapelessRecipe(String resultId) {
        return ShapelessRecipeBuilder.of(resultId, 1);
    }

    /**
     * Creates a builder for a shapeless custom recipe.
     *
     * @param resultId the item ID of the recipe result
     * @param amount   the amount produced by the recipe
     * @return a shapeless recipe builder
     */
    public static ShapelessRecipeBuilder shapelessRecipe(String resultId, int amount) {
        return ShapelessRecipeBuilder.of(resultId, amount);
    }

    /**
     * Removes all recipes currently registered in the recipe registry.
     */
    public static void clear() {
        BlightedRecipe.REGISTERED_RECIPES.clear();
    }
}
