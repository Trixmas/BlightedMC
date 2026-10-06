package fr.moussax.blightedSMP.content.entities.factions.blightsworn;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import static fr.moussax.blightedSMP.engine.loot.decorators.EntityLootRarity.*;

public final class BlightswornSkeleton extends BlightswornArcherArchetype {

    public BlightswornSkeleton() {
        super("BLIGHTSWORN_SKELETON", "Blightsworn Skeleton", EntityType.SKELETON);
        setDamage(6);
        setDroppedExp(12);
        equipment(eq -> eq.mainHand(Material.BOW));

        spawning(spawn -> spawn
                .overworld()
                .overworldHostile()
        );

        loot(loot -> loot
                .drop(Material.BONE, 2, 5, 1.0)
                .drop(Material.ARROW, 2, 5, 1.0)
                .damagedItem(Material.BOW, 0.1, 0.8, 0.15, RARE)
                .blight(5, 0.04, VERY_RARE)
        );
    }

    @Override
    protected void applyArrowEffects(Arrow arrow, boolean isPhaseTwo) {
        if (isPhaseTwo) {
            arrow.addCustomEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0), true);
        }
    }

    @Override
    protected void onEnrage(LivingEntity entity) {
        Location location = entity.getLocation().add(0, 1, 0);
        entity.getWorld().playSound(location, Sound.ENTITY_SKELETON_DEATH, 1.0f, 0.5f);
        entity.getWorld().spawnParticle(Particle.CRIT, location, 30, 0.5, 1.0, 0.5, 0.05);
        entity.getWorld().spawnParticle(Particle.DUST, location, 30, 0.5, 1.0, 0.5, 0.0, BLIGHT_DUST);
    }
}
