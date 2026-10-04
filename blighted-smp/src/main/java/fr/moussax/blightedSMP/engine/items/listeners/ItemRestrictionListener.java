package fr.moussax.blightedSMP.engine.items.listeners;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.ItemRestriction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Event listener enforcing item restriction flags and processing custom item consumption callbacks.
 */
public final class ItemRestrictionListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack itemStack = event.getItem();
        BlightedItem item = BlightedItem.fromItemStack(itemStack);

        if (item != null && item.hasRestriction(ItemRestriction.PREVENT_INTERACTION)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        boolean mainHandRestricted = checkAndRestrictHandItem(player, player.getInventory().getItemInMainHand());
        boolean offHandRestricted = checkAndRestrictHandItem(player, player.getInventory().getItemInOffHand());

        if (mainHandRestricted || offHandRestricted) {
            event.setCancelled(true);
        }
    }

    private boolean checkAndRestrictHandItem(Player player, ItemStack itemStack) {
        BlightedItem item = BlightedItem.fromItemStack(itemStack);
        if (item != null && item.hasRestriction(ItemRestriction.PREVENT_PROJECTILE_LAUNCH)) {
            player.setCooldown(itemStack.getType(), 0);
            return true;
        }
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        ItemStack handItem = event.getPlayer().getInventory().getItem(event.getHand());
        BlightedItem item = BlightedItem.fromItemStack(handItem);
        if (item != null && item.hasRestriction(ItemRestriction.PREVENT_BUCKET_INTERACTIONS)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        BlightedItem item = BlightedItem.fromItemStack(event.getItemInHand());
        if (item != null && item.hasRestriction(ItemRestriction.PREVENT_PLACEMENT)) {
            event.setCancelled(true);
            event.getPlayer().updateInventory();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        BlightedItem item = BlightedItem.fromItemStack(event.getItemDrop().getItemStack());
        if (item != null && item.hasRestriction(ItemRestriction.PREVENT_DROP)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        BlightedItem item = BlightedItem.fromItemStack(event.getItem());
        if (item == null) return;

        if (item.hasRestriction(ItemRestriction.PREVENT_CONSUMPTION)) {
            event.setCancelled(true);
            return;
        }

        if (item.getConsumeHandler() != null) {
            item.getConsumeHandler().onConsume(event.getPlayer(), event.getItem());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        BlightedItem item = BlightedItem.fromItemStack(event.getCurrentItem());
        if (item != null && item.hasRestriction(ItemRestriction.PREVENT_INTERACTION)) {
            event.setCancelled(true);
        }
    }
}
