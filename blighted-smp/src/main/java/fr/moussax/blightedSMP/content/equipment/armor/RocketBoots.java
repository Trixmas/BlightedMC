package fr.moussax.blightedSMP.content.equipment.armor;

import fr.moussax.blightedSMP.content.equipment.abilities.RocketBootsAbility;
import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.ItemRarity;
import fr.moussax.blightedSMP.engine.items.ItemType;
import fr.moussax.blightedSMP.registry.RegistryModule;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

public class RocketBoots implements RegistryModule<Consumer<BlightedItem>> {

    @Override
    public void register(Consumer<BlightedItem> registry) {
        BlightedItem rocketBoots = new BlightedItem("ROCKET_BOOTS", ItemType.BOOTS, ItemRarity.RARE, Material.LEATHER_BOOTS);
        rocketBoots.setDisplayName("Rocket Boots");

        rocketBoots.setLeatherColor("#B02E26").setArmorTrim(TrimMaterial.QUARTZ, TrimPattern.BOLT);
        rocketBoots.addItemFlag(ItemFlag.HIDE_DYE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ARMOR_TRIM);

        rocketBoots.setFullSetBonus(new RocketBootsAbility());
        rocketBoots.setMaxDurability(140);

        registry.accept(rocketBoots);
    }
}
