package fr.moussax.blightedSMP.content.factions.twisted;

import fr.moussax.blightedSMP.engine.loot.decorators.EntityLootRarity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class TwistedCow extends TwistedCreature {

    private static final long RAM_COOLDOWN_MS = 8000L;
    private boolean isRamming = false;

    public TwistedCow() {
        super("TWISTED_COW", "Twisted Cow", EntityType.COW, 28, 4);
        setDroppedExp(6);

        attributes(attr -> attr
                .movementSpeed(0.25)
                .knockbackResistance(0.4)
                .followRange(32)
        );

        loot(loot -> loot
                .maxDrops(3)
                .drop(Material.BEEF, 1, 3, 1.0)
                .drop(Material.LEATHER, 0, 2, 1.0)
                .blight(2, 0.015, EntityLootRarity.VERY_RARE)
        );
    }

    @Override
    protected void onDefineBehavior() {
        super.onDefineBehavior();
        addCoreAbility(14L, 14L, this::handleCowBehavior);
    }

    private void handleCowBehavior() {
        if (!isAlive() || isRamming) return;
        if (!isCooldownReady("cow_ram", RAM_COOLDOWN_MS)) return;

        Player target = getTargetPlayer();
        if (target == null) return;

        double distance = entity.getLocation().distance(target.getLocation());
        if (distance >= 4.5 && distance <= 14.0
                && hasLineOfSight(target)
                && isPathViable(target.getLocation())
                && checkAndTriggerCooldown("cow_ram", RAM_COOLDOWN_MS)) {
            executeRam(target);
        }
    }

    private boolean isPathViable(Location targetLocation) {
        Vector step = targetLocation.toVector().subtract(entity.getLocation().toVector()).setY(0);
        double distance = step.length();
        if (distance < 1.0) return true;
        step.normalize().multiply(1.5);

        Location check = entity.getLocation().clone();
        for (double distanceCovered = 1.5; distanceCovered < distance - 1.0; distanceCovered += 1.5) {
            check.add(step);
            if (check.getBlock().getType().isSolid() || check.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                return false;
            }
            // Check ground below to avoid charging into pits or lava
            Location ground = check.clone().subtract(0, 1, 0);
            if (!ground.getBlock().getType().isSolid() && !ground.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
                return false; // Cliff / pit
            }
        }
        return true;
    }

    private void executeRam(Player target) {
        isRamming = true;

        faceLocation(target.getLocation());
        entity.setVelocity(new Vector(0, 0, 0));
        playSound(Sound.BLOCK_SCULK_CHARGE, 0.45f, 0.9f);
        emitSoulLeakage(2, 0.02);

        // Windup duration: 16 ticks (~0.8s)
        addCoreDelayedAction(16L, () -> {
            if (!isAlive()) {
                isRamming = false;
                return;
            }

            if (!hasLineOfSight(target)) {
                isRamming = false;
                return;
            }

            Vector targetDirection = target.getLocation().toVector().subtract(entity.getLocation().toVector());
            targetDirection.setY(0);
            final Vector ramDirection = (targetDirection.lengthSquared() > 0.001)
                    ? targetDirection.normalize()
                    : entity.getLocation().getDirection().setY(0).normalize();

            entity.setVelocity(ramDirection.clone().multiply(1.35).setY(0.08));
            playSound(Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.9f, 0.5f);

            // Ram impact detection & trailing soul visual cues during sprint
            for (int tick = 2; tick <= 14; tick += 2) {
                final int currentTick = tick;
                addCoreDelayedAction(currentTick, () -> {
                    if (!isAlive() || !isRamming) return;

                    // Sparse soul trail behind cow; souls subtly drift toward the player
                    entity.getWorld();
                    Location rear = entity.getLocation().clone().add(0, 0.6, 0).subtract(ramDirection.clone().multiply(0.8));
                    Vector toPlayer;
                    if (target.isValid() && target.isOnline() && !target.isDead() && target.getWorld().equals(entity.getWorld())) {
                        toPlayer = target.getLocation().toVector().subtract(rear.toVector()).normalize().multiply(0.08);
                    } else {
                        toPlayer = ramDirection.clone().multiply(-0.08);
                    }
                    entity.getWorld().spawnParticle(
                            Particle.SOUL,
                            rear,
                            0,
                            toPlayer.getX(),
                            0.02,
                            toPlayer.getZ(),
                            0.05
                    );

                    // Collision with wall
                    Location ahead = entity.getLocation().add(ramDirection.clone().multiply(0.8));
                    if (ahead.getBlock().getType().isSolid()) {
                        entity.setVelocity(new Vector(0, 0, 0));
                        playSound(Sound.ENTITY_PLAYER_ATTACK_WEAK, 0.8f, 0.7f);
                        isRamming = false;
                        return;
                    }

                    for (Player nearby : getNearbyPlayers(1.8)) {
                        nearby.damage(getDamage() * 1.75, entity);
                        Vector displacement = ramDirection.clone().multiply(1.4).setY(0.42);
                        nearby.setVelocity(displacement);
                        playSound(Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.7f);
                        emitSoulLeakage(4, 0.05);
                        isRamming = false;
                        break;
                    }
                });
            }

            // Loses momentum and recovers before resuming pursuit
            addCoreDelayedAction(22L, () -> {
                if (isAlive()) {
                    entity.setVelocity(new Vector(0, 0, 0));
                }
                isRamming = false;
            });
        });
    }

    @Override
    protected double getPursuitSpeedModifier() {
        return 1.20D;
    }
}
