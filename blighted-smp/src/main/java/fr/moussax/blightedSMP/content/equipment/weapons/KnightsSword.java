package fr.moussax.blightedSMP.content.equipment.weapons;

import fr.moussax.blightedSMP.content.equipment.abilities.KnightsSlamAbility;
import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.ItemRarity;
import fr.moussax.blightedSMP.engine.items.ItemType;
import fr.moussax.blightedSMP.registry.RegistryModule;
import org.bukkit.Material;

import java.util.function.Consumer;

public class KnightsSword implements RegistryModule<Consumer<BlightedItem>> {

    @Override
    public void register(Consumer<BlightedItem> registry) {
        BlightedItem knightSword = new BlightedItem("ANCIENT_KNIGHT_SWORD", ItemType.LONGSWORD, ItemRarity.UNIQUE, Material.NETHERITE_SWORD);
        knightSword.setDisplayName("Knight's Sword");
        knightSword.setAttackDamage(10);
        knightSword.setAttackSpeed(1.2);
        knightSword.addAbility(new KnightsSlamAbility());

        registry.accept(knightSword);
    }
}
