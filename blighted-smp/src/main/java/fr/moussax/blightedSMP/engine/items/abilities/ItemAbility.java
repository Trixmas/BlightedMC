package fr.moussax.blightedSMP.engine.items.abilities;

import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import org.bukkit.event.Event;

/**
 * Defines execution logic, lifecycle hooks, and resource requirements for an item ability.
 *
 * @param <T> Bukkit event type that triggers this ability
 */
public interface ItemAbility<T extends Event> {

    /**
     * Gets the display name of this ability.
     *
     * @return ability display name
     */
    String getName();

    /**
     * Gets the trigger condition for this ability.
     *
     * @return ability trigger
     */
    AbilityTrigger getTrigger();

    /**
     * Executes the ability logic when triggered by an event.
     *
     * @param event triggering Bukkit event
     * @return {@code true} if ability execution succeeded, {@code false} otherwise
     */
    boolean triggerAbility(T event);

    /**
     * Gets the cooldown duration applied after triggering this ability.
     *
     * @return cooldown duration in seconds, defaulting to {@code 0}
     */
    default int getCooldownSeconds() {
        return 0;
    }

    /**
     * Gets the mana cost required to activate this ability.
     *
     * @return required mana cost, defaulting to {@code 0}
     */
    default int getManaCost() {
        return 0;
    }

    /**
     * Evaluates whether a player meets requirements to trigger this ability.
     *
     * @param player player context to evaluate
     * @return {@code true} if the player can activate this ability, {@code false} otherwise
     */
    default boolean canTrigger(BlightedPlayer player) {
        return true;
    }

    /**
     * Called immediately following successful ability execution.
     *
     * @param player player context that activated the ability
     */
    default void start(BlightedPlayer player) {
    }

    /**
     * Cleans up active ability effects or background tasks for a player.
     *
     * @param player player context owning the active ability
     */
    default void stop(BlightedPlayer player) {
    }

    /**
     * Determines whether the triggering Bukkit event should be cancelled.
     *
     * @param success outcome returned by {@link #triggerAbility(Event)}
     * @return {@code true} to cancel the triggering event, {@code false} to allow normal resolution
     */
    default boolean cancelEvent(boolean success) {
        return true;
    }

    /**
     * Gets description lines for item tooltip lore display.
     *
     * @return array of description lore lines
     */
    default String[] getDescription() {
        return new String[0];
    }

    /**
     * Determines whether this ability should be rendered in item tooltip lore.
     *
     * @return {@code true} if ability lore should be rendered, {@code false} to suppress it
     */
    default boolean hasLore() {
        return true;
    }
}
