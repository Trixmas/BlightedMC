package fr.moussax.blightedSMP.content.factions.twisted;

import fr.moussax.blightedSMP.engine.loot.decorators.EntityLootRarity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Twisted Chicken — fast nuisance and swarm pressure mob.
 *
 * <p>Pursues players rapidly while moving erratically and making sudden direction changes.
 * Low individual damage, but dangerous when gathering together.</p>
 */
public final class TwistedChicken extends TwistedCreature {

    public TwistedChicken() {
        super("TWISTED_CHICKEN", "Twisted Chicken", EntityType.CHICKEN, 12, 2);
        setDroppedExp(4);

        attributes(attr -> attr
                .movementSpeed(0.33)
                .followRange(28)
        );

        loot(loot -> loot
                .maxDrops(3)
                .drop(Material.CHICKEN, 1, 2, 1.0)
                .drop(Material.FEATHER, 1, 2, 1.0)
                .blight(2, 0.015, EntityLootRarity.VERY_RARE)
        );
    }

    @Override
    protected void onDefineBehavior() {
        super.onDefineBehavior();
        addCoreAbility(15L, 20L, this::handleErraticMovement);
    }

    private void handleErraticMovement() {
        if (!isAlive()) return;
        Player target = getTargetPlayer();
        if (target == null) return;

        double distanceSq = entity.getLocation().distanceSquared(target.getLocation());
        // Only make erratic corrections when within engagement range (between 2 and 16 blocks)
        if (distanceSq > 16.0 * 16.0 || distanceSq < 2.0 * 2.0) return;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vector toTarget = target.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize();
        Vector lateral = new Vector(-toTarget.getZ(), 0, toTarget.getX()).normalize();
        if (random.nextBoolean()) {
            lateral.multiply(-1);
        }

        // Check if forward landing position is not hazard (lava, void) or wall
        Location checkLoc = entity.getLocation().add(toTarget.clone().multiply(1.2)).add(lateral.clone().multiply(1.0));
        if (checkLoc.getBlock().getType().isSolid()) {
            lateral.multiply(-1);
        }

        Vector impulse = toTarget.multiply(0.20).add(lateral.multiply(0.38));
        impulse.setY(0.10); // Small, physically believable hop for chicken
        entity.setVelocity(impulse);

        // Quiet sculk-like click accompanies rare movement changes instead of chicken hurt sound
        if (random.nextDouble() < 0.35) {
            playSound(Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.4f, 1.9f);
            emitSoulLeakage(1, 0.02);
        }
    }

    @Override
    protected void onConfigureAI(LivingEntity spawned) {
        super.onConfigureAI(spawned);
        applyTwistedHostileGoals(spawned, 1.35D);
    }
}
