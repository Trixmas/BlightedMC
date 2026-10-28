package fr.moussax.blightedSMP.engine.player;

import fr.moussax.blightedSMP.BlightedSMP;
import fr.moussax.blightedSMP.engine.entities.BlightedEntity;
import fr.moussax.blightedSMP.engine.entities.EntityManager;
import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.player.cinematic.FirstJoinCinematic;
import org.bukkit.ChatColor;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        BlightedPlayer.get(event.getPlayer());
        event.setJoinMessage(" §f" + event.getPlayer().getName() + " §7joined the SMP.");

        if (!player.hasPlayedBefore()) {
            new FirstJoinCinematic(BlightedSMP.getInstance(), player).start();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        clearTargetedMobs(player);

        BlightedPlayer blighted = BlightedPlayer.get(player);
        if (blighted != null) {
            blighted.saveData();
            BlightedPlayer.removePlayer(player);
        }
        event.setQuitMessage(" §f" + player.getName() + " §7left the SMP.");
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (FirstJoinCinematic.isCinematicActive(player)) {
                event.setCancelled(true);
                return;
            }

            if (!event.isCancelled()) {
                BlightedPlayer blighted = BlightedPlayer.get(player);
                if (blighted != null) {
                    blighted.onDamageTaken();
                }
            }
        }
    }

    private static final Map<UUID, List<ItemStack>> SAVED_SOULBOUND_ITEMS = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deadPlayer = event.getEntity();

        clearTargetedMobs(deadPlayer);

        if (!event.getKeepInventory()) {
            List<ItemStack> drops = event.getDrops();
            List<ItemStack> kept = new ArrayList<>();
            for (Iterator<ItemStack> iterator = drops.iterator(); iterator.hasNext(); ) {
                ItemStack drop = iterator.next();
                if (BlightedItem.isSoulbound(drop)) {
                    kept.add(drop.clone());
                    iterator.remove();
                }
            }
            if (!kept.isEmpty()) {
                SAVED_SOULBOUND_ITEMS.put(deadPlayer.getUniqueId(), kept);
                persistSoulboundItems(deadPlayer.getUniqueId(), kept);
            }
        }

        String deathMessage = event.getDeathMessage();
        if (deathMessage == null) return;

        Entity killer = event.getEntity().getKiller();
        if (killer == null && event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent damageEvent) {
            killer = damageEvent.getDamager();
            if (killer instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
                killer = shooter;
            }
        }

        if (killer == null) return;

        BlightedEntity blighted = EntityManager.getBlightedEntity(killer);
        if (blighted == null) return;

        String victimName = event.getEntity().getName();
        String blightedCreature = blighted.getName();
        String customNameWithHealth = killer.getCustomName();

        if (customNameWithHealth != null && deathMessage.contains(customNameWithHealth)) {
            event.setDeathMessage(deathMessage.replace(customNameWithHealth, blightedCreature));
            return;
        }

        String strippedMessage = ChatColor.stripColor(deathMessage);
        String strippedKiller = ChatColor.stripColor(customNameWithHealth != null ? customNameWithHealth : killer.getName());

        if (strippedMessage.contains(strippedKiller)) {
            String action = strippedMessage.replace(victimName, "").replace(strippedKiller, "").trim();
            event.setDeathMessage(String.format("§r%s %s %s", victimName, action, blightedCreature));
        }
    }

    private void clearTargetedMobs(Player targetPlayer) {
        for (BlightedEntity blighted : EntityManager.getActiveEntities()) {
            LivingEntity entity = blighted.getEntity();
            if (entity instanceof Mob mob && targetPlayer.equals(mob.getTarget())) {
                mob.setTarget(null);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        List<ItemStack> saved = loadAndRemoveSoulboundItems(player.getUniqueId());
        if (saved != null && !saved.isEmpty()) {
            for (ItemStack item : saved) {
                var leftover = player.getInventory().addItem(item);
                for (ItemStack overflow : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), overflow);
                }
            }
        }
    }

    private void persistSoulboundItems(UUID playerId, List<ItemStack> items) {
        File folder = new File(BlightedSMP.getInstance().getDataFolder(), "soulbound");
        if (!folder.exists() && !folder.mkdirs()) {
            return;
        }
        File targetFile = new File(folder, playerId.toString() + ".bin");
        try (var output = new BukkitObjectOutputStream(new FileOutputStream(targetFile))) {
            output.writeInt(items.size());
            for (ItemStack item : items) {
                output.writeObject(item);
            }
        } catch (IOException exception) {
            BlightedSMP.getInstance().getLogger().warning("Failed to persist soulbound items for " + playerId + ": " + exception.getMessage());
        }
    }

    private List<ItemStack> loadAndRemoveSoulboundItems(UUID playerId) {
        List<ItemStack> inMemory = SAVED_SOULBOUND_ITEMS.remove(playerId);
        File folder = new File(BlightedSMP.getInstance().getDataFolder(), "soulbound");
        File targetFile = new File(folder, playerId.toString() + ".bin");
        if (!targetFile.exists()) {
            return inMemory;
        }

        List<ItemStack> loadedItems = new ArrayList<>();
        try (var input = new BukkitObjectInputStream(new FileInputStream(targetFile))) {
            int count = input.readInt();
            for (int index = 0; index < count; index++) {
                loadedItems.add((ItemStack) input.readObject());
            }
        } catch (IOException | ClassNotFoundException exception) {
            BlightedSMP.getInstance().getLogger().warning("Failed to load soulbound items for " + playerId + ": " + exception.getMessage());
        } finally {
            targetFile.delete();
        }
        return inMemory != null && !inMemory.isEmpty() ? inMemory : loadedItems;
    }
}
