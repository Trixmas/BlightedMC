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
        enchantedCoal.description(
                "§8Thermal Fuel",
                "",
                "Ultra-dense carbon radiating",
                "with intense heat that adds",
                "§6\uD83E\uDEA3 3,000mB §7to a refuelable",
                "machine."
        );
        enchantedCoal.glow();

        BlightedItem enchantedLavaBucket = new BlightedItem("ENCHANTED_LAVA_BUCKET", ItemType.UNCATEGORIZED, ItemRarity.RARE, Material.LAVA_BUCKET);
        enchantedLavaBucket.setDisplayName("Enchanted Lava Bucket");
        enchantedLavaBucket.description(
                "§8Thermal Fuel",
                "",
                "Enriched lava capable of",
                "prolonged burning that adds",
                "§6\uD83E\uDEA3 10,000mB §7to a refuelable",
                "machine."
        );
        enchantedLavaBucket.glow();
        enchantedLavaBucket.preventBucketInteractions();

        BlightedItem magmaBucket = new BlightedItem("MAGMA_BUCKET", ItemType.UNCATEGORIZED, ItemRarity.EPIC, Material.LAVA_BUCKET);
        magmaBucket.setDisplayName("Magma Bucket");
        magmaBucket.description(
                "§8Thermal Fuel",
                "",
                "A superheated amalgam of",
                "compressed magma that adds",
                "§6\uD83E\uDEA3 20,000mB §7to a refuelable",
                "machine."
        );
        magmaBucket.glow();
        magmaBucket.preventBucketInteractions();

        BlightedItem plasmaBucket = new BlightedItem("PLASMA_BUCKET", ItemType.UNCATEGORIZED, ItemRarity.LEGENDARY, Material.LAVA_BUCKET);
        plasmaBucket.setDisplayName("Plasma Bucket");
        plasmaBucket.description(
                "§8Thermal Fuel",
                "",
                "Stable ionized matter containing",
                "stellar-grade heat that adds",
                "§6\uD83E\uDEA3 50,000mB §7to a refuelable",
                "machine."
        );
        plasmaBucket.glow();
        plasmaBucket.preventBucketInteractions();

        registry.accept(enchantedCoal);
        registry.accept(enchantedLavaBucket);
        registry.accept(magmaBucket);
        registry.accept(plasmaBucket);
    }
}
