package fr.moussax.blightedSMP.engine.items;

import lombok.Getter;

/**
 * Defines the rarity tier of a BlightedMC item.
 *
 * <p>Uses a lean structure inspired by Minecraft Dungeons with a Special tier:
 * Common, Rare, Unique, and Special.</p>
 */
@Getter
public enum ItemRarity {

    /**
     * Standard gear, baseline crafting ingredients, and utility items.
     */
    COMMON("§f§lCOMMON", "§f"),

    /**
     * Infused items possessing custom abilities, perks, or stat enhancements.
     */
    RARE("§b§lRARE", "§b"),

    /**
     * Distinctive, build-defining artifacts and boss relics with innate powers.
     */
    UNIQUE("§6§lUNIQUE", "§6"),

    /**
     * Reserved for one-of-a-kind relics, seasonal events, or administrative items.
     */
    SPECIAL("§c§lSPECIAL", "§c");

    private final String name;
    private final String colorPrefix;

    ItemRarity(String name, String colorPrefix) {
        this.name = name;
        this.colorPrefix = colorPrefix;
    }
}
