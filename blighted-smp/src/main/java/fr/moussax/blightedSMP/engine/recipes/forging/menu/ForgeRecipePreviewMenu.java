package fr.moussax.blightedSMP.engine.recipes.forging.menu;

import fr.moussax.blightedSMP.content.sound.BlightedSounds;
import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.recipes.RecipeIngredient;
import fr.moussax.blightedSMP.engine.recipes.RecipePreviewManager;
import fr.moussax.blightedSMP.engine.recipes.forging.ForgeRecipe;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import fr.moussax.bedrock.scheduling.PluginContext;
import fr.moussax.bedrock.text.Formatter;
import fr.moussax.bedrock.text.Messenger;
import fr.moussax.bedrock.ui.menu.Menu;
import fr.moussax.bedrock.ui.menu.TickableMenu;
import fr.moussax.bedrock.utils.ItemBuilder;
import fr.moussax.blightedSMP.utils.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

public final class ForgeRecipePreviewMenu extends Menu implements TickableMenu {

    private static final int[] GRID_SLOTS = {19, 20, 21, 28, 29, 30, 37, 38, 39};
    private static final int[] REQUIRED_ITEM_INDICATOR_SLOTS = {10, 11, 12, 13};
    private static final int[] FORGED_ITEM_INDICATOR_SLOTS = {15, 16};
    private static final int FORGE_SLOT = 14;
    private static final int ITEM_INDICATOR = 23;
    private static final int RESULT_SLOT = 25;
    private static final int HYPERFORGE_SLOT = 32;
    private static final int FUEL_INFO_SLOT = 34;

    private final ForgeRecipe recipe;
    private final BlightedItem targetItem;
    private final Menu previousMenu;
    private int lastStateHash = -1;

    public ForgeRecipePreviewMenu(@NonNull ForgeRecipe recipe, @Nullable BlightedItem targetItem, @Nullable Menu previousMenu) {
        super(ChatColor.stripColor(recipe.getForgedItem().getDisplayName()), 54);
        this.recipe = recipe;
        this.targetItem = targetItem;
        this.previousMenu = previousMenu;
    }

    public ForgeRecipePreviewMenu(@NonNull ForgeRecipe recipe, @Nullable Menu previousMenu) {
        this(recipe, recipe.getForgedItem(), previousMenu);
    }

    @Override
    public long tickPeriodTicks() {
        return 10L;
    }

    @Override
    public void onTick(Player player) {
        BlightedPlayer blightedPlayer = BlightedPlayer.get(player);
        int currentFuel = blightedPlayer != null ? blightedPlayer.getForgeFuel() : 0;

        Map<String, IngredientInfo> requirements = aggregateForgeIngredients(recipe);
        Map<String, Integer> inventoryCounts = RecipePreviewManager.countInventoryItems(player, requirements.keySet());

        int currentHash = Objects.hash(currentFuel, inventoryCounts);
        if (lastStateHash == currentHash) return;

        this.lastStateHash = currentHash;
        refresh(player);
    }

    public ForgeRecipePreviewMenu(@NonNull ForgeRecipe recipe) {
        this(recipe, recipe.getForgedItem(), null);
    }

    @Override
    public void build(Player player) {
        setTitle(ChatColor.stripColor(recipe.getForgedItem().getDisplayName()));
        setupRecipeVisualization(player);
        setupNavigation();
    }

    private void setupRecipeVisualization(Player player) {
        setupStatusPanes(player);
        setupIngredientGrid();
        setupResultDisplay();
        setupFuelDisplay();
        setupHyperforgeButton(player);
    }

    private void setupStatusPanes(Player player) {
        boolean canHyperforge = checkCanHyperforge(player);
        Material indicator = canHyperforge ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;

        ItemStack requiredItemsPane = new ItemBuilder(indicator, "§6Items to Process")
                .addLore("§7The materials required to forge", "§7the item are displayed in this side.")
                .toItemStack();

        ItemStack forgedItemPane = new ItemBuilder(indicator, "§6Item to Forge")
                .addLore("§7The item you will create", "§7with the required materials.")
                .toItemStack();

        fillSlots(REQUIRED_ITEM_INDICATOR_SLOTS, requiredItemsPane);
        fillSlots(FORGED_ITEM_INDICATOR_SLOTS, forgedItemPane);

        setItem(ITEM_INDICATOR, new ItemBuilder(indicator).hideTooltip().toItemStack());

        setItem(FORGE_SLOT, new ItemBuilder(Material.BLAST_FURNACE, "§fBlighted Forge")
                .addLore("§7Forge this recipe using a blighted", "§7forge or Hyperforge.")
                .toItemStack());
    }

