package fr.moussax.blightedSMP.engine.loot.results.blightstone;

import fr.moussax.bedrock.text.Formatter;
import fr.moussax.bedrock.text.Messenger;
import fr.moussax.bedrock.ui.actionbar.Actionbar;
import fr.moussax.blightedSMP.BlightedSMP;
import fr.moussax.blightedSMP.content.sound.BlightedSounds;
import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.abilities.AbilityTrigger;
import fr.moussax.blightedSMP.engine.items.abilities.ItemAbility;
import fr.moussax.blightedSMP.engine.items.registry.ItemRegistry;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import fr.moussax.blightedSMP.engine.player.hud.PlayerHudManager;
import org.bukkit.NamespacedKey;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Representation of a consumable Resonant Blightstone item carrying a Blight currency quantity.
 *
 * @param amount quantity of Blight carried by this blightstone item
 */
public record ResonantBlightstoneItem(int amount) implements Supplier<ItemStack> {

    public static final NamespacedKey BLIGHT_KEY = BlightedSMP.getInstance() != null
            ? new NamespacedKey(BlightedSMP.getInstance(), "blight")
            : NamespacedKey.fromString("blightedsmp:blight");

    /**
     * Constructs a ResonantBlightstoneItem by reading the currency quantity from an existing item stack's persistent data.
     *
     * @param itemStack item stack containing blightstone persistent data
     */
    public ResonantBlightstoneItem(ItemStack itemStack) {
        ItemMeta meta = Objects.requireNonNull(itemStack.getItemMeta(), "itemMeta cannot be null");
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer value = pdc.get(BLIGHT_KEY, PersistentDataType.INTEGER);
        this(value != null ? value : 1);
    }

    /**
     * Adds this blightstone's Blight quantity to a player's balance.
     *
     * @param player player receiving the Blight
     */
    public void addBlight(BlightedPlayer player) {
        player.addBlight(amount);
    }

    /**
     * Ability handler for consuming Resonant Blightstone items on player interaction.
     */
    public static class ResonantBlightstoneAbility implements ItemAbility<PlayerInteractEvent> {

        @Override
        public String getName() {
            return "Consume Blightstone";
        }

        @Override
        public AbilityTrigger getTrigger() {
            return AbilityTrigger.RIGHT_CLICK;
        }

        @Override
        public boolean hasLore() {
            return false;
        }

        @Override
        public boolean triggerAbility(PlayerInteractEvent event) {
            if (event.getItem() == null) return false;
            BlightedPlayer blightedPlayer = BlightedPlayer.get(event.getPlayer());
            if (blightedPlayer == null) return false;

            ResonantBlightstoneItem blightstone = new ResonantBlightstoneItem(event.getItem());

            if (blightstone.amount <= 0) {
                Messenger.warn(event.getPlayer(), "This blightstone doesn't have any Blight to redeem.");
                return false;
            }

            blightstone.addBlight(blightedPlayer);
            Actionbar.sendSlotAlert(
                    event.getPlayer(),
                    PlayerHudManager.SECTION_BLIGHT,
                    "§3" + Formatter.formatDecimalWithCommas(blightedPlayer.getBlight()) + "❖ Blight §b(+" + Formatter.formatDecimalWithCommas(blightstone.amount) + ")",
                    Duration.ofSeconds(2)
            );
            BlightedSounds.RESONANT_BLIGHTSTONE_CONSUME.play(event.getPlayer().getLocation());
            BlightstoneConsumptionEffect.play(event.getPlayer());

            EquipmentSlot hand = event.getHand();
            if (hand != null) {
                ItemStack handItem = event.getPlayer().getInventory().getItem(hand);
                if (handItem != null && !handItem.getType().isAir()) {
                    if (handItem.getAmount() > 1) {
                        handItem.setAmount(handItem.getAmount() - 1);
                    } else {
                        event.getPlayer().getInventory().setItem(hand, null);
                    }
                }
            } else {
                ItemStack item = event.getItem();
                if (item != null) {
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        item.setAmount(0);
                    }
                }
            }
            event.setCancelled(true);
            return true;
        }
    }

    /**
     * Constructs the {@link ItemStack} for this blightstone with lore and persistent data applied.
     *
     * @return constructed blightstone item stack
     */
    @Override
    public ItemStack get() {
        BlightedItem prototype = ItemRegistry.getOrThrow("RESONANT_BLIGHTSTONE");
        ItemStack itemStack = prototype.toItemStack();

        ItemMeta meta = Objects.requireNonNull(itemStack.getItemMeta(), "itemMeta cannot be null");
        meta.getPersistentDataContainer().set(BLIGHT_KEY, PersistentDataType.INTEGER, amount);

        List<String> lore = meta.getLore();
        if (lore != null) {
            lore.replaceAll(line -> line.contains("Blight") ? "§8 Blight Sealed: §3" + this.amount + "❖" : line);
            meta.setLore(lore);
        }

        itemStack.setItemMeta(meta);
        return itemStack;
    }
}
