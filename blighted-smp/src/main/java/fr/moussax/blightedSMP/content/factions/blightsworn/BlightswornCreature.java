package fr.moussax.blightedSMP.content.factions.blightsworn;

import fr.moussax.blightedSMP.engine.entities.EntityManager;
import fr.moussax.blightedSMP.engine.entities.spawnable.SpawnableEntity;
import fr.moussax.bedrock.utils.ItemBuilder;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public sealed abstract class BlightswornCreature extends SpawnableEntity
        permits BlightswornArcherArchetype, BlightswornBruteArchetype, BlightswornDrowned, BlightswornEliteArchetype {

    protected static final double DEFAULT_SPAWN_PROBABILITY = 0.05;
    protected static final double ENRAGE_HEALTH_THRESHOLD = 0.50;

    protected static final String PHASE_ONE_COLOR = "#2C1654";
    protected static final String PHASE_TWO_COLOR = "#6B1F9E";

    protected static final Particle.DustOptions BLIGHT_DUST =
            new Particle.DustOptions(Color.fromRGB(107, 31, 158), 1.2f);
    private static final Particle.DustOptions BLIGHT_RESONANCE_DUST =
            new Particle.DustOptions(Color.fromRGB(160, 60, 220), 0.8f);

    @Getter
    private boolean isResonating = false;
    private double baseSpeedBeforeResonance = -1;

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
     * Sealed entry point for the Blightsworn phase and resonance setup.
     * Subclasses must not override this — use {@link #onDefineAdditionalBehavior()} instead.
     */
    @Override
    protected final void onDefineBehavior() {
        registerPhase(1.0, this::onNormalBehavior);
        registerPhase(ENRAGE_HEALTH_THRESHOLD, () -> {
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.5f);
            equipEnragedArmor(entity.getEquipment());
            onEnrage(entity);
            onEnrageBehavior();
        });
        addCoreAbility(20L, 20L, this::evaluateResonance);
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
        evaluateResonance();
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

    private void evaluateResonance() {
        if (!isAlive()) return;

        boolean hasNearbyAlly = false;

        for (Entity nearby : entity.getNearbyEntities(12, 12, 12)) {
            var blighted = EntityManager.getBlightedEntity(nearby);
            if (!(blighted instanceof BlightswornCreature ally) || !ally.isAlive()) continue;

            hasNearbyAlly = true;
            drawResonanceLine(nearby.getLocation().add(0, 1, 0));
        }

        setResonating(hasNearbyAlly);
    }

    private void drawResonanceLine(Location target) {
        Location origin = entity.getLocation().add(0, 1, 0);
        Vector direction = target.toVector().subtract(origin.toVector());
        double length = direction.length();

        if (length < 0.5) return;

        direction.normalize().multiply(0.6);
        Location current = origin.clone();
        int steps = (int) (length / 0.6);

        for (int i = 0; i < steps; i++) {
            current.add(direction);
            entity.getWorld().spawnParticle(Particle.DUST, current, 1, 0, 0, 0, 0, BLIGHT_RESONANCE_DUST);
        }
    }

    public void setResonating(boolean resonating) {
        if (this.isResonating == resonating || !isAlive()) return;
        this.isResonating = resonating;

        var speedAttr = entity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr == null) return;

        if (resonating) {
            baseSpeedBeforeResonance = speedAttr.getBaseValue();
            speedAttr.setBaseValue(baseSpeedBeforeResonance * 1.1);
        } else if (baseSpeedBeforeResonance > 0) {
            speedAttr.setBaseValue(baseSpeedBeforeResonance);
            baseSpeedBeforeResonance = -1;
        }
    }
}
