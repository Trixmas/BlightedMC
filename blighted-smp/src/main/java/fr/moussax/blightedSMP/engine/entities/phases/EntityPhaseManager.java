package fr.moussax.blightedSMP.engine.entities.phases;

import fr.moussax.bedrock.utils.debug.Log;
import fr.moussax.blightedSMP.engine.entities.BlightedEntity;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.DoubleToLongFunction;

/**
 * Manages health-based phase transitions, core abilities, and phase-bound lifecycle tasks for an entity.
 */
public final class EntityPhaseManager {

    private final BlightedEntity owner;
    private final DoubleToLongFunction transitionHandler;
    private final NavigableMap<Double, Runnable> phaseThresholds;
    private LifecycleTaskManager coreTasks = new LifecycleTaskManager();
    private LifecycleTaskManager phaseTasks = new LifecycleTaskManager();
    private boolean isPhaseCallbackRunning;

    public EntityPhaseManager(@NonNull BlightedEntity owner, @NonNull DoubleToLongFunction transitionHandler) {
        this(owner, transitionHandler, new TreeMap<>(Collections.reverseOrder()));
    }

    private EntityPhaseManager(
            @NonNull BlightedEntity owner,
            @NonNull DoubleToLongFunction transitionHandler,
            @NonNull NavigableMap<Double, Runnable> thresholds
    ) {
        this.owner = Objects.requireNonNull(owner, "owner cannot be null");
        this.transitionHandler = Objects.requireNonNull(transitionHandler, "transitionHandler cannot be null");
        this.phaseThresholds = new TreeMap<>(thresholds);
    }

    /**
     * Registers a phase triggered at or below the given health percentage.
     *
     * @param healthPercentage threshold in the range {@code 0.0–1.0}
     * @param onTransition     action executed when the phase is triggered
     */
    public void registerPhase(double healthPercentage, @NonNull Runnable onTransition) {
        phaseThresholds.put(healthPercentage, Objects.requireNonNull(onTransition, "onTransition cannot be null"));
    }

    /**
     * Evaluates registered phases against the entity's current health.
     *
     * @param currentHealth current health value
     */
    public void evaluatePhases(double currentHealth) {
        if (phaseThresholds.isEmpty()) {
            return;
        }

        double maxHealth = Math.max(1, owner.getMaxHealth());
        double healthPercentage = currentHealth / maxHealth;

        while (!phaseThresholds.isEmpty() && healthPercentage <= phaseThresholds.firstKey()) {
            Map.Entry<Double, Runnable> entry = phaseThresholds.pollFirstEntry();
            if (entry == null) {
                break;
            }

            phaseTasks.cancelAll();
            phaseTasks = new LifecycleTaskManager();
            isPhaseCallbackRunning = true;
            try {
                entry.getValue().run();
            } finally {
                isPhaseCallbackRunning = false;
            }
            long transitionDuration = transitionHandler.applyAsLong(entry.getKey());

            if (transitionDuration > 0) {
                owner.setPerformingAbility(true);
                addCoreDelayedAction(transitionDuration, () -> {
                    owner.setPerformingAbility(false);
                    phaseTasks.scheduleAll();
                });
            } else {
                phaseTasks.scheduleAll();
            }
        }
    }

    /**
     * Registers a repeating task in the core lifecycle.
     *
     * @param delayTicks  initial delay in server ticks
     * @param periodTicks execution interval in server ticks
     * @param action      task action
     */
    public void addCoreAbility(long delayTicks, long periodTicks, @NonNull Runnable action) {
        scheduleAbility(coreTasks, delayTicks, periodTicks, action);
    }

    /**
     * Registers a repeating task in the current phase lifecycle.
     *
     * @param delayTicks  initial delay in server ticks
     * @param periodTicks execution interval in server ticks
     * @param action      task action
     */
    public void addPhaseAbility(long delayTicks, long periodTicks, @NonNull Runnable action) {
        scheduleAbility(phaseTasks, delayTicks, periodTicks, action);
    }

    /**
     * Registers a delayed task in the core lifecycle.
     *
     * @param delayTicks delay in server ticks
     * @param action     task action
     */
    public void addCoreDelayedAction(long delayTicks, @NonNull Runnable action) {
        scheduleDelayedAction(coreTasks, delayTicks, action);
    }

    /**
     * Registers a delayed task in the current phase lifecycle.
     *
     * @param delayTicks delay in server ticks
     * @param action     task action
     */
    public void addPhaseDelayedAction(long delayTicks, @NonNull Runnable action) {
        scheduleDelayedAction(phaseTasks, delayTicks, action);
    }

    /**
     * Schedules all core and phase tasks with the server scheduler.
     */
    public void scheduleAllTasks() {
        coreTasks.scheduleAll();
        phaseTasks.scheduleAll();
    }

    /**
     * Schedules all core tasks with the server scheduler.
     */
    public void scheduleAllCoreTasks() {
        coreTasks.scheduleAll();
    }

    /**
     * Cancels all scheduled core and phase tasks without wiping registered task definitions.
     */
    public void cancelAllTasks() {
        coreTasks.cancelAll();
        phaseTasks.cancelAll();
    }

    /**
     * Cancels all scheduled tasks and permanently clears all task definitions.
     */
    public void clearAllTasks() {
        coreTasks.clearAll();
        phaseTasks.clearAll();
    }

    private void scheduleAbility(LifecycleTaskManager manager, long delayTicks, long periodTicks, Runnable action) {
        manager.addRepeatingTask(() -> {
            if (!owner.isAlive()) {
                return;
            }
            try {
                action.run();
            } catch (Exception exception) {
                Log.warn("EntityPhaseManager", "Ability threw an exception on entity '" + owner.getName() + "': " + exception.getMessage());
            }
        }, delayTicks, periodTicks);
        if (canScheduleTask() && (!isPhaseCallbackRunning || manager != phaseTasks)) {
            manager.scheduleLast();
        }
    }

    private void scheduleDelayedAction(LifecycleTaskManager manager, long delayTicks, Runnable action) {
        manager.addDelayedTask(() -> {
            if (!owner.isAlive()) {
                return;
            }
            try {
                action.run();
            } catch (Exception exception) {
                Log.warn("EntityPhaseManager", "Delayed action threw an exception on entity '" + owner.getName() + "': " + exception.getMessage());
            }
        }, delayTicks);
        if (canScheduleTask() && (!isPhaseCallbackRunning || manager != phaseTasks)) {
            manager.scheduleLast();
        }
    }

    private boolean canScheduleTask() {
        return owner.getEntity() != null && !owner.getEntity().isDead() && owner.isRuntimeInitialized();
    }

    /**
     * Creates a detached copy of this phase manager bound to a new owner and transition handler.
     *
     * @param newOwner   target entity instance
     * @param newHandler transition callback
     * @return detached phase manager
     */
    @NonNull
    public EntityPhaseManager copy(@NonNull BlightedEntity newOwner, @NonNull DoubleToLongFunction newHandler) {
        return new EntityPhaseManager(newOwner, newHandler, this.phaseThresholds);
    }
}
