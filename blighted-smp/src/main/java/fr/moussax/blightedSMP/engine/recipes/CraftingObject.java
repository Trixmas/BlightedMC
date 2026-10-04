package fr.moussax.blightedSMP.engine.recipes;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Represents an ingredient used in a crafting recipe, which can be either
 * a custom item managed by {@link BlightedItem} or a vanilla {@link Material}.
 */
public final class CraftingObject {

    @Getter
    private final BlightedItem manager;
    private final ItemStack vanillaItem;
    @Getter
    private final int amount;
    private final String itemId;

    /**
     * Creates a crafting object representing a custom item.
     *
     * @param manager the {@link BlightedItem} managing the custom item
     * @param amount  the required quantity
     */
    public CraftingObject(BlightedItem manager, int amount) {
        this.manager = manager;
        this.vanillaItem = null;
        this.amount = amount;
        this.itemId = manager != null ? manager.getItemId() : "";
    }

    /**
     * Creates a crafting object representing a vanilla item.
     *
     * @param material the {@link Material} type
     * @param amount   the required quantity
     */
    public CraftingObject(Material material, int amount) {
        this.manager = null;
        this.vanillaItem = new ItemStack(material, amount);
        this.amount = amount;
        this.itemId = "vanilla:" + material.name();
    }

    /**
     * Returns a clone of the vanilla item stack.
     *
     * @return a cloned {@link ItemStack} or {@code null} if this is a custom item
     */
    public ItemStack getVanillaItem() {
        return vanillaItem == null ? null : vanillaItem.clone();
    }

    /**
     * Checks whether this crafting object represents a custom item.
     *
     * @return {@code true} if custom, {@code false} otherwise
     */
    public boolean isCustom() {
        return manager != null;
    }


    /**
     * Checks whether this crafting object represents a vanilla item.
     *
     * @return {@code true} if vanilla, {@code false} otherwise
     */
    public boolean isVanilla() {
        return vanillaItem != null;
    }

    /**
     * Returns the unique identifier of this crafting object.
     * Custom items use their manager's ID; vanilla items use the format {@code vanilla:MATERIAL_NAME}.
     *
     * @return the item identifier
     */
    public String getId() {
        return itemId;
    }
}
