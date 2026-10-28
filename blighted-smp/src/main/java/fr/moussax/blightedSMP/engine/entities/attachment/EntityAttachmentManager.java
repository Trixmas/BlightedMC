package fr.moussax.blightedSMP.engine.entities.attachment;

import fr.moussax.blightedSMP.BlightedSMP;
import fr.moussax.blightedSMP.engine.entities.BlightedEntity;
import fr.moussax.blightedSMP.engine.entities.EntityManager;
import fr.moussax.blightedSMP.engine.entities.equipment.EntityEquipmentHolder;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * Manages multipart and visual attachments bound to a parent {@link BlightedEntity}.
 *
 * <p>Handles local 3D offset calculations, relative yaw/pitch rotation tracking,
 * persistent data tagging, and lifecycle cleanup.</p>
 */
public final class EntityAttachmentManager {

    public static final NamespacedKey ATTACHMENT_OWNER_KEY =
            new NamespacedKey(BlightedSMP.getInstance(), "blighted_attachment_owner");
    public static final NamespacedKey ATTACHMENT_ROLE_KEY =
            new NamespacedKey(BlightedSMP.getInstance(), "blighted_attachment_role");
    public static final NamespacedKey ATTACHMENT_OFFSET_X_KEY =
            new NamespacedKey(BlightedSMP.getInstance(), "blighted_attachment_offset_x");
    public static final NamespacedKey ATTACHMENT_OFFSET_Y_KEY =
            new NamespacedKey(BlightedSMP.getInstance(), "blighted_attachment_offset_y");
    public static final NamespacedKey ATTACHMENT_OFFSET_Z_KEY =
            new NamespacedKey(BlightedSMP.getInstance(), "blighted_attachment_offset_z");
    public static final NamespacedKey ATTACHMENT_SYNC_YAW_KEY =
            new NamespacedKey(BlightedSMP.getInstance(), "blighted_attachment_sync_yaw");
    public static final NamespacedKey ATTACHMENT_SYNC_PITCH_KEY =
            new NamespacedKey(BlightedSMP.getInstance(), "blighted_attachment_sync_pitch");

    private final BlightedEntity owner;
    private final Set<EntityAttachment> attachments = new CopyOnWriteArraySet<>();

    public EntityAttachmentManager(@NonNull BlightedEntity owner) {
        this.owner = Objects.requireNonNull(owner, "owner cannot be null");
    }

    /**
     * Attaches a non-interactive {@link ItemDisplay} entity with local 3D translation offset.
     *
     * @param offset       local offset relative to base entity origin and facing yaw
     * @param configurator optional configuration consumer
     * @return the created ItemDisplay attachment, or null if entity is not alive
     */
    @Nullable
    public ItemDisplay attachItemDisplay(@Nullable Vector offset, @Nullable Consumer<ItemDisplay> configurator) {
        LivingEntity live = owner.getEntity();
        if (live == null || !live.isValid() || live.isDead()) {
            return null;
        }

        Vector localOffset = offset != null ? offset : new Vector(0, 0, 0);
        Location spawnLocation = live.getLocation().clone().add(localOffset);
        ItemDisplay display = live.getWorld().spawn(spawnLocation, ItemDisplay.class, itemDisplay -> {
            if (configurator != null) {
                configurator.accept(itemDisplay);
            }
        });
        addAttachment(display, AttachmentRole.VISUAL, localOffset, true, false);
        return display;
    }

    /**
     * Attaches a non-interactive {@link BlockDisplay} entity with local 3D translation offset.
     *
     * @param offset       local offset relative to base entity origin and facing yaw
     * @param configurator optional configuration consumer
     * @return the created BlockDisplay attachment, or null if entity is not alive
     */
    @Nullable
    public BlockDisplay attachBlockDisplay(@Nullable Vector offset, @Nullable Consumer<BlockDisplay> configurator) {
        LivingEntity live = owner.getEntity();
        if (live == null || !live.isValid() || live.isDead()) {
            return null;
        }

        Vector localOffset = offset != null ? offset : new Vector(0, 0, 0);
        Location spawnLocation = live.getLocation().clone().add(localOffset);
        BlockDisplay display = live.getWorld().spawn(spawnLocation, BlockDisplay.class, blockDisplay -> {
            if (configurator != null) {
                configurator.accept(blockDisplay);
            }
        });
        addAttachment(display, AttachmentRole.VISUAL, localOffset, true, false);
        return display;
    }

