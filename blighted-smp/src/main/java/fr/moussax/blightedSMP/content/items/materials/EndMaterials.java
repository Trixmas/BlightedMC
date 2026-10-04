package fr.moussax.blightedSMP.content.items.materials;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.ItemRarity;
import fr.moussax.blightedSMP.engine.items.ItemType;
import fr.moussax.blightedSMP.registry.RegistryModule;
import org.bukkit.Material;

import java.util.function.Consumer;

public class EndMaterials implements RegistryModule<Consumer<BlightedItem>> {
    @Override
    public void register(Consumer<BlightedItem> registry) {
        BlightedItem enchantedEnderPearl = new BlightedItem("ENCHANTED_ENDER_PEARL", ItemType.MATERIAL, ItemRarity.UNCOMMON, Material.ENDER_PEARL);
        enchantedEnderPearl.setDisplayName("Enchanted Ender Pearl");
        enchantedEnderPearl.glow();
        enchantedEnderPearl.preventProjectileLaunch();

        BlightedItem enchantedEndstone = new BlightedItem("ENCHANTED_END_STONE", ItemType.MATERIAL, ItemRarity.UNCOMMON, Material.END_STONE);
        enchantedEndstone.setDisplayName("Enchanted End Stone");
        enchantedEndstone.glow();
        enchantedEndstone.preventPlacement();

        BlightedItem enchantedChorusFruit = new BlightedItem("ENCHANTED_CHORUS_FRUIT", ItemType.MATERIAL, ItemRarity.UNCOMMON, Material.CHORUS_FRUIT);
        enchantedChorusFruit.setDisplayName("Enchanted Chorus Fruit");
        enchantedChorusFruit.glow();

        BlightedItem voidResidue = new BlightedItem("VOID_RESIDUE", ItemType.MATERIAL, ItemRarity.UNCOMMON, Material.PURPLE_DYE);
        voidResidue.setDisplayName("Voidling Residue");
        voidResidue.description(
                "The tangible byproduct of entropy,",
                "harvested from the §5Outer Islands",
                "where reality begins to fray."
        );
        voidResidue.glow();

        registry.accept(enchantedEnderPearl);
        registry.accept(enchantedEndstone);
        registry.accept(enchantedChorusFruit);
        registry.accept(voidResidue);
    }
}
