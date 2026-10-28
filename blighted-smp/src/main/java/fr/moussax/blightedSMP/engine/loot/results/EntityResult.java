package fr.moussax.blightedSMP.engine.loot.results;

import fr.moussax.blightedSMP.engine.loot.LootContext;
import fr.moussax.blightedSMP.engine.loot.LootResult;
import fr.moussax.bedrock.text.Formatter;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A {@link LootResult} that spawns a vanilla or custom entity at the loot origin.
 */
public final class EntityResult implements LootResult {

    private final EntityType entityType;
    private final Function<Location, LivingEntity> customSpawner;
    private final Consumer<LivingEntity> entityModifier;

    private EntityResult(
            EntityType entityType,
            Function<Location, LivingEntity> customSpawner,
            Consumer<LivingEntity> entityModifier
    ) {
        this.entityType = entityType;
        this.customSpawner = customSpawner;
        this.entityModifier = entityModifier;
    }

    /**
     * Creates an entity result for a vanilla entity type.
     *
     * @param entityType entity type to spawn
     * @return new entity result
     */
    public static EntityResult vanilla(EntityType entityType) {
        return new EntityResult(Objects.requireNonNull(entityType), null, null);
    }

    /**
     * Creates an entity result for a vanilla entity type with a modifier function.
     *
     * @param entityType     entity type to spawn
     * @param entityModifier modifier applied to the spawned entity
     * @return new entity result
     */
    public static EntityResult vanilla(EntityType entityType, Consumer<LivingEntity> entityModifier) {
        return new EntityResult(Objects.requireNonNull(entityType), null, entityModifier);
    }

    /**
     * Creates an entity result for a custom Blighted entity.
     *
     * @param blightedEntity Blighted entity to spawn
     * @return new entity result
     */
    public static EntityResult blighted(fr.moussax.blightedSMP.engine.entities.BlightedEntity blightedEntity) {
        Objects.requireNonNull(blightedEntity, "blightedEntity cannot be null");
        return custom(spawnLocation -> blightedEntity.createInstance().spawn(spawnLocation));
    }

    /**
     * Creates an entity result for a custom entity spawner function.
     *
     * @param customSpawner function that spawns the entity at the given location
     * @return new entity result
     */
    public static EntityResult custom(Function<Location, LivingEntity> customSpawner) {
        return new EntityResult(null, Objects.requireNonNull(customSpawner), null);
    }

    /**
     * Spawns the entity at the loot origin and applies initial velocity and modifiers.
     *
     * @param context loot context
     * @param amount  quantity parameter (unused for single entity spawn)
     */
    @Override
    public void execute(LootContext context, int amount) {
        LivingEntity spawned = null;

        if (entityType != null) {
            Object rawEntity = Objects.requireNonNull(context.origin().getWorld())
                    .spawnEntity(context.origin(), entityType);

            if (rawEntity instanceof LivingEntity living) {
                spawned = living;
            } else {
                return;
            }
        } else if (customSpawner != null) {
            spawned = customSpawner.apply(context.origin());
        }

        if (spawned != null) {
            if (entityModifier != null) {
                entityModifier.accept(spawned);
            }
            if (context.velocity() != null) {
                spawned.setVelocity(context.velocity());
            }
        }
    }

    /**
     * Returns the formatted display name of the spawned entity type or generic description.
     *
     * @param amount drop quantity
     * @return formatted display name
     */
    @Override
    public String displayName(int amount) {
        if (entityType != null) {
            return Formatter.formatEnumName(entityType.name());
        }
        return "Blighted Entity";
    }
}
