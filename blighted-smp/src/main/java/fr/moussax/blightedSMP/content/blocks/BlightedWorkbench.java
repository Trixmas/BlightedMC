package fr.moussax.blightedSMP.content.blocks;

import fr.moussax.blightedSMP.engine.blocks.BlightedBlock;
import fr.moussax.blightedSMP.engine.items.registry.ItemRegistry;
import fr.moussax.blightedSMP.engine.recipes.crafting.menu.CraftingTableMenu;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Objects;

public class BlightedWorkbench extends BlightedBlock {

    public BlightedWorkbench() {
        super(Material.SCULK_CATALYST, Objects.requireNonNull(ItemRegistry.get("FORGOTTEN_WORKBENCH")));
    }

    @Override
    public void onPlace(BlockPlaceEvent event) {
        event.getPlayer().playSound(event.getBlockPlaced().getLocation(), Sound.ENTITY_WARDEN_AGITATED, 100.0F, 0.85F);
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (event.getAction().toString().contains("RIGHT_CLICK")) {
            event.setCancelled(true);
            new CraftingTableMenu().open(player);
        }
    }
}
