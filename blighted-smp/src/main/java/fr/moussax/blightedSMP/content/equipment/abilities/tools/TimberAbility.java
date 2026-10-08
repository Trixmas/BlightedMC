package fr.moussax.blightedSMP.content.equipment.abilities.tools;

import fr.moussax.blightedSMP.engine.items.abilities.ItemAbility;
import fr.moussax.blightedSMP.engine.items.abilities.AbilityTrigger;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.*;

public class TimberAbility implements ItemAbility<BlockBreakEvent> {

    @Override
    public String getName() {
        return "Timber";
    }

    @Override
    public AbilityTrigger getTrigger() {
        return AbilityTrigger.BLOCK_BREAK;
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Fells entire trees in a single strike."
        };
    }

    private static final int MAX_LOGS = 32;
    private static final Set<Material> LOGS = Set.of(
            Material.OAK_LOG, Material.OAK_WOOD,
            Material.SPRUCE_LOG, Material.SPRUCE_WOOD,
            Material.BIRCH_LOG, Material.BIRCH_WOOD,
            Material.JUNGLE_LOG, Material.JUNGLE_WOOD,
            Material.ACACIA_LOG, Material.ACACIA_WOOD,
            Material.DARK_OAK_LOG, Material.DARK_OAK_WOOD,
            Material.PALE_OAK_LOG, Material.PALE_OAK_WOOD,
            Material.MANGROVE_LOG, Material.MANGROVE_WOOD,
            Material.CHERRY_LOG, Material.CHERRY_WOOD,
            Material.CRIMSON_STEM, Material.CRIMSON_HYPHAE,
            Material.WARPED_STEM, Material.WARPED_HYPHAE
    );

    @Override
    public boolean triggerAbility(BlockBreakEvent event) {
        Block origin = event.getBlock();

        if (!LOGS.contains(origin.getType())) {
            return false;
        }

        Set<Block> tree = findTree(origin);

        for (Block log : tree) {
            if (log.equals(origin)) {
                continue;
            }

            log.breakNaturally(event.getPlayer()
                    .getInventory()
                    .getItemInMainHand()
            );
        }
        return true;
    }

    private Set<Block> findTree(Block origin) {
        Set<Block> found = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();

        Material type = origin.getType();

        found.add(origin);
        queue.add(origin);

        while (!queue.isEmpty() && found.size() < MAX_LOGS) {
            Block current = queue.poll();

            for (Block nearby : getNeighbours(current)) {
                if (found.contains(nearby)) {
                    continue;
                }

                if (!LOGS.contains(nearby.getType())) {
                    continue;
                }

                found.add(nearby);
                queue.add(nearby);
            }
        }
        return found;
    }

    private List<Block> getNeighbours(Block block) {
        List<Block> blocks = new ArrayList<>();

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    blocks.add(block.getRelative(x, y, z));
                }
            }
        }

        return blocks;
    }

    @Override
    public int getCooldownSeconds() {
        return 2;
    }

    @Override
    public boolean cancelEvent(boolean success) {
        return false;
    }
}
