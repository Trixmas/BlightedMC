package fr.moussax.blightedSMP.content.items;

import fr.moussax.blightedSMP.content.items.abilities.WitherImpactAbility;
import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.ItemRarity;
import fr.moussax.blightedSMP.engine.items.ItemType;
import fr.moussax.blightedSMP.registry.RegistryModule;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;

import java.util.function.Consumer;

public class Hyperion implements RegistryModule<Consumer<BlightedItem>> {

    @Override
    public void register(Consumer<BlightedItem> registry) {
        BlightedItem hyperion = new BlightedItem("HYPERION", ItemType.SWORD, ItemRarity.UNIQUE, Material.IRON_SWORD);
        hyperion.setDisplayName("Hyperion");
        hyperion.setUnbreakable(true);
        hyperion.addItemFlag(ItemFlag.HIDE_UNBREAKABLE);
        hyperion.addAbility(new WitherImpactAbility());

        registry.accept(hyperion);
    }
}
