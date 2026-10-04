package fr.moussax.blightedSMP.utils;

import fr.moussax.blightedSMP.engine.recipes.RecipeIngredient;
import fr.moussax.bedrock.text.Formatter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

import static fr.moussax.blightedSMP.engine.items.BlightedItem.BLIGHTED_ID_KEY;

/**
 * Utility class providing general-purpose helper methods for the BlightedMC plugin.
 */
public final class Utilities {

    private Utilities() {
    }

    /**
     * Resolves the effective identifier of the given item stack.
     *
     * @param item         the item stack whose identifier is resolved
     * @param resolutionId hint used to force or influence identifier resolution
     * @return the resolved item identifier
     */
    public static String resolveItemId(@NonNull ItemStack item, @NonNull String resolutionId) {
        var meta = item.getItemMeta();
        if (meta != null) {
            String customId = meta.getPersistentDataContainer().get(BLIGHTED_ID_KEY, PersistentDataType.STRING);
            if (customId != null) {
                return customId;
            }
        }
        return "vanilla:" + item.getType().name();
    }

    /**
     * Returns a display name for the given crafting ingredient.
     *
     * @param ingredient the ingredient to get the name from
     * @return display the name of the ingredient
     */
    public static String extractIngredientName(RecipeIngredient ingredient) {
        if (ingredient.isCustom() && ingredient.getItem() != null) {
            return Objects.requireNonNull(ingredient.getItem().toItemStack().getItemMeta()).getDisplayName();
        }
        return "§f" + Formatter.formatEnumName(Objects.requireNonNull(ingredient.getMaterial()).name());
    }

    /**
     * Removes the specified amount of the ingredient from the player's inventory.
     *
     * @param player     the player whose inventory to modify
     * @param ingredient the ingredient and amount to consume
     */
    public static void consumeItemsFromInventory(Player player, RecipeIngredient ingredient) {
        String requiredId = ingredient.getId();
        int remainingToRemove = ingredient.getAmount();
        ItemStack[] contents = player.getInventory().getContents();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType() == Material.AIR || remainingToRemove <= 0) continue;

            String currentId = resolveItemId(item, requiredId);
            if (!currentId.equals(requiredId)) continue;

            int amountToDeduct = Math.min(item.getAmount(), remainingToRemove);
            int newAmount = item.getAmount() - amountToDeduct;
            remainingToRemove -= amountToDeduct;

            if (newAmount <= 0) {
                player.getInventory().setItem(slot, null);
            } else {
                item.setAmount(newAmount);
            }
        }
    }
}
