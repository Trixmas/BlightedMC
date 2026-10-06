package fr.moussax.blightedSMP.content.entities.powerful;

import fr.moussax.blightedSMP.content.utils.ai.EndermanAI;
import fr.moussax.blightedSMP.engine.entities.spawnable.SpawnableEntity;
import fr.moussax.blightedSMP.engine.entities.spawnable.condition.SpawnRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;

import java.util.Random;

import static fr.moussax.blightedSMP.engine.loot.decorators.EntityLootRarity.*;

public class Watchling extends SpawnableEntity {

    private static final long TELEPORT_COOLDOWN = 4000;
    private final Random random = new Random();

    public Watchling() {
        super("WATCHLING", "§dWatchling", EntityType.ENDERMAN);
        setMaxHealth(20);
        setDamage(10);
        setDroppedExp(10);
        spawning(spawn -> spawn
                .probability(0.001)
                .anyOf(
                        SpawnRules.biome(Biome.END_BARRENS, Biome.END_MIDLANDS),
                        SpawnRules.insideStructure(Structure.END_CITY)
                )
        );

        attributes(attributes -> attributes
                .scale(0.7)
                .movementSpeed(0.35)
                .followRange(50)
        );

        loot(loot -> loot
                .maxDrops(2)
                .drop(Material.ENDER_PEARL, 1, 2, 1.0)
                .blight(5, 0.03, VERY_RARE)
        );
    }

    @Override
    protected void onDefineBehavior() {
        addCoreAbility(5L, 5L, this::handleCombatLogic);
    }

    private void handleCombatLogic() {
        if (!isAlive()) return;
        Player target = getTargetPlayer();
        if (target == null) return;

        double distance = entity.getLocation().distance(target.getLocation());

        if (distance > 6 && distance < 20 && checkAndTriggerCooldown("teleport", TELEPORT_COOLDOWN)) {
            teleportToTarget(target);
        }
    }

    private void teleportToTarget(Player target) {
        Location location = target.getLocation().add(
            (random.nextDouble() - 0.5) * 2,
            0,
            (random.nextDouble() - 0.5) * 2
        );

        entity.teleport(location);
        playSound(location, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);

        if (random.nextDouble() < 0.3) {
            performHeavyAttack(target);
        }
    }

    private void performHeavyAttack(Player target) {
        swingMainHand();
        swingOffHand();

        addCoreDelayedAction(10L, () -> {
            if (!isAlive() || target.getLocation().distance(entity.getLocation()) > 3) return;

            target.damage(getDamage() * 2, entity);
            playSound(Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.5f, 1.2f);

            if (random.nextBoolean()) {
                executeEscapeTeleport();
            }
        });
    }

    private void executeEscapeTeleport() {
        Location escapeLoc = entity.getLocation().add(
            (random.nextDouble() - 0.5) * 15,
            0,
            (random.nextDouble() - 0.5) * 15
        );
        entity.teleport(escapeLoc);
        entity.setInvisible(true);
        addCoreDelayedAction(40L, () -> {
            if (isAlive()) entity.setInvisible(false);
        });
    }

    @Override
    protected void onConfigureAI(LivingEntity spawned) {
        EndermanAI.init(spawned);
    }
}
