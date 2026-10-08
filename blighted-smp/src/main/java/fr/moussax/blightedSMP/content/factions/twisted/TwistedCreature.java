package fr.moussax.blightedSMP.content.factions.twisted;

import fr.moussax.blightedSMP.engine.entities.spawnable.SpawnableEntity;
import fr.moussax.blightedSMP.engine.entities.state.EntityAttributeInjector;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.entity.CraftMob;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public sealed abstract class TwistedCreature extends SpawnableEntity
        permits TwistedChicken, TwistedCow, TwistedPig, TwistedSheep, TwistedSkeleton,
        TwistedSpider, TwistedWolf, TwistedZombie {

    public static final double DEFAULT_TWISTED_SPAWN_PROBABILITY = 0.08;

    protected static final String CORRUPTION_HEX = "#175E6E";
    protected static final Color CORRUPTION_PURPLE = Color.fromRGB(0x4A, 0x15, 0x6D);
    protected static final Particle.DustOptions CORRUPTION_DUST = new Particle.DustOptions(CORRUPTION_PURPLE, 1.0f);

    private UUID lastKnownTargetId = null;

    protected TwistedCreature(String entityId, String name, EntityType entityType, int maxHealth, int damage) {
        this(entityId, name, entityType, maxHealth, damage, DEFAULT_TWISTED_SPAWN_PROBABILITY);
    }

    protected TwistedCreature(String entityId, String name, EntityType entityType, int maxHealth, int damage, double spawnProbability) {
        super(entityId, name, entityType);
        setMaxHealth(maxHealth);
        setDamage(damage);
        setDroppedExp(5);
        spawning(spawn -> {
            spawn.probability(spawnProbability).overworld();
            if (isPassiveAnimal(entityType)) {
                spawn.notInLiquid();
            } else {
                spawn.overworldHostile();
            }
        });
    }

    private static boolean isPassiveAnimal(EntityType type) {
        return type == EntityType.CHICKEN || type == EntityType.COW || type == EntityType.PIG
                || type == EntityType.SHEEP || type == EntityType.WOLF;
    }

    @Override
    protected void onDefineBehavior() {
        // Periodic check for target acquisition and ambient corruption leakage
        addCoreAbility(10L, 10L, this::handleTwistedTick);
    }

    private void handleTwistedTick() {
        if (!isAlive()) return;

        Player currentTarget = getTargetPlayer();
        UUID currentTargetId = currentTarget != null ? currentTarget.getUniqueId() : null;

        if (currentTargetId != null && !Objects.equals(currentTargetId, lastKnownTargetId)) {
            lastKnownTargetId = currentTargetId;
            onTargetAcquired(currentTarget);
        } else if (currentTargetId == null) {
            lastKnownTargetId = null;
        }

        // Very subtle, occasional soul emission when actively hunting (10% per half-second)
        if (currentTarget != null && ThreadLocalRandom.current().nextDouble() < 0.10) {
            emitSoulLeakage(1, 0.02);
        }
    }

    /**
     * Triggered when the creature notices a player. Emits a brief soul leak.
     */
    protected void onTargetAcquired(Player target) {
        emitSoulLeakage(3, 0.04);
        playSound(Sound.PARTICLE_SOUL_ESCAPE, 0.45f, 1.4f);
    }

    /**
     * Emits subtle soul particles around the creature.
     *
     * @param count particle count
     * @param speed particle speed
     */
    public void emitSoulLeakage(int count, double speed) {
        if (!isAlive()) return;
        Location center = entity.getLocation().add(0, entity.getHeight() * 0.55, 0);
        entity.getWorld().spawnParticle(
                Particle.SOUL,
                center,
                count,
                entity.getWidth() * 0.3,
                entity.getHeight() * 0.3,
                entity.getWidth() * 0.3,
                speed
        );
        entity.getWorld().spawnParticle(
                Particle.DUST,
                center,
                Math.max(1, count / 2),
                entity.getWidth() * 0.25,
                entity.getHeight() * 0.25,
                entity.getWidth() * 0.25,
                0.0,
                CORRUPTION_DUST
        );
    }

    /**
     * Emits a directional soul leak during an impactful attack or charge.
     */
    public void emitSoulBurst(Location origin, Vector direction, int count, double speed) {
        if (origin.getWorld() == null) return;
        Vector normalized = direction.clone().normalize();
        for (int i = 0; i < count; i++) {
            origin.getWorld().spawnParticle(
                    Particle.SOUL,
                    origin,
                    0,
                    normalized.getX(),
                    normalized.getY() + 0.05,
                    normalized.getZ(),
                    speed
            );
        }
        origin.getWorld().spawnParticle(Particle.DUST, origin, count, 0.2, 0.2, 0.2, 0.0, CORRUPTION_DUST);
    }

    @Override
    public void onDamageTaken(EntityDamageEvent event) {
        super.onDamageTaken(event);
        if (!isAlive()) return;

        // Heavy damage briefly disturbs the corruption and releases souls
        if (event.getFinalDamage() >= 4.0 || (getHealth() - event.getFinalDamage()) <= (getMaxHealth() * 0.4)) {
            emitSoulLeakage(4, 0.05);
            playSound(Sound.PARTICLE_SOUL_ESCAPE, 0.5f, 1.2f);
        }

        // If damaged by a player without a current target, immediately fixate on them
        if (event instanceof EntityDamageByEntityEvent damageByEntity) {
            LivingEntity damager = getDirectDamager(damageByEntity.getDamager());
            if (damager instanceof Player playerDamager && (getTarget() == null || !getTarget().isValid())) {
                setAITarget(playerDamager);
            }
        }
    }

    @Override
    public void onDamageDealt(EntityDamageByEntityEvent event) {
        super.onDamageDealt(event);
        if (!isAlive()) return;

        // Impactful melee attack releases a small soul trace
        emitSoulLeakage(2, 0.03);
    }

    @Override
    public void onDeath(Location location) {
        super.onDeath(location);
        if (location.getWorld() == null) return;

        // Death provides the clearest faction signature: short burst of souls before fading
        Location chest = location.clone().add(0, 0.8, 0);
        location.getWorld().spawnParticle(Particle.SOUL, chest, 14, 0.35, 0.45, 0.35, 0.06);
        location.getWorld().spawnParticle(Particle.DUST, chest, 10, 0.35, 0.45, 0.35, 0.0, CORRUPTION_DUST);
        location.getWorld().playSound(chest, Sound.PARTICLE_SOUL_ESCAPE, 0.7f, 0.9f);
        location.getWorld().playSound(chest, Sound.BLOCK_SCULK_CHARGE, 0.4f, 1.4f);
    }

    /**
     * Returns the pursuit movement speed modifier for hostile MeleeAttackGoal.
     * Can be overridden by subclasses to configure custom pursuit speed.
     *
     * @return speed modifier
     */
    protected double getPursuitSpeedModifier() {
        return 1.25D;
    }

    @Override
    protected void onConfigureAI(LivingEntity spawned) {
        super.onConfigureAI(spawned);
        if (spawned instanceof org.bukkit.entity.Ageable ageable && !ageable.isAdult()) {
            ageable.setAdult();
        }
        applyTwistedHostileGoals(spawned, getPursuitSpeedModifier());
    }

    /**
     * Reconfigures the entity's pathfinding goals with NMS to make it persistently hostile
     * toward players, replacing vanilla passivity or panic goals.
     *
     * @param spawned       bound entity
     * @param speedModifier pursuit movement speed modifier
     */
    protected void applyTwistedHostileGoals(LivingEntity spawned, double speedModifier) {
        if (!(spawned instanceof CraftMob craftMob)) return;
        if (spawned.getAttribute(Attribute.ATTACK_DAMAGE) == null) {
            EntityAttributeInjector.injectAttribute(spawned, Attribute.ATTACK_DAMAGE, Math.max(1.0, getDamage()));
        }

        var nmsMob = craftMob.getHandle();
        nmsMob.goalSelector.removeAllGoals(goal -> true);
        nmsMob.targetSelector.removeAllGoals(goal -> true);

        nmsMob.goalSelector.addGoal(0, new FloatGoal(nmsMob));
        if (nmsMob instanceof PathfinderMob pathfinderMob) {
            nmsMob.goalSelector.addGoal(1, new MeleeAttackGoal(pathfinderMob, speedModifier, false));
            nmsMob.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(pathfinderMob, 0.85D));
            nmsMob.targetSelector.addGoal(1, new HurtByTargetGoal(pathfinderMob).setAlertOthers());
        }
        nmsMob.goalSelector.addGoal(6, new LookAtPlayerGoal(nmsMob, net.minecraft.world.entity.player.Player.class, 16.0F));
        nmsMob.goalSelector.addGoal(7, new RandomLookAroundGoal(nmsMob));

        nmsMob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                nmsMob,
                net.minecraft.world.entity.player.Player.class,
                true
        ));
    }

    private LivingEntity getDirectDamager(Entity entity) {
        if (entity instanceof LivingEntity living) return living;
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity shooter) {
            return shooter;
        }
        return null;
    }
}
