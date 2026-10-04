package fr.moussax.blightedSMP.content.items;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.ItemRarity;
import fr.moussax.blightedSMP.engine.items.ItemType;
import fr.moussax.blightedSMP.registry.RegistryModule;
import org.bukkit.Material;

import java.util.function.Consumer;

public class ThermalFuels implements RegistryModule<Consumer<BlightedItem>> {

    @Override
    public void register(Consumer<BlightedItem> registry) {
        BlightedItem enchantedCoal = new BlightedItem("ENCHANTED_COAL", ItemType.MATERIAL, ItemRarity.UNCOMMON, Material.COAL);
        enchantedCoal.setDisplayName("Enchanted Coal");
        enchantedCoal.addLore(
                "§8Thermal Fuel",
                "",
                " §7Ultra-dense carbon radiating",
                " §7with intense heat that adds",
                " §6\uD83E\uDEA3 3,000mB §7to a refuelable",
                " §7machine.",
                ""
        );
        enchantedCoal.glow();

        BlightedItem enchantedLavaBucket = new BlightedItem("ENCHANTED_LAVA_BUCKET", ItemType.UNCATEGORIZED, ItemRarity.RARE, Material.LAVA_BUCKET);
        enchantedLavaBucket.setDisplayName("Enchanted Lava Bucket");
        enchantedLavaBucket.addLore(
                "§8Thermal Fuel",
                "",
                " §7Enriched lava capable of",
                " §7prolonged burning that adds",
                " §6\uD83E\uDEA3 10,000mB §7to a refuelable",
                " §7machine.",
                ""
        );
        enchantedLavaBucket.glow();
        enchantedLavaBucket.preventBucketInteractions();

        BlightedItem magmaBucket = new BlightedItem("MAGMA_BUCKET", ItemType.UNCATEGORIZED, ItemRarity.EPIC, Material.LAVA_BUCKET);
        magmaBucket.setDisplayName("Magma Bucket");
        magmaBucket.addLore(
                "§8Thermal Fuel",
                "",
                " §7A superheated amalgam of",
                " §7compressed magma that adds",
                " §6\uD83E\uDEA3 20,000mB §7to a refuelable",
                " §7machine.",
                ""
        );
        magmaBucket.glow();
        magmaBucket.preventBucketInteractions();

        BlightedItem plasmaBucket = new BlightedItem("PLASMA_BUCKET", ItemType.UNCATEGORIZED, ItemRarity.LEGENDARY, Material.LAVA_BUCKET);
        plasmaBucket.setDisplayName("Plasma Bucket");
        plasmaBucket.addLore(
                "§8Thermal Fuel",
                "",
                " §7Stable ionized matter containing",
                " §7stellar-grade heat that adds",
                " §6\uD83E\uDEA3 50,000mB §7to a refuelable",
                " §7machine.",
                ""
        );
        plasmaBucket.glow();
        plasmaBucket.preventBucketInteractions();

        registry.accept(enchantedCoal);
        registry.accept(enchantedLavaBucket);
        registry.accept(magmaBucket);
        registry.accept(plasmaBucket);
    }
}