    private boolean checkCanHyperforge(Player player) {
        BlightedPlayer blightedPlayer = BlightedPlayer.get(player);
        int currentFuel = blightedPlayer != null ? blightedPlayer.getForgeFuel() : 0;
        int fuelCost = recipe.getFuelCost();
        boolean hasSufficientFuel = currentFuel >= fuelCost;

        Map<String, IngredientInfo> requirements = aggregateForgeIngredients(recipe);
        Map<String, Integer> inventoryCounts = RecipePreviewManager.countInventoryItems(player, requirements.keySet());

        return hasSufficientFuel && requirements.entrySet().stream()
                .allMatch(entry -> inventoryCounts.getOrDefault(
                        entry.getKey(), 0) >= entry.getValue().amount);
    }

    private void setupIngredientGrid() {
        ItemBuilder emptySlotBuilder = new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§cUnused Slot")
                .addLore("§7This slot isn't used for", "§7the selected recipe.");
        fillSlots(GRID_SLOTS, emptySlotBuilder.toItemStack());

        List<RecipeIngredient> ingredients = recipe.getIngredients();
        for (int i = 0; i < ingredients.size() && i < GRID_SLOTS.length; i++) {
            RecipeIngredient ingredient = ingredients.get(i);
            ItemStack displayItem = createIngredientDisplay(ingredient);

            setItem(GRID_SLOTS[i], displayItem, (clickingPlayer, _) -> {
                if (!ingredient.isCustom() || ingredient.getItem() == null) return;
                RecipePreviewManager.openPreview(clickingPlayer, ingredient.getItem(), this);
            });
        }
    }

    private ItemStack createIngredientDisplay(RecipeIngredient ingredient) {
        return ingredient.toItemStack();
    }

    private void setupResultDisplay() {
        ItemStack result = recipe.getForgedItem().toItemStack().clone();
        result.setAmount(Math.max(1, recipe.getForgedAmount()));
        setItem(RESULT_SLOT, result);
    }

    private void setupFuelDisplay() {
        ItemStack fuelInfo = new ItemBuilder(Material.CAMPFIRE, "§6Fuel Requirement")
                .addLore(
                        "§7Requires §6🪣 " + Formatter.formatDecimalWithCommas(recipe.getFuelCost()) + "mB §7of thermal fuel",
                        "§7to power the forging process."
                )
                .toItemStack();
        setItem(FUEL_INFO_SLOT, fuelInfo);
    }

