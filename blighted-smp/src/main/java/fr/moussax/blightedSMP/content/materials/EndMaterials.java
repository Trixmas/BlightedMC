package fr.moussax.blightedSMP.content.materials;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.ItemRarity;
import fr.moussax.blightedSMP.engine.items.ItemType;
import fr.moussax.blightedSMP.registry.RegistryModule;
import org.bukkit.Material;

import java.util.function.Consumer;

public class EndMaterials implements RegistryModule<Consumer<BlightedItem>> {
    @Override
    public void register(Consumer<BlightedItem> registry) {
        BlightedItem enchantedEnderPearl = new BlightedItem("ENCHANTED_ENDER_PEARL", ItemType.MATERIAL, ItemRarity.COMMON, Material.ENDER_PEARL);
        enchantedEnderPearl.setDisplayName("Enchanted Ender Pearl");
        enchantedEnderPearl.glow();
        enchantedEnderPearl.preventProjectileLaunch();
        enchantedEnderPearl.flushRarity();

        BlightedItem enchantedEndstone = new BlightedItem("ENCHANTED_END_STONE", ItemType.MATERIAL, ItemRarity.COMMON, Material.END_STONE);
        enchantedEndstone.setDisplayName("Enchanted End Stone");
        enchantedEndstone.glow();
        enchantedEndstone.preventPlacement();
        enchantedEndstone.flushRarity();

        BlightedItem enchantedChorusFruit = new BlightedItem("ENCHANTED_CHORUS_FRUIT", ItemType.MATERIAL, ItemRarity.COMMON, Material.CHORUS_FRUIT);
        enchantedChorusFruit.setDisplayName("Enchanted Chorus Fruit");
        enchantedChorusFruit.glow();
        enchantedChorusFruit.flushRarity();

        BlightedItem voidResidue = new BlightedItem("VOID_RESIDUE", ItemType.MATERIAL, ItemRarity.RARE, Material.PURPLE_DYE);
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
