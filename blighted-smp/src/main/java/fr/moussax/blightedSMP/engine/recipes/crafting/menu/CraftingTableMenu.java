package fr.moussax.blightedSMP.engine.recipes.crafting.menu;

import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedRecipe;
import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedShapedRecipe;
import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedShapelessRecipe;
import fr.moussax.blightedSMP.engine.recipes.CraftingObject;
import fr.moussax.bedrock.ui.menu.types.InteractiveMenu;
import fr.moussax.bedrock.ui.menu.interaction.MenuElementPreset;
import fr.moussax.bedrock.utils.ItemBuilder;
import fr.moussax.blightedSMP.utils.Utilities;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.*;

public final class CraftingTableMenu extends InteractiveMenu {

    private static final int OUTPUT_SLOT = 23;
    private static final int[] INPUT_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int[] INDICATOR_SLOTS_LEFT = {45, 46, 47, 48};
    private static final int[] INDICATOR_SLOTS_RIGHT = {50, 51, 52, 53};

    public CraftingTableMenu() {
        super("Crafting Table", 54);
        addInteractableSlots(INPUT_SLOTS);
    }

    @Override
    public void build(Player player) {
        setTitle("Craft Item");
        fillEmptyWith(MenuElementPreset.EMPTY_SLOT_FILLER);

        for (int slot : INPUT_SLOTS) {
            slots.remove(slot);
        }

        setItem(25, new ItemBuilder(Material.KNOWLEDGE_BOOK, "§6Crafting Recipes")
                .addLore("§7A tainted book that holds", "§7secrets of §5blighted §7items.", "", "§eClick to view!")
                .toItemStack(), (clickingPlayer, _) -> new RecipeBookMenu(new CraftingTableMenu()).open(clickingPlayer)
        );

        setCloseButton(49);

        ItemStack indicator = new ItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .setDisplayName("§r").setHideTooltip(true).toItemStack();

        for (int slot : INDICATOR_SLOTS_LEFT)
            setItem(slot, indicator);

        for (int slot : INDICATOR_SLOTS_RIGHT)
            setItem(slot, indicator);

        setupOutputSlot();
    }

    @Override
    public void onUpdate(@NonNull Player player) {
        BlightedRecipe recipe = getMatchingRecipe();
        updateOutputSlot(recipe);
        updateIndicators(recipe != null);
    }

    private void setupOutputSlot() {
        setItem(OUTPUT_SLOT, RECIPE_REQUIRED(), (player, type) -> {
            BlightedRecipe recipe = getMatchingRecipe();
            if (recipe == null) return;

            playClickSound(player);
            if (isShiftClick(type)) {
                craftMaximum(recipe, player);
            } else {
                craftOnce(recipe, player);
            }

            onUpdate(player);
        }).withoutSound();
    }

    private void craftOnce(BlightedRecipe recipe, Player player) {
        ItemStack resultItem = recipe.assemble(getInputGrid());
        ItemStack cursorItem = player.getItemOnCursor();

        if (!canAddToCursor(cursorItem, resultItem)) return;

        if (cursorItem.getType() != Material.AIR) {
            cursorItem.setAmount(cursorItem.getAmount() + resultItem.getAmount());
            player.setItemOnCursor(cursorItem);
        } else {
            player.setItemOnCursor(resultItem);
        }

        consumeIngredients(recipe, 1);
    }