    private void setupHyperforgeButton(Player player) {
        BlightedPlayer blightedPlayer = BlightedPlayer.get(player);
        int currentFuel = blightedPlayer != null ? blightedPlayer.getForgeFuel() : 0;
        int fuelCost = recipe.getFuelCost();
        boolean hasSufficientFuel = currentFuel >= fuelCost;

        Map<String, IngredientInfo> requirements = aggregateForgeIngredients(recipe);
        Map<String, Integer> inventoryCounts = RecipePreviewManager.countInventoryItems(player, requirements.keySet());

        boolean hasAllIngredients = true;
        ItemBuilder builder = new ItemBuilder(Material.ANVIL, "§fHyperforge");
        builder.addLore(
                "§7Forge this item instantly from your",
                "§7inventory materials & thermal fuel.",
                "",
                " §7Requirements:"
        );

        for (Map.Entry<String, IngredientInfo> entry : requirements.entrySet()) {
            String ingredientId = entry.getKey();
            IngredientInfo info = entry.getValue();
            int requiredAmount = info.amount;
            int owned = inventoryCounts.getOrDefault(ingredientId, 0);
            boolean hasEnough = owned >= requiredAmount;

            if (!hasEnough) {
                hasAllIngredients = false;
            }

            String status = hasEnough ? "§a✔" : "§c❌";
            String countColor = hasEnough ? "§a" : "§c";
            String name = Utilities.extractIngredientName(info.ingredient);

            builder.addLore(String.format(" %s §7%s §8x%d §7(%s%d§7/§a%d§7)",
                    status, name, requiredAmount, countColor, owned, requiredAmount));
        }

        String fuelStatus = hasSufficientFuel ? "§a✔" : "§c❌";
        String fuelCountColor = hasSufficientFuel ? "§a" : "§c";
        builder.addLore(
                " " + fuelStatus + " §7Thermal Fuel §8" + Formatter.formatDecimalWithCommas(fuelCost) + "mB §7(" + fuelCountColor + Formatter.formatDecimalWithCommas(currentFuel) + "§7/§a" + Formatter.formatDecimalWithCommas(fuelCost) + "mB§7)"
        );

        final boolean canHyperforge = hasAllIngredients && hasSufficientFuel;
        builder.addItemFlag(ItemFlag.HIDE_ATTRIBUTES);
        builder.addLore(
                "",
                canHyperforge ? "§eClick to Hyperforge!" : "§cMissing ingredients or insufficient fuel!"
        ).setEnchantmentGlint(canHyperforge);

        setItem(HYPERFORGE_SLOT, builder.toItemStack(), (clickingPlayer, _) -> {
            if (!canHyperforge) {
                playErrorSound(clickingPlayer);
                Messenger.warn(clickingPlayer, "You're missing some ingredients or insufficient fuel to Hyperforge this item!");
                return;
            }
            PluginContext.delay(() -> executeHyperforge(clickingPlayer, blightedPlayer, requirements, fuelCost), 1L);
        }).withoutSound();
    }

    private void executeHyperforge(Player player, BlightedPlayer blightedPlayer, Map<String, IngredientInfo> requirements, int fuelCost) {
        if (blightedPlayer == null) return;

        Map<String, Integer> inventoryCounts = RecipePreviewManager.countInventoryItems(player, requirements.keySet());
        boolean verifiedIngredients = requirements.entrySet().stream()
                .allMatch(entry -> inventoryCounts.getOrDefault(entry.getKey(), 0) >= entry.getValue().amount);
        boolean verifiedFuel = blightedPlayer.getForgeFuel() >= fuelCost;

        if (!verifiedIngredients || !verifiedFuel) {
            Messenger.warn(player, "You're missing some ingredients or insufficient fuel to Hyperforge this item!");
            refresh(player);
            return;
        }

        for (IngredientInfo info : requirements.values()) {
            RecipeIngredient consumeObject = info.ingredient.isCustom()
                    ? RecipeIngredient.of(Objects.requireNonNull(info.ingredient.getItem()), info.amount)
                    : RecipeIngredient.of(Objects.requireNonNull(info.ingredient.getMaterial()), info.amount);
            Utilities.consumeItemsFromInventory(player, consumeObject);
        }

        blightedPlayer.removeForgeFuel(fuelCost);
        blightedPlayer.saveData();

        ItemStack result = recipe.getForgedItem().toItemStack().clone();
        int amount = Math.max(1, recipe.getForgedAmount());
        result.setAmount(amount);

        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(result);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        BlightedSounds.FORGE_ITEM.play(player.getLocation());
        refresh(player);
    }

    private Map<String, IngredientInfo> aggregateForgeIngredients(ForgeRecipe recipe) {
        Map<String, IngredientInfo> recipeIngredients = new LinkedHashMap<>();
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            if (ingredient == null) continue;
            String ingredientId = ingredient.getId();
            if (ingredientId.isEmpty()) continue;
            if (!recipeIngredients.containsKey(ingredientId)) {
                recipeIngredients.put(ingredientId, new IngredientInfo(ingredient, ingredient.getAmount()));
            } else {
                IngredientInfo info = recipeIngredients.get(ingredientId);
                info.amount += ingredient.getAmount();
            }
        }
        return recipeIngredients;
    }

    private static class IngredientInfo {
        final RecipeIngredient ingredient;
        int amount;

        IngredientInfo(RecipeIngredient ingredient, int amount) {
            this.ingredient = ingredient;
            this.amount = amount;
        }
    }

    private void setupNavigation() {
        RecipePreviewManager.setupNavigation(this, recipe, targetItem, previousMenu);
    }
}
