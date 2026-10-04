package fr.moussax.blightedSMP.content.items.armors;

import fr.moussax.blightedSMP.content.items.abilities.HomodeusFlightAbility;
import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.ItemRarity;
import fr.moussax.blightedSMP.engine.items.ItemType;
import fr.moussax.blightedSMP.engine.items.abilities.FullSetBonus;
import fr.moussax.blightedSMP.registry.RegistryModule;
import org.bukkit.Material;

import java.util.function.Consumer;

public class HomodeusArmor implements RegistryModule<Consumer<BlightedItem>> {

    @Override
    public void register(Consumer<BlightedItem> registry) {
        FullSetBonus homodeusFlightBonus = new HomodeusFlightAbility();

        BlightedItem homodeusHelmet = new BlightedItem("HOMODEUS_HELMET", ItemType.HELMET, ItemRarity.LEGENDARY, Material.LEATHER_HELMET);
        homodeusHelmet.setDisplayName("Homodeus Helmet");
        setupHomodeusPiece(homodeusHelmet, homodeusFlightBonus);

        BlightedItem homodeusChestplate = new BlightedItem("HOMODEUS_CHESTPLATE", ItemType.CHESTPLATE, ItemRarity.LEGENDARY, Material.LEATHER_CHESTPLATE);
        homodeusChestplate.setDisplayName("Homodeus Chestplate");
        setupHomodeusPiece(homodeusChestplate, homodeusFlightBonus);

        BlightedItem homodeusLeggings = new BlightedItem("HOMODEUS_LEGGINGS", ItemType.LEGGINGS, ItemRarity.LEGENDARY, Material.LEATHER_LEGGINGS);
        homodeusLeggings.setDisplayName("Homodeus Leggings");
        setupHomodeusPiece(homodeusLeggings, homodeusFlightBonus);

        BlightedItem homodeusBoots = new BlightedItem("HOMODEUS_BOOTS", ItemType.BOOTS, ItemRarity.LEGENDARY, Material.LEATHER_BOOTS);
        homodeusBoots.setDisplayName("Homodeus Boots");
        setupHomodeusPiece(homodeusBoots, homodeusFlightBonus);

        registry.accept(homodeusHelmet);
        registry.accept(homodeusChestplate);
        registry.accept(homodeusLeggings);
        registry.accept(homodeusBoots);
    }

    private void setupHomodeusPiece(BlightedItem piece, FullSetBonus bonus) {
        piece.unbreakable();
        piece.glow();
        piece.setLeatherColor("#ffffff");
        piece.hideAllFlags();

        piece.setFullSetBonus(bonus);
    }
}
