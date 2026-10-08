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
 * Twisted Spider — gap closer / ambush mob.
 *
 * <p>Retains spider agility and climbing while occasionally performing a sudden,
 * short dash toward the player without noisy telegraphing or UI.</p>
 */
public final class TwistedSpider extends TwistedCreature {

    private static final long DASH_COOLDOWN_MS = 4500L;

    public TwistedSpider() {
        super("TWISTED_SPIDER", "Twisted Spider", EntityType.SPIDER, 20, 4);
        setDroppedExp(5);

        attributes(attr -> attr
                .movementSpeed(0.34)
                .followRange(32)
        );

        loot(loot -> loot
                .maxDrops(3)
                .drop(Material.STRING, 0, 2, 1.0)
                .drop(Material.SPIDER_EYE, 0, 1, 0.33)
                .blight(2, 0.015, EntityLootRarity.VERY_RARE)
        );
    }

    @Override
    protected void onDefineBehavior() {
        super.onDefineBehavior();
        // Gap closing burst check every 15 ticks
        addCoreAbility(15L, 15L, this::handleSuddenDash);
    }

    private void handleSuddenDash() {
        if (!isAlive()) return;
        // Check cooldown early before target distance and prediction calculations
        if (!isCooldownReady("spider_dash", DASH_COOLDOWN_MS)) return;

        Player target = getTargetPlayer();
        if (target == null) return;

        double distance = entity.getLocation().distance(target.getLocation());
        // Do not dash if already in melee range (< 3.0 blocks) or too far (> 9.0 blocks)
        if (distance < 3.0 || distance > 9.0) return;

        if (hasLineOfSight(target) && checkAndTriggerCooldown("spider_dash", DASH_COOLDOWN_MS)) {
            // Predict landing point based on player velocity
            Vector playerVel = target.getVelocity();
            Location predictedLoc = target.getLocation().clone();
            if (playerVel.lengthSquared() > 0.01) {
                predictedLoc.add(playerVel.clone().multiply(3.0));
            }

            // Check if landing / destination is safe (not solid wall, not in lava/pit)
            if (isDestinationSafe(predictedLoc)) {
                executeDash(predictedLoc);
            }
        }
    }

    private boolean isDestinationSafe(Location destination) {
        if (destination.getBlock().getType().isSolid()) {
            return false;
        }
        Location floor = destination.clone().subtract(0, 1, 0);
        return floor.getBlock().getType().isSolid();
    }

    private void executeDash(Location landingTarget) {
        Vector toTarget = landingTarget.toVector().subtract(entity.getLocation().toVector());
        double horizontalDist = Math.hypot(toTarget.getX(), toTarget.getZ());
        if (horizontalDist < 0.1) return;

        Vector dashVelocity = toTarget.clone().setY(0).normalize().multiply(1.18);
        dashVelocity.setY(0.24); // Low agile pounce arc

        entity.setVelocity(dashVelocity);
        playSound(Sound.ENTITY_SPIDER_STEP, 0.7f, 1.5f);

        // Movement behavior: tiny soul leak during the movement
        if (ThreadLocalRandom.current().nextDouble() < 0.40) {
            emitSoulLeakage(1, 0.02);
        }
    }

    @Override
    protected void onConfigureAI(LivingEntity spawned) {
        super.onConfigureAI(spawned);
        applyTwistedHostileGoals(spawned, 1.30D);
    }
}
