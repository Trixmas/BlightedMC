package fr.moussax.blightedSMP.engine.blocks;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

/**
 * Base class for custom BlightedMC blocks.
 * <p>
 * Wraps a {@link Material} and a {@link BlightedItem}, providing hooks
 * for block events such as placement, interaction, and breaking.
 * Subclasses can override event methods to implement custom behavior.
 */
@Getter
public abstract class BlightedBlock {

    protected final Material material;
    protected final BlightedItem blightedItem;

    public BlightedBlock(@NonNull Material material, @NonNull BlightedItem blightedItem) {
        this.material = material;
        this.blightedItem = blightedItem;
    }

    /**
     * Returns the unique ID of this block's associated item.
     *
     * @return item ID
     */
    public String getId() {
        return blightedItem.getItemId();
    }

    /**
     * Called when the block is placed in the world.
     * <p>
     * Override to add custom placement behavior.
     *
     * @param event the block placement event
     */
    public void onPlace(BlockPlaceEvent event) {
    }

    /**
     * Called when a player interacts with this block.
     * <p>
     * Override to add custom interaction behavior.
     *
     * @param event the player interaction event
     */
    public void onInteract(PlayerInteractEvent event) {
    }

    /**
     * Called when the block is broken.
     * <p>
     * Override to modify or replace the dropped item.
     *
     * @param event       the block break event
     * @param droppedItem the default item to drop (can be modified or null)
     * @return the item to drop, or null for no drop
     */
    public ItemStack onBreak(BlockBreakEvent event, ItemStack droppedItem) {
        return droppedItem;
    }

    /**
     * Called when the block is broken without a player interaction event.
     * <p>
     * This method is used in situations like explosions where a BlockBreakEvent is not available.
     * Override to modify or replace the dropped item.
     *
     * @param droppedItem the default item to drop (can be modified or null)
     * @return the item to drop, or null for no drop
     */
    public ItemStack onBreak(ItemStack droppedItem) {
        return droppedItem;
    }
}
