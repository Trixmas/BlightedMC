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

        var blightedWorkbench = new BlightedItem("FORGOTTEN_WORKBENCH", ItemType.BLOCK, ItemRarity.COMMON, Material.SCULK_CATALYST);
        blightedWorkbench.setDisplayName("Forgotten Workbench");
        blightedWorkbench.description(
                "The Forgotten used it to shape",
                "things touched by the §3Blight§7.",
                "Its sculk still carries their",
                "ancient §3resonance§7, waiting for",
                "another hand to put it to use."
        );
        blightedWorkbench.glow();

        BlightedItem blightedForge = new BlightedItem("BLIGHTED_FORGE", ItemType.BLOCK, ItemRarity.RARE, Material.BLAST_FURNACE);
        blightedForge.setDisplayName("Blighted Forge");
        blightedForge.addLore(
                "§8Placeable Machine",
                "",
                " §7An industrial crucible powered by ",
                " §3Blight energy§7, designed to fuse ",
                " §7magic and metal under heat",
                " §7intolerable to mortal craft."
        );
        blightedForge.glow();

        registry.accept(blightedWorkbench);
        registry.accept(blightedForge);
    }
}
