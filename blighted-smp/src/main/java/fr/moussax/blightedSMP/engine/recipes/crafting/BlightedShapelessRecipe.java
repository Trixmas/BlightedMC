package fr.moussax.blightedSMP.engine.recipes.crafting;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.recipes.RecipeIngredient;
import fr.moussax.blightedSMP.utils.Utilities;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Concrete implementation of a shapeless Blighted crafting recipe.
 * <p>
 * Ingredient order is ignored, and attribute transfer may occur from
 * a designated ingredient instance found in the crafting grid.
 */
public final class BlightedShapelessRecipe extends BlightedRecipe {
    private final List<RecipeIngredient> ingredientList = new ArrayList<>();
    private final Map<String, Integer> ingredientCountMap = new HashMap<>();
    private final BlightedItem resultBlightedItem;
    private final int resultAmount;

    /**
     * Ingredient acting as the attribute source.
     * {@code null} disables attribute transfer.
     */
    private RecipeIngredient attributeSourceIngredient = null;

    public BlightedShapelessRecipe(BlightedItem resultBlightedItem) {
        this(resultBlightedItem, 1);
    }

    public BlightedShapelessRecipe(BlightedItem resultBlightedItem, int resultAmount) {
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
     * Builds the result item and transfers attributes from the first
     * matching source ingredient found in the grid, if configured.
     *
     * @param craftingGrid crafting grid contents
     * @return assembled result item
     */
    @Override
    public ItemStack assemble(List<ItemStack> craftingGrid) {
        ItemStack result = resultBlightedItem.toItemStack().clone();
        result.setAmount(resultAmount);

        if (attributeSourceIngredient != null) {
            String targetId = attributeSourceIngredient.getId();
            for (ItemStack stack : craftingGrid) {
                if (stack == null || stack.getType() == Material.AIR) continue;

                String stackId = Utilities.resolveItemId(stack, targetId);
                if (stackId.equals(targetId)) {
                    transferAttributes(stack, result);
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Registers an ingredient and updates its required count.
     *
     * @param ingredient recipe ingredient
     */
    public void addIngredient(RecipeIngredient ingredient) {
        ingredientList.add(ingredient);
        ingredientCountMap.merge(ingredient.getId(), ingredient.getAmount(), Integer::sum);
    }

    /**
     * Defines which ingredient provides enchantments and durability.
     *
     * @param ingredient registered ingredient
     */
    public void setAttributeSource(RecipeIngredient ingredient) {
        if (!ingredientList.contains(ingredient)) {
            throw new IllegalArgumentException("Attribute source must be a registered ingredient of this recipe");
        }
        this.attributeSourceIngredient = ingredient;
    }

    /** @return immutable list of registered ingredients */
    public List<RecipeIngredient> getIngredients() {
        return Collections.unmodifiableList(ingredientList);
    }

    /** @return immutable map of item ID → required amount */
    public Map<String, Integer> getIngredientCountMap() {
        return Collections.unmodifiableMap(ingredientCountMap);
    }
}
