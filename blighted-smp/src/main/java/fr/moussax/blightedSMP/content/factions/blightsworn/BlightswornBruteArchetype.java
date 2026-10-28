package fr.moussax.blightedSMP.content.factions.blightsworn;

import fr.moussax.bedrock.utils.ItemBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Objects;

public sealed abstract class BlightswornBruteArchetype extends BlightswornCreature
        permits BlightswornHusk, BlightswornZombie {

    private static final double HEAVY_SLAM_TRIGGER_DISTANCE = 4.5;
    private static final int SURGE_STRIKE_RADIUS = 3;
    private static final int MAX_HEALTH = 35;

    private boolean isAttacking = false;
    private boolean isRecovering = false;
    private boolean isPhaseTwo = false;
    private long nextHeavySlamTick = 0;

    protected BlightswornBruteArchetype(String entityId, String name, EntityType entityType) {
        super(entityId, name, entityType, MAX_HEALTH);
    }

    @Override
    protected void onDefineAdditionalBehavior() {
        addCoreAbility(10L, 5L, this::evaluateHeavySlam);
    }

    @Override
    protected void onNormalBehavior() {
        isPhaseTwo = false;
    }

    @Override
    protected void onEnrageBehavior() {
    }

    @Override
    protected void onEnrage(LivingEntity entity) {
        isPhaseTwo = true;
    }

    /**
     * Delegates on-hit effects for the heavy slam to the concrete mob.
     * For example, Husk applies Hunger here.
     */
    protected abstract void applySurgeHitEffects(Player player);

    private void evaluateHeavySlam() {
        if (!isAlive() || isAttacking || isRecovering || entity.getTicksLived() < nextHeavySlamTick) return;

        Player target = getNearestPlayer(HEAVY_SLAM_TRIGGER_DISTANCE);
        if (target == null || target.isDead() || !hasLineOfSight(target)) return;

        triggerHeavySlam(target);
    }

    private void triggerHeavySlam(Player target) {
        isAttacking = true;

        double originalSpeed = Objects.requireNonNull(
                entity.getAttribute(Attribute.MOVEMENT_SPEED)).getBaseValue();
        // Halt briefly during windup to telegraphed committed attack
        Objects.requireNonNull(entity.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.0);

        // Readable telegraph: sound and small particle cue
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.6f);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.7f, 0.5f);
        entity.getWorld().spawnParticle(
                Particle.DUST,
                entity.getLocation().add(0, 1.2, 0),
                8, 0.3, 0.3, 0.3, 0.0,
                BLIGHT_DUST
        );

        // Windup duration: 18 ticks normally, 12 ticks when enraged
        int windupTicks = isPhaseTwo ? 12 : 18;

        addCoreDelayedAction(windupTicks, () -> executeSlamLunge(target, originalSpeed));
    }

    private void executeSlamLunge(Player target, double originalSpeed) {
        if (!isAlive()) return;

        Vector direction;
        if (target != null && !target.isDead() && target.getWorld().equals(entity.getWorld())) {
            direction = target.getLocation().toVector().subtract(entity.getLocation().toVector());
        } else {
            direction = entity.getLocation().getDirection();
        }

        direction.setY(0).normalize().multiply(1.3).setY(0.2);
        entity.setVelocity(direction);

        addCoreDelayedAction(8L, () -> executeSlamImpactAndRecover(originalSpeed));
    }

    private void executeSlamImpactAndRecover(double originalSpeed) {
        if (!isAlive()) return;

        isAttacking = false;
        isRecovering = true;

        Location impactLocation = entity.getLocation();
        impactLocation.getWorld().playSound(impactLocation, Sound.ITEM_MACE_SMASH_GROUND, 1.0f, 0.8f);
        impactLocation.getWorld().spawnParticle(Particle.DUST, impactLocation.add(0, 0.2, 0), 15, 0.6, 0.1, 0.6, 0.0, BLIGHT_DUST);

        getNearbyPlayers(SURGE_STRIKE_RADIUS).forEach(player -> {
            player.damage(this.damage * 1.5, entity);
            applySurgeHitEffects(player);
        });

        // Slow down significantly during recovery window, creating an opening for the player to punish
        Objects.requireNonNull(entity.getAttribute(Attribute.MOVEMENT_SPEED))
                .setBaseValue(originalSpeed * 0.4);

        if (isPhaseTwo) {
            spawnBlightResidue();
        }

        // Recovery window: 30 ticks normally, 20 ticks when enraged
        int recoveryTicks = isPhaseTwo ? 20 : 30;
        int cooldownTicks = isPhaseTwo ? 70 : 100;

        addCoreDelayedAction(recoveryTicks, () -> {
            if (!isAlive()) return;
            isRecovering = false;
            Objects.requireNonNull(entity.getAttribute(Attribute.MOVEMENT_SPEED))
                    .setBaseValue(originalSpeed);
            nextHeavySlamTick = entity.getTicksLived() + (cooldownTicks - recoveryTicks);
        });
    }

    private void spawnBlightResidue() {
        AreaEffectCloud cloud = entity.getWorld().spawn(entity.getLocation(), AreaEffectCloud.class);
        cloud.setRadius(1.5f);
        cloud.setDuration(40);
        cloud.setWaitTime(0);
        cloud.setParticle(Particle.DUST, BLIGHT_DUST);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 0), true);
    }

    @Override
    protected void equipEnragedArmor(EntityEquipment equipment) {
        setArmor(
                new ItemBuilder(Material.LEATHER_HELMET)
                        .setArmorTrim(TrimMaterial.AMETHYST, TrimPattern.FLOW)
                        .setLeatherColor(PHASE_TWO_COLOR)
                        .unbreakable()
                        .toItemStack(),
                new ItemBuilder(Material.LEATHER_CHESTPLATE)
                        .setArmorTrim(TrimMaterial.AMETHYST, TrimPattern.SNOUT)
                        .setLeatherColor(PHASE_TWO_COLOR)
                        .unbreakable()
                        .toItemStack(),
                new ItemBuilder(Material.IRON_LEGGINGS).unbreakable().toItemStack(),
                new ItemBuilder(Material.IRON_BOOTS).unbreakable().toItemStack()
        );
    }
}