    /**
     * Attaches a multipart hittable {@link Interaction} hitbox entity.
     *
     * @param offset       local offset relative to base entity origin and facing yaw
     * @param width        hitbox width
     * @param height       hitbox height
     * @param configurator optional configuration consumer
     * @return the created Interaction attachment, or null if entity is not alive
     */
    @Nullable
    public Interaction attachHitbox(
            @Nullable Vector offset,
            float width,
            float height,
            @Nullable Consumer<Interaction> configurator
    ) {
        LivingEntity live = owner.getEntity();
        if (live == null || !live.isValid() || live.isDead()) {
            return null;
        }

        Vector localOffset = offset != null ? offset : new Vector(0, 0, 0);
        Location spawnLocation = live.getLocation().clone().add(localOffset);
        Interaction interaction = live.getWorld().spawn(spawnLocation, Interaction.class, hitbox -> {
            hitbox.setInteractionWidth(width);
            hitbox.setInteractionHeight(height);
            if (configurator != null) {
                configurator.accept(hitbox);
            }
        });
        addAttachment(interaction, AttachmentRole.HITBOX, localOffset, true, false);
        return interaction;
    }

    /**
     * Attaches an entity using {@link AttachmentRole#SUBORDINATE}.
     *
     * @param attachmentEntity entity to attach
     */
    public void addAttachment(@Nullable Entity attachmentEntity) {
        addAttachment(attachmentEntity, AttachmentRole.SUBORDINATE, new Vector(0, 0, 0), true, false);
    }

    /**
     * Attaches an entity with the given role.
     *
     * @param attachmentEntity entity to attach
     * @param role             attachment role
     */
    public void addAttachment(@Nullable Entity attachmentEntity, @NonNull AttachmentRole role) {
        addAttachment(attachmentEntity, role, new Vector(0, 0, 0), true, false);
    }

    /**
     * Attaches an entity with the given role and local offset.
     *
     * @param attachmentEntity entity to attach
     * @param role             attachment role
     * @param offset           local 3D offset
     */
    public void addAttachment(@Nullable Entity attachmentEntity, @NonNull AttachmentRole role, @Nullable Vector offset) {
        addAttachment(attachmentEntity, role, offset, true, false);
    }

    /**
     * Attaches an entity with full offset and rotation synchronization configuration.
     *
     * @param attachmentEntity entity to attach
     * @param role             attachment role
     * @param offset           local 3D offset
     * @param syncYaw          whether horizontal rotation follows base yaw
     * @param syncPitch        whether vertical rotation follows base pitch
     */
    public void addAttachment(
            @Nullable Entity attachmentEntity,
            @NonNull AttachmentRole role,
            @Nullable Vector offset,
            boolean syncYaw,
            boolean syncPitch
    ) {
        if (attachmentEntity == null) {
            return;
        }

        Vector vector = offset != null ? offset : new Vector(0, 0, 0);
        attachments.add(new EntityAttachment(attachmentEntity, role, vector, syncYaw, syncPitch));
        EntityManager.registerAttachment(attachmentEntity, owner);

        LivingEntity live = owner.getEntity();
        if (live != null) {
            attachmentEntity.getPersistentDataContainer().set(
                    ATTACHMENT_OWNER_KEY, PersistentDataType.STRING, live.getUniqueId().toString());
            attachmentEntity.getPersistentDataContainer().set(
                    ATTACHMENT_ROLE_KEY, PersistentDataType.STRING, role.name());
            attachmentEntity.getPersistentDataContainer().set(
                    ATTACHMENT_OFFSET_X_KEY, PersistentDataType.DOUBLE, vector.getX());
            attachmentEntity.getPersistentDataContainer().set(
                    ATTACHMENT_OFFSET_Y_KEY, PersistentDataType.DOUBLE, vector.getY());
            attachmentEntity.getPersistentDataContainer().set(
                    ATTACHMENT_OFFSET_Z_KEY, PersistentDataType.DOUBLE, vector.getZ());
            attachmentEntity.getPersistentDataContainer().set(
                    ATTACHMENT_SYNC_YAW_KEY, PersistentDataType.BYTE, (byte) (syncYaw ? 1 : 0));
            attachmentEntity.getPersistentDataContainer().set(
                    ATTACHMENT_SYNC_PITCH_KEY, PersistentDataType.BYTE, (byte) (syncPitch ? 1 : 0));
        }

        if (attachmentEntity instanceof Display display) {
            display.setTeleportDuration(1);
            display.setInterpolationDuration(1);
        }

        if (attachmentEntity instanceof LivingEntity living) {
            EntityEquipment equipment = living.getEquipment();
            if (equipment != null) {
                EntityEquipmentHolder.zeroEquipmentDropChances(equipment);
            }
        }
        attachmentEntity.addScoreboardTag(BlightedEntity.FAST_PASS_TAG);
    }

