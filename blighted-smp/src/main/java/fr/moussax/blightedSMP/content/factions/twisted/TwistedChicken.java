package fr.moussax.blightedSMP.content.factions.twisted;

import fr.moussax.blightedSMP.engine.loot.decorators.EntityLootRarity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

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

        double distanceSquared = entity.getLocation().distanceSquared(target.getLocation());
        if (distanceSquared > 16.0 * 16.0 || distanceSquared < 2.0 * 2.0) return;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vector toTarget = target.getLocation().toVector().subtract(entity.getLocation().toVector()).setY(0);
        if (toTarget.lengthSquared() < 0.001) return;
        toTarget.normalize();

        Vector lateral = new Vector(-toTarget.getZ(), 0, toTarget.getX());
        if (random.nextBoolean()) {
            lateral.multiply(-1);
        }

        Location checkLocation = entity.getLocation().add(toTarget.clone().multiply(1.2))
                .add(lateral.clone().multiply(1.0));
        if (checkLocation.getBlock().getType().isSolid()) {
            lateral.multiply(-1);
        }

        if (entity.isOnGround()) {
            Vector currentVelocity = entity.getVelocity();
            Vector impulse = toTarget.multiply(0.16).add(lateral.multiply(0.30));
            entity.setVelocity(currentVelocity.add(impulse).setY(0.0));
        }

        if (random.nextDouble() < 0.35) {
            playSound(Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.4f, 1.9f);
            emitSoulLeakage(1, 0.02);
        }
    }

    @Override
    protected double getPursuitSpeedModifier() {
        return 1.35D;
    }
}
