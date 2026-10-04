package fr.moussax.blightedSMP.content.blocks;

import fr.moussax.blightedSMP.engine.blocks.BlightedBlock;
import fr.moussax.blightedSMP.engine.recipes.forging.menu.ForgeMenu;
import fr.moussax.blightedSMP.engine.items.registry.ItemRegistry;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class BlightedForge extends BlightedBlock {
    public static BlightedForge instance;

    public BlightedForge() {
        super(Material.BLAST_FURNACE, ItemRegistry.get("BLIGHTED_FORGE"));
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (event.getAction().toString().contains("RIGHT_CLICK")) {
            event.setCancelled(true);
            new ForgeMenu(null).open(player);
        }
    }
}
