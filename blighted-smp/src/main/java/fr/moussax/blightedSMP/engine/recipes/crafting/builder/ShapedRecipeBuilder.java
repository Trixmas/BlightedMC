package fr.moussax.blightedSMP.engine.recipes.crafting.builder;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.recipes.CraftingObject;
import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedShapedRecipe;
import fr.moussax.blightedSMP.engine.recipes.ShapeEncoder;
import fr.moussax.blightedSMP.engine.items.registry.ItemRegistry;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

/**
 * Fluent builder for creating {@link BlightedShapedRecipe} instances.
 *
 * <p>Defines a three-row crafting shape and binds each shape character to a
 * material or custom item. An optional crafting slot may be designated as the
 * attribute source for the resulting item.</p>
 */
public final class ShapedRecipeBuilder {

    private final BlightedItem result;
    private final int amount;
    private String line1;
    private String line2;
    private String line3;
    private final Map<Character, CraftingObject> bindings = new HashMap<>();
    private int attributeSourceSlot = -1;

    private ShapedRecipeBuilder(BlightedItem result, int amount) {
        this.result = result;
        this.amount = amount;
    }

    /**
     * Creates a builder for a shaped recipe producing the specified item.
     *
     * @param result the item produced by the recipe
     * @param amount the amount produced by the recipe
     * @return a new shaped recipe builder
     */
    public static ShapedRecipeBuilder of(BlightedItem result, int amount) {
        return new ShapedRecipeBuilder(result, amount);
    }

    /**
     * Creates a builder for a shaped recipe producing the item registered
     * under the specified ID.
     *
     * @param resultId the ID of the result item
     * @param amount   the amount produced by the recipe
     * @return a new shaped recipe builder
     * @throws IllegalArgumentException if the item ID is not registered
     */
    public static ShapedRecipeBuilder of(String resultId, int amount) {
        BlightedItem result = ItemRegistry.get(resultId);
        if (result == null) {
            throw new IllegalArgumentException("Unknown recipe result item ID: '" + resultId + "'");
        }
        return new ShapedRecipeBuilder(result, amount);
    }

    /**
     * Defines the three-row crafting shape used by the recipe.
     *
     * @param line1 the first row of the crafting shape
     * @param line2 the second row of the crafting shape
     * @param line3 the third row of the crafting shape
     * @return this builder
     */
    public ShapedRecipeBuilder shape(String line1, String line2, String line3) {
        this.line1 = line1;
        this.line2 = line2;
        this.line3 = line3;
        return this;
    }

    /**
     * Binds a crafting shape character to a single material ingredient.
     *
     * @param key      the character used in the recipe shape
     * @param material the material represented by the character
     * @return this builder
     */
    // ponytail: simplified — single-item quantity default overload
    public ShapedRecipeBuilder bind(char key, Material material) {
        return bind(key, material, 1);
    }

    /**
     * Binds a crafting shape character to a material.
     *
     * @param key      the character used in the recipe shape
     * @param material the material represented by the character
     * @param amount   the required amount of the material
     * @return this builder
     */
    public ShapedRecipeBuilder bind(char key, Material material, int amount) {
        bindings.put(key, new CraftingObject(material, amount));
        return this;
    }

    /**
     * Binds a crafting shape character to a single custom item ingredient.
     *
     * @param key  the character used in the recipe shape
     * @param item the custom item represented by the character
     * @return this builder
     */
    // ponytail: simplified — single-item quantity default overload
    public ShapedRecipeBuilder bind(char key, BlightedItem item) {
        return bind(key, item, 1);
    }

    /**
     * Binds a crafting shape character to a custom item.
     *
     * @param key    the character used in the recipe shape
     * @param item   the custom item represented by the character
     * @param amount the required amount of the item
     * @return this builder
     */
    public ShapedRecipeBuilder bind(char key, BlightedItem item, int amount) {
        bindings.put(key, new CraftingObject(item, amount));
        return this;
    }

    /**
     * Binds a crafting shape character to a single registered custom item ingredient.
     *
     * @param key    the character used in the recipe shape
     * @param itemId the ID of the custom item represented by the character
     * @return this builder
     * @throws IllegalArgumentException if the item ID is not registered
     */
    // ponytail: simplified — single-item quantity default overload
    public ShapedRecipeBuilder bind(char key, String itemId) {
        return bind(key, itemId, 1);
    }

    /**
     * Binds a crafting shape character to a registered custom item.
     *
     * @param key    the character used in the recipe shape
     * @param itemId the ID of the custom item represented by the character
     * @param amount the required amount of the item
     * @return this builder
     * @throws IllegalArgumentException if the item ID is not registered
     */
    public ShapedRecipeBuilder bind(char key, String itemId, int amount) {
        return bind(key, ItemRegistry.get(itemId), amount);
    }

    /**
     * Designates a crafting grid slot as the attribute source.
     *
     * <p>The slot index uses the standard zero-based nine-slot crafting grid,
     * ranging from {@code 0} to {@code 8}.</p>
     *
     * @param slotIndex the zero-based crafting grid slot
     * @return this builder
     * @throws IllegalArgumentException if the slot index is outside the range
     *                                  {@code 0} to {@code 8}
     */
    public ShapedRecipeBuilder attributeSource(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= 9) {
            throw new IllegalArgumentException("Attribute source slot must be between 0 and 8");
        }
        this.attributeSourceSlot = slotIndex;
        return this;
    }

    /**
     * Builds the configured shaped recipe.
     *
     * <p>The recipe shape is encoded and transferred to the resulting recipe,
     * along with the optional attribute source slot.</p>
     *
     * @return the constructed shaped recipe
     * @throws IllegalStateException if the recipe shape has not been defined
     */
    // ponytail: simplified — order-independent shape and key binding resolution upon build
    public BlightedShapedRecipe build() {
        if (line1 == null || line2 == null || line3 == null) {
            throw new IllegalStateException("Recipe shape has not been defined yet. Call .shape() first.");
        }
        ShapeEncoder encoder = new ShapeEncoder(line1, line2, line3);
        bindings.forEach(encoder::bindKey);

        BlightedShapedRecipe recipe = new BlightedShapedRecipe(result, amount);
        recipe.setRecipe(encoder.encodeCraftingRecipe());

        if (attributeSourceSlot != -1) {
            recipe.setAttributeSourceSlot(attributeSourceSlot);
        }

        return recipe;
    }
}
