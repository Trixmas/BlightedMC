package fr.moussax.blightedSMP.content.entities.factions.blightsworn;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import static fr.moussax.blightedSMP.engine.loot.decorators.EntityLootRarity.*;

public final class BlightswornWitherSkeleton extends BlightswornEliteArchetype {

    private static final double LUNGE_RANGE = 12.0;

    public BlightswornWitherSkeleton() {
        super("BLIGHTSWORN_WITHER_SKELETON", "Blightsworn Wither Skeleton", EntityType.WITHER_SKELETON);
        loot(loot -> loot
                .maxDrops(4)
                .drop(Material.BONE, 2, 5, 1.0)
                .drop(Material.COAL, 1, 3, 0.5)
                .drop(Material.WITHER_SKELETON_SKULL, 0.03, VERY_RARE)
                .blight(5, 0.04, VERY_RARE)
        );

        setDamage(8);
        setDroppedExp(20);
        equipment(eq -> eq.mainHand(Material.STONE_SWORD));

        spawning(spawn -> spawn
                .insideStructure(Structure.FORTRESS)
                .maxBlockLight(0)
        );
    }

    @Override
    protected void onNormalBehavior() {
        addPhaseAbility(100L, 100L, () -> executePhantomLunge(false));
    }

    @Override
    protected void onEnrageBehavior() {
        addPhaseAbility(80L, 80L, () -> executePhantomLunge(true));
    }

    @Override
    protected void onEnrage(LivingEntity entity) {
        Location location = entity.getLocation().add(0, 1, 0);
        entity.getWorld().playSound(location, Sound.ENTITY_WITHER_SKELETON_DEATH, 1.0f, 0.5f);
        entity.getWorld().playSound(location, Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);

        entity.getWorld().spawnParticle(Particle.LARGE_SMOKE, location, 50, 0.5, 1.0, 0.5, 0.05);
        entity.getWorld().spawnParticle(Particle.DUST, location, 30, 0.5, 1.0, 0.5, 0.0, BLIGHT_DUST);
    }

    private void executePhantomLunge(boolean isPhaseTwo) {
        if (!isAlive()) return;

        Player target = getNearestPlayer(LUNGE_RANGE);
        if (target == null || target.isDead() || target.getWorld() != entity.getWorld() || !hasLineOfSight(target)) return;

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        entity.getWorld().spawnParticle(Particle.PORTAL, entity.getLocation().add(0, 1, 0), 20, 0.5, 1.0, 0.5, 0.1);

        addCoreDelayedAction(10L, () -> {
            if (!isAlive()) return;

            Vector lungeDirection = target.getLocation().toVector().subtract(entity.getLocation().toVector());
            if (lungeDirection.lengthSquared() > 0) {
                lungeDirection.normalize().multiply(isPhaseTwo ? 1.6 : 1.2).setY(0.25);
                entity.setVelocity(lungeDirection);

                entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.5f, 1.2f);

                if (isPhaseTwo) {
                    spawnWitherResidue();
                }
            }
        });
    }

    private void spawnWitherResidue() {
        AreaEffectCloud cloud = entity.getWorld().spawn(entity.getLocation(), AreaEffectCloud.class);
        cloud.setRadius(1.5f);
        cloud.setDuration(60);
        cloud.setWaitTime(0);
        cloud.setParticle(Particle.SMOKE);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.WITHER, 60, 0), true);
    }
}
