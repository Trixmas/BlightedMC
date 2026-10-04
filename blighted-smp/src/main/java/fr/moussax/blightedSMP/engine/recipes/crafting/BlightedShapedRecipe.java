package fr.moussax.blightedSMP.engine.recipes.crafting;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.recipes.RecipeIngredient;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Concrete implementation of a shaped Blighted crafting recipe.
 * <p>
 * Uses a fixed 3×3 pattern and optionally supports attribute transfer
 * from a single designated ingredient slot.
 */
public final class BlightedShapedRecipe extends BlightedRecipe {
    private final BlightedItem resultBlightedItem;
    private final int resultAmount;
    private final List<RecipeIngredient> recipePattern = new ArrayList<>();

    /**
     * Index of the slot used as the attribute source.
     * A value of {@code -1} disables attribute transfer.
     */
    private int attributeSourceSlotIndex = -1;

    public BlightedShapedRecipe(BlightedItem resultBlightedItem, int resultAmount) {
        this.resultBlightedItem = resultBlightedItem;
        this.resultAmount = resultAmount;
    }

    @Override
    public BlightedItem getResult() {
        return resultBlightedItem;
    }

    @Override
    public int getAmount() {
        return resultAmount;
    }

    /**
     * Builds the result item and transfers attributes from the configured
     * source slot, if defined.
     *
     * @param craftingGrid 3×3 crafting grid
     * @return assembled result item
     */
    @Override
    public ItemStack assemble(List<ItemStack> craftingGrid) {
        ItemStack result = resultBlightedItem.toItemStack().clone();
        result.setAmount(resultAmount);

        if (attributeSourceSlotIndex >= 0 && attributeSourceSlotIndex < craftingGrid.size()) {
            ItemStack source = craftingGrid.get(attributeSourceSlotIndex);
            transferAttributes(source, result);
        }

        return result;
    }

    /** @return immutable view of the recipe pattern */
    public List<RecipeIngredient> getRecipe() {
        return Collections.unmodifiableList(recipePattern);
    }

    /** Replaces the entire recipe pattern. */
    public void setRecipe(List<RecipeIngredient> recipePattern) {
        this.recipePattern.clear();
        this.recipePattern.addAll(recipePattern);
    }

    /** Appends a single ingredient to the recipe pattern. */
    public void addIngredient(RecipeIngredient ingredient) {
        this.recipePattern.add(ingredient);
    }

    /**
     * Defines which grid slot provides enchantments and durability.
     *
     * @param slotIndex slot index in range 0–8
     */
    public void setAttributeSourceSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= 9) {
            throw new IllegalArgumentException("Slot index must be in range 0–8");
        }
        this.attributeSourceSlotIndex = slotIndex;
    }

    /**
     * Computes the total required amount per ingredient ID.
     *
     * @return immutable map of item ID → required amount
     */
    public Map<String, Integer> getIngredientCountMap() {
        var ingredientCountMap = new HashMap<String, Integer>();
        for (RecipeIngredient ingredient : recipePattern) {
            if (ingredient == null) continue;
            ingredientCountMap.merge(ingredient.getId(), ingredient.getAmount(), Integer::sum);
        }
        return Collections.unmodifiableMap(ingredientCountMap);
    }
}
