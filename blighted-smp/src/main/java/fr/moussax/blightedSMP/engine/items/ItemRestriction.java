package fr.moussax.blightedSMP.engine.items;

/**
 * Defines behavioral restriction flags for custom items.
 */
public enum ItemRestriction {

    /** Prevents the item from being placed as a block in the world. */
    PREVENT_PLACEMENT,

    /** Prevents the item from being eaten or drank. */
    PREVENT_CONSUMPTION,

    /** Prevents projectile launching (e.g. throwing pearls, firing bows). */
    PREVENT_PROJECTILE_LAUNCH,

    /** Prevents emptying or filling bucket items. */
    PREVENT_BUCKET_INTERACTIONS,

    /** Prevents the item from being dropped by a player. */
    PREVENT_DROP,

    /** Prevents generic interaction with the item. */
    PREVENT_INTERACTION
}
