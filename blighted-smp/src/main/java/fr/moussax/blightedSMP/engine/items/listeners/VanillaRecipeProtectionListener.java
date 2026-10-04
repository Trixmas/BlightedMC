package fr.moussax.blightedSMP.engine.items.listeners;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedRecipe;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public final class VanillaRecipeProtectionListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();

        boolean containsCustomItem = false;
        for (ItemStack itemStack : matrix) {
            if (isCustomItem(itemStack)) {
                containsCustomItem = true;
                break;
            }
        }

        if (!containsCustomItem) return;

        boolean matchesCustomRecipe = !BlightedRecipe.findMatchingRecipes(Arrays.asList(matrix)).isEmpty();
        if (!matchesCustomRecipe) {
            inventory.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();
        for (ItemStack itemStack : matrix) {
            if (isCustomItem(itemStack)) {
                boolean matchesCustomRecipe = !BlightedRecipe.findMatchingRecipes(Arrays.asList(matrix)).isEmpty();
                if (!matchesCustomRecipe) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack leftItem = inventory.getItem(0);
        ItemStack rightItem = inventory.getItem(1);

        if (isCustomNonEquipment(leftItem) || isCustomNonEquipment(rightItem)) {
            event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
        if (isCustomNonEquipment(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        if (isCustomNonEquipment(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnaceStartSmelt(FurnaceStartSmeltEvent event) {
        if (isCustomItem(event.getSource())) {
            event.setTotalCookTime(Integer.MAX_VALUE);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        if (isCustomItem(event.getFuel())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        ItemStack[] contents = event.getInventory().getContents();
        for (ItemStack itemStack : contents) {
            if (isCustomNonEquipment(itemStack)) {
                event.setResult(null);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onStonecutterClick(InventoryClickEvent event) {
        if (event.getInventory().getType() == InventoryType.STONECUTTER) {
            if (isCustomNonEquipment(event.getCurrentItem()) || isCustomNonEquipment(event.getCursor())) {
                event.setCancelled(true);
            }
        }
    }

    private static boolean isCustomItem(ItemStack itemStack) {
        return itemStack != null && itemStack.getType() != Material.AIR && BlightedItem.fromItemStack(itemStack) != null;
    }

    private static boolean isCustomNonEquipment(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType() == Material.AIR) return false;
        BlightedItem blightedItem = BlightedItem.fromItemStack(itemStack);
        return blightedItem != null && !blightedItem.isEquipment();
    }
}
