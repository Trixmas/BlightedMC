package fr.moussax.blightedSMP.engine.loot.results.gems;

import fr.moussax.blightedSMP.BlightedSMP;
import fr.moussax.blightedSMP.content.sound.BlightedSounds;
import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.abilities.ItemAbility;
import fr.moussax.blightedSMP.engine.items.abilities.AbilityTrigger;
import fr.moussax.blightedSMP.engine.items.registry.ItemRegistry;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import fr.moussax.bedrock.text.Messenger;
import org.bukkit.NamespacedKey;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Representation of a consumable Blighted Gemstone item carrying a gem quantity.
 *
 * @param amount quantity of gems carried by this gemstone item
 */
public record GemsItem(int amount) implements Supplier<ItemStack> {

    private static final NamespacedKey GEMS_KEY = BlightedSMP.getInstance() != null
            ? new NamespacedKey(BlightedSMP.getInstance(), "gems")
            : NamespacedKey.fromString("blightedsmp:gems");

    /**
     * Constructs a GemsItem by reading the gem quantity from an existing item stack's persistent data.
     *
     * @param itemStack item stack containing gemstone persistent data
     */
    public GemsItem(ItemStack itemStack) {
        ItemMeta meta = Objects.requireNonNull(itemStack.getItemMeta(), "itemMeta cannot be null");

        Integer value = meta.getPersistentDataContainer().get(GEMS_KEY, PersistentDataType.INTEGER);
        this(value != null ? value : 1);
    }

    /**
     * Adds this gemstone's gem quantity to a player's balance.
     *
     * @param player player receiving the gems
     */
    public void addGems(BlightedPlayer player) {
        player.addGems(amount);
    }

    /**
     * Ability handler for consuming Blighted Gemstone items on player interaction.
     */
    public static class BlightedGemstoneAbility implements ItemAbility<PlayerInteractEvent> {

        @Override
        public String getName() {
            return "Consume Gems";
        }

        @Override
        public AbilityTrigger getTrigger() {
            return AbilityTrigger.RIGHT_CLICK;
        }

        @Override
        public String[] getDescription() {
            return new String[]{
                    "Right click to consume."
            };
        }

        @Override
        public boolean triggerAbility(PlayerInteractEvent event) {
            if (event.getItem() == null) return false;
            BlightedPlayer blightedPlayer = BlightedPlayer.get(event.getPlayer());
            GemsItem gemsItem = new GemsItem(event.getItem());

            if (gemsItem.amount <= 0) {
                Messenger.warn(event.getPlayer(), "This gemstone doesn't have any gems to redeem.");
                return false;
            }

            gemsItem.addGems(blightedPlayer);
            event.getPlayer().sendMessage("§8§l +§d" + gemsItem.amount + "✵ Gems §8(Blighted Gemstone)");
            BlightedSounds.BLIGHTED_GEMSTONE_CONSUME.play(event.getPlayer().getLocation());
            event.getPlayer().getInventory().remove(event.getItem());
            event.setCancelled(true);
            return true;
        }
    }

    /**
     * Constructs the {@link ItemStack} for this gemstone with lore and persistent data applied.
     *
     * @return constructed gemstone item stack
     */
    @Override
    public ItemStack get() {
        BlightedItem prototype = ItemRegistry.getOrThrow("BLIGHTED_GEMSTONE");
        ItemStack itemStack = prototype.toItemStack();

        ItemMeta meta = Objects.requireNonNull(itemStack.getItemMeta(), "itemMeta cannot be null");
        meta.getPersistentDataContainer().set(GEMS_KEY, PersistentDataType.INTEGER, amount);

        java.util.List<String> lore = meta.getLore();
        if (lore != null) {
            lore.replaceAll(line -> line.contains("Gems:") ? "§8 Gems: §d" + this.amount + "✵" : line);
            meta.setLore(lore);
        }

        itemStack.setItemMeta(meta);
        return itemStack;
    }
}
