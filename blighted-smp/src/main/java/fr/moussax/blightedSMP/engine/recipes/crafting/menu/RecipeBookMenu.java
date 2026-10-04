package fr.moussax.blightedSMP.engine.recipes.crafting.menu;

import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedRecipe;
import fr.moussax.blightedSMP.engine.recipes.crafting.registry.RecipeRegistry;
import fr.moussax.bedrock.ui.menu.Menu;
import fr.moussax.bedrock.ui.menu.types.PaginatedMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class RecipeBookMenu extends PaginatedMenu {

    private final List<BlightedRecipe> cachedRecipes;

    public RecipeBookMenu(@org.jspecify.annotations.Nullable Menu previousMenu) {
        super("Recipes", 54, previousMenu != null ? previousMenu : new CraftingTableMenu());
        this.cachedRecipes = new ArrayList<>(RecipeRegistry.getAll());
        this.cachedRecipes.sort((firstRecipe, secondRecipe) -> {
            String firstName = firstRecipe.getResult().getDisplayName();
            String secondName = secondRecipe.getResult().getDisplayName();
            return (firstName != null ? firstName : "").compareTo(secondName != null ? secondName : "");
        });
    }

    public RecipeBookMenu() {
        this(null);
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
        setTitle("(" + getCurrentPageNumber() + "/" + getTotalPages() + ") Recipes");
    }

    @Override
    protected ItemStack getItem(@NonNull Player player, int index) {
        if (index >= cachedRecipes.size()) return new ItemStack(Material.AIR);

        BlightedRecipe recipe = cachedRecipes.get(index);
        ItemStack resultItem = recipe.getResult().toItemStack().clone();

        var meta = resultItem.getItemMeta();
        if (meta == null) return resultItem;

        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();
        lore.add("");
        lore.add("§eClick to view recipe!");
        meta.setLore(lore);
        resultItem.setItemMeta(meta);

        return resultItem;
    }

    @Override
    protected void onItemClick(@NonNull Player player, int index, @NonNull ClickType clickType) {
        if (index >= cachedRecipes.size()) return;

        new CraftingRecipePreviewMenu(cachedRecipes.get(index), this).open(player);
    }
}
