package fr.moussax.blightedSMP.content.factions.twisted;

import fr.moussax.blightedSMP.engine.entities.BlightedEntity;
import fr.moussax.blightedSMP.engine.entities.EntityManager;
import fr.moussax.blightedSMP.engine.loot.decorators.EntityLootRarity;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;

import java.util.concurrent.ThreadLocalRandom;

public final class TwistedSheep extends TwistedCreature {

    public TwistedSheep() {
        super("TWISTED_SHEEP", "Twisted Sheep", EntityType.SHEEP, 18, 3);
        setDroppedExp(5);

        attributes(attr -> attr
                .movementSpeed(0.26)
                .followRange(30)
        );

        loot(loot -> loot
                .maxDrops(3)
                .drop(Material.MUTTON, 1, 2, 1.0)
                .drop(Material.PURPLE_WOOL, 1, 1.0)
                .blight(2, 0.015, EntityLootRarity.VERY_RARE)
        );
    }

    @Override
    public LivingEntity spawn(Location location) {
        LivingEntity spawned = super.spawn(location);
        if (spawned instanceof Sheep sheep) {
            sheep.setColor(DyeColor.PURPLE);
        }
        return spawned;
    }

    @Override
    protected void onRehydrate(LivingEntity existing) {
        super.onRehydrate(existing);
        if (existing instanceof Sheep sheep) {
            sheep.setColor(DyeColor.PURPLE);
        }
    }

    @Override
    protected void onDefineBehavior() {
        super.onDefineBehavior();
        addCoreAbility(25L, 25L, this::handleFlockConvergence);
    }

    private void handleFlockConvergence() {
        if (!isAlive()) return;
        Player myTarget = getTargetPlayer();

        if (myTarget != null) {
            boolean alertedAny = false;
            for (Entity nearby : entity.getNearbyEntities(14.0, 6.0, 14.0)) {
                BlightedEntity blighted = EntityManager.getBlightedEntity(nearby);
                if (blighted instanceof TwistedSheep ally && ally.isAlive()) {
                    if (ally.getTarget() == null || !ally.getTarget().isValid()) {
                        ally.setAITarget(myTarget);
                        alertedAny = true;
                    }
                }
            }

            if (alertedAny && ThreadLocalRandom.current().nextDouble() < 0.35) {
                playSound(Sound.BLOCK_SCULK_SENSOR_CLICKING, 0.45f, 1.7f);
                emitSoulLeakage(1, 0.02);
            }
        }
    }
}
