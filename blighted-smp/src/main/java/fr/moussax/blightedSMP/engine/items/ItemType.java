package fr.moussax.blightedSMP.engine.items;

import lombok.Getter;

/**
 * Defines the functional classification and category mapping for custom items.
 */
@Getter
public enum ItemType {

    /**
     * Helmet armor piece.
     */
    HELMET(Category.ARMOR, "HELMET"),
    /**
     * Chestplate armor piece.
     */
    CHESTPLATE(Category.ARMOR, "CHESTPLATE"),
    /**
     * Leggings armor piece.
     */
    LEGGINGS(Category.ARMOR, "LEGGINGS"),
    /**
     * Boots armor piece.
     */
    BOOTS(Category.ARMOR, "BOOTS"),

    /**
     * One-handed sword melee weapon.
     */
    SWORD(Category.MELEE_WEAPON, "SWORD"),
    /**
     * Two-handed longsword melee weapon.
     */
    LONGSWORD(Category.MELEE_WEAPON, "LONGSWORD"),
    /**
     * Thrown or thrusting spear weapon.
     */
    SPEAR(Category.RANGE_WEAPON, "SPEAR"),
    /**
     * Magical wand weapon.
     */
    WAND(Category.MELEE_WEAPON, "WAND"),

    /**
     * Ranged bow weapon.
     */
    BOW(Category.RANGE_WEAPON, "BOW"),

    /**
     * Mining pickaxe tool.
     */
    PICKAXE(Category.TOOLS, "PICKAXE"),
    /**
     * Mining drill tool.
     */
    DRILL(Category.TOOLS, "DRILL"),
    /**
     * Woodcutting axe tool.
     */
    AXE(Category.TOOLS, "AXE"),
    /**
     * Farming hoe tool.
     */
    HOE(Category.TOOLS, "HOE"),
    /**
     * Excavating shovel tool.
     */
    SHOVEL(Category.TOOLS, "SHOVEL"),

    /**
     * Standard fishing rod.
     */
    FISHING_ROD(Category.TOOLS, "FISHING ROD"),
    /**
     * Specialized lava fishing rod.
     */
    LAVA_FISHING_ROD(Category.TOOLS, "FISHING ROD"),
    /**
     * Specialized void fishing rod.
     */
    VOID_FISHING_ROD(Category.TOOLS, "FISHING ROD"),

    /**
     * Crafting material item.
     */
    MATERIAL(Category.MATERIAL, null),

    /**
     * Placeable custom block item.
     */
    BLOCK(Category.BLOCKS, null),

    /**
     * Uncategorized or miscellaneous item.
     */
    UNCATEGORIZED(Category.MISCELLANEOUS, null);

    private final Category category;
    private final String displayName;

    ItemType(Category category, String displayName) {
        this.category = category;
        this.displayName = displayName;
    }

    /**
     * Broad grouping for item types used in inventory filtering and display categories.
     */
    public enum Category {
        ARMOR,
        MELEE_WEAPON,
        RANGE_WEAPON,
        TOOLS,
        BLOCKS,
        MATERIAL,
        MISCELLANEOUS
    }
}
