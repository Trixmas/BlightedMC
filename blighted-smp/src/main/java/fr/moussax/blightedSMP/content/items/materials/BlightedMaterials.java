package fr.moussax.blightedSMP.content.items.materials;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.ItemRarity;
import fr.moussax.blightedSMP.engine.items.ItemType;
import fr.moussax.blightedSMP.registry.RegistryModule;
import java.util.function.Consumer;
import org.bukkit.Material;

public class BlightedMaterials implements RegistryModule<Consumer<BlightedItem>> {

    @Override
    public void register(Consumer<BlightedItem> registry) {
        BlightedItem enchantedIronIngot =
                createMaterialItem("ENCHANTED_IRON_INGOT", ItemType.MATERIAL, ItemRarity.COMMON, Material.IRON_INGOT,
                        "Enchanted Iron Ingot", false, false);

        BlightedItem enchantedIronBlock =
                createMaterialItem("ENCHANTED_IRON_BLOCK", ItemType.MATERIAL, ItemRarity.RARE, Material.IRON_BLOCK,
                        "Enchanted Iron Block", true, false);

        BlightedItem enchantedCopperIngot =
                createMaterialItem("ENCHANTED_COPPER_INGOT", ItemType.MATERIAL, ItemRarity.COMMON, Material.COPPER_INGOT,
                        "Enchanted Copper Ingot", false, false);

        BlightedItem enchantedGoldIngot =
                createMaterialItem("ENCHANTED_GOLD_INGOT", ItemType.MATERIAL, ItemRarity.COMMON, Material.GOLD_INGOT,
                        "Enchanted Gold Ingot", false, false);

        BlightedItem enchantedLapisLazuli =
                createMaterialItem("ENCHANTED_LAPIS_LAZULI", ItemType.MATERIAL, ItemRarity.COMMON, Material.LAPIS_LAZULI,
                        "Enchanted Lapis Lazuli", false, false);

        BlightedItem enchantedRedstone =
                createMaterialItem("ENCHANTED_REDSTONE", ItemType.MATERIAL, ItemRarity.COMMON, Material.REDSTONE,
                        "Enchanted Redstone", false, false);

        BlightedItem enchantedAmethystShard =
                createMaterialItem("ENCHANTED_AMETHYST_SHARD", ItemType.MATERIAL, ItemRarity.COMMON, Material.AMETHYST_SHARD,
                        "Enchanted Amethyst Shard", false, false);

        BlightedItem enchantedEmerald =
                createMaterialItem("ENCHANTED_EMERALD", ItemType.MATERIAL, ItemRarity.COMMON, Material.EMERALD,
                        "Enchanted Emerald", false, false);

        BlightedItem enchantedRottenFlesh =
                createMaterialItem("ENCHANTED_ROTTEN_FLESH", ItemType.MATERIAL, ItemRarity.COMMON, Material.ROTTEN_FLESH,
                        "Enchanted Rotten Flesh", false, false);

        BlightedItem enchantedBone =
                createMaterialItem("ENCHANTED_BONE", ItemType.MATERIAL, ItemRarity.COMMON, Material.BONE,
                        "Enchanted Bone", false, false);

        BlightedItem enchantedString =
                createMaterialItem("ENCHANTED_STRING", ItemType.MATERIAL, ItemRarity.COMMON, Material.STRING,
                        "Enchanted String", false, false);

        BlightedItem enchantedGunpowder =
                createMaterialItem("ENCHANTED_GUNPOWDER", ItemType.MATERIAL, ItemRarity.COMMON, Material.GUNPOWDER,
                        "Enchanted Gunpowder", false, false);

        BlightedItem enchantedSpiderEye =
                createMaterialItem("ENCHANTED_SPIDER_EYE", ItemType.MATERIAL, ItemRarity.COMMON, Material.SPIDER_EYE,
                        "Enchanted Spider Eye", false, false);

        BlightedItem enchantedSlimeBall =
                createMaterialItem("ENCHANTED_SLIME_BALL", ItemType.MATERIAL, ItemRarity.COMMON, Material.SLIME_BALL,
                        "Enchanted Slime Ball", false, false);

        BlightedItem enchantedPhantomMembrane =
                createMaterialItem("ENCHANTED_PHANTOM_MEMBRANE", ItemType.MATERIAL, ItemRarity.COMMON, Material.PHANTOM_MEMBRANE,
                        "Enchanted Phantom Membrane", false, false);

        BlightedItem enchantedResinClump =
                createMaterialItem("ENCHANTED_RESIN_CLUMP", ItemType.MATERIAL, ItemRarity.COMMON, Material.RESIN_CLUMP,
                        "Enchanted Resin Clump", false, false);

        BlightedItem enchantedCobblestone =
                createMaterialItem("ENCHANTED_COBBLESTONE", ItemType.MATERIAL, ItemRarity.COMMON, Material.COBBLESTONE,
                        "Enchanted Cobblestone", true, false);

        BlightedItem enchantedObsidian =
                createMaterialItem("ENCHANTED_OBSIDIAN", ItemType.MATERIAL, ItemRarity.RARE, Material.OBSIDIAN,
                        "Enchanted Obsidian", true, false);

        BlightedItem enchantedPaper =
                createMaterialItem("ENCHANTED_PAPER", ItemType.MATERIAL, ItemRarity.COMMON, Material.PAPER,
                        "Enchanted Paper", false, false);

        BlightedItem enchantedClayBall =
                createMaterialItem("ENCHANTED_CLAY_BALL", ItemType.MATERIAL, ItemRarity.COMMON, Material.CLAY_BALL,
                        "Enchanted Clay Ball", false, false);

        BlightedItem enchantedCod =
                createMaterialItem("ENCHANTED_COD", ItemType.MATERIAL, ItemRarity.COMMON, Material.COD,
                        "Enchanted Cod", false, true);

        BlightedItem enchantedSalmon =
                createMaterialItem("ENCHANTED_SALMON", ItemType.MATERIAL, ItemRarity.COMMON, Material.SALMON,
                        "Enchanted Salmon", false, true);

        BlightedItem enchantedTropicalFish =
                createMaterialItem("ENCHANTED_TROPICAL_FISH", ItemType.MATERIAL, ItemRarity.COMMON, Material.TROPICAL_FISH,
                        "Enchanted Tropical Fish", false, true);

        BlightedItem enchantedPufferfish =
                createMaterialItem("ENCHANTED_PUFFERFISH", ItemType.MATERIAL, ItemRarity.COMMON, Material.PUFFERFISH,
                        "Enchanted Pufferfish", false, true);

        BlightedItem enchantedSeaPickle =
                createMaterialItem("ENCHANTED_SEA_PICKLE", ItemType.MATERIAL, ItemRarity.COMMON, Material.SEA_PICKLE,
                        "Enchanted Sea Pickle", true, false);

        registry.accept(enchantedIronIngot);
        registry.accept(enchantedIronBlock);
        registry.accept(enchantedCopperIngot);
        registry.accept(enchantedGoldIngot);
        registry.accept(enchantedLapisLazuli);
        registry.accept(enchantedRedstone);
        registry.accept(enchantedAmethystShard);
        registry.accept(enchantedEmerald);
        registry.accept(enchantedRottenFlesh);
        registry.accept(enchantedBone);
        registry.accept(enchantedString);
        registry.accept(enchantedGunpowder);
        registry.accept(enchantedSpiderEye);
        registry.accept(enchantedSlimeBall);
        registry.accept(enchantedPhantomMembrane);
        registry.accept(enchantedResinClump);
        registry.accept(enchantedCobblestone);
        registry.accept(enchantedObsidian);
        registry.accept(enchantedPaper);
        registry.accept(enchantedClayBall);
        registry.accept(enchantedCod);
        registry.accept(enchantedSalmon);
        registry.accept(enchantedTropicalFish);
        registry.accept(enchantedPufferfish);
        registry.accept(enchantedSeaPickle);
    }

    private BlightedItem createMaterialItem(
            String id,
            ItemType type,
            ItemRarity rarity,
            Material material,
            String displayName,
            boolean preventPlacement,
            boolean preventConsume
    ) {
        BlightedItem item = new BlightedItem(id, type, rarity, material);
        item.setDisplayName(displayName);
        item.glow();
        item.flushRarity();

        if (preventPlacement) {
            item.preventPlacement();
        }

        if (preventConsume) {
            item.preventConsume();
        }

        return item;
    }
}
