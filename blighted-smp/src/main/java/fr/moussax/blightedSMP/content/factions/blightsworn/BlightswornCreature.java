package fr.moussax.blightedSMP.content.factions.blightsworn;

import fr.moussax.bedrock.utils.ItemBuilder;
import fr.moussax.blightedSMP.engine.entities.spawnable.SpawnableEntity;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;

public sealed abstract class BlightswornCreature extends SpawnableEntity
        permits BlightswornArcherArchetype, BlightswornBruteArchetype, BlightswornDrowned, BlightswornEliteArchetype {

    protected static final double DEFAULT_SPAWN_PROBABILITY = 0.05;
    protected static final double ENRAGE_HEALTH_THRESHOLD = 0.50;

    protected static final String PHASE_ONE_COLOR = "#2C1654";
    protected static final String PHASE_TWO_COLOR = "#6B1F9E";

    protected static final Particle.DustOptions BLIGHT_DUST =
            new Particle.DustOptions(Color.fromRGB(107, 31, 158), 1.2f);

    protected BlightswornCreature(String entityId, String name, EntityType entityType, int maxHealth) {
        this(entityId, name, entityType, maxHealth, DEFAULT_SPAWN_PROBABILITY);
    }

    protected BlightswornCreature(String entityId, String name, EntityType entityType, int maxHealth, double spawnProbability) {
        super(entityId, name, entityType);
        setMaxHealth(maxHealth);
        spawning(spawn -> spawn.probability(spawnProbability));
        setupDefaultArmor();
    }

    /**
     * Sealed entry point for the Blightsworn phase setup.
     * Subclasses must not override this — use {@link #onDefineAdditionalBehavior()} instead.
     */
    @Override
    protected final void onDefineBehavior() {
        registerPhase(1.0, this::onNormalBehavior);
        registerPhase(ENRAGE_HEALTH_THRESHOLD, () -> {
            entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.2f);
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.2f, 1.4f);
            entity.getWorld().spawnParticle(Particle.DUST, entity.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.0, BLIGHT_DUST);
            equipEnragedArmor(entity.getEquipment());
            onEnrage(entity);
            onEnrageBehavior();
        });
        onDefineAdditionalBehavior();
    }

    /**
     * Hook for archetype and concrete classes to register core abilities.
     * Always call {@code super.onDefineAdditionalBehavior()} when overriding in archetypes.
     */
    protected void onDefineAdditionalBehavior() {}

    @Override
    protected void onRehydrate(LivingEntity existing) {
        super.onRehydrate(existing);
        evaluatePhases(existing.getHealth());
    }

    protected abstract void onNormalBehavior();

    protected abstract void onEnrageBehavior();

    protected abstract void onEnrage(LivingEntity entity);

    protected abstract void equipEnragedArmor(EntityEquipment equipment);

    private void setupDefaultArmor() {
        setArmor(
                new ItemBuilder(Material.LEATHER_HELMET)
                        .setLeatherColor(PHASE_ONE_COLOR)
                        .unbreakable()
                        .toItemStack(),
                new ItemBuilder(Material.LEATHER_CHESTPLATE)
                        .setLeatherColor(PHASE_ONE_COLOR)
                        .unbreakable()
                        .toItemStack(),
                null,
                null
        );
    }
}
