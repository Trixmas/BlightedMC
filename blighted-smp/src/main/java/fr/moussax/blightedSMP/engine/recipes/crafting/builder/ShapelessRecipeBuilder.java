package fr.moussax.blightedSMP.engine.recipes.crafting.builder;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.registry.ItemRegistry;
import fr.moussax.blightedSMP.engine.recipes.CraftingObject;
import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedShapelessRecipe;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fluent builder for creating {@link BlightedShapelessRecipe} instances.
 *
 * <p>Ingredients are added without regard to their position in a crafting
 * grid. One ingredient may optionally be designated as the attribute source
 * for the resulting recipe.</p>
 */
public final class ShapelessRecipeBuilder {

    private final BlightedItem result;
    private final int amount;
    private final List<CraftingObject> ingredients = new ArrayList<>();
    private CraftingObject attributeSource = null;

    private ShapelessRecipeBuilder(BlightedItem result, int amount) {
        this.result = Objects.requireNonNull(result, "result");
        this.amount = amount;
    }

    /**
     * Creates a builder for a shapeless recipe producing a single instance of the specified item.
     *
     * @param result the item produced by the recipe
     * @return a new shapeless recipe builder
     */
    public static ShapelessRecipeBuilder of(BlightedItem result) {
        return of(result, 1);
    }

    /**
     * Creates a builder for a shapeless recipe producing the specified item and amount.
     *
     * @param result the item produced by the recipe
     * @param amount the amount produced by the recipe
     * @return a new shapeless recipe builder
     */
    public static ShapelessRecipeBuilder of(BlightedItem result, int amount) {
        return new ShapelessRecipeBuilder(result, amount);
    }

    /**
     * Creates a builder for a shapeless recipe producing a single instance of the item registered
     * under the specified ID.
     *
     * @param resultId the ID of the result item
     * @return a new shapeless recipe builder
     * @throws IllegalArgumentException if the item ID is not registered
     */
    public static ShapelessRecipeBuilder of(String resultId) {
        return of(resultId, 1);
    }

    /**
     * Creates a builder for a shapeless recipe producing the item registered
     * under the specified ID and amount.
     *
     * @param resultId the ID of the result item
     * @param amount   the amount produced by the recipe
     * @return a new shapeless recipe builder
     * @throws IllegalArgumentException if the item ID is not registered
     */
    public static ShapelessRecipeBuilder of(String resultId, int amount) {
        BlightedItem result = ItemRegistry.get(resultId);
        if (result == null) {
            throw new IllegalArgumentException("Unknown recipe result item ID: '" + resultId + "'");
        }
        return new ShapelessRecipeBuilder(result, amount);
    }

    /**
     * Adds a single material ingredient to the recipe.
     *
     * @param material the material required by the recipe
     * @return this builder
     */
    public ShapelessRecipeBuilder addIngredient(Material material) {
        return addIngredient(material, 1, false);
    }

    /**
     * Adds a material ingredient to the recipe.
     *
     * @param material the material required by the recipe
     * @param amount   the required amount
     * @return this builder
     */
    public ShapelessRecipeBuilder addIngredient(Material material, int amount) {
        return addIngredient(material, amount, false);
    }

    /**
     * Adds a material ingredient to the recipe and optionally designates it
     * as the attribute source.
     *
     * @param material          the material required by the recipe
     * @param amount            the required amount
     * @param isAttributeSource whether this ingredient provides the attributes
     *                          for the resulting item
     * @return this builder
     */
    public ShapelessRecipeBuilder addIngredient(
            Material material,
            int amount,
            boolean isAttributeSource
    ) {
        CraftingObject craftingObject = new CraftingObject(material, amount);
        ingredients.add(craftingObject);
        if (isAttributeSource) this.attributeSource = craftingObject;
        return this;
    }

    /**
     * Adds a single custom item ingredient to the recipe.
     *
     * @param item the custom item required by the recipe
     * @return this builder
     */
    // ponytail: simplified — single-item quantity default overload
    public ShapelessRecipeBuilder addIngredient(BlightedItem item) {
        return addIngredient(item, 1, false);
    }

    /**
     * Adds a custom item ingredient to the recipe.
     *
     * @param item   the custom item required by the recipe
     * @param amount the required amount
     * @return this builder
     */
    public ShapelessRecipeBuilder addIngredient(BlightedItem item, int amount) {
        return addIngredient(item, amount, false);
    }

    /**
     * Adds a custom item ingredient to the recipe and optionally designates
     * it as the attribute source.
     *
     * @param item              the custom item required by the recipe
     * @param amount            the required amount
     * @param isAttributeSource whether this ingredient provides the attributes
     *                          for the resulting item
     * @return this builder
     */
    public ShapelessRecipeBuilder addIngredient(
            BlightedItem item,
            int amount,
            boolean isAttributeSource
    ) {
        CraftingObject craftingObject = new CraftingObject(item, amount);
        ingredients.add(craftingObject);
        if (isAttributeSource) this.attributeSource = craftingObject;
        return this;
    }

    /**
     * Adds a single registered custom item as an ingredient.
     *
     * @param itemId the ID of the required custom item
     * @return this builder
     * @throws IllegalArgumentException if the item ID is not registered
     */
    public ShapelessRecipeBuilder addIngredient(String itemId) {
        return addIngredient(ItemRegistry.get(itemId), 1, false);
    }

    /**
     * Adds a registered custom item as an ingredient.
     *
     * @param itemId the ID of the required custom item
     * @param amount the required amount
     * @return this builder
     * @throws IllegalArgumentException if the item ID is not registered
     */
    public ShapelessRecipeBuilder addIngredient(String itemId, int amount) {
        return addIngredient(ItemRegistry.get(itemId), amount, false);
    }

    /**
     * Adds a registered custom item as an ingredient and optionally
     * designates it as the attribute source.
     *
     * @param itemId            the ID of the required custom item
     * @param amount            the required amount
     * @param isAttributeSource whether this ingredient provides the attributes
     *                          for the resulting item
     * @return this builder
     * @throws IllegalArgumentException if the item ID is not registered
     */
    public ShapelessRecipeBuilder addIngredient(
            String itemId,
            int amount,
            boolean isAttributeSource
    ) {
        return addIngredient(ItemRegistry.get(itemId), amount, isAttributeSource);
    }

    /**
     * Builds the configured shapeless recipe.
     *
     * <p>All configured ingredients and the optional attribute source are
     * transferred to the resulting recipe.</p>
     *
     * @return the constructed shapeless recipe
     */
    public BlightedShapelessRecipe build() {
        BlightedShapelessRecipe recipe = new BlightedShapelessRecipe(result, amount);
        for (CraftingObject ingredient : ingredients) {
            recipe.addIngredient(ingredient);
        }

        if (attributeSource != null) {
            recipe.setAttributeSource(attributeSource);
        }

        return recipe;
    }
}
