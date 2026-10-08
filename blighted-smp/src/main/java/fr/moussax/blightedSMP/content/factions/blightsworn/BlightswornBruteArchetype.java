package fr.moussax.blightedSMP.content.factions.blightsworn;

import fr.moussax.bedrock.utils.ItemBuilder;
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

    private static final int ACCUMULATION_RADIUS = 8;
    private static final int SURGE_STRIKE_RADIUS = 3;
    private static final int MAX_HEALTH = 35;

    private int blightStacks = 0;
    private boolean isSurging = false;
    private boolean isRecovering = false;
    private boolean isPhaseTwo = false;

    protected BlightswornBruteArchetype(String entityId, String name, EntityType entityType) {
        super(entityId, name, entityType, MAX_HEALTH);
    }

    @Override
    protected void onDefineAdditionalBehavior() {
        addCoreAbility(20L, 10L, this::handleBlightAccumulation);
    }

    @Override
    protected void onNormalBehavior() {
        isPhaseTwo = false;
        blightStacks = 0;
    }

    @Override
    protected void onEnrageBehavior() {
        // Phase 2 escalation is handled via the isPhaseTwo flag inside handleBlightAccumulation.
        // The stack threshold halves, and the surge leaves a Blight residue on impact.
    }

    @Override
    protected void onEnrage(LivingEntity entity) {
        isPhaseTwo = true;
        blightStacks = 0;
    }

    /**
     * Delegates on-hit effects for the Blight Surge to the concrete mob.
     * For example, Husk applies Hunger here.
     */
    protected abstract void applySurgeHitEffects(Player player);

    private void handleBlightAccumulation() {
        if (!isAlive() || isSurging || isRecovering) return;

        Player target = getNearestPlayer(ACCUMULATION_RADIUS);
        if (target != null && hasLineOfSight(target)) {
            blightStacks++;
            entity.getWorld().spawnParticle(
                    Particle.WAX_OFF,
                    entity.getLocation().add(0, 1, 0),
                    2, 0.3, 0.3, 0.3, 0.01
            );
        } else {
            blightStacks = Math.max(0, blightStacks - 1);
        }

        int threshold = isPhaseTwo ? 6 : 12;
        if (blightStacks >= threshold) {
            triggerBlightSurge(target);
        }
    }

    private void triggerBlightSurge(Player target) {
        isSurging = true;
        blightStacks = 0;

        double originalSpeed = Objects.requireNonNull(
                entity.getAttribute(Attribute.MOVEMENT_SPEED)).getBaseValue();
        Objects.requireNonNull(entity.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.0);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f, 1.5f);
        entity.getWorld().spawnParticle(
                Particle.SQUID_INK,
                entity.getLocation().add(0, 1, 0),
                10, 0.4, 0.4, 0.4, 0.0
        );

        addCoreDelayedAction(20L, () -> executeSurgeLunge(target, originalSpeed));
    }

    private void executeSurgeLunge(Player target, double originalSpeed) {
        if (!isAlive()) return;

        Vector direction;
        if (target != null && !target.isDead() && target.getWorld().equals(entity.getWorld())) {
            direction = target.getLocation().toVector().subtract(entity.getLocation().toVector());
        } else {
            direction = entity.getLocation().getDirection();
        }

        direction.setY(0).normalize().multiply(1.6).setY(0.2);
        entity.setVelocity(direction);
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.0f, 0.6f);

        addCoreDelayedAction(10L, () -> executeSurgeStrikeAndRecover(originalSpeed));
    }

    private void executeSurgeStrikeAndRecover(double originalSpeed) {
        if (!isAlive()) return;

        isSurging = false;
        isRecovering = true;

        getNearbyPlayers(SURGE_STRIKE_RADIUS).forEach(player -> {
            player.damage(this.damage * 1.5, entity);
            applySurgeHitEffects(player);
        });

        Objects.requireNonNull(entity.getAttribute(Attribute.MOVEMENT_SPEED))
                .setBaseValue(originalSpeed * 0.5);

        if (isPhaseTwo) {
            spawnBlightResidue();
        }

        addCoreDelayedAction(30L, () -> {
            if (!isAlive()) return;
            isRecovering = false;
            Objects.requireNonNull(entity.getAttribute(Attribute.MOVEMENT_SPEED))
                    .setBaseValue(originalSpeed);
        });
    }

    private void spawnBlightResidue() {
        AreaEffectCloud cloud = entity.getWorld().spawn(entity.getLocation(), AreaEffectCloud.class);
        cloud.setRadius(2.0f);
        cloud.setDuration(60);
        cloud.setWaitTime(0);
        cloud.setParticle(Particle.DUST, BLIGHT_DUST);
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0), true);
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
