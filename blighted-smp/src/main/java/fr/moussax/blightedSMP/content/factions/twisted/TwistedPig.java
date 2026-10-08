package fr.moussax.blightedSMP.content.factions.twisted;

import fr.moussax.blightedSMP.engine.loot.decorators.EntityLootRarity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class TwistedPig extends TwistedCreature {

    private static final long CHARGE_COOLDOWN_MS = 6000L;
    private boolean isCharging = false;

    public TwistedPig() {
        super("TWISTED_PIG", "Twisted Pig", EntityType.PIG, 22, 3);
        setDroppedExp(5);

        attributes(attr -> attr
                .movementSpeed(0.28)
                .followRange(32)
        );

        loot(loot -> loot
                .maxDrops(3)
                .drop(Material.PORKCHOP, 1, 3, 1.0)
                .blight(2, 0.015, EntityLootRarity.VERY_RARE)
        );
    }

    @Override
    protected void onDefineBehavior() {
        super.onDefineBehavior();
        addCoreAbility(12L, 12L, this::handlePigBehavior);
    }

    private void handlePigBehavior() {
        if (!isAlive() || isCharging) return;
        if (!isCooldownReady("pig_charge", CHARGE_COOLDOWN_MS)) return;

        Player target = getTargetPlayer();
        if (target == null) return;

        double distance = entity.getLocation().distance(target.getLocation());
        if (distance >= 4.0 && distance <= 14.0
                && hasLineOfSight(target)
                && isPathClear(target.getLocation())
                && checkAndTriggerCooldown("pig_charge", CHARGE_COOLDOWN_MS)) {
            executeCharge(target);
        }
    }

    private boolean isPathClear(Location targetLocation) {
        Vector step = targetLocation.toVector().subtract(entity.getLocation().toVector()).setY(0);
        double distance = step.length();
        if (distance < 1.0) return true;
        step.normalize().multiply(1.2);

        Location check = entity.getLocation().clone();
        for (double distanceCovered = 1.2; distanceCovered < distance - 1.0; distanceCovered += 1.2) {
            check.add(step);
            if (check.getBlock().getType().isSolid() || check.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                return false;
            }
        }
        return true;
    }

    private void executeCharge(Player target) {
        isCharging = true;
        faceLocation(target.getLocation());
        playSound(Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.5f, 1.6f);
        emitSoulLeakage(2, 0.03);

        addCoreDelayedAction(10L, () -> {
            if (!isAlive()) {
                isCharging = false;
                return;
            }

            if (!hasLineOfSight(target)) {
                isCharging = false;
                return;
            }

            Vector targetDirection = target.getLocation().toVector().subtract(entity.getLocation().toVector());
            targetDirection.setY(0);
            final Vector chargeDirection = (targetDirection.lengthSquared() > 0.001)
                    ? targetDirection.normalize()
                    : entity.getLocation().getDirection().setY(0).normalize();

            entity.setVelocity(chargeDirection.clone().multiply(1.25).setY(0.08));
            playSound(Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.8f, 0.8f);
            emitSoulBurst(entity.getLocation().add(0, 0.4, 0), chargeDirection.clone().multiply(-1), 5, 0.12);

            for (int tick = 2; tick <= 12; tick += 2) {
                final int currentTick = tick;
                addCoreDelayedAction(currentTick, () -> {
                    if (!isAlive() || !isCharging) return;

                    // Obstruction check: if pig runs into a wall, cancel/collide
                    Location ahead = entity.getLocation().add(chargeDirection.clone().multiply(0.8));
                    if (ahead.getBlock().getType().isSolid()) {
                        entity.setVelocity(new Vector(0, 0, 0));
                        playSound(Sound.ENTITY_PLAYER_ATTACK_WEAK, 0.7f, 0.8f);
                        isCharging = false;
                        return;
                    }

                    for (Player nearbyPlayer : getNearbyPlayers(1.6)) {
                        nearbyPlayer.damage(getDamage() * 1.5, entity);
                        Vector knockback = chargeDirection.clone().multiply(0.8).setY(0.3);
                        nearbyPlayer.setVelocity(knockback);
                        playSound(Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 1.2f);
                        emitSoulLeakage(3, 0.05);
                        isCharging = false;
                        break;
                    }
                });
            }

            // Recovery phase: pig loses momentum for a brief period before resuming pursuit
            addCoreDelayedAction(16L, () -> {
                if (isAlive()) {
                    entity.setVelocity(new Vector(0, 0, 0));
                }
                isCharging = false;
            });
        });
    }
}
