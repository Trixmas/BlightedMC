package fr.moussax.blightedSMP.engine.items.listeners;

import fr.moussax.blightedSMP.BlightedSMP;
import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.abilities.AbilityExecutor;
import fr.moussax.blightedSMP.engine.items.abilities.ItemAbility;
import fr.moussax.blightedSMP.engine.items.equipment.ArmorSetManager;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Listens for Bukkit interaction, block break, and inventory events to trigger item abilities and schedule armor updates.
 */
public final class ItemAbilityListener implements Listener {
    private final Set<UUID> dirtyArmorPlayers = new HashSet<>();
    private boolean updateTaskScheduled = false;

    private void scheduleArmorUpdate(Player player) {
        if (player == null) return;
        dirtyArmorPlayers.add(player.getUniqueId());

        if (updateTaskScheduled) return;

        updateTaskScheduled = true;
        Bukkit.getScheduler().runTask(BlightedSMP.getInstance(), this::processArmorUpdates);
    }

    private void processArmorUpdates() {
        updateTaskScheduled = false;
        if (dirtyArmorPlayers.isEmpty()) return;

        for (UUID uuid : dirtyArmorPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;

            BlightedPlayer blightedPlayer = BlightedPlayer.get(player);
            if (blightedPlayer == null) continue;

            ArmorSetManager.updatePlayerArmor(blightedPlayer);
        }
        dirtyArmorPlayers.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        boolean isArmorSlot = event.getSlotType() == InventoryType.SlotType.ARMOR;
        boolean isNumberKey = event.getClick().name().contains("NUMBER_KEY");

        if (isArmorSlot || event.isShiftClick() || isNumberKey) {
            scheduleArmorUpdate(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        for (int slot : event.getRawSlots()) {
            if (slot >= 5 && slot <= 8) { // Standard survival inventory armor slots
                scheduleArmorUpdate(player);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDispenseArmor(BlockDispenseArmorEvent event) {
        if (event.getTargetEntity() instanceof Player player) {
            scheduleArmorUpdate(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uniqueId = event.getPlayer().getUniqueId();
        dirtyArmorPlayers.remove(uniqueId);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemBreak(PlayerItemBreakEvent event) {
        scheduleArmorUpdate(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        scheduleArmorUpdate(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        scheduleArmorUpdate(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        scheduleArmorUpdate(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneakToggle(PlayerToggleSneakEvent event) {
        BlightedPlayer blightedPlayer = BlightedPlayer.get(event.getPlayer());
        if (blightedPlayer != null) {
            ArmorSetManager.handleSneakUpdate(blightedPlayer, event.isSneaking());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getItem() != null && isArmorMaterial(event.getItem().getType().name())) {
            scheduleArmorUpdate(event.getPlayer());
        }

        // Prevent double triggering from off-hand interactions
        if (event.getHand() != EquipmentSlot.HAND) return;

        if (event.getItem() != null) {
            BlightedItem blightedItem = BlightedItem.fromItemStack(event.getItem());
            if (blightedItem != null && blightedItem.isRecipePreviewEnabled()) {
                Action action = event.getAction();
                if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                    if (blightedItem.openRecipePreview(event.getPlayer(), null)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        trigger(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        trigger(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDropItem(org.bukkit.event.block.BlockDropItemEvent event) {
        trigger(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            trigger(player, event);
        }
    }

    private boolean isArmorMaterial(String name) {
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || name.equals("ELYTRA");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T extends Event> void trigger(Player player, T event) {
        BlightedPlayer blightedPlayer = BlightedPlayer.get(player);
        if (blightedPlayer == null) return;

        BlightedItem blightedItem = null;

        if (event instanceof PlayerInteractEvent interactEvent) {
            if (interactEvent.getItem() != null) {
                blightedItem = BlightedItem.fromItemStack(interactEvent.getItem());
            }
        } else if (event instanceof BlockBreakEvent || event instanceof org.bukkit.event.block.BlockDropItemEvent || event instanceof EntityDamageByEntityEvent) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (!mainHand.getType().isAir()) {
                blightedItem = BlightedItem.fromItemStack(mainHand);
            }
        } else {
            blightedItem = blightedPlayer.getEquippedItem();
        }

        if (blightedItem == null) return;

        List<ItemAbility<? extends Event>> abilities = blightedItem.getAbilities();
        if (abilities.isEmpty()) return;

        for (ItemAbility<? extends Event> ability : abilities) {
            if (ability.getTrigger().matches(event)) {
                AbilityExecutor.execute((ItemAbility) ability, blightedPlayer, event);
            }
        }
    }
}
