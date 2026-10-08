package fr.moussax.blightedSMP.content.factions.blightsworn;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import static fr.moussax.blightedSMP.engine.loot.decorators.EntityLootRarity.*;

public final class BlightswornBogged extends BlightswornArcherArchetype {

    public BlightswornBogged() {
        super("BLIGHTSWORN_BOGGED", "Blightsworn Bogged", EntityType.BOGGED);
        setDamage(6);
        setDroppedExp(12);
        equipment(eq -> eq.mainHand(Material.BOW));

        spawning(spawn -> spawn
                .biomes(Biome.SWAMP, Biome.MANGROVE_SWAMP)
                .overworldHostile()
        );

        loot(loot -> loot
                .drop(Material.BONE, 2, 4, 1.0)
                .drop(Material.ARROW, 2, 5, 1.0)
                .drop(Material.TIPPED_ARROW,
                        builder -> builder.setItemMeta(
                                meta -> ((PotionMeta) meta).setBasePotionType(PotionType.POISON)),
                        1,
                        3,
                        0.4
                )
                .damagedItem(Material.BOW, 0.10, 0.75, 0.15, RARE)
                .blight(5, 0.04, VERY_RARE)
        );
    }

    @Override
    protected void applyArrowEffects(Arrow arrow, boolean isPhaseTwo) {
        int duration = isPhaseTwo ? 100 : 80;
        int amplifier = isPhaseTwo ? 1 : 0;
        arrow.addCustomEffect(new PotionEffect(PotionEffectType.POISON, duration, amplifier), true);
    }

    @Override
    protected void onEnrage(LivingEntity entity) {
        Location location = entity.getLocation().add(0, 1, 0);
        entity.getWorld().playSound(location, Sound.ENTITY_BOGGED_DEATH, 1.0f, 0.5f);
        entity.getWorld().spawnParticle(Particle.SNEEZE, location, 50, 0.5, 1.0, 0.5, 0.05);
        entity.getWorld().spawnParticle(Particle.DUST, location, 30, 0.5, 1.0, 0.5, 0.0, BLIGHT_DUST);
    }
}
