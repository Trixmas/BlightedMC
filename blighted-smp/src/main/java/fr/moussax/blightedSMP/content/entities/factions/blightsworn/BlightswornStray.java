package fr.moussax.blightedSMP.content.entities.factions.blightsworn;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import static fr.moussax.blightedSMP.engine.loot.decorators.EntityLootRarity.*;

public final class BlightswornStray extends BlightswornArcherArchetype {
    public BlightswornStray() {
        super("BLIGHTSWORN_STRAY", "Blightsworn Stray", EntityType.STRAY);
        setDamage(6);
        setDroppedExp(12);
        equipment(eq -> eq.mainHand(Material.BOW));

        spawning(spawn -> spawn
                .biomes(
                        Biome.SNOWY_PLAINS,
                        Biome.ICE_SPIKES,
                        Biome.FROZEN_OCEAN,
                        Biome.DEEP_FROZEN_OCEAN,
                        Biome.FROZEN_RIVER,
                        Biome.SNOWY_SLOPES,
                        Biome.JAGGED_PEAKS,
                        Biome.FROZEN_PEAKS
                )
                .overworldSurfaceHostile()
        );

        loot(loot -> loot
                .maxDrops(4)
                .drop(Material.BONE, 2, 5, 1.0)
                .drop(Material.ARROW, 2, 5, 1.0)
                .drop(Material.TIPPED_ARROW, builder -> builder.setItemMeta(
                                meta -> ((PotionMeta) meta).setBasePotionType(PotionType.SLOWNESS)
                        ),
                        1,
                        3,
                        0.4
                )
                .blight(5, 0.04, VERY_RARE)
        );
    }

    @Override
    protected void applyArrowEffects(Arrow arrow, boolean isPhaseTwo) {
    }

    @Override
    protected void onEnrage(LivingEntity entity) {
    }
}
