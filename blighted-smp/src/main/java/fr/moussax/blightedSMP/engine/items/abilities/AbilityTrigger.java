package fr.moussax.blightedSMP.engine.items.abilities;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Defines trigger conditions and interaction modes for item abilities.
 */
@Getter
public enum AbilityTrigger {

    /**
     * Ability triggered by right-clicking.
     */
    RIGHT_CLICK("§d§lRIGHT CLICK"),

    /**
     * Ability triggered by left-clicking.
     */
    LEFT_CLICK("§d§lLEFT CLICK"),

    /**
     * Ability triggered by clicking.
     */
    CLICK("§d§lCLICK"),

    /**
     * Ability triggered by sneaking and right-clicking.
     */
    SNEAK_RIGHT_CLICK("§d§lSNEAK RIGHT CLICK"),

    /**
     * Ability triggered by sneaking and left-clicking.
     */
    SNEAK_LEFT_CLICK("§d§lSNEAK LEFT CLICK"),

    /**
     * Ability triggered by sneaking and clicking.
     */
    SNEAK_CLICK("§d§lSNEAK CLICK"),

    /**
     * Ability triggered on attacking an entity.
     */
    ENTITY_HIT("§d§lON HIT"),

    /**
     * Ability triggered when breaking a block.
     */
    BLOCK_BREAK("§d§lON BREAK"),

    /**
     * Ability triggered when block drops are generated.
     */
    BLOCK_DROP("§d§lPASSIVE");

    private final String displayName;

    AbilityTrigger(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Determines whether a Bukkit event satisfies this trigger condition.
     *
     * @param event Bukkit event to evaluate
     * @return {@code true} if the event satisfies this trigger condition, {@code false} otherwise
     */
    public boolean matches(Event event) {
        if (event instanceof PlayerInteractEvent interactEvent) {
            Action action = interactEvent.getAction();
            boolean isSneaking = interactEvent.getPlayer().isSneaking();
            boolean isLeft = (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);
            boolean isRight = (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK);

            return switch (this) {
                case RIGHT_CLICK -> isRight && !isSneaking;
                case LEFT_CLICK -> isLeft && !isSneaking;
                case CLICK -> (isLeft || isRight) && !isSneaking;
                case SNEAK_RIGHT_CLICK -> isRight && isSneaking;
                case SNEAK_LEFT_CLICK -> isLeft && isSneaking;
                case SNEAK_CLICK -> (isLeft || isRight) && isSneaking;
                default -> false;
            };
        }

        if (event instanceof EntityDamageByEntityEvent) {
            return this == ENTITY_HIT;
        }

        if (event instanceof BlockBreakEvent) {
            return this == BLOCK_BREAK;
        }

        if (event instanceof BlockDropItemEvent) {
            return this == BLOCK_DROP;
        }

        return false;
    }
}
