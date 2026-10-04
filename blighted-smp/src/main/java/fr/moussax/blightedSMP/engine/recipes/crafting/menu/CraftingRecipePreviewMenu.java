package fr.moussax.blightedSMP.engine.recipes.crafting.menu;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.recipes.CraftingObject;
import fr.moussax.blightedSMP.engine.recipes.RecipePreviewManager;
import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedRecipe;
import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedShapedRecipe;
import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedShapelessRecipe;
import fr.moussax.bedrock.scheduling.PluginContext;
import fr.moussax.bedrock.text.Messenger;
import fr.moussax.bedrock.ui.menu.Menu;
import fr.moussax.bedrock.ui.menu.TickableMenu;
import fr.moussax.bedrock.utils.ItemBuilder;
import fr.moussax.blightedSMP.utils.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

public final class CraftingRecipePreviewMenu extends Menu implements TickableMenu {

    private static final int[] CRAFTING_GRID_SLOTS = {
            10, 11, 12,
            19, 20, 21,
            28, 29, 30
    };

    private static final int WORKBENCH_SLOT = 23;
    private static final int RESULT_SLOT = 25;
    private static final int QUICKCRAFT_SLOT = 32;

    private final BlightedRecipe recipe;
    private final BlightedItem targetItem;
    private final Menu previousMenu;
    private int lastIngredientHash = -1;

    public CraftingRecipePreviewMenu(@NonNull BlightedRecipe recipe, @Nullable BlightedItem targetItem, @Nullable Menu previousMenu) {
        super(ChatColor.stripColor(recipe.getResult().getDisplayName()), 54);
        this.recipe = recipe;
        this.targetItem = targetItem;
        this.previousMenu = previousMenu;
    }

    public CraftingRecipePreviewMenu(@NonNull BlightedRecipe recipe, @Nullable Menu previousMenu) {
        this(recipe, recipe.getResult(), previousMenu);
    }

    @Override
    public long tickPeriodTicks() {
        return 10L;
    }

    @Override
    public void onTick(Player player) {
        Map<String, IngredientInfo> requirements = aggregateRecipeIngredients(recipe);
        Map<String, Integer> inventoryCounts = RecipePreviewManager.countInventoryItems(player, requirements.keySet());
        int currentHash = inventoryCounts.hashCode();

        if (lastIngredientHash == currentHash) return;

        this.lastIngredientHash = currentHash;
        refresh(player);
    }

    public CraftingRecipePreviewMenu(@NonNull BlightedRecipe recipe) {
        this(recipe, recipe.getResult(), null);
    }

    @Override
    public void build(Player player) {
        setTitle(ChatColor.stripColor(recipe.getResult().getDisplayName()));
        setupRecipeVisualization(player);
        setupNavigation();
    }

    private void setupRecipeVisualization(Player player) {
        if (recipe instanceof BlightedShapedRecipe shapedRecipe) {
            setupShapedRecipeGrid(shapedRecipe);
        } else if (recipe instanceof BlightedShapelessRecipe shapelessRecipe) {
            setupShapelessRecipeGrid(shapelessRecipe);
        }

        setItem(WORKBENCH_SLOT, new ItemBuilder(Material.CRAFTING_TABLE, "§fBlighted Workbench")
                .addLore("§7Craft this recipe by using a blighted", "§7workbench or Quickcraft. ")
                .toItemStack());

        ItemStack resultItem = recipe.assemble(createVirtualCraftingGrid());
        int amount = recipe.getAmount() > 0 ? recipe.getAmount() : 1;
        resultItem.setAmount(amount);
        setItem(RESULT_SLOT, resultItem);

        setupQuickcraftButton(player);
    }

    private void setupShapedRecipeGrid(BlightedShapedRecipe shapedRecipe) {
        List<CraftingObject> pattern = shapedRecipe.getRecipe();

        for (int i = 0; i < pattern.size() && i < CRAFTING_GRID_SLOTS.length; i++) {
            CraftingObject craftingObject = pattern.get(i);

            if (craftingObject == null) {
                setItem(CRAFTING_GRID_SLOTS[i], new ItemStack(Material.AIR));
                continue;
            }

            ItemStack ingredientItem = createIngredientDisplay(craftingObject);
            setItem(CRAFTING_GRID_SLOTS[i], ingredientItem, (clickingPlayer, _) -> {
                if (!craftingObject.isCustom() || craftingObject.getManager() == null) return;
                RecipePreviewManager.openPreview(clickingPlayer, craftingObject.getManager(), this);
            });
        }
    }

    private void setupShapelessRecipeGrid(BlightedShapelessRecipe shapelessRecipe) {
        List<CraftingObject> ingredients = shapelessRecipe.getIngredients();

        for (int i = 0; i < CRAFTING_GRID_SLOTS.length; i++) {
            if (i < ingredients.size()) {
                CraftingObject ingredient = ingredients.get(i);
                ItemStack ingredientItem = createIngredientDisplay(ingredient);

                setItem(CRAFTING_GRID_SLOTS[i], ingredientItem, (clickingPlayer, _) -> {
                    if (!ingredient.isCustom() || ingredient.getManager() == null) return;
                    RecipePreviewManager.openPreview(clickingPlayer, ingredient.getManager(), this);
                });
            } else {
                setItem(CRAFTING_GRID_SLOTS[i], new ItemStack(Material.AIR));
            }
        }
    }

