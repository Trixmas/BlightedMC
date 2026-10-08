package fr.moussax.blightedSMP.engine.entities;

import fr.moussax.blightedSMP.BlightedSMP;
import fr.moussax.blightedSMP.engine.entities.attachment.AttachmentRole;
import fr.moussax.blightedSMP.engine.entities.attachment.EntityAttachment;
import fr.moussax.blightedSMP.engine.entities.registry.EntitiesRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static fr.moussax.blightedSMP.engine.entities.BlightedEntity.ENTITY_ID_KEY;
import static fr.moussax.blightedSMP.engine.entities.BlightedEntity.FAST_PASS_TAG;
import static fr.moussax.blightedSMP.engine.entities.attachment.EntityAttachmentManager.*;

/**
 * Central runtime entity manager tracking active {@link BlightedEntity} instances,
 * attachments, chunk rehydration, and orphan sweeping.
 */
public final class EntityManager {

    private static final Map<UUID, BlightedEntity> BLIGHTED_ENTITIES = new ConcurrentHashMap<>();
    private static final Map<UUID, BlightedEntity> ATTACHMENT_OWNERS = new ConcurrentHashMap<>();
    private static final long ORPHAN_SWEEP_PERIOD_TICKS = 100L; // 5s

    private EntityManager() {
    }

    /**
     * Initializes a background orphan entity cleanup task.
     */
    public static void initialize() {
        Bukkit.getScheduler().runTaskTimer(
                BlightedSMP.getInstance(), EntityManager::sweepOrphanedEntities,
                ORPHAN_SWEEP_PERIOD_TICKS, ORPHAN_SWEEP_PERIOD_TICKS
        );
    }

    /**
     * Sweeps and cleans up invalid or dead entities from tracking maps.
     */
    public static void sweepOrphanedEntities() {
        for (BlightedEntity blighted : List.copyOf(BLIGHTED_ENTITIES.values())) {
            LivingEntity entity = blighted.getEntity();
            if (entity == null || entity.isDead() || !entity.isValid()) {
                if (entity != null) {
                    BLIGHTED_ENTITIES.remove(entity.getUniqueId(), blighted);
                } else {
                    BLIGHTED_ENTITIES.values().remove(blighted);
                }
                blighted.cleanup();
            }
        }
    }

    /**
     * Registers an active entity instance with its runtime wrapper.
     *
     * @param entity   living entity to track
     * @param blighted blighted entity wrapper instance
     */
    public static void registerEntity(LivingEntity entity, BlightedEntity blighted) {
        if (entity == null || blighted == null) return;
        BLIGHTED_ENTITIES.put(entity.getUniqueId(), blighted);
    }

    /**
     * Unregisters active entity instance from tracking.
     *
     * @param entity living entity to stop tracking
     */
    public static void unregisterEntity(LivingEntity entity) {
        if (entity == null) return;
        BLIGHTED_ENTITIES.remove(entity.getUniqueId());
    }

    /**
     * Registers attachment entity to its owning blighted entity wrapper.
     *
     * @param attachment attachment entity
     * @param owner      owning blighted entity wrapper
     */
    public static void registerAttachment(Entity attachment, BlightedEntity owner) {
        if (attachment == null || owner == null) return;
        ATTACHMENT_OWNERS.put(attachment.getUniqueId(), owner);
    }

    /**
     * Unregisters attachment entity from tracking.
     *
     * @param attachment attachment entity to unregister
     */
    public static void unregisterAttachment(Entity attachment) {
        if (attachment == null) return;
        ATTACHMENT_OWNERS.remove(attachment.getUniqueId());
    }

    /**
     * Retrieves blighted entity wrapper associated with entity or attachment entity.
     *
     * @param entity target entity
     * @return blighted entity wrapper, or {@code null} if untracked
     */
    @Nullable
    public static BlightedEntity getBlightedEntity(Entity entity) {
        if (entity == null) return null;
        UUID id = entity.getUniqueId();
        BlightedEntity blighted = BLIGHTED_ENTITIES.get(id);
        return blighted != null ? blighted : ATTACHMENT_OWNERS.get(id);
    }

    /**
     * Retrieves owning blighted entity wrapper for attachment entity UUID.
     *
     * @param entityId attachment entity unique identifier
     * @return owning blighted entity wrapper, or {@code null} if untracked
     */
    @Nullable
    public static BlightedEntity getAttachmentOwner(UUID entityId) {
        if (entityId == null) return null;
        return ATTACHMENT_OWNERS.get(entityId);
    }

    /**
     * Retrieves registered blighted entity wrapper by entity UUID.
     *
     * @param entityId target entity unique identifier
     * @return direct blighted entity wrapper, or {@code null} if untracked
     */
    @Nullable
    public static BlightedEntity getDirectBlightedEntity(UUID entityId) {
        if (entityId == null) return null;
        return BLIGHTED_ENTITIES.get(entityId);
    }

    /**
     * Returns snapshot collection of all currently tracked active blighted entities.
     *
     * @return active blighted entity collection
     */
    public static Collection<BlightedEntity> getActiveEntities() {
        return List.copyOf(BLIGHTED_ENTITIES.values());
    }

    /**
     * Removes attachment owner tracking entry by entity UUID.
     *
     * @param entityId target attachment entity unique identifier
     */
    public static void removeAttachmentOwner(UUID entityId) {
        if (entityId == null) return;
        ATTACHMENT_OWNERS.remove(entityId);
    }

