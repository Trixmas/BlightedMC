package fr.moussax.blightedSMP.content.blocks;

import fr.moussax.blightedSMP.engine.blocks.BlightedBlock;
import fr.moussax.blightedSMP.engine.recipes.crafting.menu.CraftingTableMenu;
import fr.moussax.blightedSMP.engine.items.registry.ItemRegistry;
import fr.moussax.bedrock.text.Messenger;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class BlightedWorkbench extends BlightedBlock {

    public BlightedWorkbench() {
        super(Material.ENCHANTING_TABLE, ItemRegistry.get("BLIGHTED_WORKBENCH"));
    }

    @Override
    public void onPlace(BlockPlaceEvent event) {
        event.getPlayer().playSound(event.getBlockPlaced().getLocation(), Sound.BLOCK_TRIAL_SPAWNER_OMINOUS_ACTIVATE, 100.0F, 0.85F);
        Messenger.inform(event.getPlayer(), "You have placed a §dBlighted Workbench§7!");
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