    private void setupQuickcraftButton(Player player) {
        Map<String, IngredientInfo> requirements = aggregateRecipeIngredients(recipe);
        Map<String, Integer> inventoryCounts = RecipePreviewManager.countInventoryItems(player, requirements.keySet());

        boolean hasAllIngredients = true;
        ItemBuilder builder = new ItemBuilder(Material.GOLDEN_PICKAXE, "§fQuickcraft");
        builder.addLore(
                "§7Craft this item instantly from",
                "§7your inventory materials.",
                "",
                " §7Ingredients required:"
        );

        for (Map.Entry<String, IngredientInfo> entry : requirements.entrySet()) {
            IngredientInfo info = entry.getValue();
            int owned = inventoryCounts.getOrDefault(entry.getKey(), 0);
            boolean hasEnough = owned >= info.amount;

            if (!hasEnough) {
                hasAllIngredients = false;
            }

            String status = hasEnough ? "§a✔" : "§c❌";
            String countColor = hasEnough ? "§a" : "§c";
            String name = Utilities.extractIngredientName(info.ingredient);

            builder.addLore(" " + status + " §7" + name + " §8x" + info.amount + " §7(" + countColor + owned + "§7/§a" + info.amount + "§7)");
        }

        final boolean canQuickcraft = hasAllIngredients;

        builder.addLore("", canQuickcraft ? "§eClick to Quickcraft!" : "§cMissing ingredients!")
                .setEnchantmentGlint(canQuickcraft);
        builder.addItemFlag(ItemFlag.HIDE_ATTRIBUTES);

        setItem(QUICKCRAFT_SLOT, builder.toItemStack(), (clickingPlayer, _) -> {
            if (!canQuickcraft) {
                playErrorSound(clickingPlayer);
                Messenger.warn(clickingPlayer, "You're missing some ingredients to Quickcraft this item!");
                return;
            }
            PluginContext.delay(() -> executeQuickcraft(clickingPlayer, requirements), 1L);
        }).withoutSound();
    }

    private void executeQuickcraft(Player player, Map<String, IngredientInfo> requirements) {
        Map<String, Integer> inventoryCounts = RecipePreviewManager.countInventoryItems(player, requirements.keySet());
        boolean verified = requirements.entrySet().stream()
                .allMatch(entry -> inventoryCounts.getOrDefault(entry.getKey(), 0) >= entry.getValue().amount);

        if (!verified) {
            Messenger.warn(player, "You're missing some ingredients to Quickcraft this item!");
            refresh(player);
            return;
        }

        for (IngredientInfo info : requirements.values()) {
            CraftingObject consumeObject = info.ingredient.isCustom()
                    ? new CraftingObject(Objects.requireNonNull(info.ingredient.getManager()), info.amount)
                    : new CraftingObject(Objects.requireNonNull(info.ingredient.getVanillaItem()).getType(), info.amount);
            Utilities.consumeItemsFromInventory(player, consumeObject);
        }

        List<ItemStack> virtualGrid = createVirtualCraftingGrid();
        ItemStack result = recipe.assemble(virtualGrid);

        int amount = recipe.getAmount() > 0 ? recipe.getAmount() : 1;
        result.setAmount(amount);

        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(result);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1f, 1f);
        refresh(player);
    }

    private List<ItemStack> createVirtualCraftingGrid() {
        if (recipe instanceof BlightedShapedRecipe shapedRecipe) {
            return shapedRecipe.getRecipe().stream()
                    .map(object -> object != null ? getCraftingObjectItem(object) : null)
                    .toList();
        }
        if (recipe instanceof BlightedShapelessRecipe shapelessRecipe) {
            return shapelessRecipe.getIngredients().stream()
                    .map(object -> object != null ? getCraftingObjectItem(object) : null)
                    .toList();
        }
        return List.of();
    }

    private Map<String, IngredientInfo> aggregateRecipeIngredients(BlightedRecipe recipe) {
        Map<String, IngredientInfo> map = new LinkedHashMap<>();
        List<CraftingObject> rawList;
        if (recipe instanceof BlightedShapedRecipe shapedRecipe) {
            rawList = shapedRecipe.getRecipe().stream().filter(Objects::nonNull).toList();
        } else if (recipe instanceof BlightedShapelessRecipe shapelessRecipe) {
            rawList = shapelessRecipe.getIngredients().stream().filter(Objects::nonNull).toList();
        } else {
            rawList = List.of();
        }

        for (CraftingObject ingredient : rawList) {
            String ingredientId = ingredient.getId();
            if (ingredientId.isEmpty()) continue;
            if (!map.containsKey(ingredientId)) {
                map.put(ingredientId, new IngredientInfo(ingredient, ingredient.getAmount()));
            } else {
                IngredientInfo info = map.get(ingredientId);
                info.amount += ingredient.getAmount();
            }
        }
        return map;
    }

    private static class IngredientInfo {
        final CraftingObject ingredient;
        int amount;

        IngredientInfo(CraftingObject ingredient, int amount) {
            this.ingredient = ingredient;
            this.amount = amount;
        }
    }

    private ItemStack createIngredientDisplay(CraftingObject craftingObject) {
        ItemStack ingredientItem = getCraftingObjectItem(craftingObject);
        ingredientItem.setAmount(Math.max(1, craftingObject.getAmount()));
        return ingredientItem;
    }

    private ItemStack getCraftingObjectItem(CraftingObject craftingObject) {
        if (craftingObject.isCustom() && craftingObject.getManager() != null) {
            return craftingObject.getManager().toItemStack().clone();
        }
        if (craftingObject.isVanilla() && craftingObject.getVanillaItem() != null) {
            return craftingObject.getVanillaItem().clone();
        }
        return new ItemStack(Material.AIR);
    }

    private void setupNavigation() {
        RecipePreviewManager.setupNavigation(this, recipe, targetItem, previousMenu);
    }
}