    private void craftMaximum(BlightedRecipe recipe, Player player) {
        int maxCrafts = getMaxCraftCount(recipe);
        if (maxCrafts <= 0) return;

        ItemStack resultItem = recipe.assemble(getInputGrid());
        resultItem.setAmount(resultItem.getAmount() * maxCrafts);

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(resultItem);
        leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));

        consumeIngredients(recipe, maxCrafts);
    }

    private void consumeIngredients(BlightedRecipe recipe, int times) {
        if (recipe instanceof BlightedShapedRecipe shaped) {
            consumeShaped(shaped, times);
        } else if (recipe instanceof BlightedShapelessRecipe shapeless) {
            consumeShapeless(shapeless, times);
        }
    }

    private void consumeShaped(BlightedShapedRecipe recipe, int times) {
        List<CraftingObject> pattern = recipe.getRecipe();
        for (int i = 0; i < INPUT_SLOTS.length; i++) {
            if (i >= pattern.size()) break;
            CraftingObject requiredIngredient = pattern.get(i);
            if (requiredIngredient == null) continue;

            ItemStack stack = inventory.getItem(INPUT_SLOTS[i]);
            if (stack == null) continue;

            int newAmount = stack.getAmount() - (requiredIngredient.getAmount() * times);
            if (newAmount <= 0) {
                inventory.setItem(INPUT_SLOTS[i], null);
            } else {
                stack.setAmount(newAmount);
            }
        }
    }

    private void consumeShapeless(BlightedShapelessRecipe recipe, int times) {
        Map<String, Integer> required = new HashMap<>(recipe.getIngredientCountMap());
        required.replaceAll((k, v) -> v * times);

        for (int slot : INPUT_SLOTS) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null) continue;

            String matchedId = null;
            for (String requiredIdentifier : required.keySet()) {
                if (Utilities.resolveItemId(stack, requiredIdentifier).equals(requiredIdentifier)) {
                    matchedId = requiredIdentifier;
                    break;
                }
            }

            if (matchedId == null) continue;

            int needed = required.get(matchedId);
            if (needed <= 0) continue;

            int toRemove = Math.min(needed, stack.getAmount());
            int newAmount = stack.getAmount() - toRemove;

            if (newAmount <= 0) {
                inventory.setItem(slot, null);
            } else {
                stack.setAmount(newAmount);
            }
            required.put(matchedId, needed - toRemove);
        }
    }

    private int getMaxCraftCount(BlightedRecipe recipe) {
        if (recipe instanceof BlightedShapedRecipe shaped) {
            int max = Integer.MAX_VALUE;
            List<CraftingObject> pattern = shaped.getRecipe();
            for (int i = 0; i < INPUT_SLOTS.length; i++) {
                if (i >= pattern.size()) continue;
                CraftingObject requiredIngredient = pattern.get(i);
                if (requiredIngredient == null) continue;

                ItemStack stack = inventory.getItem(INPUT_SLOTS[i]);
                if (stack == null) return 0;
                max = Math.min(max, stack.getAmount() / requiredIngredient.getAmount());
            }
            return max == Integer.MAX_VALUE ? 0 : max;
        }

        if (recipe instanceof BlightedShapelessRecipe shapeless) {
            Map<String, Integer> required = shapeless.getIngredientCountMap();
            Map<String, Integer> available = new HashMap<>();

            for (int slot : INPUT_SLOTS) {
                ItemStack stack = inventory.getItem(slot);
                if (stack == null) continue;

                for (String reqId : required.keySet()) {
                    if (Utilities.resolveItemId(stack, reqId).equals(reqId)) {
                        available.merge(reqId, stack.getAmount(), Integer::sum);
                    }
                }
            }

            int max = Integer.MAX_VALUE;
            for (Map.Entry<String, Integer> entry : required.entrySet()) {
                int avail = available.getOrDefault(entry.getKey(), 0);
                if (avail < entry.getValue()) return 0;
                max = Math.min(max, avail / entry.getValue());
            }
            return max == Integer.MAX_VALUE ? 0 : max;
        }
        return 0;
    }

    private List<ItemStack> getInputGrid() {
        List<ItemStack> grid = new ArrayList<>();
        for (int slot : INPUT_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            grid.add((item != null && item.getType() != Material.AIR) ? item : null);
        }
        return grid;
    }

    private BlightedRecipe getMatchingRecipe() {
        Set<BlightedRecipe> matches = BlightedRecipe.findMatchingRecipes(getInputGrid());
        return matches.isEmpty() ? null : matches.iterator().next();
    }

    private void updateOutputSlot(BlightedRecipe recipe) {
        if (recipe == null) {
            inventory.setItem(OUTPUT_SLOT, RECIPE_REQUIRED());
            return;
        }

        ItemStack result = recipe.assemble(getInputGrid());
        ItemBuilder builder = new ItemBuilder(result);
        builder.addLore("§8§m----------------", "§7This is the item you are crafting.");
        inventory.setItem(OUTPUT_SLOT, builder.toItemStack());
    }

    private void updateIndicators(boolean isValid) {
        ItemStack indicator = new ItemBuilder(isValid ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
                .setDisplayName("§r").setHideTooltip(true).toItemStack();

        for (int slot : INDICATOR_SLOTS_LEFT) inventory.setItem(slot, indicator);
        for (int slot : INDICATOR_SLOTS_RIGHT) inventory.setItem(slot, indicator);
    }

    private ItemStack RECIPE_REQUIRED() {
        return new ItemBuilder(Material.BARRIER, "§cRecipe Required")
                .addLore("§7Add items for a valid recipe in", "§7the crafting grid to the left.")
                .toItemStack();
    }

    private boolean canAddToCursor(ItemStack cursor, ItemStack result) {
        if (cursor == null || cursor.getType() == Material.AIR) return true;
        return cursor.isSimilar(result) && cursor.getAmount() + result.getAmount() <= cursor.getMaxStackSize();
    }

    private boolean isShiftClick(ClickType type) {
        return type == ClickType.SHIFT_LEFT || type == ClickType.SHIFT_RIGHT;
    }

    public void returnItems(Player player) {
        for (int slot : INPUT_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> left = player.getInventory().addItem(item);
                left.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
            }
        }
    }

    @Override
    public void onClose(Player player) {
        returnItems(player);
    }
}