    /**
     * Synchronizes all registered attachments to their relative world position based on base location and facing yaw.
     */
    public void syncAttachments() {
        LivingEntity live = owner.getEntity();
        if (live == null || !live.isValid() || live.isDead() || attachments.isEmpty()) {
            return;
        }

        Location baseLocation = live.getLocation();
        double radians = Math.toRadians(baseLocation.getYaw());
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        for (EntityAttachment attachment : attachments) {
            Entity attachedEntity = attachment.entity();
            if (attachedEntity == null || !attachedEntity.isValid() || attachedEntity.isDead()) {
                attachments.remove(attachment);
                if (attachedEntity != null) {
                    EntityManager.unregisterAttachment(attachedEntity);
                }
                continue;
            }

            if (attachment.role() == AttachmentRole.SUBORDINATE) {
                continue;
            }

            if (live.getPassengers().contains(attachedEntity)) {
                continue;
            }

            Vector offset = attachment.localOffset();
            double xPrime = baseLocation.getX() + (offset.getX() * cos - offset.getZ() * sin);
            double zPrime = baseLocation.getZ() + (offset.getX() * sin + offset.getZ() * cos);
            double yPrime = baseLocation.getY() + offset.getY();

            Location currentLocation = attachedEntity.getLocation();
            float targetYaw = attachment.syncYaw() ? baseLocation.getYaw() : currentLocation.getYaw();
            float targetPitch = attachment.syncPitch() ? baseLocation.getPitch() : currentLocation.getPitch();

            if (currentLocation.getWorld() == baseLocation.getWorld()
                    && Math.abs(currentLocation.getX() - xPrime) < 0.001
                    && Math.abs(currentLocation.getY() - yPrime) < 0.001
                    && Math.abs(currentLocation.getZ() - zPrime) < 0.001
                    && Math.abs(currentLocation.getYaw() - targetYaw) < 0.1f
                    && Math.abs(currentLocation.getPitch() - targetPitch) < 0.1f) {
                continue;
            }

            Location targetLocation = new Location(baseLocation.getWorld(), xPrime, yPrime, zPrime, targetYaw, targetPitch);
            attachedEntity.teleport(targetLocation);
        }
    }

    /**
     * Removes and destroys all attached entities.
     */
    public void killAllAttachments() {
        if (attachments.isEmpty()) {
            return;
        }

        for (EntityAttachment attachment : attachments) {
            Entity attachmentEntity = attachment.entity();
            if (attachmentEntity == null) {
                continue;
            }

            EntityManager.unregisterAttachment(attachmentEntity);
            attachmentEntity.remove();
        }
        attachments.clear();
    }

    /**
     * Removes and destroys attached entities matching the specified role.
     *
     * @param role attachment role to remove
     */
    public void killAttachments(@Nullable AttachmentRole role) {
        if (attachments.isEmpty() || role == null) {
            return;
        }

        attachments.removeIf(attachment -> {
            if (attachment.role() != role) {
                return false;
            }
            Entity attachmentEntity = attachment.entity();
            if (attachmentEntity != null) {
                EntityManager.unregisterAttachment(attachmentEntity);
                attachmentEntity.remove();
            }
            return true;
        });
    }

    /**
     * Checks whether a living body attachment is currently present.
     *
     * @return {@code true} if a living body attachment exists
     */
    public boolean hasLivingBodyAttachment() {
        for (EntityAttachment attachment : attachments) {
            if (attachment.entity() instanceof LivingEntity living && !living.isDead()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether subordinate attachments are present.
     *
     * @return {@code true} if subordinate attachments exist
     */
    public boolean hasSubordinateAttachments() {
        for (EntityAttachment attachment : attachments) {
            if (attachment.role() == AttachmentRole.SUBORDINATE && attachment.entity().isValid()) {
                return true;
            }
        }
        return false;
    }

    private long lastDamageTick = -1;
    private java.util.UUID lastDamagerUuid = null;

    /**
     * Prevents multi-hitbox attachments from applying double damage from the same attack in the same tick.
     *
     * @param damager attacking entity
     * @return {@code true} if damage should be blocked
     */
    public boolean shouldBlockSameTickDamage(@Nullable Entity damager) {
        if (damager == null) {
            return false;
        }
        LivingEntity live = owner.getEntity();
        long currentTick = live != null
                ? live.getWorld().getGameTime()
                : System.currentTimeMillis();
        java.util.UUID damagerUuid = damager.getUniqueId();

        if (currentTick == lastDamageTick && java.util.Objects.equals(damagerUuid, lastDamagerUuid)) {
            return true;
        }

        lastDamageTick = currentTick;
        lastDamagerUuid = damagerUuid;
        return false;
    }

    /**
     * Returns an unmodifiable snapshot view of all registered attachments.
     *
     * @return registered attachments
     */
    public Set<EntityAttachment> getAttachments() {
        return Collections.unmodifiableSet(attachments);
    }

    /**
     * Re-registers an attachment discovered during chunk rehydration.
     *
     * @param attachment rehydrated entity attachment
     */
    public void registerRehydratedAttachment(@NonNull EntityAttachment attachment) {
        if (attachment.entity() != null) {
            attachments.removeIf(existingAttachment -> existingAttachment.entity() != null
                    && existingAttachment.entity().getUniqueId().equals(attachment.entity().getUniqueId()));
        }
        attachments.add(attachment);
    }

    public void clear() {
        attachments.clear();
    }
}
