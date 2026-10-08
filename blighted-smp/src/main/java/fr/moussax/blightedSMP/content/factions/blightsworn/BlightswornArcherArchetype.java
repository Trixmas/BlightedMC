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
        addCoreAbility(20L, 5L, this::performScatterEvasion);
    }

    @Override
    protected void onNormalBehavior() {
        addPhaseAbility(100L, 100L, () -> executeAimedShot(false));
    }

    @Override
    protected void onEnrageBehavior() {
        addPhaseAbility(80L, 80L, () -> executeAimedShot(true));
    }

    protected abstract void applyArrowEffects(Arrow arrow, boolean isPhaseTwo);

    private void executeAimedShot(boolean isPhaseTwo) {
        if (!isAlive() || isAiming) return;

        Player target = getNearestPlayer(AIMED_SHOT_RANGE);
        if (target == null || !hasLineOfSight(target)) return;

        isAiming = true;

        double originalSpeed = Objects.requireNonNull(
                entity.getAttribute(Attribute.MOVEMENT_SPEED)).getBaseValue();
        Objects.requireNonNull(entity.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.0);

        entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_CROSSBOW_LOADING_MIDDLE, 1.0f, 1.0f);
        entity.getWorld().spawnParticle(
                Particle.CRIT,
                entity.getLocation().add(0, 1.5, 0),
                15, 0.3, 0.3, 0.3, 0.02
        );

        addCoreDelayedAction(20L, () -> {
            if (!isAlive()) return;

            isAiming = false;
            Objects.requireNonNull(entity.getAttribute(Attribute.MOVEMENT_SPEED))
                    .setBaseValue(originalSpeed);

            if (target.isDead() || target.getWorld() != entity.getWorld()) return;

            fireArrowAt(target, isPhaseTwo);

            if (isPhaseTwo) {
                addCoreDelayedAction(15L, () -> fireArrowAt(target, true));
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

        chosenDirection.multiply(1.2).setY(0.3);
        entity.setVelocity(chosenDirection);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.8f);

        nextScatterTick = entity.getTicksLived() + SCATTER_COOLDOWN_TICKS;
    }

    private boolean isSafeEvasionDirection(Vector direction) {
        Location targetLoc = entity.getLocation().add(direction.getX() * 2.0, 0, direction.getZ() * 2.0);

        for (int y = 0; y <= 3; y++) {
            Material type = targetLoc.getBlock().getType();
            if (type.isSolid() && type != Material.MAGMA_BLOCK && type != Material.CACTUS) {
                return true;
            }
            if (type == Material.LAVA) {
                return false;
            }
            targetLoc.subtract(0, 1, 0);
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
