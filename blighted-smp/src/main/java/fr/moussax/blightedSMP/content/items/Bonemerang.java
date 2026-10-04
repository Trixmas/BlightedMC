package fr.moussax.blightedSMP.content.items;

import fr.moussax.blightedSMP.content.items.abilities.BonemerangAbility;
import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.ItemRarity;
import fr.moussax.blightedSMP.engine.items.ItemType;
import fr.moussax.blightedSMP.registry.RegistryModule;
import org.bukkit.Material;

import java.util.function.Consumer;

public class Bonemerang implements RegistryModule<Consumer<BlightedItem>> {

    @Override
    public void register(Consumer<BlightedItem> registry) {
        BlightedItem bonemerang = new BlightedItem("BONEMERANG", ItemType.BOW, ItemRarity.EPIC, Material.BONE);
        bonemerang.setDisplayName("Bonemerang");
        bonemerang.addAbility(new BonemerangAbility());
        bonemerang.glow();
        bonemerang.unstackable();

        registry.accept(bonemerang);
    }
}
