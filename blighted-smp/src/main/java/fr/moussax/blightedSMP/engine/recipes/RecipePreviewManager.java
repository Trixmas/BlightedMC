package fr.moussax.blightedSMP.engine.recipes;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedRecipe;
import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedShapedRecipe;
import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedShapelessRecipe;
import fr.moussax.blightedSMP.engine.recipes.crafting.menu.CraftingRecipePreviewMenu;
import fr.moussax.blightedSMP.engine.recipes.forging.ForgeRecipe;
import fr.moussax.blightedSMP.engine.recipes.forging.menu.ForgeRecipePreviewMenu;
import fr.moussax.blightedSMP.engine.recipes.forging.registry.ForgeRegistry;
import fr.moussax.bedrock.ui.menu.Menu;
import fr.moussax.bedrock.ui.menu.interaction.MenuElementPreset;
import fr.moussax.bedrock.utils.ItemBuilder;
import fr.moussax.blightedSMP.utils.Utilities;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RecipePreviewManager {

    private RecipePreviewManager() {
    }

    public static List<Object> getAllRecipesForItem(@NonNull BlightedItem targetItem) {
        List<Object> recipes = new ArrayList<>();

        for (BlightedRecipe blightedRecipe : BlightedRecipe.REGISTERED_RECIPES) {
            if (blightedRecipe != null && blightedRecipe.getResult() != null && blightedRecipe.getResult().equals(targetItem)) {
                recipes.add(blightedRecipe);
            }
        }

        for (ForgeRecipe forgeRecipe : ForgeRegistry.getAll()) {
            if (forgeRecipe != null && forgeRecipe.getForgedItem() != null && forgeRecipe.getForgedItem().equals(targetItem)) {
                recipes.add(forgeRecipe);
            }
        }

        for (BlightedRecipe blightedRecipe : BlightedRecipe.REGISTERED_RECIPES) {
            if (isIngredientInCraftingRecipe(targetItem, blightedRecipe) && !recipes.contains(blightedRecipe)) {
                recipes.add(blightedRecipe);
            }
        }

        for (ForgeRecipe forgeRecipe : ForgeRegistry.getAll()) {
            if (forgeRecipe != null && isIngredientInForgeRecipe(targetItem, forgeRecipe) && !recipes.contains(forgeRecipe)) {
                recipes.add(forgeRecipe);
            }
        }

        return recipes;
    }

    private static boolean isIngredientInCraftingRecipe(BlightedItem targetItem, BlightedRecipe blightedRecipe) {
        List<CraftingObject> ingredients;
        if (blightedRecipe instanceof BlightedShapedRecipe shapedRecipe) {
            ingredients = shapedRecipe.getRecipe();
        } else if (blightedRecipe instanceof BlightedShapelessRecipe shapelessRecipe) {
            ingredients = shapelessRecipe.getIngredients();
        } else {
            return false;
        }

        for (CraftingObject ingredient : ingredients) {
            if (ingredient != null && ingredient.isCustom() && targetItem.equals(ingredient.getManager())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isIngredientInForgeRecipe(BlightedItem targetItem, ForgeRecipe forgeRecipe) {
        if (forgeRecipe.getIngredients() == null) return false;
        for (CraftingObject ingredient : forgeRecipe.getIngredients()) {
            if (ingredient != null && ingredient.isCustom() && targetItem.equals(ingredient.getManager())) {
                return true;
            }
        }
        return false;
    }

    public static boolean openPreview(@NonNull Player player, @NonNull Object recipe, @Nullable BlightedItem targetItem, @Nullable Menu previousMenu) {
        if (recipe instanceof BlightedRecipe craftingRecipe) {
            new CraftingRecipePreviewMenu(craftingRecipe, targetItem, previousMenu).open(player);
            return true;
        }
        if (recipe instanceof ForgeRecipe forgeRecipe) {
            new ForgeRecipePreviewMenu(forgeRecipe, targetItem, previousMenu).open(player);
            return true;
        }
        return false;
    }

    public static boolean openPreview(@NonNull Player player, @NonNull Object recipe, @Nullable Menu previousMenu) {
        return openPreview(player, recipe, null, previousMenu);
    }

    public static boolean openPreview(@NonNull Player player, @NonNull BlightedItem item, @Nullable Menu previousMenu) {
        List<Object> recipes = getAllRecipesForItem(item);
        if (recipes.isEmpty()) return false;
        return openPreview(player, recipes.getFirst(), item, previousMenu);
    }

    public static boolean openPreview(@NonNull Player player, @NonNull BlightedItem item) {
        return openPreview(player, item, null);
    }

    public static void setupNavigation(@NonNull Menu menu, @NonNull Object recipe, @Nullable BlightedItem targetItem, @Nullable Menu previousMenu) {
        menu.setCloseButton(49);

        BlightedItem previewItem = targetItem;
        if (previewItem == null) {
            if (recipe instanceof BlightedRecipe blightedRecipe) {
                previewItem = blightedRecipe.getResult();
            } else if (recipe instanceof ForgeRecipe forgeRecipe) {
                previewItem = forgeRecipe.getForgedItem();
            }
        }

        if (previewItem == null) {
            menu.fillEmptyWith(MenuElementPreset.EMPTY_SLOT_FILLER);
            return;
        }

        List<Object> allRecipes = getAllRecipesForItem(previewItem);
        int currentIndex = allRecipes.indexOf(recipe);
        if (currentIndex < 0) currentIndex = 0;
        int totalRecipes = allRecipes.size();

        if (currentIndex > 0) {
            Object previousRecipeObject = allRecipes.get(currentIndex - 1);
            ItemStack previousRecipeItem = new ItemBuilder(Material.ARROW, "§aPrevious Recipe").toItemStack();
            menu.setItem(48, previousRecipeItem, (clickingPlayer, _) ->
                    openPreview(clickingPlayer, previousRecipeObject, targetItem, previousMenu)
            ).withPageTurnSound();
        } else if (previousMenu != null) {
            menu.setBackButton(48, previousMenu);
        }

        if (currentIndex < totalRecipes - 1) {
            Object nextRecipeObject = allRecipes.get(currentIndex + 1);
            ItemStack nextRecipeItem = new ItemBuilder(Material.ARROW, "§aNext Recipe").toItemStack();
            menu.setItem(50, nextRecipeItem, (clickingPlayer, _) ->
                    openPreview(clickingPlayer, nextRecipeObject, targetItem, previousMenu)
            ).withPageTurnSound();
        } else {
            menu.setItem(50, MenuElementPreset.EMPTY_SLOT_FILLER);
        }

        menu.fillEmptyWith(MenuElementPreset.EMPTY_SLOT_FILLER);
    }

    public static Map<String, Integer> countInventoryItems(@NonNull Player player, @NonNull Set<String> requiredIds) {
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType() == Material.AIR) continue;
            for (String requiredIdentifier : requiredIds) {
                if (Utilities.resolveItemId(stack, requiredIdentifier).equals(requiredIdentifier)) {
                    counts.put(requiredIdentifier, counts.getOrDefault(requiredIdentifier, 0) + stack.getAmount());
                }
            }
        }
        return counts;
    }
}
