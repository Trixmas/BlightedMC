package fr.moussax.blightedSMP.engine.entities;

import fr.moussax.blightedSMP.BlightedSMP;
import fr.moussax.blightedSMP.engine.entities.attachment.AttachmentRole;
import fr.moussax.blightedSMP.engine.entities.attachment.EntityAttachment;
import fr.moussax.blightedSMP.engine.entities.attachment.EntityAttachmentManager;
import fr.moussax.bedrock.ui.bossbar.Bossbar;
import fr.moussax.bedrock.ui.bossbar.BossbarSection;
import fr.moussax.blightedSMP.engine.entities.components.EntityComponent;
import fr.moussax.blightedSMP.engine.entities.components.EntityComponentManager;
import fr.moussax.blightedSMP.engine.entities.defense.DamageType;
import fr.moussax.blightedSMP.engine.entities.defense.DefensesBuilder;
import fr.moussax.blightedSMP.engine.entities.defense.EntityDefenses;
import fr.moussax.blightedSMP.engine.entities.defense.EntityImmunity;
import fr.moussax.blightedSMP.engine.entities.equipment.EntityEquipmentBuilder;
import fr.moussax.blightedSMP.engine.entities.equipment.EntityEquipmentHolder;
import fr.moussax.blightedSMP.engine.entities.phases.EntityPhaseManager;
import fr.moussax.blightedSMP.engine.entities.phases.EntityPhasesBuilder;
import fr.moussax.blightedSMP.engine.entities.registry.EntitiesRegistry;
import fr.moussax.blightedSMP.engine.entities.state.EntityAttributeManager;
import fr.moussax.blightedSMP.engine.entities.state.EntityAttributesBuilder;
import fr.moussax.blightedSMP.engine.entities.state.EntityCooldowns;
import fr.moussax.blightedSMP.engine.entities.util.EntityEffects;
import fr.moussax.blightedSMP.engine.entities.util.EntitySpatialQueries;
import fr.moussax.blightedSMP.engine.loot.LootContext;
import fr.moussax.blightedSMP.engine.loot.LootTable;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Biome;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Base class for custom runtime-controlled entities backed by a Bukkit {@link LivingEntity}.
 *
 * <p>Manages entity lifecycle, attributes, phases, components, scheduled tasks,
 * attachments, immunities, and optional boss bar support.</p>
 *
 * <p>Instances are stateful and bound to one entity at runtime. API methods must
 * be called from the server thread unless stated otherwise.</p>
 */
public abstract class BlightedEntity {

    public static final NamespacedKey ENTITY_ID_KEY = new NamespacedKey(BlightedSMP.getInstance(), "blighted_entity_id");
    public static final String FAST_PASS_TAG = "blighted_opt";

    @Getter
    protected EntityAttachmentManager attachmentManager = new EntityAttachmentManager(this);
    @Getter
    protected EntityEquipmentHolder equipmentHolder = new EntityEquipmentHolder();
    @Getter
    protected EntityCooldowns cooldowns = new EntityCooldowns();
    @Getter
    protected EntityComponentManager componentManager = new EntityComponentManager();
    @Getter
    protected EntityPhaseManager phaseManager = new EntityPhaseManager(this, this::onPhaseTransition);
    @Getter
    protected EntityAttributeManager attributes = new EntityAttributeManager();
    @Getter
    protected BarColor bossBarColor = BarColor.RED;
    @Getter
    protected BarStyle bossBarStyle = BarStyle.SOLID;
    @Getter
    protected double bossBarRadius = 60.0;
    @Getter
    protected Set<BarFlag> bossBarFlags = EnumSet.noneOf(BarFlag.class);
    @Nullable
    protected Consumer<BossbarSection.Builder> bossBarConfigurator;
    @Nullable
    private String bossBarSectionId;
    @Getter
    private EntityDefenses defenses = EntityDefenses.fromClass(getClass());

    @Getter
    protected String entityId;
    @Getter
    protected String name;
    @Getter
    protected EntityType entityType;
    @Getter
    protected LivingEntity entity;
    @Getter
    protected int maxHealth;
    @Getter
    protected int damage;
    @Getter
    protected int defense;
    @Setter
    @Getter
    protected int droppedExp = 0;

    @Getter
    protected LootTable lootTable;

    @Getter
    @Setter
    protected boolean isBoss = false;

    @Getter
    @Setter
    protected boolean isPerformingAbility = false;

    @Getter
    private boolean runtimeInitialized = false;

    public static final int DEFAULT_MAX_HEALTH = 20;
    public static final int DEFAULT_DAMAGE = 2;
    public static final int DEFAULT_DEFENSE = 0;

