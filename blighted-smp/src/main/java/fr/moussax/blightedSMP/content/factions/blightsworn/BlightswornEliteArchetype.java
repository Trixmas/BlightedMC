package fr.moussax.blightedSMP.content.factions.blightsworn;

import fr.moussax.bedrock.utils.ItemBuilder;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.Objects;

public sealed abstract class BlightswornEliteArchetype extends BlightswornCreature
        permits BlightswornPiglin, BlightswornWitherSkeleton, BlightswornZombifiedPiglin {

    private static final int WARD_DELAY_TICKS = 160;
    private static final int WARD_PERIOD_TICKS = 180;
    private static final int WARD_DURATION_TICKS = 50;
    private static final double WARD_SPEED_MULTIPLIER = 1.2;
    private static final double WARD_DAMAGE_REDUCTION = 0.6;

    private static final int MAX_HEALTH = 35;

    @Getter
    private boolean isWarded = false;

    protected BlightswornEliteArchetype(String entityId, String name, EntityType entityType) {
        super(entityId, name, entityType, MAX_HEALTH);
    }

    protected BlightswornEliteArchetype(String entityId, String name, EntityType entityType, double spawnProbability) {
        super(entityId, name, entityType, MAX_HEALTH, spawnProbability);
    }

    @Override
    protected void onDefineAdditionalBehavior() {
        addCoreAbility(WARD_DELAY_TICKS, WARD_PERIOD_TICKS, this::triggerBlightWard);
    }

    @Override
    public void onDamageTaken(EntityDamageEvent event) {
        super.onDamageTaken(event);
        if (isWarded) {
            event.setDamage(event.getDamage() * WARD_DAMAGE_REDUCTION);
        }
    }

    private void triggerBlightWard() {
        if (!isAlive()) return;
        isWarded = true;

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 0.8f);
        entity.getWorld().spawnParticle(
                Particle.WITCH,
                entity.getLocation().add(0, 1, 0),
                20, 0.5, 0.5, 0.5, 0.1
        );
        entity.getWorld().spawnParticle(
                Particle.DUST,
                entity.getLocation().add(0, 1, 0),
                12, 0.4, 0.4, 0.4, 0.0,
                BLIGHT_DUST
        );

        double baseSpeed = getAttributeValue(Attribute.MOVEMENT_SPEED);
        setAttribute(Attribute.MOVEMENT_SPEED, baseSpeed * WARD_SPEED_MULTIPLIER);

        addCoreDelayedAction(WARD_DURATION_TICKS, () -> {
            if (!isAlive()) return;
            isWarded = false;
            setAttribute(Attribute.MOVEMENT_SPEED, baseSpeed);
        });
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
                        .setArmorTrim(TrimMaterial.AMETHYST, TrimPattern.FLOW)
                        .setLeatherColor(PHASE_TWO_COLOR)
                        .unbreakable()
                        .toItemStack(),
                new ItemBuilder(Material.IRON_LEGGINGS).unbreakable().toItemStack(),
                new ItemBuilder(Material.IRON_BOOTS).unbreakable().toItemStack()
        );
    }


}
