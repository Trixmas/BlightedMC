package fr.moussax.blightedSMP.engine.quest;

import fr.moussax.bedrock.text.Messenger;
import fr.moussax.blightedSMP.BlightedSMP;
import fr.moussax.blightedSMP.content.entities.factions.blightsworn.BlightswornCreature;
import fr.moussax.blightedSMP.engine.entities.BlightedEntity;
import fr.moussax.blightedSMP.engine.entities.EntityManager;
import fr.moussax.blightedSMP.engine.items.BlightedItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Objects;

public final class BlightedQuestListener implements Listener {
    private final BlightedSMP plugin = BlightedSMP.getInstance();

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity deadEntity = event.getEntity();
        Player killer = deadEntity.getKiller();

        if (killer != null) {
            BlightedEntity blightedEntity = EntityManager.getBlightedEntity(deadEntity);
            if (blightedEntity instanceof BlightswornCreature) {
                incrementCodexTrappedSouls(killer);
            }
        }
    }

    private void incrementCodexTrappedSouls(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                BlightedItem blightedItem = BlightedItem.fromItemStack(item);
                if (blightedItem != null && "BLIGHTED_CODEX".equals(blightedItem.getItemId())) {
                    applySoulToCodex(player, item);
                    break;
                }
            }
        }
    }

    private void applySoulToCodex(Player player, ItemStack codexItem) {
        ItemMeta meta = codexItem.getItemMeta();
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
                if (line.contains("Souls trapped:")) {
                    lore.set(lineIndex, "§8 Souls trapped: §d" + updatedTrappedSoulsCount + " ☠");
                    break;
                }
            }
            meta.setLore(lore);
        }

        codexItem.setItemMeta(meta);

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f);
        player.sendMessage("§d ⚚ §fThe §dBlighted Codex§f has absorbed a §3Blighted soul§f!");
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getInventory().getType() == InventoryType.ENCHANTING) {
            if (event.getPlayer() instanceof Player player) {
                if (isBlightedCodex(player.getInventory().getItemInMainHand())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isRightOrLeftClickOnBlock(event) && event.getHand() == EquipmentSlot.HAND) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && clickedBlock.getType() == Material.ENCHANTING_TABLE) {
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    attemptRitualStart(event, clickedBlock);
                }
            }
        }
    }

    private boolean isRightOrLeftClickOnBlock(PlayerInteractEvent event) {
        return event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK;
    }

    private void attemptRitualStart(PlayerInteractEvent event, Block clickedBlock) {
        Player player = event.getPlayer();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();

        if (isBlightedCodex(mainHandItem)) {
            event.setCancelled(true);

            if (!hasClearWorkspace(clickedBlock)) {
                Messenger.warn(player, "The ritual requires a clear space of 5 blocks on all sides and 5 blocks above the table.");
                return;
            }

            if (hasAbsorbedSoul(mainHandItem) && hasAmethystShard(player) && hasBlightedBanner(player)) {
                consumeRitualItems(player);

                player.sendMessage("§d ⚚ §fYou have solved the §dCodex Riddle§f. The ritual begins...");
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (!onlinePlayer.equals(player)) {
                        onlinePlayer.sendMessage("§d ⚚ §7" + player.getName() + " §fhas solved the §dCodex Riddle§f.");
                    }
                }
                new BlightedRitualAnimation(plugin, player, clickedBlock).runTaskTimer(plugin, 0L, 1L);
            }
        }
    }

    private boolean hasClearWorkspace(Block center) {
        for (int offsetX = -5; offsetX <= 5; offsetX++) {
            for (int offsetY = 1; offsetY <= 5; offsetY++) {
                for (int offsetZ = -5; offsetZ <= 5; offsetZ++) {
                    if (!center.getRelative(offsetX, offsetY, offsetZ).isPassable()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean isBlightedCodex(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        BlightedItem blightedItem = BlightedItem.fromItemStack(item);
        return blightedItem != null && "BLIGHTED_CODEX".equals(blightedItem.getItemId());
    }

    private boolean hasAbsorbedSoul(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        NamespacedKey absorbedSoulKey = new NamespacedKey(plugin, "cipher_absorbed_soul");
        PersistentDataContainer persistentDataContainer = Objects.requireNonNull(item.getItemMeta()).getPersistentDataContainer();

        if (persistentDataContainer.has(absorbedSoulKey, PersistentDataType.BOOLEAN)) {
            return Boolean.TRUE.equals(persistentDataContainer.get(absorbedSoulKey, PersistentDataType.BOOLEAN));
        }
        if (persistentDataContainer.has(absorbedSoulKey, PersistentDataType.BYTE)) {
            Byte byteValue = persistentDataContainer.get(absorbedSoulKey, PersistentDataType.BYTE);
            return byteValue != null && byteValue == 1;
        }
        return false;
    }

    private boolean hasAmethystShard(Player player) {
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        return offHandItem.getType() == Material.AMETHYST_SHARD && offHandItem.getAmount() >= 1;
    }

    private boolean hasBlightedBanner(Player player) {
        ItemStack helmetItem = player.getInventory().getHelmet();
        if (helmetItem == null || helmetItem.getType() == Material.AIR) return false;
        BlightedItem blightedItem = BlightedItem.fromItemStack(helmetItem);
        return blightedItem != null && "TWISTED_BANNER".equals(blightedItem.getItemId());
    }

    private void consumeRitualItems(Player player) {
        player.getInventory().setHelmet(null);

        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        if (offHandItem.getAmount() <= 1) {
            player.getInventory().setItemInOffHand(null);
        } else {
            offHandItem.setAmount(offHandItem.getAmount() - 1);
        }
        player.getInventory().setItemInMainHand(null);
    }
}
