package fr.moussax.blightedSMP.engine.recipes.crafting.registry;

import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedRecipe;
import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedShapedRecipe;
import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedShapelessRecipe;
import fr.moussax.blightedSMP.engine.recipes.crafting.builder.ShapedRecipeBuilder;
import fr.moussax.blightedSMP.engine.recipes.crafting.builder.ShapelessRecipeBuilder;
import fr.moussax.blightedSMP.registry.RegistryModule;
import fr.moussax.bedrock.utils.debug.Log;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Consumer;

/**
 * Central registry for custom {@link BlightedRecipe} definitions.
 *
 * <p>Recipes are provided by registered {@link RegistryModule} implementations
 * and added to the recipe collection when {@link #initialize(List)} is called.</p>
 */
public final class RecipeRegistry {

    private static final Set<BlightedRecipe> RECIPES = new LinkedHashSet<>();

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
        if (modules != null) {
            modules.forEach(module -> module.register(RecipeRegistry::register));
        }
        Log.success("RecipeRegistry", "Registered " + RECIPES.size() + " custom recipes.");
    }

    /**
     * Registers a custom recipe.
     *
     * @param recipe the recipe to register
     */
    public static void register(@NonNull BlightedRecipe recipe) {
        RECIPES.add(Objects.requireNonNull(recipe, "recipe cannot be null"));
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
     * Returns an unmodifiable collection of all registered recipes.
     *
     * @return unmodifiable collection of recipes
     */
    public static Collection<BlightedRecipe> getAll() {
        return Collections.unmodifiableCollection(RECIPES);
    }

    /**
     * Resolves all recipes matching the given crafting grid.
     *
     * @param craftingGrid 3×3 grid in row-major order
     * @return set of matching recipes
     */
    public static Set<BlightedRecipe> findMatchingRecipes(List<ItemStack> craftingGrid) {
        if (craftingGrid == null || craftingGrid.isEmpty()) return Collections.emptySet();

        boolean isEmpty = craftingGrid.stream()
                .allMatch(item -> item == null || item.getType() == Material.AIR);
        if (isEmpty) return Collections.emptySet();

        List<String> craftingGridItemIds = BlightedRecipe.resolveItemIdsFromGrid(craftingGrid);
        Set<BlightedRecipe> matchingRecipes = new LinkedHashSet<>();

        for (BlightedRecipe recipe : RECIPES) {
            if (recipe.getResult() == null) continue;

            boolean isMatch = switch (recipe) {
                case BlightedShapedRecipe shapedRecipe ->
                        BlightedRecipe.matchesShapedRecipe(shapedRecipe, craftingGrid, craftingGridItemIds);
                case BlightedShapelessRecipe shapelessRecipe ->
                        BlightedRecipe.matchesShapelessRecipe(shapelessRecipe, craftingGrid, craftingGridItemIds);
            };

            if (isMatch) matchingRecipes.add(recipe);
        }

        return matchingRecipes;
    }

    /**
     * Resolves the first recipe matching the given crafting grid.
     *
     * @param craftingGrid 3×3 grid in row-major order
     * @return matching recipe, or empty if none match
     */
    public static Optional<BlightedRecipe> findFirstMatching(List<ItemStack> craftingGrid) {
        return findMatchingRecipes(craftingGrid).stream().findFirst();
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
        RECIPES.clear();
    }
}
