package fr.moussax.blightedSMP.content.entities.factions.blightsworn;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import static fr.moussax.blightedSMP.engine.loot.decorators.EntityLootRarity.*;

public final class BlightswornHusk extends BlightswornBruteArchetype {

    public BlightswornHusk() {
        super("BLIGHTSWORN_HUSK", "Blightsworn Husk", EntityType.HUSK);
        setDamage(6);
        setDroppedExp(12);

        spawning(spawn -> spawn
                .biomes(Biome.DESERT)
                .overworldSurfaceHostile()
        );

        loot(loot -> loot
                .drop(Material.ROTTEN_FLESH, 2, 5, 1.0)
                .drop(Material.SAND, 1, 3, 0.3)
                .drop(Material.IRON_INGOT, 1, 2, 0.1, RARE)
                .blight(5, 0.04, VERY_RARE)
        );
    }

    @Override
    public void onDamageDealt(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 60, 0));
        }
    }

    @Override
    protected void applySurgeHitEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 80, 1));
    }
}
