package fr.moussax.blightedSMP.engine.quest;

import fr.moussax.blightedSMP.BlightedSMP;
import fr.moussax.blightedSMP.engine.entities.BlightedEntity;
import fr.moussax.blightedSMP.engine.entities.EntityManager;
import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.player.cinematic.WorkbenchCinematic;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Objects;

/**
 * Handles progression triggers for the Echoing Twisted Orb, Blighted soul harvesting,
 * and the Forgotten Workbench awakening ritual.
 */
public final class BlightedQuestListener implements Listener {
    public static final int REQUIRED_RITUAL_SOULS = 10;

    private final BlightedSMP plugin = BlightedSMP.getInstance();

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();
        Player killer = deadEntity.getKiller();

        if (killer != null) {
            BlightedEntity blightedEntity = EntityManager.getBlightedEntity(deadEntity);
            if (blightedEntity != null) {
                harvestSoulToOrb(killer, deadEntity.getLocation());
            }
        }
    }

    private void harvestSoulToOrb(Player player, Location entityDeathLocation) {
        ItemStack orbItem = null;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                BlightedItem blightedItem = BlightedItem.fromItemStack(item);
                if (blightedItem != null && "ECHOING_TWISTED_ORB".equals(blightedItem.getItemId())) {
                    orbItem = item;
                    break;
                }
            }
        }

        if (orbItem == null) return;

        applySoulToOrb(player, orbItem, entityDeathLocation);
    }

    private void applySoulToOrb(Player player, ItemStack orbItem, Location deathLocation) {
        ItemMeta meta = orbItem.getItemMeta();
        if (meta == null) return;

        NamespacedKey trappedSoulsKey = new NamespacedKey(plugin, "souls_trapped");
        PersistentDataContainer persistentDataContainer = meta.getPersistentDataContainer();

        int trappedSoulsCount = 0;
        if (persistentDataContainer.has(trappedSoulsKey, PersistentDataType.INTEGER)) {
            Integer storedValue = persistentDataContainer.get(trappedSoulsKey, PersistentDataType.INTEGER);
            if (storedValue != null) {
                trappedSoulsCount = storedValue;
            }
        }

        int updatedTrappedSoulsCount = trappedSoulsCount + 1;
        persistentDataContainer.set(trappedSoulsKey, PersistentDataType.INTEGER, updatedTrappedSoulsCount);

        NamespacedKey absorbedSoulKey = new NamespacedKey(plugin, "cipher_absorbed_soul");
        persistentDataContainer.set(absorbedSoulKey, PersistentDataType.BOOLEAN, true);

        List<String> lore = meta.getLore();
        if (lore != null) {
            for (int lineIndex = 0; lineIndex < lore.size(); lineIndex++) {
                String line = lore.get(lineIndex);
                if (line.contains("Souls Bound:")) {
                    lore.set(lineIndex, "§8 Souls Bound: §3" + updatedTrappedSoulsCount + " ☠");
                    break;
                }
            }
            meta.setLore(lore);
        }

        orbItem.setItemMeta(meta);

        if (deathLocation.getWorld() != null) {
            Location playerChest = player.getLocation().add(0, 1.0, 0);
            Vector toPlayer = playerChest.toVector().subtract(deathLocation.toVector()).normalize().multiply(0.4);
            deathLocation.getWorld().spawnParticle(Particle.SOUL, deathLocation.add(0, 0.8, 0), 0, toPlayer.getX(), toPlayer.getY(), toPlayer.getZ(), 0.15);
            player.spawnParticle(Particle.SCULK_SOUL, playerChest, 6, 0.2, 0.3, 0.2, 0.02);
        }

        // Restrained audio feedback
        player.playSound(player.getLocation(), Sound.PARTICLE_SOUL_ESCAPE, 0.55f, 1.4f);
        player.playSound(player.getLocation(), Sound.BLOCK_SCULK_CHARGE, 0.45f, 1.3f);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.CRAFTING_TABLE) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();

        if (!isEchoingTwistedOrb(mainHand)) {
            return;
        }

        // The player is trying to interact with a Crafting Table while holding the Echoing Twisted Orb
        if (hasEnoughSouls(mainHand, REQUIRED_RITUAL_SOULS)
                && hasGlowInkSac(player)
                && hasTwistedBanner(player)
                && hasForgottenPattern(player)) {
            event.setCancelled(true);

            if (WorkbenchCinematic.isRitualActiveAt(clickedBlock.getLocation())) {
                return;
            }

            // Note: Consumables (Glowing Ink Sac & Forgotten Pattern) and soul reset
            // are NOT consumed up-front. They are consumed only upon successful completion
            // inside ForgottenWorkbenchCinematic, ensuring items are protected if interrupted.
            new WorkbenchCinematic(plugin, player, clickedBlock, mainHand).start();
        }
    }

    private boolean isEchoingTwistedOrb(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        BlightedItem blightedItem = BlightedItem.fromItemStack(item);
        return blightedItem != null && "ECHOING_TWISTED_ORB".equals(blightedItem.getItemId());
    }

    private boolean hasEnoughSouls(ItemStack item, int requiredSouls) {
        if (item == null || !item.hasItemMeta()) return false;
        NamespacedKey trappedSoulsKey = new NamespacedKey(plugin, "souls_trapped");
        PersistentDataContainer persistentDataContainer = Objects.requireNonNull(item.getItemMeta()).getPersistentDataContainer();
        Integer stored = persistentDataContainer.get(trappedSoulsKey, PersistentDataType.INTEGER);
        return stored != null && stored >= requiredSouls;
    }

    private boolean hasGlowInkSac(Player player) {
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        if (offHandItem.getType() == Material.GLOW_INK_SAC && offHandItem.getAmount() >= 1) {
            return true;
        }
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() == Material.GLOW_INK_SAC && item.getAmount() >= 1) {
                return true;
            }
        }
        return false;
    }

    private boolean hasForgottenPattern(Player player) {
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item != null && item.getType() != Material.AIR) {
                BlightedItem blighted = BlightedItem.fromItemStack(item);
                if (blighted != null && "FORGOTTEN_PATTERN".equals(blighted.getItemId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasTwistedBanner(Player player) {
        ItemStack helmetItem = player.getInventory().getHelmet();
        if (helmetItem == null || helmetItem.getType() == Material.AIR) return false;
        BlightedItem blightedItem = BlightedItem.fromItemStack(helmetItem);
        return blightedItem != null && "TWISTED_BANNER".equals(blightedItem.getItemId());
    }
}