    /**
     * Creates an entity definition with basic identity requirements.
     * Combat stats and behavior can be configured using fluent setters.
     *
     * @param entityId   unique entity identifier
     * @param name       entity display name
     * @param entityType Bukkit entity type
     */
    public BlightedEntity(@NonNull String entityId, @NonNull String name, @NonNull EntityType entityType) {
        this.entityId = Objects.requireNonNull(entityId, "entityId cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.entityType = Objects.requireNonNull(entityType, "entityType cannot be null");
        this.maxHealth = DEFAULT_MAX_HEALTH;
        this.damage = DEFAULT_DAMAGE;
        this.defense = DEFAULT_DEFENSE;
    }

    /**
     * Spawns and initializes the entity at the given location.
     *
     * @param location spawn location
     * @return the spawned entity
     * @throws IllegalStateException if {@code entityType} is not set
     */
    public LivingEntity spawn(Location location) {
        if (entityType == null) {
            throw new IllegalStateException("EntityType cannot be null");
        }

        entity = (LivingEntity) Objects.requireNonNull(location.getWorld()).spawnEntity(location, entityType);
        entity.addScoreboardTag(FAST_PASS_TAG);
        entity.getPersistentDataContainer().set(ENTITY_ID_KEY, PersistentDataType.STRING, getEntityId());

        attributes.initialize(entity, maxHealth, damage, defense, isBoss);
        configureEquipment();
        onConfigureAI(entity);

        if (isBoss) {
            spawnBossBar();
        }

        EntityManager.registerEntity(entity, this);
        componentManager.init(entity);
        initRuntime();

        return entity;
    }

    /**
     * Binds this instance to an existing entity and restores its runtime state.
     *
     * @param existing entity to bind to
     */
    public void attachToExisting(LivingEntity existing) {
        this.entity = existing;
        if (!entity.getScoreboardTags().contains(FAST_PASS_TAG)) {
            entity.addScoreboardTag(FAST_PASS_TAG);
        }

        attributes.rehydrate(existing, maxHealth, damage, defense, isBoss);
        onConfigureAI(existing);

        if (isBoss) {
            spawnBossBar();
        }
        EntityManager.registerEntity(existing, this);

        componentManager.init(existing);
        onRehydrate(existing);

        if (!runtimeInitialized) {
            initRuntime();
        } else {
            phaseManager.scheduleAllTasks();
        }
    }

    /**
     * Kills the entity and performs full lifecycle cleanup.
     */
    public void kill() {
        if (!isAlive()) return;
        cleanup();
        entity.setHealth(0);
    }

    /**
     * Stops runtime tasks, removes attachments, destroys components,
     * and unregisters the entity from the framework.
     */
    public void cleanup() {
        Location currentLocation = entity != null ? entity.getLocation() : null;
        componentManager.onDeath(this, currentLocation);

        removeBossBar();
        killAllAttachments();
        componentManager.destroy(entity);
        phaseManager.cancelAllTasks();
        EntityManager.unregisterEntity(entity);
    }

    /**
     * Called when the entity takes damage.
     *
     * @param event damage event
     */
    public void onDamageTaken(EntityDamageEvent event) {
    }

    /**
     * Called when this entity deals damage to a target entity.
     *
     * @param event damage by entity event
     */
    public void onDamageDealt(EntityDamageByEntityEvent event) {
    }

    /**
     * Called when the entity dies.
     *
     * @param location death location
     */
    public void onDeath(Location location) {
    }

    /**
     * Defines repeating and delayed runtime behavior.
     */
    protected void onDefineBehavior() {
    }

    /**
     * Configures the entity's AI with NMS after spawn or attachment.
     *
     * @param spawned bound entity
     */
    protected void onConfigureAI(LivingEntity spawned) {
    }

    /**
     * Restores custom state after binding to an existing entity.
     *
     * @param existing bound entity
     */
    protected void onRehydrate(LivingEntity existing) {
    }

    /**
     * Registers a phase triggered at or below the given health ratio.
     *
     * @param healthPercentage threshold in the range {@code 0.0–1.0}
     * @param onTransition     action executed when the phase is triggered
     */
    public final void registerPhase(double healthPercentage, Runnable onTransition) {
        phaseManager.registerPhase(healthPercentage, onTransition);
    }

    /**
     * Called when a registered phase is triggered.
     *
     * @param healthThreshold triggered threshold in the range {@code 0.0–1.0}
     * @return transition duration in ticks, or {@code 0} for no delay
     */
    protected long onPhaseTransition(double healthThreshold) {
        return 0L;
    }

    /**
     * Evaluates registered phases against the current health.
     *
     * @param currentHealth current health value
     */
    public final void evaluatePhases(double currentHealth) {
        phaseManager.evaluatePhases(currentHealth);
    }

    /**
     * Registers a repeating task in the core lifecycle.
     *
     * @param delayTicks  initial delay in server ticks
     * @param periodTicks execution interval in server ticks
     * @param action      task action
     */
    @SuppressWarnings("SameParameterValue")
    protected final void addCoreAbility(long delayTicks, long periodTicks, Runnable action) {
        phaseManager.addCoreAbility(delayTicks, periodTicks, action);
    }

    /**
     * Registers a repeating task in the current phase lifecycle.
     *
     * @param delayTicks  initial delay in server ticks
     * @param periodTicks execution interval in server ticks
     * @param action      task action
     */
    protected final void addPhaseAbility(long delayTicks, long periodTicks, Runnable action) {
        phaseManager.addPhaseAbility(delayTicks, periodTicks, action);
    }

    /**
     * Registers a delayed task in the core lifecycle.
     *
     * @param delayTicks delay in server ticks
     * @param action     task action
     */
    protected final void addCoreDelayedAction(long delayTicks, Runnable action) {
        phaseManager.addCoreDelayedAction(delayTicks, action);
    }

    /**
     * Registers a delayed task in the current phase lifecycle.
     *
     * @param delayTicks delay in server ticks
     * @param action     task action
     */
    @SuppressWarnings("SameParameterValue")
    protected final void addPhaseDelayedAction(long delayTicks, Runnable action) {
        phaseManager.addPhaseDelayedAction(delayTicks, action);
    }

    /**
     * Deals damage to the entity.
     *
     * @param amount damage amount
     */
    public void damage(double amount) {
        if (!isAlive()) {
            return;
        }
        entity.damage(amount);
    }

    /**
     * Performs a melee attack against the target.
     *
     * @param target attack target
     */
    public void meleeAttack(Entity target) {
        if (!isAlive()) {
            return;
        }
        entity.attack(target);
    }

    /**
     * Sets the entity's AI target when supported by its Bukkit type.
     *
     * @param target new AI target
     */
    public void setAITarget(LivingEntity target) {
        if (entity instanceof Mob mob) {
            mob.setTarget(target);
        }
    }

    /**
     * Checks whether the entity has a direct line of sight to a target.
     *
     * @param target target entity
     * @return {@code true} if the target is visible
     */
    public boolean hasLineOfSight(Entity target) {
        if (!isAlive()) {
            return false;
        }
        return entity.hasLineOfSight(target);
    }

    /**
     * Returns the current Bukkit target of this entity if it is a {@link Mob}.
     *
     * @return current target living entity, or {@code null} if none or not a Mob
     */
    @Nullable
    public LivingEntity getTarget() {
        return entity instanceof Mob mob ? mob.getTarget() : null;
    }

    /**
     * Returns the current Bukkit target of this entity if it is a {@link Player}.
     *
     * @return current target player, or {@code null} if none or target is not a Player
     */
    @Nullable
    public Player getTargetPlayer() {
        return entity instanceof Mob mob && mob.getTarget() instanceof Player player ? player : null;
    }

    /**
     * Returns whether this mob currently has an active target.
     *
     * @return {@code true} if targeting an entity
     */
    public boolean hasTarget() {
        return getTarget() != null;
    }

    /**
     * Sets the target of this entity if it is a {@link Mob}.
     *
     * @param target new target entity, or {@code null} to clear
     */
    public void setTarget(@Nullable LivingEntity target) {
        if (entity instanceof Mob mob) {
            mob.setTarget(target);
        }
    }

    /**
     * Returns nearby survival-mode players around the entity.
     *
     * @param radius search radius
     * @return nearby players, or an empty list if unavailable
     */
    public List<Player> getNearbyPlayers(double radius) {
        return EntitySpatialQueries.getNearbyPlayers(entity != null ? entity.getLocation() : null, radius);
    }

    /**
     * Returns nearby survival-mode players around a location.
     *
     * @param center search center
     * @param radius search radius
     * @return nearby players, or an empty list if the center is invalid
     */
    public List<Player> getNearbyPlayers(Location center, double radius) {
        return EntitySpatialQueries.getNearbyPlayers(center, radius);
    }

    /**
     * Returns the nearest survival-mode player within the given radius.
     *
     * @param radius search radius
     * @return nearest player, or {@code null} if none exists
     */
    public Player getNearestPlayer(double radius) {
        return EntitySpatialQueries.getNearestPlayer(entity != null ? entity.getLocation() : null, radius);
    }

    /**
     * Returns the nearest survival-mode player around a location.
     *
     * @param center search center
     * @param radius search radius
     * @return nearest player, or {@code null} if none exists
     */
    public Player getNearestPlayer(Location center, double radius) {
        return EntitySpatialQueries.getNearestPlayer(center, radius);
    }

    /**
     * Returns nearby {@link BlightedPlayer} instances.
     *
     * @param radius search radius
     * @return nearby BlightedMC players
     */
    public List<BlightedPlayer> getNearbyBlightedPlayers(double radius) {
        return EntitySpatialQueries.getNearbyBlightedPlayers(entity != null ? entity.getLocation() : null, radius);
    }

    /**
     * Returns nearby {@link BlightedPlayer} instances around a location.
     *
     * @param center search center
     * @param radius search radius
     * @return nearby BlightedMC players
     */
    public List<BlightedPlayer> getNearbyBlightedPlayers(Location center, double radius) {
        return EntitySpatialQueries.getNearbyBlightedPlayers(center, radius);
    }

    /**
     * Returns the nearest {@link BlightedPlayer}.
     *
     * @param radius search radius
     * @return nearest BlightedMC player, or {@code null}
     */
    public BlightedPlayer getNearestBlightedPlayer(double radius) {
        return EntitySpatialQueries.getNearestBlightedPlayer(entity != null ? entity.getLocation() : null, radius);
    }

    /**
     * Returns the nearest {@link BlightedPlayer} around a location.
     *
     * @param center search center
     * @param radius search radius
     * @return nearest BlightedMC player, or {@code null}
     */
    public BlightedPlayer getNearestBlightedPlayer(Location center, double radius) {
        return EntitySpatialQueries.getNearestBlightedPlayer(center, radius);
    }

    /**
     * Attacks the nearest player unless an ability is currently active.
     *
     * @param radius search radius
     */
    public void meleeAttackNearestPlayer(double radius) {
        if (isPerformingAbility()) {
            return;
        }
        Player target = getNearestPlayer(radius);
        if (target != null) {
            meleeAttack(target);
        }
    }

    /**
     * Damages all nearby survival-mode players.
     *
     * @param center       damage center
     * @param radius       damage radius
     * @param damageAmount damage dealt
     */
    public void damageNearbyPlayers(Location center, double radius, double damageAmount) {
        EntitySpatialQueries.damageNearbyPlayers(center, radius, damageAmount, entity);
    }

    /**
     * Damages all nearby survival-mode players around the entity.
     *
     * @param radius       damage radius
     * @param damageAmount damage dealt
     */
    public void damageNearbyPlayers(double radius, double damageAmount) {
        EntitySpatialQueries.damageNearbyPlayers(entity != null ? entity.getLocation() : null, radius, damageAmount, entity);
    }

    /**
     * Damages and knocks back all nearby survival-mode players.
     *
     * @param center            effect center
     * @param radius            effect radius
     * @param damageAmount      damage dealt
     * @param knockbackStrength horizontal knockback strength
     * @param verticalKnockback vertical knockback velocity
     */
    public void damageAndKnockbackNearbyPlayers(
            Location center,
            double radius,
            double damageAmount,
            double knockbackStrength,
            double verticalKnockback
    ) {
        EntitySpatialQueries.damageAndKnockbackNearbyPlayers(
                center, radius, damageAmount, knockbackStrength, verticalKnockback, entity);
    }

    /**
     * Disables a target player's shield if they are currently blocking.
     *
     * @param target        target player
     * @param cooldownTicks duration in ticks to put the shield on cooldown
     */
    public void disableShieldIfBlocking(Player target, int cooldownTicks) {
        EntitySpatialQueries.disableShieldIfBlocking(target, cooldownTicks);
    }

    /**
     * Rotates the entity to face a target location on the horizontal plane.
     *
     * @param target target location
     */
    public void faceLocation(Location target) {
        EntitySpatialQueries.faceLocation(entity, target);
    }

    /**
     * Configures this entity's loot table using a fluent builder consumer.
     *
     * @param consumer action configuring the entity loot table builder
     */
    public void loot(@NonNull Consumer<EntityLootTableBuilder> consumer) {
        EntityLootTableBuilder builder = new EntityLootTableBuilder();
        consumer.accept(builder);
        this.lootTable = builder.build();
    }

    /**
     * Configures this entity's equipment using a fluent builder consumer.
     *
     * @param consumer action configuring the entity equipment builder
     */
    public void equipment(@NonNull Consumer<EntityEquipmentBuilder> consumer) {
        EntityEquipmentBuilder builder = new EntityEquipmentBuilder();
        consumer.accept(builder);
        equipmentHolder.applyBuilder(builder, entity);
    }

    /**
     * Marks this entity as a boss and customizes its Bedrock {@link BossbarSection.Builder}.
     *
     * @param configurator customizer consumer for the bossbar section builder
     */
    public void boss(@NonNull Consumer<BossbarSection.Builder> configurator) {
        this.isBoss = true;
        this.bossBarConfigurator = Objects.requireNonNull(configurator, "configurator cannot be null");
    }

    /**
     * Marks this entity as a boss with default boss bar appearance.
     */
    public void boss() {
        this.isBoss = true;
    }

    /**
     * Marks this entity as a boss with specified color and style.
     *
     * @param color boss bar color
     * @param style boss bar style
     */
    public void boss(@NonNull BarColor color, @NonNull BarStyle style) {
        this.isBoss = true;
        this.bossBarColor = Objects.requireNonNull(color, "color cannot be null");
        this.bossBarStyle = Objects.requireNonNull(style, "style cannot be null");
    }

    /**
     * Marks this entity as a boss with specified color, style, and viewer radius.
     *
     * @param color  boss bar color
     * @param style  boss bar style
     * @param radius player detection radius in blocks
     */
    public void boss(@NonNull BarColor color, @NonNull BarStyle style, double radius) {
        boss(color, style);
        this.bossBarRadius = Math.max(1.0, radius);
    }

    /**
     * Configures this entity's defenses (immunities and resistances) using a fluent consumer.
     *
     * @param consumer action configuring the defense builder
     */
    public void defenses(@NonNull Consumer<DefensesBuilder> consumer) {
        consumer.accept(new DefensesBuilder(this));
    }

    /**
     * Configures this entity's health-based phase transitions using a fluent consumer.
     *
     * @param consumer action configuring the phase builder
     */
    public void phases(@NonNull Consumer<EntityPhasesBuilder> consumer) {
        consumer.accept(new EntityPhasesBuilder(this));
    }

    /**
     * Configures mob attributes using a fluent consumer builder with typed convenience methods.
     *
     * @param consumer action configuring the attributes builder
     */
    public void attributes(@NonNull Consumer<EntityAttributesBuilder> consumer) {
        EntityAttributesBuilder builder = new EntityAttributesBuilder();
        consumer.accept(builder);
        builder.applyTo(this.attributes, this.entity);
        Double configuredMaxHealth = builder.getAttributes().get(Attribute.MAX_HEALTH);
        if (configuredMaxHealth != null) {
            this.maxHealth = (int) Math.round(configuredMaxHealth);
        }
        Double configuredDamage = builder.getAttributes().get(Attribute.ATTACK_DAMAGE);
        if (configuredDamage != null) {
            this.damage = (int) Math.round(configuredDamage);
        }
        Double configuredDefense = builder.getAttributes().get(Attribute.ARMOR);
        if (configuredDefense != null) {
            this.defense = (int) Math.round(configuredDefense);
        }
    }

    /**
     * Plays a sound effect at the entity's current location.
     *
     * @param sound  sound to play
     * @param volume volume level
     * @param pitch  pitch modifier
     */
    public void playSound(@NonNull Sound sound, float volume, float pitch) {
        EntityEffects.playSound(entity, sound, volume, pitch);
    }

    /**
     * Plays a sound effect at the entity's current location with default volume and pitch (1.0f).
     *
     * @param sound sound to play
     */
    public void playSound(@NonNull Sound sound) {
        EntityEffects.playSound(entity, sound, 1.0f, 1.0f);
    }

    /**
     * Plays a sound effect at a specific world location.
     *
     * @param location location to play the sound at
     * @param sound    sound to play
     * @param volume   volume level
     * @param pitch    pitch modifier
     */
    public void playSound(@NonNull Location location, @NonNull Sound sound, float volume, float pitch) {
        EntityEffects.playSound(location, sound, volume, pitch);
    }

    /**
     * Spawns particles at the entity's current location.
     *
     * @param particle particle type
     * @param count    number of particles
     */
    public void spawnParticle(@NonNull Particle particle, int count) {
        EntityEffects.spawnParticle(entity, particle, count);
    }

    /**
     * Spawns particles at the entity's current location with random positional offsets.
     *
     * @param particle particle type
     * @param count    number of particles
     * @param offsetX  maximum X axis offset
     * @param offsetY  maximum Y axis offset
     * @param offsetZ  maximum Z axis offset
     */
    public void spawnParticle(@NonNull Particle particle, int count, double offsetX, double offsetY, double offsetZ) {
        EntityEffects.spawnParticle(entity, particle, count, offsetX, offsetY, offsetZ);
    }

    /**
     * Spawns particles at the entity's current location with custom particle data (e.g. DustOptions).
     *
     * @param particle particle type
     * @param count    number of particles
     * @param data     particle data object, or null
     * @param <T>      particle data type
     */
    public <T> void spawnParticle(@NonNull Particle particle, int count, @Nullable T data) {
        EntityEffects.spawnParticle(entity, particle, count, data);
    }

    /**
     * Spawns particles at a specific world location.
     *
     * @param location location to spawn particles at
     * @param particle particle type
     * @param count    number of particles
     */
    public void spawnParticle(@NonNull Location location, @NonNull Particle particle, int count) {
        EntityEffects.spawnParticle(location, particle, count);
    }

    /**
     * Spawns particles at a specific world location with custom particle data (e.g. DustOptions).
     *
     * @param location location to spawn particles at
     * @param particle particle type
     * @param count    number of particles
     * @param data     particle data object, or null
     * @param <T>      particle data type
     */
    public <T> void spawnParticle(@NonNull Location location, @NonNull Particle particle, int count, @Nullable T data) {
        EntityEffects.spawnParticle(location, particle, count, data);
    }

    /**
     * Triggers the entity's main-hand swinging animation.
     */
    public void swingMainHand() {
        EntityEffects.swingMainHand(entity);
    }

    /**
     * Triggers the entity's off-hand swinging animation.
     */
    public void swingOffHand() {
        EntityEffects.swingOffHand(entity);
    }

    /**
     * Checks whether the cooldown for the specified ability key has expired.
     *
     * @param abilityKey     unique identifier for the ability
     * @param cooldownMillis cooldown duration in milliseconds
     * @return {@code true} if the cooldown has elapsed and the ability can run
     */
    public boolean isCooldownReady(@NonNull String abilityKey, long cooldownMillis) {
        return cooldowns.isReady(abilityKey, cooldownMillis);
    }

    /**
     * Sets or resets the cooldown timestamp for the specified ability to current system time.
     *
     * @param abilityKey unique identifier for the ability
     */
    public void triggerCooldown(@NonNull String abilityKey) {
        cooldowns.trigger(abilityKey);
    }

    /**
     * Checks if the ability cooldown has expired, and if so, immediately records
     * the current timestamp and returns {@code true}.
     *
     * @param abilityKey     unique identifier for the ability
     * @param cooldownMillis cooldown duration in milliseconds
     * @return {@code true} if the cooldown was ready and was triggered
     */
    public boolean checkAndTriggerCooldown(@NonNull String abilityKey, long cooldownMillis) {
        return cooldowns.checkAndTrigger(abilityKey, cooldownMillis);
    }

    /**
     * Resets any recorded cooldown timestamp for the specified ability key.
     *
     * @param abilityKey unique identifier for the ability
     */
    public void resetCooldown(@NonNull String abilityKey) {
        cooldowns.reset(abilityKey);
    }

    /**
     * Equips or unequips the configured main-hand item.
     *
     * @param equipped whether the item should be equipped
     */
    public void setMainHandEquipped(boolean equipped) {
        if (!isAlive() || entity.getEquipment() == null) {
            return;
        }
        entity.getEquipment().setItemInMainHand(equipped ? equipmentHolder.getItemInMainHand() : null);
    }

    /**
     * Equips or unequips the configured off-hand item.
     *
     * @param equipped whether the item should be equipped
     */
    public void setOffHandEquipped(boolean equipped) {
        if (!isAlive() || entity.getEquipment() == null) {
            return;
        }
        entity.getEquipment().setItemInOffHand(equipped ? equipmentHolder.getItemInOffHand() : null);
    }

    /**
     * Sets the maximum health for this entity.
     * If the entity is currently active in the world, dynamically updates
     * the live {@link Attribute#MAX_HEALTH} and clamps current health.
     *
     * @param maxHealth maximum health value
     */
    public void setMaxHealth(double maxHealth) {
        this.maxHealth = (int) Math.round(maxHealth);
        setAttribute(Attribute.MAX_HEALTH, maxHealth);
        if (isAlive() && entity != null) {
            entity.setHealth(Math.min(entity.getHealth(), maxHealth));
        }
    }

    /**
     * Sets the live entity's current health, clamped between 0 and maximum health.
     *
     * @param health target health value
     */
    public void setHealth(double health) {
        attributes.setHealth(entity, health, this.maxHealth);
    }

    /**
     * Returns the live entity's current health, or {@code 0.0} if unspawned or dead.
     *
     * @return current health
     */
    public double getHealth() {
        return attributes.getHealth(entity);
    }

    /**
     * Sets the base attack damage value. Updates the live entity if active.
     *
     * @param damage attack damage
     */
    public void setDamage(int damage) {
        this.damage = damage;
        setAttribute(Attribute.ATTACK_DAMAGE, damage);
    }

    /**
     * Sets the base defense/armor value. Updates the live entity if active.
     *
     * @param defense defense value
     */
    public void setDefense(int defense) {
        this.defense = defense;
        if (defense > 0 || isAlive()) {
            setAttribute(Attribute.ARMOR, defense);
        }
    }

    /**
     * Sets the entity's helmet. Updates live equipment if the entity is active.
     *
     * @param helmet helmet item or null to unequip
     */
    public void setHelmet(@Nullable ItemStack helmet) {
        equipmentHolder.setHelmet(helmet, entity);
    }

    /**
     * Sets the entity's chestplate. Updates live equipment if the entity is active.
     *
     * @param chestplate chestplate item or null to unequip
     */
    public void setChestplate(@Nullable ItemStack chestplate) {
        equipmentHolder.setChestplate(chestplate, entity);
    }

    /**
     * Sets the entity's leggings. Updates live equipment if the entity is active.
     *
     * @param leggings leggings item or null to unequip
     */
    public void setLeggings(@Nullable ItemStack leggings) {
        equipmentHolder.setLeggings(leggings, entity);
    }

    /**
     * Sets the entity's boots. Updates live equipment if the entity is active.
     *
     * @param boots boots item or null to unequip
     */
    public void setBoots(@Nullable ItemStack boots) {
        equipmentHolder.setBoots(boots, entity);
    }

    /**
     * Sets all four armor slots in natural top-to-bottom order (helmet to boots).
     * Any slot may be null to leave it unequipped.
     *
     * @param helmet     helmet item or null
     * @param chestplate chestplate item or null
     * @param leggings   leggings item or null
     * @param boots      boots item or null
     */
    public void setArmor(
            @Nullable ItemStack helmet,
            @Nullable ItemStack chestplate,
            @Nullable ItemStack leggings,
            @Nullable ItemStack boots
    ) {
        equipmentHolder.setArmor(helmet, chestplate, leggings, boots, entity);
    }

    /**
     * Clears all armor slots.
     */
    public void clearArmor() {
        equipmentHolder.clearArmor(entity);
    }

    /**
     * Clears all equipment slots (armor, main-hand, and off-hand).
     */
    public void clearEquipment() {
        equipmentHolder.clearEquipment(entity);
    }

    /**
     * Sets the main-hand item. Updates live equipment if the entity is active.
     *
     * @param item main-hand item or null to unequip
     */
    public void setItemInMainHand(@Nullable ItemStack item) {
        equipmentHolder.setMainHand(item, entity);
    }

    /**
     * Sets the off-hand item. Updates live equipment if the entity is active.
     *
     * @param item off-hand item or null to unequip
     */
    public void setItemInOffHand(@Nullable ItemStack item) {
        equipmentHolder.setOffHand(item, entity);
    }

    /**
     * Returns the helmet item from the live entity if active, or from the configured template.
     *
     * @return current helmet item, or null
     */
    @Nullable
    public ItemStack getHelmet() {
        return equipmentHolder.getHelmet(entity);
    }

    /**
     * Returns the chestplate item from the live entity if active, or from the configured template.
     *
     * @return current chestplate item, or null
     */
    @Nullable
    public ItemStack getChestplate() {
        return equipmentHolder.getChestplate(entity);
    }

    /**
     * Returns the leggings item from the live entity if active, or from the configured template.
     *
     * @return current leggings item, or null
     */
    @Nullable
    public ItemStack getLeggings() {
        return equipmentHolder.getLeggings(entity);
    }

    /**
     * Returns the boots item from the live entity if active, or from the configured template.
     *
     * @return current boots item, or null
     */
    @Nullable
    public ItemStack getBoots() {
        return equipmentHolder.getBoots(entity);
    }

    /**
     * Returns the main hand item from the live entity if active, or from the configured template.
     *
     * @return current main hand item, or null
     */
    @Nullable
    public ItemStack getItemInMainHand() {
        return equipmentHolder.getMainHand(entity);
    }

    /**
     * Returns the off hand item from the live entity if active, or from the configured template.
     *
     * @return current off hand item, or null
     */
    @Nullable
    public ItemStack getItemInOffHand() {
        return equipmentHolder.getOffHand(entity);
    }

    /**
     * Attaches a non-interactive {@link ItemDisplay} entity with local 3D translation offset.
     *
     * @param offset       local offset relative to base entity origin and facing yaw
     * @param configurator optional configuration consumer
     * @return the created ItemDisplay attachment
     */
    public ItemDisplay attachItemDisplay(Vector offset, Consumer<ItemDisplay> configurator) {
        return attachmentManager.attachItemDisplay(offset, configurator);
    }

    /**
     * Attaches a non-interactive {@link BlockDisplay} entity with local 3D translation offset.
     *
     * @param offset       local offset relative to base entity origin and facing yaw
     * @param configurator optional configuration consumer
     * @return the created BlockDisplay attachment
     */
    public BlockDisplay attachBlockDisplay(Vector offset, Consumer<BlockDisplay> configurator) {
        return attachmentManager.attachBlockDisplay(offset, configurator);
    }

    /**
     * Attaches a multipart hittable {@link Interaction} hitbox entity.
     *
     * @param offset       local offset relative to base entity origin and facing yaw
     * @param width        hitbox width
     * @param height       hitbox height
     * @param configurator optional configuration consumer
     * @return the created Interaction attachment
     */
    public Interaction attachHitbox(Vector offset, float width, float height, Consumer<Interaction> configurator) {
        return attachmentManager.attachHitbox(offset, width, height, configurator);
    }

    /**
     * Attaches an entity using {@link AttachmentRole#SUBORDINATE}.
     *
     * @param attachmentEntity entity to attach
     */
    public void addAttachment(Entity attachmentEntity) {
        attachmentManager.addAttachment(attachmentEntity);
    }

    /**
     * Attaches an entity with the given role.
     *
     * @param attachmentEntity entity to attach
     * @param role             attachment role
     */
    public void addAttachment(Entity attachmentEntity, AttachmentRole role) {
        attachmentManager.addAttachment(attachmentEntity, role);
    }

    /**
     * Attaches an entity with the given role and local offset.
     *
     * @param attachmentEntity entity to attach
     * @param role             attachment role
     * @param offset           local 3D offset
     */
    public void addAttachment(Entity attachmentEntity, AttachmentRole role, Vector offset) {
        attachmentManager.addAttachment(attachmentEntity, role, offset);
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
    public void addAttachment(Entity attachmentEntity, AttachmentRole role, Vector offset, boolean syncYaw, boolean syncPitch) {
        attachmentManager.addAttachment(attachmentEntity, role, offset, syncYaw, syncPitch);
    }

    /**
     * Synchronizes all registered attachments to their relative world position based on base location and facing yaw.
     */
    public void syncAttachments() {
        attachmentManager.syncAttachments();
    }

    /**
     * Removes and destroys all attached entities.
     */
    public void killAllAttachments() {
        attachmentManager.killAllAttachments();
    }

    /**
     * Removes and destroys attached entities matching the specified role.
     *
     * @param role attachment role to remove
     */
    public void killAttachments(AttachmentRole role) {
        attachmentManager.killAttachments(role);
    }

    /**
     * Checks whether a living body attachment is currently present.
     *
     * @return {@code true} if a living body attachment exists
     */
    public boolean hasLivingBodyAttachment() {
        return attachmentManager.hasLivingBodyAttachment();
    }

    /**
     * Checks whether an active subordinate companion attachment is currently present.
     *
     * @return {@code true} if a non-dead subordinate attachment exists
     */
    public boolean hasSubordinateAttachments() {
        return attachmentManager.hasSubordinateAttachments();
    }

    /**
     * Prevents multi-hit exploits (e.g. Sweeping Edge or AoE hitting multiple hitboxes in the same tick).
     *
     * @param damager attacker entity or projectile shooter
     * @return {@code true} if damage was already processed this tick by the same attacker
     */
    public boolean shouldBlockSameTickDamage(Entity damager) {
        return attachmentManager.shouldBlockSameTickDamage(damager);
    }

    /**
     * Registers a component with this entity.
     *
     * <p>Components added after initialization are initialized immediately.</p>
     *
     * @param component component to register
     */
    public void addComponent(EntityComponent component) {
        componentManager.addComponent(component, entity);
    }

    /**
     * Returns an unmodifiable view of active entity attachments.
     *
     * @return active entity attachments
     */
    public Set<EntityAttachment> getAttachments() {
        return attachmentManager.getAttachments();
    }

    /**
     * Returns a registered component by identifier.
     *
     * @param id  component identifier
     * @param <T> expected component type
     * @return component, or {@code null} if not registered
     */
    public <T extends EntityComponent> T getComponent(String id) {
        return componentManager.getComponent(id);
    }

    /**
     * Returns the first registered component matching the specified class type.
     *
     * @param componentClass expected component class type
     * @param <T>            expected component type
     * @return matching component instance, or {@code null} if not registered
     */
    public <T extends EntityComponent> T getComponent(Class<T> componentClass) {
        return componentManager.getComponent(componentClass);
    }

    /**
     * Returns a snapshot of all registered components.
     *
     * @return registered components
     */
    public Collection<EntityComponent> getComponents() {
        return componentManager.getComponents();
    }

    /**
     * Updates the boss bar for this entity if active.
     */
    public void updateBossBar() {
        if (!isAlive()) {
            removeBossBar();
        }
    }

    /**
     * Sets the boss bar color and style.
     *
     * @param color bar color
     * @param style bar style
     */
    public void setBossBarAppearance(@NonNull BarColor color, @NonNull BarStyle style) {
        this.bossBarColor = Objects.requireNonNull(color, "color cannot be null");
        this.bossBarStyle = Objects.requireNonNull(style, "style cannot be null");
        if (bossBarSectionId != null) {
            spawnBossBar();
        }
    }

    /**
     * Registers the boss bar for this entity using Bedrock's Bossbar API.
     */
    public void spawnBossBar() {
        if (!isBoss || entity == null || !entity.isValid()) {
            return;
        }

        EntityType type = entityType;
        if (type == EntityType.WITHER || type == EntityType.ENDER_DRAGON) {
            return;
        }

        removeBossBar();

        this.bossBarSectionId = "boss-" + entity.getUniqueId();

        BossbarSection.Builder builder = BossbarSection.builder(bossBarSectionId)
                .title(_ -> "§f§l" + name)
                .progress(this::getHealthRatio)
                .color(bossBarColor)
                .style(bossBarStyle)
                .visibleWhen(Bossbar.within(entity, bossBarRadius))
                .expireWhen(() -> !isAlive());

        if (!bossBarFlags.isEmpty()) {
            builder.flags(bossBarFlags.toArray(BarFlag[]::new));
        }

        if (bossBarConfigurator != null) {
            bossBarConfigurator.accept(builder);
        }

        Bossbar.register(BlightedSMP.getInstance(), builder.build());
    }

    /**
     * Calculates the current health ratio of the entity in {@code [0.0, 1.0]}.
     *
     * @return current health divided by max health
     */
    public double getHealthRatio() {
        if (entity == null || !entity.isValid() || entity.isDead()) {
            return 0.0;
        }
        AttributeInstance maxHealthAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
        double maxHealthVal = (maxHealthAttribute != null && maxHealthAttribute.getValue() > 0)
                ? maxHealthAttribute.getValue()
                : Math.max(1.0, this.maxHealth);
        return Math.clamp(entity.getHealth() / maxHealthVal, 0.0, 1.0);
    }

    /**
     * Removes the boss bar associated with this entity.
     */
    public void removeBossBar() {
        if (bossBarSectionId != null) {
            Bossbar.unregister(bossBarSectionId);
            bossBarSectionId = null;
        }
    }

    /**
     * Executes the configured loot table at the given location.
     *
     * @param location drop location and loot context
     * @param player   associated player
     */
    public void dropLoot(Location location, BlightedPlayer player) {
        if (lootTable == null) {
            return;
        }

        World world = Objects.requireNonNull(location.getWorld());
        Biome biome = world.getBiome(location);
        LootContext context = new LootContext(player, world, biome, location, ThreadLocalRandom.current(), null);
        lootTable.execute(context);
    }

    /**
     * Sets an attribute base value. If the entity is currently active,
     * updates the live Bukkit AttributeInstance immediately.
     *
     * @param attribute target attribute
     * @param value     base attribute value
     */
    public void setAttribute(Attribute attribute, double value) {
        attributes.setAttribute(attribute, value, entity);
    }

    /**
     * Returns the effective base value for an attribute, querying the live
     * entity if active, or the configured attribute map if unspawned.
     *
     * @param attribute target attribute
     * @return effective base value, or 0.0 if not configured
     */
    public double getAttributeValue(Attribute attribute) {
        return attributes.getAttributeValue(attribute, entity);
    }

    /**
     * Returns the first immunity rule matching a damage event.
     *
     * @param target entity receiving the event
     * @param event  damage event
     * @return matching immunity, or {@code null} if none applies
     */
    public EntityImmunity getTriggeredImmunity(LivingEntity target, EntityDamageEvent event) {
        return defenses.getTriggeredImmunity(target, event);
    }

    /**
     * Returns the highest resistance percentage matching a damage event.
     *
     * @param target entity receiving the event
     * @param event  damage event
     * @return resistance percentage (0.0 if no resistance applies)
     */
    public double getResistancePercent(LivingEntity target, EntityDamageEvent event) {
        return defenses.getResistancePercent(target, event);
    }

    /**
     * Initializes runtime systems (behavior, tasks, phases). Executed once per instance.
     */
    private void initRuntime() {
        if (runtimeInitialized) {
            return;
        }
        onDefineBehavior();

        addCoreAbility(1L, 1L, this::syncAttachments);
        addCoreAbility(5L, 5L, () -> componentManager.tick(this));

        runtimeInitialized = true;
        phaseManager.scheduleAllCoreTasks();
        phaseManager.evaluatePhases(maxHealth);
    }

    private void configureEquipment() {
        equipmentHolder.applyTo(entity);
    }

    /**
     * Adds an immunity rule to this entity.
     *
     * @param immunity immunity rule
     */
    public void addImmunity(EntityImmunity immunity) {
        defenses.addImmunity(immunity);
    }

    /**
     * Adds a damage resistance percentage rule to this entity.
     *
     * @param type    damage type
     * @param percent percentage resisted (0 - 100)
     */
    public void addResistance(DamageType type, double percent) {
        defenses.addResistance(type, percent);
    }

    /**
     * Returns whether the bound entity is currently valid and alive.
     *
     * @return {@code true} if the entity can still be used
     */
    public boolean isAlive() {
        return entity != null && entity.isValid() && !entity.isDead();
    }

    /**
     * Creates a pristine, unspawned instance of this entity ready for world spawning.
     * Instantiates via {@link fr.moussax.blightedSMP.engine.entities.registry.EntitiesRegistry},
     * or invokes the zero-argument constructor directly.
     *
     * @return fresh unspawned entity instance
     */
    @NonNull
    public BlightedEntity createInstance() {
        BlightedEntity fresh = EntitiesRegistry.create(getEntityId());
        if (fresh != null) {
            return fresh;
        }
        try {
            var constructor = getClass().getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to instantiate entity of type " + getClass().getName()
                    + ". Ensure a public zero-argument constructor is present or register a factory.", exception);
        }
    }
}
