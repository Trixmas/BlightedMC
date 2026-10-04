package fr.moussax.blightedSMP.engine.recipes.forging;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.recipes.CraftingObject;
import fr.moussax.blightedSMP.engine.items.registry.ItemRegistry;
import lombok.Getter;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a recipe for forging a custom item.
 *
 * <p>A forge recipe specifies the resulting item and amount, the ingredients
 * required to forge it, and the resource costs associated with the operation.</p>
 *
 * <p>Instances are created through {@link Builder}.</p>
 */
@Getter
public final class ForgeRecipe {

    private final BlightedItem forgedItem;
    private final int forgedAmount;
    private final List<CraftingObject> ingredients;
    private final int fuelCost;

    private ForgeRecipe(
            BlightedItem forgedItem,
            int forgedAmount,
            List<CraftingObject> ingredients,
            int fuelCost,
            int gemsCost,
            int levelCost
    ) {
        this.forgedItem = forgedItem;
        this.forgedAmount = forgedAmount;
        this.ingredients = List.copyOf(ingredients);
        this.fuelCost = fuelCost;
    }

    /**
     * Fluent builder for constructing a {@link ForgeRecipe}.
     *
     * <p>The builder supports vanilla materials, registered custom items,
     * and complete {@link CraftingObject} instances as ingredients.</p>
     */
    public static final class Builder {

        private final BlightedItem forgedItem;
        private final int forgedAmount;
        private final List<CraftingObject> ingredients = new ArrayList<>();
        private int fuelCost;
        private int gemsCost;
        private int levelCost;

        private Builder(BlightedItem forgedItem, int forgedAmount) {
            if (forgedItem == null) {
                throw new IllegalArgumentException("Forged item cannot be null");
            }
            if (forgedAmount < 1) {
                throw new IllegalArgumentException("Forged amount must be positive");
            }

            this.forgedItem = forgedItem;
            this.forgedAmount = forgedAmount;
        }

        /**
         * Creates a builder for the specified forged item.
         *
         * @param item   item produced by the recipe
         * @param amount amount produced by the recipe
         * @return a new forge recipe builder
         */
        public static Builder of(BlightedItem item, int amount) {
            return new Builder(item, amount);
        }

        /**
         * Creates a builder for a registered custom item.
         *
         * @param itemId identifier of the item produced by the recipe
         * @param amount amount produced by the recipe
         * @return a new forge recipe builder
         */
        public static Builder of(String itemId, int amount) {
            return new Builder(ItemRegistry.get(itemId), amount);
        }

        /**
         * Adds an ingredient to the recipe.
         *
         * @param ingredient required ingredient
         * @return this builder
         */
        public Builder addIngredient(CraftingObject ingredient) {
            this.ingredients.add(ingredient);
            return this;
        }

        /**
         * Adds a vanilla material as an ingredient.
         *
         * @param material required material
         * @param amount   required amount
         * @return this builder
         */
        public Builder addIngredient(Material material, int amount) {
            return addIngredient(new CraftingObject(material, amount));
        }

        /**
         * Adds a custom item as an ingredient.
         *
         * @param item   required custom item
         * @param amount required amount
         * @return this builder
         */
        public Builder addIngredient(BlightedItem item, int amount) {
            return addIngredient(new CraftingObject(item, amount));
        }

        /**
         * Adds a registered custom item as an ingredient.
         *
         * @param itemId identifier of the required custom item
         * @param amount required amount
         * @return this builder
         */
        public Builder addIngredient(String itemId, int amount) {
            return addIngredient(ItemRegistry.get(itemId), amount);
        }

        /**
         * Adds multiple ingredients to the recipe.
         *
         * @param ingredients ingredients required by the recipe
         * @return this builder
         */
        public Builder ingredients(CraftingObject... ingredients) {
            this.ingredients.addAll(List.of(ingredients));
            return this;
        }

        /**
         * Sets the amount of forge fuel required by the recipe.
         *
         * @param amount required fuel amount
         * @return this builder
         * @throws IllegalArgumentException if the amount is negative
         */
        public Builder fuelCost(int amount) {
            if (amount < 0) {
                throw new IllegalArgumentException("Fuel cost cannot be negative");
            }
            this.fuelCost = amount;
            return this;
        }

        /**
         * Builds the configured forge recipe.
         *
         * @return a new immutable forge recipe
         */
        public ForgeRecipe build() {
            return new ForgeRecipe(
                    forgedItem,
                    forgedAmount,
                    ingredients,
                    fuelCost,
                    gemsCost,
                    levelCost
            );
        }
    }
}
