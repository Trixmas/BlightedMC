package fr.moussax.blightedSMP.engine.recipes.forging.menu;

import fr.moussax.blightedSMP.engine.recipes.CraftingObject;
import fr.moussax.blightedSMP.engine.recipes.forging.ForgeRecipe;
import fr.moussax.blightedSMP.engine.recipes.forging.registry.ForgeRegistry;
import fr.moussax.bedrock.text.Formatter;
import fr.moussax.bedrock.ui.menu.Menu;
import fr.moussax.bedrock.ui.menu.types.PaginatedMenu;
import fr.moussax.bedrock.utils.ItemBuilder;
import fr.moussax.blightedSMP.utils.Utilities;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ForgeRecipesMenu extends PaginatedMenu {

    private final List<ForgeRecipe> cachedRecipes;

    public ForgeRecipesMenu(@Nullable Menu previousMenu) {
        super("Forge Recipes", 54, previousMenu != null ? previousMenu : new ForgeMenu(null));
        this.cachedRecipes = new ArrayList<>(ForgeRegistry.getAll());
        this.cachedRecipes.sort(Comparator.comparing(
                recipe -> recipe.getForgedItem().getDisplayName() != null
                        ? recipe.getForgedItem().getDisplayName()
                        : ""
        ));
    }

    @Override
    protected boolean useStandardFrame() {
        return true;
    }

    @Override
    protected int getTotalItems(@NonNull Player player) {
        return cachedRecipes.size();
    }

    @Override
    protected void onBuildHeader(@NonNull Player viewer) {
        setTitle("(" + getCurrentPageNumber() + "/" + getTotalPages() + ") Forge Recipes");
    }

    @Override
    protected ItemStack getItem(@NonNull Player player, int index) {
        if (index >= cachedRecipes.size()) {
            return new ItemStack(Material.AIR);
        }

        ForgeRecipe recipe = cachedRecipes.get(index);
        return buildRecipeDisplayItem(recipe);
    }

    @Override
    protected void onItemClick(@NonNull Player player, int index, @NonNull ClickType clickType) {
        if (index >= cachedRecipes.size()) {
            return;
        }

        ForgeRecipe recipe = cachedRecipes.get(index);
        openSubMenu(new ForgeMenu(recipe, this));
    }

    private ItemStack buildRecipeDisplayItem(ForgeRecipe recipe) {
        ItemBuilder builder = new ItemBuilder(recipe.getForgedItem().toItemStack().clone());
        builder.setAmount(recipe.getForgedAmount());

        builder.addLore("", " §7Items required:");
        for (CraftingObject ingredient : recipe.getIngredients()) {
            builder.addLore(" §8‣ " + Utilities.extractIngredientName(ingredient) + " §8x" + ingredient.getAmount());
        }

        builder.addLore(
                "",
                " §8Consumes §6🪣 " + Formatter.formatDecimalWithCommas(recipe.getFuelCost()) + "mB §8of ",
                " §8thermal fuel to forge.",
                "",
                "§eClick to select!"
        );

        return builder.toItemStack();
    }
}
