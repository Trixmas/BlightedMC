package fr.moussax.blightedSMP.content.factions.blightsworn;

import fr.moussax.bedrock.utils.ItemBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public sealed abstract class BlightswornArcherArchetype extends BlightswornCreature
        permits BlightswornBogged, BlightswornParched, BlightswornSkeleton, BlightswornStray {

    private static final double SCATTER_TRIGGER_RADIUS = 5.0;
    private static final double AIMED_SHOT_RANGE = 20.0;
    private static final int SCATTER_COOLDOWN_TICKS = 160;
    private static final int MAX_HEALTH = 25;

    private boolean isAiming = false;
    private long nextScatterTick = 0;

    protected BlightswornArcherArchetype(String entityId, String name, EntityType entityType) {
        super(entityId, name, entityType, MAX_HEALTH);
    }

    @Override
    protected void onDefineAdditionalBehavior() {
        addCoreAbility(10L, 5L, this::performTacticalMovement);
    }

    @Override
    protected void onNormalBehavior() {
        addPhaseAbility(80L, 60L, () -> evaluateDeliberateShot(false));
    }

    @Override
    protected void onEnrageBehavior() {
        addPhaseAbility(50L, 40L, () -> evaluateDeliberateShot(true));
    }

    protected abstract void applyArrowEffects(Arrow arrow, boolean isPhaseTwo);

    private void evaluateDeliberateShot(boolean isPhaseTwo) {
        if (!isAlive() || isAiming) return;

        Player target = getNearestPlayer(AIMED_SHOT_RANGE);
        if (target == null || target.isDead() || !hasLineOfSight(target)) return;

        double distanceSquared = entity.getLocation().distanceSquared(target.getLocation());
        // If the player is rushing right in front of the archer, prioritize dodging/repositioning first
        if (distanceSquared < 3.5 * 3.5) {
            performScatterEvasion();
            return;
        }

        executeAimedShot(target, isPhaseTwo);
    }

    private void executeAimedShot(Player target, boolean isPhaseTwo) {
        isAiming = true;

        double originalSpeed = Objects.requireNonNull(
                entity.getAttribute(Attribute.MOVEMENT_SPEED)).getBaseValue();
        Objects.requireNonNull(entity.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.0);

        entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_CROSSBOW_LOADING_MIDDLE, 1.0f, 1.0f);
        entity.getWorld().spawnParticle(
                Particle.CRIT,
                entity.getLocation().add(0, 1.5, 0),
                8, 0.2, 0.2, 0.2, 0.02
        );

        int windupTicks = isPhaseTwo ? 14 : 20;

        addCoreDelayedAction(windupTicks, () -> {
            if (!isAlive()) return;

            isAiming = false;
            Objects.requireNonNull(entity.getAttribute(Attribute.MOVEMENT_SPEED))
                    .setBaseValue(originalSpeed);

            if (target.isDead() || target.getWorld() != entity.getWorld() || !hasLineOfSight(target)) return;

            fireArrowAt(target, isPhaseTwo);

            if (isPhaseTwo) {
                addCoreDelayedAction(12L, () -> {
                    if (isAlive() && !target.isDead() && target.getWorld() == entity.getWorld() && hasLineOfSight(target)) {
                        fireArrowAt(target, true);
                    }
                });
            }
        });
    }

    private void fireArrowAt(Player target, boolean isPhaseTwo) {
        if (!isAlive()) return;

        Arrow arrow = entity.launchProjectile(Arrow.class);
        Vector trajectory = target.getEyeLocation().toVector()
                .subtract(entity.getEyeLocation().toVector())
                .normalize()
                .multiply(1.8);
        arrow.setVelocity(trajectory);

        // Scales the base damage by 1.5x while neutralizing the 1.8 velocity multiplier
        arrow.setDamage((this.damage * 1.5) / 1.8);
        arrow.setShooter(entity);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_SKELETON_SHOOT, 1.0f, 0.8f);
        applyArrowEffects(arrow, isPhaseTwo);
    }

    private void performTacticalMovement() {
        if (!isAlive() || isAiming) return;

        Player threat = getNearestPlayer(SCATTER_TRIGGER_RADIUS);
        if (threat != null && hasLineOfSight(threat)) {
            performScatterEvasion();
            return;
        }

        // At mid-range (6 - 15 blocks), occasionally reposition laterally to maintain a clean firing angle
        if (entity.getTicksLived() >= nextScatterTick) {
            Player target = getNearestPlayer(14.0);
            if (target != null && hasLineOfSight(target)) {
                double distance = entity.getLocation().distance(target.getLocation());
                if (distance >= 6.0 && ThreadLocalRandom.current().nextDouble() < 0.25) {
                    performLateralReposition(target);
                }
            }
        }
    }

    private void performLateralReposition(Player target) {
        Vector toTarget = target.getLocation().toVector().subtract(entity.getLocation().toVector()).setY(0);
        if (toTarget.lengthSquared() == 0) return;
        toTarget.normalize();

        Vector strafe = ThreadLocalRandom.current().nextBoolean()
                ? new Vector(-toTarget.getZ(), 0, toTarget.getX())
                : new Vector(toTarget.getZ(), 0, -toTarget.getX());

        if (isSafeEvasionDirection(strafe)) {
            strafe.multiply(0.65).setY(0.1);
            entity.setVelocity(strafe);
            nextScatterTick = entity.getTicksLived() + 50; // brief delay before next reposition
        }
    }

    private void performScatterEvasion() {
        if (!isAlive() || isAiming || entity.getTicksLived() < nextScatterTick) return;

        Player threat = getNearestPlayer(SCATTER_TRIGGER_RADIUS);
        if (threat == null || !hasLineOfSight(threat)) return;

        Vector awayFromThreat = entity.getLocation().toVector()
                .subtract(threat.getLocation().toVector())
                .setY(0);

        if (awayFromThreat.lengthSquared() == 0) return;

        awayFromThreat.normalize();

        double x = awayFromThreat.getX();
        double z = awayFromThreat.getZ();

        Vector left = new Vector(-z, 0, x);
        Vector right = new Vector(z, 0, -x);

        boolean preferLeft = ThreadLocalRandom.current().nextBoolean();
        Vector primary = preferLeft ? left : right;
        Vector secondary = preferLeft ? right : left;

        Vector chosenDirection = null;

        if (isSafeEvasionDirection(primary)) {
            chosenDirection = primary;
        } else if (isSafeEvasionDirection(secondary)) {
            chosenDirection = secondary;
        } else if (isSafeEvasionDirection(awayFromThreat)) {
            chosenDirection = awayFromThreat;
        }

        if (chosenDirection == null) return;

        chosenDirection.multiply(1.1).setY(0.25);
        entity.setVelocity(chosenDirection);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_BREEZE_SLIDE, 0.8f, 1.4f);

        nextScatterTick = entity.getTicksLived() + SCATTER_COOLDOWN_TICKS;
    }

    private boolean isSafeEvasionDirection(Vector direction) {
        Location targetLocation = entity.getLocation().add(direction.getX() * 2.0, 0, direction.getZ() * 2.0);

        for (int y = 0; y <= 3; y++) {
            Material type = targetLocation.getBlock().getType();
            if (type.isSolid() && type != Material.MAGMA_BLOCK && type != Material.CACTUS) {
                return true;
            }
            if (type == Material.LAVA) {
                return false;
            }
            targetLocation.subtract(0, 1, 0);
        }

        return false;
    }

    @Override
    protected void equipEnragedArmor(EntityEquipment equipment) {
        setArmor(
                new ItemBuilder(Material.LEATHER_HELMET)
                        .setArmorTrim(TrimMaterial.AMETHYST, TrimPattern.SENTRY)
                        .setLeatherColor(PHASE_TWO_COLOR)
                        .unbreakable()
                        .toItemStack(),
                new ItemBuilder(Material.LEATHER_CHESTPLATE)
                        .setArmorTrim(TrimMaterial.AMETHYST, TrimPattern.SENTRY)
                        .setLeatherColor(PHASE_TWO_COLOR)
                        .unbreakable()
                        .toItemStack(),
                new ItemBuilder(Material.IRON_LEGGINGS).unbreakable().toItemStack(),
                new ItemBuilder(Material.IRON_BOOTS).unbreakable().toItemStack()
        );
    }


}
