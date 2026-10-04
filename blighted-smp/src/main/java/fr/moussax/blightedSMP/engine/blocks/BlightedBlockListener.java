package fr.moussax.blightedSMP.engine.blocks;

import fr.moussax.blightedSMP.BlightedSMP;
import fr.moussax.blightedSMP.engine.blocks.registry.BlockRegistry;
import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.server.database.PluginDatabase;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Random;

public final class BlightedBlockListener implements Listener {
    private final BlightedSMP plugin = BlightedSMP.getInstance();
    private final NamespacedKey BLOCK_ID_KEY = new NamespacedKey(plugin, "blighted_block_id");
    private final PluginDatabase database;

    public BlightedBlockListener() {
        this.database = plugin.getDatabase();
    }

    private String getBlockId(Block block) {
        List<MetadataValue> metadata = block.getMetadata("blighted_id");
        for (MetadataValue value : metadata) {
            if (value.getOwningPlugin() == plugin) return value.asString();
        }

        if (block.getState() instanceof TileState tile) {
            String id = tile.getPersistentDataContainer().get(BLOCK_ID_KEY, PersistentDataType.STRING);
            if (id != null) {
                cacheMetadata(block, id);
                return id;
            }
        }

        String id = database.getBlockId(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        if (id != null) {
            cacheMetadata(block, id);
            return id;
        }
        return null;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String id = meta.getPersistentDataContainer().get(BlightedItem.BLIGHTED_ID_KEY, PersistentDataType.STRING);
        if (id == null) return;

        BlightedBlock customBlock = BlockRegistry.get(id);
        if (customBlock == null) return;

        Block placed = event.getBlockPlaced();

        BlockState state = placed.getState();
        if (state instanceof TileState tile) {
            tile.getPersistentDataContainer().set(BLOCK_ID_KEY, PersistentDataType.STRING, id);
            tile.update();
        }

        database.addBlock(placed.getWorld().getUID(), placed.getX(), placed.getY(), placed.getZ(), id);

        cacheMetadata(placed, id);
        customBlock.onPlace(event);
    }

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;

        String id = getBlockId(block);
        if (id == null) return;

        BlightedBlock customBlock = BlockRegistry.get(id);
        if (customBlock != null) {
            customBlock.onInteract(event);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String id = getBlockId(block);
        if (id == null) return;

        BlightedBlock customBlock = BlockRegistry.get(id);
        if (customBlock == null) return;

        event.setDropItems(false); // Cancel vanilla drops
        event.setExpToDrop(0);

        Location blockLocation = block.getLocation();

        ItemStack drop = customBlock.onBreak(event, customBlock.getBlightedItem().toItemStack());
        if (drop != null) {
            block.getWorld().dropItemNaturally(blockLocation, drop);
        }

        cleanupBlockData(block);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (getBlockId(block) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (getBlockId(block) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList(), event.getYield());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList(), event.getYield());
    }

    private void handleExplosion(List<Block> blocks, float yield) {
        var iterator = blocks.iterator();
        Random random = new Random();

        while (iterator.hasNext()) {
            Block block = iterator.next();
            String id = getBlockId(block);

            if (id == null) continue;

            BlightedBlock customBlock = BlockRegistry.get(id);
            if (customBlock == null) continue;

            iterator.remove();

            cleanupBlockData(block);
            block.setType(Material.AIR);

            if (random.nextFloat() <= yield) {
                ItemStack drop = customBlock.onBreak(customBlock.getBlightedItem().toItemStack());
                if (drop != null) {
                    block.getWorld().dropItemNaturally(block.getLocation(), drop);
                }
            }
        }
    }

    private void cleanupBlockData(Block block) {
        database.removeBlock(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        block.removeMetadata("blighted_id", plugin);

        BlockState state = block.getState();
        if (state instanceof TileState tile) {
            tile.getPersistentDataContainer().remove(BLOCK_ID_KEY);
            tile.update();
        }
    }

    private void cacheMetadata(Block block, String id) {
        block.setMetadata("blighted_id", new FixedMetadataValue(plugin, id));
    }
}