    /**
     * Removes direct blighted entity tracking entry by entity UUID.
     *
     * @param entityId target entity unique identifier
     */
    public static void removeDirectBlightedEntity(UUID entityId) {
        if (entityId == null) return;
        BLIGHTED_ENTITIES.remove(entityId);
    }

    /**
     * Rehydrates blighted entities and attachment entities stored in persistent data when chunk loads.
     *
     * @param chunk loaded chunk to rehydrate
     */
    public static void rehydrateChunk(Chunk chunk) {
        if (chunk == null || !chunk.isLoaded()) {
            return;
        }
        Entity[] entities = chunk.getEntities();

        // Pass 1: rehydrate main blighted entities.
        for (Entity entity : entities) {
            if (!(entity instanceof LivingEntity living)) continue;
            if (!living.getScoreboardTags().contains(FAST_PASS_TAG)) continue;

            PersistentDataContainer persistentDataContainer = living.getPersistentDataContainer();
            if (!persistentDataContainer.has(ENTITY_ID_KEY, PersistentDataType.STRING)) continue;

            BlightedEntity existing = BLIGHTED_ENTITIES.get(living.getUniqueId());
            if (existing != null) {
                existing.attachToExisting(living);
                continue;
            }

            String entityId = persistentDataContainer.get(ENTITY_ID_KEY, PersistentDataType.STRING);
            BlightedEntity blighted = EntitiesRegistry.create(entityId);
            if (blighted == null) continue;

            blighted.attachToExisting(living);
        }

        boolean hasAttachments = false;

        // Pass 2: re-register attachment entities carrying ATTACHMENT_OWNER_KEY.
        for (Entity entity : entities) {
            if (!entity.getScoreboardTags().contains(FAST_PASS_TAG)) continue;

            PersistentDataContainer persistentDataContainer = entity.getPersistentDataContainer();
            if (!persistentDataContainer.has(ATTACHMENT_OWNER_KEY, PersistentDataType.STRING)) continue;

            hasAttachments = true;

            String ownerUuidString = persistentDataContainer.get(ATTACHMENT_OWNER_KEY, PersistentDataType.STRING);
            String roleString = persistentDataContainer.get(ATTACHMENT_ROLE_KEY, PersistentDataType.STRING);
            if (ownerUuidString == null || roleString == null) continue;

            UUID ownerUuid;
            try {
                ownerUuid = UUID.fromString(ownerUuidString);
            } catch (IllegalArgumentException _) {
                continue;
            }

            BlightedEntity owner = BLIGHTED_ENTITIES.get(ownerUuid);
            if (owner == null) continue;

            AttachmentRole role;
            try {
                role = AttachmentRole.valueOf(roleString);
            } catch (IllegalArgumentException _) {
                role = AttachmentRole.SUBORDINATE;
            }

            Double offsetX = persistentDataContainer.get(ATTACHMENT_OFFSET_X_KEY, PersistentDataType.DOUBLE);
            Double offsetY = persistentDataContainer.get(ATTACHMENT_OFFSET_Y_KEY, PersistentDataType.DOUBLE);
            Double offsetZ = persistentDataContainer.get(ATTACHMENT_OFFSET_Z_KEY, PersistentDataType.DOUBLE);
            Vector offset = new Vector(
                    offsetX != null ? offsetX : 0.0,
                    offsetY != null ? offsetY : 0.0,
                    offsetZ != null ? offsetZ : 0.0
            );

            Byte yawByte = persistentDataContainer.get(ATTACHMENT_SYNC_YAW_KEY, PersistentDataType.BYTE);
            Byte pitchByte = persistentDataContainer.get(ATTACHMENT_SYNC_PITCH_KEY, PersistentDataType.BYTE);
            boolean syncYaw = yawByte == null || yawByte == 1;
            boolean syncPitch = pitchByte != null && pitchByte == 1;

            owner.getAttachmentManager().registerRehydratedAttachment(
                    new EntityAttachment(entity, role, offset, syncYaw, syncPitch));
            registerAttachment(entity, owner);
        }

        if (!hasAttachments) return;

        // Pass 3: Purge orphan attachments whose owner entity no longer exists.
        Bukkit.getScheduler().runTaskLater(BlightedSMP.getInstance(), () -> {
            if (!chunk.isLoaded()) return;
            for (Entity entity : chunk.getEntities()) {
                if (!entity.getScoreboardTags().contains(FAST_PASS_TAG)) continue;
                PersistentDataContainer persistentDataContainer = entity.getPersistentDataContainer();
                if (!persistentDataContainer.has(ATTACHMENT_OWNER_KEY, PersistentDataType.STRING)) continue;

                String ownerUuidString = persistentDataContainer.get(ATTACHMENT_OWNER_KEY, PersistentDataType.STRING);
                if (ownerUuidString == null) continue;

                try {
                    UUID ownerUuid = UUID.fromString(ownerUuidString);
                    BlightedEntity owner = BLIGHTED_ENTITIES.get(ownerUuid);
                    if (owner == null || owner.getEntity() == null || !owner.getEntity().isValid()) {
                        entity.remove();
                        ATTACHMENT_OWNERS.remove(entity.getUniqueId());
                    }
                } catch (IllegalArgumentException _) {
                    entity.remove();
                }
            }
        }, 3L);
    }
}
