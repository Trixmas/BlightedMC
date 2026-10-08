package fr.moussax.blightedSMP.engine.entities.phases;

import fr.moussax.blightedSMP.BlightedSMP;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manages scheduler tasks bound to the lifecycle of a {@link fr.moussax.blightedSMP.engine.entities.BlightedEntity}.
 * <p>
 * Allows entities to register delayed or repeating tasks that are automatically
 * scheduled on initialization and canceled on destruction.
 */
public final class LifecycleTaskManager {

    private List<ScheduledTask> tasks;

    /**
     * Adds a repeating task using a {@link Runnable} action.
     *
     * @param action      task action
     * @param delayTicks  initial delay in ticks
     * @param periodTicks execution interval in ticks
     */
    public void addRepeatingTask(Runnable action, long delayTicks, long periodTicks) {
        ensureList();
        tasks.add(new ScheduledTask(Objects.requireNonNull(action, "action cannot be null"), delayTicks, periodTicks, true));
    }

    /**
     * Adds a delayed task using a {@link Runnable} action.
     *
     * @param action     task action
     * @param delayTicks delay in ticks
     */
    public void addDelayedTask(Runnable action, long delayTicks) {
        ensureList();
        tasks.add(new ScheduledTask(Objects.requireNonNull(action, "action cannot be null"), delayTicks, 0L, false));
    }

    /**
     * Schedules all registered tasks.
     */
    public void scheduleAll() {
        if (tasks == null) return;
        for (ScheduledTask task : new ArrayList<>(tasks)) {
            task.schedule(this);
        }
    }

    /**
     * Schedules only the most recently added task.
     */
    public void scheduleLast() {
        if (tasks == null || tasks.isEmpty()) return;
        tasks.getLast().schedule(this);
    }

    /**
     * Cancels all currently running tasks associated with this manager without clearing task definitions.
     */
    public void cancelAll() {
        if (tasks == null) return;
        for (ScheduledTask task : new ArrayList<>(tasks)) {
            task.cancel();
        }
    }

    /**
     * Cancels all currently running tasks and clears all registered task definitions permanently.
     */
    public void clearAll() {
        if (tasks == null) return;
        for (ScheduledTask task : new ArrayList<>(tasks)) {
            task.cancel();
        }
        tasks.clear();
        tasks = null;
    }

    private void ensureList() {
        if (tasks == null) tasks = new ArrayList<>(4);
    }

    private void onTaskComplete(ScheduledTask task) {
        if (tasks == null) return;
        tasks.remove(task);
    }

    private static final class ScheduledTask {
        private final Runnable action;
        private final long delayTicks;
        private final long periodTicks;
        private final boolean repeating;
        private BukkitTask currentTask;

        private ScheduledTask(Runnable action, long delayTicks, long periodTicks, boolean repeating) {
            this.action = action;
            this.delayTicks = delayTicks;
            this.periodTicks = periodTicks;
            this.repeating = repeating;
        }

        private void schedule(LifecycleTaskManager manager) {
            cancel();

            var plugin = BlightedSMP.getInstance();

            if (repeating) {
                currentTask = Bukkit.getScheduler().runTaskTimer(plugin, action, delayTicks, periodTicks);
                return;
            }

            currentTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    action.run();
                } finally {
                    manager.onTaskComplete(ScheduledTask.this);
                }
            }, delayTicks);
        }

        private void cancel() {
            if (currentTask != null) {
                try {
                    currentTask.cancel();
                } catch (Exception _) {
                }
                currentTask = null;
            }
        }
    }
}

