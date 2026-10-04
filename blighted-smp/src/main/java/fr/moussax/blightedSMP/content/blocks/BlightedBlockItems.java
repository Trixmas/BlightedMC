package fr.moussax.blightedSMP.content.blocks;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.ItemRarity;
import fr.moussax.blightedSMP.engine.items.ItemType;
import fr.moussax.blightedSMP.registry.RegistryModule;
import org.bukkit.Material;

import java.util.function.Consumer;

public class BlightedBlockItems implements RegistryModule<Consumer<BlightedItem>> {

    @Override
    public void register(Consumer<BlightedItem> registry) {

        BlightedItem blightedWorkbench = new BlightedItem("BLIGHTED_WORKBENCH", ItemType.BLOCK, ItemRarity.UNCOMMON, Material.ENCHANTING_TABLE);
        blightedWorkbench.setDisplayName("Blighted Workbench");
        blightedWorkbench.addLore(
                "§8Placeable Block",
                "",
                " §7A crafting table infused with ",
                " §5blighted energy §7capable",
                " §7of weaving forbidden magic",
                " §7into physical form."
        );
        blightedWorkbench.glow();

        BlightedItem blightedForge = new BlightedItem("BLIGHTED_FORGE", ItemType.BLOCK, ItemRarity.RARE, Material.BLAST_FURNACE);
        blightedForge.setDisplayName("Blighted Forge");
        blightedForge.addLore(
                "§8Placeable Machine",
                "",
                " §7An industrial crucible powered by ",
                " §5blighted energy§7, designed to fuse ",
                " §7magic and metal under heat",
                " §7intolerable to mortal craft."
        );
        blightedForge.glow();

        registry.accept(blightedWorkbench);
        registry.accept(blightedForge);
    }
}
