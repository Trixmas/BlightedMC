package fr.moussax.blightedSMP.engine.recipes.crafting;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.recipes.RecipeIngredient;
import fr.moussax.blightedSMP.engine.recipes.crafting.registry.RecipeRegistry;
import fr.moussax.blightedSMP.utils.Utilities;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

import java.util.*;

/**
 * Base type for all BlightedMC custom crafting recipes.
 * <p>
 * Supports shaped and shapeless recipes and handles attribute swapping
 * (e.g., enchantments, durability, repair cost) during item assembly.
 */
public sealed abstract class BlightedRecipe permits BlightedShapedRecipe, BlightedShapelessRecipe {

    /**
     * @return the logical result definition of this recipe
     */
    public abstract BlightedItem getResult();

    /**
     * @return the base output amount
     */
    public abstract int getAmount();

    /**
     * Builds the final result item using the provided crafting grid.
     * Performs attribute swapping from relevant ingredients.
     *
     * @param craftingGrid 3×3 grid in row-major order (null or AIR for empty)
     * @return assembled result item
     */
    public abstract ItemStack assemble(List<ItemStack> craftingGrid);

    /**
     * Registers this recipe in the central {@link RecipeRegistry}.
     */
    public void addRecipe() {
        RecipeRegistry.register(this);
    }

    /**
     * Resolves all recipes matching the given crafting grid.
     *
     * @param craftingGrid 3×3 grid in row-major order
     * @return matching recipes, or empty if none match
     */
    public static Set<BlightedRecipe> findMatchingRecipes(List<ItemStack> craftingGrid) {
        return RecipeRegistry.findMatchingRecipes(craftingGrid);
    }

    /**
     * Resolves custom or vanilla item IDs for each grid slot.
     */
    public static List<String> resolveItemIdsFromGrid(List<ItemStack> craftingGrid) {
        List<String> itemIdsInGrid = new ArrayList<>(craftingGrid.size());

        for (ItemStack stack : craftingGrid) {
            if (stack == null || stack.getType() == Material.AIR) {
                itemIdsInGrid.add("");
                continue;
            }
            itemIdsInGrid.add(Utilities.resolveItemId(stack, ""));
        }
        return itemIdsInGrid;
    }

    /**
     * Checks whether a shaped recipe matches the crafting grid exactly.
     */
    public static boolean matchesShapedRecipe(BlightedShapedRecipe recipe,
                                              List<ItemStack> craftingGrid,
                                              List<String> craftingGridItemIds) {

        List<RecipeIngredient> expectedPattern = recipe.getRecipe();
        if (expectedPattern.size() != craftingGrid.size()) return false;

        for (int slotIndex = 0; slotIndex < expectedPattern.size(); slotIndex++) {
            RecipeIngredient expectedSlot = expectedPattern.get(slotIndex);
            String currentItemId = craftingGridItemIds.get(slotIndex);

            if (expectedSlot == null) {
                if (!currentItemId.isEmpty()) return false;
                continue;
            }

            if (!currentItemId.equals(expectedSlot.getId())) return false;

            ItemStack currentStack = craftingGrid.get(slotIndex);
            if (currentStack == null || currentStack.getAmount() < expectedSlot.getAmount()) return false;
        }

        return true;
    }

    /**
     * Checks whether a shapeless recipe matches the crafting grid.
     */
    public static boolean matchesShapelessRecipe(BlightedShapelessRecipe recipe, List<ItemStack> craftingGrid, List<String> craftingGridItemIds) {
        Map<String, Integer> remainingRequiredCounts = new HashMap<>(recipe.getIngredientCountMap());

        for (int slotIndex = 0; slotIndex < craftingGrid.size(); slotIndex++) {
            ItemStack currentStack = craftingGrid.get(slotIndex);
            if (currentStack == null || currentStack.getType() == Material.AIR) continue;

            String currentItemId = craftingGridItemIds.get(slotIndex);

            if (currentItemId.isEmpty() || !remainingRequiredCounts.containsKey(currentItemId)) {
                return false;
            }

            int needed = remainingRequiredCounts.get(currentItemId);
            if (needed <= 0) {
                return false;
            }

            int countToDeduct = Math.min(needed, currentStack.getAmount());
            remainingRequiredCounts.put(currentItemId, needed - countToDeduct);
        }

        return remainingRequiredCounts.values().stream().allMatch(amount -> amount == 0);
    }

    /**
     * Transfers allowed attributes from source to target.
     * <p>
     * Copies enchantments, durability, and repair cost.
     * The display name is intentionally ignored.
     *
     * @param source ingredient item
     * @param target result item
     */
    protected void transferAttributes(ItemStack source, ItemStack target) {
        if (source == null || !source.hasItemMeta()) return;
        ItemMeta sourceMeta = source.getItemMeta();
        ItemMeta targetMeta = target.getItemMeta();
        if (targetMeta == null) return;

        if (Objects.requireNonNull(sourceMeta).hasEnchants()) {
            sourceMeta.getEnchants().forEach((enchantment, level) ->
                    targetMeta.addEnchant(enchantment, level, true)
            );
        }

        // Transfer repair cost
        if (sourceMeta instanceof Repairable sourceRepair
                && targetMeta instanceof Repairable targetRepair) {
            targetRepair.setRepairCost(sourceRepair.getRepairCost());
        }

        // Transfer durability damage
        if (sourceMeta instanceof Damageable sourceDamage
                && targetMeta instanceof Damageable targetDamage) {
            if (sourceDamage.hasDamage()) {
                targetDamage.setDamage(sourceDamage.getDamage());
            }
        }

        target.setItemMeta(targetMeta);
    }
}
