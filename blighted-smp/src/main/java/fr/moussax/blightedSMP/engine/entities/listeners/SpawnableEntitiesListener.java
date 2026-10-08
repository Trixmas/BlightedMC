package fr.moussax.blightedSMP.engine.entities.listeners;

import fr.moussax.blightedSMP.engine.entities.registry.EntitiesRegistry;
import fr.moussax.blightedSMP.engine.entities.spawnable.SpawnableEntity;
import fr.moussax.blightedSMP.engine.entities.spawnable.engine.SpawnEvaluator;
import fr.moussax.blightedSMP.engine.entities.spawnable.engine.SpawnMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Event listener replacing natural creature spawns with custom {@link SpawnableEntity} instances.
 */
public final class SpawnableEntitiesListener implements Listener {

    private volatile Map<EntityType, List<SpawnableEntity>> spawnCache = Collections.emptyMap();
    private volatile boolean cacheDirty = true;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (cacheDirty) rebuildCache();

        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason != CreatureSpawnEvent.SpawnReason.NATURAL
                && reason != CreatureSpawnEvent.SpawnReason.REINFORCEMENTS) {
            return;
        }

        List<SpawnableEntity> candidates = spawnCache.get(event.getEntityType());
        if (candidates == null || candidates.isEmpty()) return;

        Location location = event.getLocation();
        World world = location.getWorld();
        if (world == null) return;

        SpawnableEntity selected = SpawnEvaluator.selectCandidate(candidates, location, world, ThreadLocalRandom.current());
        if (selected != null) {
            event.setCancelled(true);
            selected.createInstance().spawn(location);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.isNewChunk()) return;
        if (cacheDirty) rebuildCache();

        for (Entity entity : event.getChunk().getEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;

            List<SpawnableEntity> candidates = spawnCache.get(living.getType());
            if (candidates == null || candidates.isEmpty()) continue;

            Location location = living.getLocation();
            World world = location.getWorld();
            if (world == null) continue;

            SpawnableEntity selected = SpawnEvaluator.selectCandidate(candidates, location, world, ThreadLocalRandom.current());
            if (selected != null) {
                living.remove();
                selected.createInstance().spawn(location);
            }
        }
    }

    /**
     * Rebuilds spawn candidate cache from registered spawnable entities configured for replacement or hybrid spawning.
     */
    public synchronized void rebuildCache() {
        if (!cacheDirty) return;

        Map<EntityType, List<SpawnableEntity>> newCache = new EnumMap<>(EntityType.class);
        for (SpawnableEntity entity : EntitiesRegistry.getSpawnables()) {
            SpawnMode mode = entity.getSpawnMode();
            if (mode == SpawnMode.REPLACEMENT || mode == SpawnMode.HYBRID) {
                newCache.computeIfAbsent(entity.getEntityType(), entityType -> new ArrayList<>()).add(entity);
            }
        }

        this.spawnCache = newCache;
        this.cacheDirty = false;
    }

    /**
     * Marks spawn cache as dirty to trigger rebuild on the next spawn event.
     */
    public void invalidateCache() {
        this.cacheDirty = true;
    }
}
