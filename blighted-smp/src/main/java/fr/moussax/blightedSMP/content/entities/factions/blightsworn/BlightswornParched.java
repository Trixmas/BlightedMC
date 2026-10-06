package fr.moussax.blightedSMP.content.entities.factions.blightsworn;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import static fr.moussax.blightedSMP.engine.loot.decorators.EntityLootRarity.*;

public final class BlightswornParched extends BlightswornArcherArchetype {

    public BlightswornParched() {
        super("BLIGHTSWORN_PARCHED", "Blightsworn Parched", EntityType.PARCHED);
        setDamage(6);
        setDroppedExp(12);
        equipment(eq -> eq.mainHand(Material.BOW));

        spawning(spawn -> spawn
                .biomes(Biome.DESERT)
                .overworldSurfaceHostile()
        );

        loot(loot -> loot
                .drop(Material.BONE, 2, 5, 1.0)
                .drop(Material.ARROW, 2, 5, 1.0)
                .damagedItem(Material.BOW, 0.10, 0.75, 0.15, RARE)
                .blight(5, 0.04, VERY_RARE)
        );
    }

    @Override
    protected void applyArrowEffects(Arrow arrow, boolean isPhaseTwo) {
        int duration = isPhaseTwo ? 100 : 80;
        int amplifier = isPhaseTwo ? 1 : 0;

        arrow.addCustomEffect(new PotionEffect(PotionEffectType.HUNGER, duration, amplifier), true);

        if (isPhaseTwo) {
            arrow.setFireTicks(100);
        }
    }

    @Override
    protected void onEnrage(LivingEntity entity) {
        Location location = entity.getLocation().add(0, 1, 0);
        entity.getWorld().playSound(location, Sound.ENTITY_SKELETON_DEATH, 1.0f, 0.5f);
        entity.getWorld().playSound(location, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.8f);
        entity.getWorld().spawnParticle(Particle.FLAME, location, 30, 0.5, 1.0, 0.5, 0.05);
        entity.getWorld().spawnParticle(Particle.DUST, location, 30, 0.5, 1.0, 0.5, 0.0, BLIGHT_DUST);
    }
}
