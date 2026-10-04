package fr.moussax.blightedSMP.engine.player;

import fr.moussax.blightedSMP.BlightedSMP;
import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.ItemType;
import fr.moussax.blightedSMP.engine.items.abilities.AbilityTrigger;
import fr.moussax.blightedSMP.engine.items.abilities.CooldownEntry;
import fr.moussax.blightedSMP.engine.items.abilities.FullSetBonus;
import fr.moussax.blightedSMP.engine.items.abilities.ItemAbility;
import fr.moussax.blightedSMP.engine.items.equipment.ArmorSetManager;
import fr.moussax.blightedSMP.server.PluginSettings;
import fr.moussax.blightedSMP.server.database.PlayerDataHandler;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side domain context for a connected {@link Player}.
 *
 * <p>Tracks persistent player resources (gems, mana pool, forge fuel), equipment state,
 * active armor set bonuses, and ability cooldowns. Context instances are keyed by player
 * {@link UUID} and retrieved via {@link #get(Player)}.</p>
 */
public final class BlightedPlayer {

    private static final Map<UUID, BlightedPlayer> players = new ConcurrentHashMap<>();

    private static final double DEFAULT_MAX_MANA = 100;
    private static final double DEFAULT_MANA_REGEN_RATE = 0.5;

    @Getter
    private final Player player;
    @Getter
    private final UUID playerId;
    private final PlayerDataHandler dataHandler;

    @Getter
    private int gems;
    @Getter
    private double currentMana;
    @Getter
    private double maxMana;
    @Getter
    @Setter
    private double manaRegenerationRate;

    private final List<FullSetBonus> activeFullSetBonuses = new ArrayList<>();
    private final List<CooldownEntry> cooldowns = new ArrayList<>();
    private final EnumMap<ItemType, BlightedItem> armorPieces = new EnumMap<>(ItemType.class);

    private ItemStack[] lastKnownArmor = new ItemStack[4];
    @Getter
    @Setter
    private int forgeFuel;

    /**
     * Constructs a player context for an online Bukkit player.
     *
     * <p>This constructor is private; instances are instantiated exclusively through
     * {@link #get(Player)} to ensure registration in the active players map.</p>
     *
     * @param player player represented by this context
     */
    private BlightedPlayer(Player player) {
        this.player = player;
        this.playerId = player.getUniqueId();
        this.dataHandler = new PlayerDataHandler(playerId, player.getName());
        this.gems = dataHandler.getSavedGems();

        this.maxMana = DEFAULT_MAX_MANA;
        this.manaRegenerationRate = Optional.ofNullable(BlightedSMP.getInstance()).map(BlightedSMP::getSettings).map(PluginSettings::getDefaultManaRegenerationRate).orElse(DEFAULT_MANA_REGEN_RATE);
        setCurrentMana(dataHandler.getSavedMana());
        this.forgeFuel = dataHandler.getSavedForgeFuel();

        ArmorSetManager.updatePlayerArmor(this);
    }

    /**
     * Ticks periodic player lifecycle tasks, including passive mana regeneration.
     */
    public void tick() {
        regenerateMana();
    }

    /**
     * Retrieves the player context associated with a Bukkit player.
     *
     * @param player player whose context to retrieve
     * @return registered player context, or {@code null} if no context exists
     */
    public static BlightedPlayer get(Player player) {
        if (player == null || !player.isOnline()) return null;
        return players.computeIfAbsent(player.getUniqueId(), _ -> new BlightedPlayer(player));
    }

    /**
     * Returns an unmodifiable view of all active registered player contexts.
     *
     * @return unmodifiable view of active player contexts
     */
    public static Collection<BlightedPlayer> getPlayers() {
        return Collections.unmodifiableCollection(players.values());
    }

    /**
     * Removes and cleans up the player context associated with a player.
     *
     * @param player player whose context to remove
     */
    public static void removePlayer(Player player) {
        BlightedPlayer blightedPlayer = players.remove(player.getUniqueId());
        if (blightedPlayer != null) {
            blightedPlayer.cleanup();
        }
    }

    /**
     * Releases active set bonuses and equipment tracking held by this context.
     */
    private void cleanup() {
        clearActiveBonuses();
        clearArmorPieces();
    }

    /**
     * Returns an unmodifiable list of active ability cooldown entries.
     *
     * @return unmodifiable list of cooldown entries
     */
    public List<CooldownEntry> getCooldowns() {
        return Collections.unmodifiableList(cooldowns);
    }

    /**
     * Adds an ability cooldown entry.
     *
     * @param entry cooldown entry to add
     */
    public void addCooldown(CooldownEntry entry) {
        cooldowns.add(entry);
    }

    /**
     * Removes an ability cooldown entry.
     *
     * @param entry cooldown entry to remove
     */
    public void removeCooldown(CooldownEntry entry) {
        cooldowns.remove(entry);
    }

    /**
     * Sets or replaces the cooldown duration for a string key and ability trigger.
     *
     * @param key     cooldown key or ability name
     * @param trigger ability trigger associated with the cooldown
     * @param seconds cooldown duration in seconds
     */
    public void setCooldown(String key, AbilityTrigger trigger, int seconds) {
        long expire = System.currentTimeMillis() + (seconds * 1000L);
        cooldowns.removeIf(currentCooldown -> currentCooldown.key().equals(key) && currentCooldown.trigger() == trigger);
        cooldowns.add(new CooldownEntry(key, trigger, expire));
    }

    /**
     * Sets or replaces the cooldown duration for an ability class and ability trigger.
     *
     * @param abilityClass ability class associated with the cooldown
     * @param trigger      ability trigger associated with the cooldown
     * @param seconds      cooldown duration in seconds
     */
    @SuppressWarnings("rawtypes")
    public void setCooldown(Class<? extends ItemAbility> abilityClass, AbilityTrigger trigger, int seconds) {
        setCooldown(abilityClass != null ? abilityClass.getName() : "", trigger, seconds);
    }

    /**
     * Returns the remaining cooldown duration in seconds for a string key and ability trigger.
     *
     * <p>Expired cooldown entries are removed before performing the lookup.</p>
     *
     * @param key     cooldown key or ability name
     * @param trigger ability trigger associated with the cooldown
     * @return remaining cooldown in seconds, or {@code 0} if no active cooldown exists
     */
    public double getRemainingCooldown(String key, AbilityTrigger trigger) {
        cooldowns.removeIf(CooldownEntry::isExpired);

        for (CooldownEntry entry : cooldowns) {
            if (entry.key().equals(key) && entry.trigger() == trigger) {
                return entry.getRemainingCooldownTimeInSeconds();
            }
        }
        return 0;
    }

    /**
     * Returns the remaining cooldown duration in seconds for an ability class and ability trigger.
     *
     * <p>Expired cooldown entries are removed before performing the lookup.</p>
     *
     * @param abilityClass ability class associated with the cooldown
     * @param trigger      ability trigger associated with the cooldown
     * @return remaining cooldown in seconds, or {@code 0} if no active cooldown exists
     */
    @SuppressWarnings("rawtypes")
    public double getRemainingCooldown(Class<? extends ItemAbility> abilityClass, AbilityTrigger trigger) {
        return getRemainingCooldown(abilityClass != null ? abilityClass.getName() : "", trigger);
    }

    /**
     * Clears all tracked armor pieces.
     */
    public void clearArmorPieces() {
        armorPieces.clear();
    }

    /**
     * Resolves the custom item held in the player's main hand.
     *
     * @return custom item held in main hand, or {@code null} if non-custom or empty
     */
    public BlightedItem getEquippedItem() {
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        return BlightedItem.fromItemStack(mainHandItem);
    }

    /**
     * Returns an unmodifiable list of active armor full-set bonuses.
     *
     * @return unmodifiable list of active full-set bonuses
     */
    public List<FullSetBonus> getActiveFullSetBonuses() {
        return Collections.unmodifiableList(activeFullSetBonuses);
    }

    /**
     * Associates a custom item with an armor slot type.
     *
     * @param type         armor slot type
     * @param blightedItem custom item occupying the slot
     */
    public void addArmorPiece(ItemType type, BlightedItem blightedItem) {
        setArmorPiece(type, blightedItem);
    }

    /**
     * Deactivates and removes all active armor full-set bonuses.
     */
    public void clearActiveBonuses() {
        for (FullSetBonus bonus : activeFullSetBonuses) {
            bonus.deactivate();
        }
        activeFullSetBonuses.clear();
    }

    /**
     * Activates and registers an armor full-set bonus.
     *
     * @param bonus full-set bonus to activate
     */
    public void addActiveBonus(FullSetBonus bonus) {
        activeFullSetBonuses.add(bonus);
        bonus.activate();
    }

    /**
     * Deactivates and removes an armor full-set bonus.
     *
     * @param bonus full-set bonus to remove
     */
    public void removeActiveBonus(FullSetBonus bonus) {
        if (activeFullSetBonuses.remove(bonus)) {
            bonus.deactivate();
        }
    }

    /**
     * Evaluates whether an active full-set bonus of the specified type exists.
     *
     * @param bonusClass full-set bonus class to check
     * @return {@code true} if an active bonus of that type exists, {@code false} otherwise
     */
    public boolean hasFullSetBonus(Class<? extends FullSetBonus> bonusClass) {
        for (FullSetBonus bonus : activeFullSetBonuses) {
            if (bonusClass.isInstance(bonus)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deactivates and removes all active full-set bonuses matching the specified class.
     *
     * @param bonusClass full-set bonus class to remove
     */
    public void removeActiveBonusByClass(Class<? extends FullSetBonus> bonusClass) {
        activeFullSetBonuses.removeIf(bonus -> {
            if (bonusClass.isInstance(bonus)) {
                bonus.deactivate();
                return true;
            }
            return false;
        });
    }

    /**
     * Associates a custom armor item with an armor item type.
     *
     * @param type         armor item type
     * @param blightedItem custom armor item
     */
    public void setArmorPiece(ItemType type, BlightedItem blightedItem) {
        armorPieces.put(type, blightedItem);
    }

    /**
     * Returns the custom armor item associated with an armor item type.
     *
     * @param type armor item type to query
     * @return associated custom item, or {@code null} if none is equipped
     */
    public BlightedItem getArmorPiece(ItemType type) {
        return armorPieces.get(type);
    }

    /**
     * Sets current mana, clamped to {@code [0, maxMana]}.
     *
     * @param currentMana new current mana value
     */
    public void setCurrentMana(double currentMana) {
        if (currentMana < 0) currentMana = 0;
        if (currentMana > maxMana) currentMana = maxMana;
        this.currentMana = currentMana;
    }

    /**
     * Sets maximum mana capacity, reducing current mana if it exceeds the new maximum.
     *
     * @param maxMana new maximum mana capacity
     */
    public void setMaxMana(double maxMana) {
        this.maxMana = maxMana;
        if (this.currentMana > maxMana) this.currentMana = maxMana;
    }

    /**
     * Evaluates whether the player has at least the required mana amount.
     *
     * @param amount required mana amount
     * @return {@code true} if sufficient mana is available, {@code false} otherwise
     */
    public boolean hasMana(double amount) {
        return currentMana >= amount;
    }

    /**
     * Consumes mana if sufficient mana is available.
     *
     * @param amount mana amount to consume
     */
    public void consumeMana(double amount) {
        if (currentMana < amount) return;
        currentMana -= amount;
    }

    /**
     * Regenerates mana by the configured regeneration rate up to maximum capacity.
     */
    public void regenerateMana() {
        currentMana = Math.min(maxMana, currentMana + manaRegenerationRate);
    }

    /**
     * Evaluates whether the player has at least the required gem balance.
     *
     * @param amount required gem amount
     * @return {@code true} if sufficient gems are available, {@code false} otherwise
     */
    public boolean hasGems(int amount) {
        return gems >= amount;
    }

    /**
     * Adds gems to the player's balance.
     *
     * @param value gem amount to add
     */
    public void addGems(int value) {
        if (value <= 0) return;
        this.gems += value;
    }

    /**
     * Removes gems from the player's balance if sufficient funds exist.
     *
     * @param value gem amount to remove
     */
    public void removeGems(int value) {
        if (value <= 0 || gems < value) return;
        this.gems -= value;
    }

    /**
     * Sets the gem balance.
     *
     * @param value new gem balance; must be non-negative
     * @throws IllegalArgumentException if {@code value} is negative
     */
    public void setGems(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Gems value cannot be negative");
        }
        this.gems = value;
    }

    /**
     * Adds forge fuel to the player's balance.
     *
     * @param amount forge fuel amount to add
     */
    public void addForgeFuel(int amount) {
        this.forgeFuel += amount;
    }

    /**
     * Removes forge fuel, clamping the resulting balance to zero.
     *
     * @param amount forge fuel amount to remove
     */
    public void removeForgeFuel(int amount) {
        this.forgeFuel = Math.max(0, this.forgeFuel - amount);
    }

    /**
     * Adds an item stack to the player's inventory, ignoring {@code null} or air items.
     *
     * @param item item stack to add
     */
    public void addItemToInventory(ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        player.getInventory().addItem(item);
    }

    /**
     * Asynchronously persists resources and forge fuel to database storage.
     */
    public void saveData() {
        int gemsToSave = this.gems;
        double manaToSave = this.currentMana;
        int forgeFuelToSave = this.forgeFuel;
        Bukkit.getScheduler().runTaskAsynchronously(BlightedSMP.getInstance(), () -> dataHandler.save(gemsToSave, manaToSave, forgeFuelToSave));
    }

    /**
     * Returns a defensive copy of the player's last known armor state.
     *
     * @return copy of the last known armor array
     */
    public ItemStack[] getLastKnownArmor() {
        return Arrays.copyOf(lastKnownArmor, lastKnownArmor.length);
    }

    /**
     * Updates the player's last known armor state.
     *
     * @param armor new armor state array
     */
    public void setLastKnownArmor(ItemStack[] armor) {
        if (armor == null) {
            this.lastKnownArmor = new ItemStack[4];
            return;
        }
        this.lastKnownArmor = Arrays.copyOf(armor, 4);
    }
}
