package fr.moussax.blightedSMP.content.entities.factions.blightsworn;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import static fr.moussax.blightedSMP.engine.loot.decorators.EntityLootRarity.*;

public final class BlightswornZombie extends BlightswornBruteArchetype {

    public BlightswornZombie() {
        super("BLIGHTSWORN_ZOMBIE", "Blightsworn Zombie", EntityType.ZOMBIE);
        setDamage(6);
        setDroppedExp(12);

        spawning(spawn -> spawn
                .overworld()
                .overworldHostile()
        );

        loot(loot -> loot
                .maxDrops(3)
                .drop(Material.ROTTEN_FLESH, 1, 2, 1.0)
                .drop(Material.POTATO, 0.025)
                .drop(Material.CARROT, 0.025)
                .drop(Material.IRON_INGOT, 0.02, RARE)
                .blight(2, 0.01, VERY_RARE)
        );
    }

    @Override
    protected void applySurgeHitEffects(Player player) {
    }
}
