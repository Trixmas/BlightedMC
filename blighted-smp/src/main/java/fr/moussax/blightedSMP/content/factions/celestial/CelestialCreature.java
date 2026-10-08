package fr.moussax.blightedSMP.content.factions.celestial;

import fr.moussax.blightedSMP.engine.entities.spawnable.SpawnableEntity;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
public abstract class CelestialCreature extends SpawnableEntity {

    protected CelestialCreature(String entityId, String name, EntityType entityType) {
        super(entityId, name, entityType);
    }

    protected CelestialCreature(String entityId, String name, int maxHealth, EntityType entityType, double probability) {
        super(entityId, name, entityType);
        setMaxHealth(maxHealth);
        spawning(spawn -> spawn.probability(probability));
    }

    @Override
    public LivingEntity spawn(Location location) {
        LivingEntity entity = super.spawn(location);
        entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, 1));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 1));
        return entity;
    }
}
