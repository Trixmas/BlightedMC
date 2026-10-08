package fr.moussax.blightedSMP.content.equipment.tools;

import fr.moussax.blightedSMP.content.equipment.abilities.tools.VeinmineAbility;
import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.ItemRarity;
import fr.moussax.blightedSMP.engine.items.ItemType;
import fr.moussax.blightedSMP.registry.RegistryModule;
import org.bukkit.Material;

import java.util.function.Consumer;

public class BlightedTools implements RegistryModule<Consumer<BlightedItem>> {

    @Override
    public void register(Consumer<BlightedItem> registry) {
        BlightedItem magmaRod = new BlightedItem("MAGMA_ROD", ItemType.LAVA_FISHING_ROD, ItemRarity.RARE, Material.FISHING_ROD);
        magmaRod.setDisplayName("Magma Rod");
        magmaRod.description(
                "Impervious to the inferno,",
                "this rod casts where others",
                "burn to dredge §6molten depths",
                "for treasures."
        );
        magmaRod.setFireResistant(true);

        BlightedItem voidRod = new BlightedItem("VOID_ROD", ItemType.VOID_FISHING_ROD, ItemRarity.RARE, Material.FISHING_ROD);
        voidRod.setDisplayName("Void Rod");
        voidRod.description("Demonstration tool");

        BlightedItem demoPickaxe = new BlightedItem("DEMO_PICKAXE", ItemType.PICKAXE, ItemRarity.SPECIAL, Material.DIAMOND_PICKAXE);
        demoPickaxe.setDisplayName("Demo Pickaxe");
        demoPickaxe.addAbility(new VeinmineAbility());
        demoPickaxe.description("Demonstration tool");

        registry.accept(demoPickaxe);
        registry.accept(magmaRod);
        registry.accept(voidRod);
    }
}
