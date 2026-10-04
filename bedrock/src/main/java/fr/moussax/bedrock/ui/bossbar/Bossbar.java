package fr.moussax.bedrock.ui.bossbar;

import fr.moussax.bedrock.scheduling.PluginContext;
import fr.moussax.bedrock.sound.SoundCue;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Static entry point for displaying and managing boss bars.
 *
 * <p>Supports timed alerts that drain over their lifespan, automated countdowns, and persistent
 * {@linkplain BossbarSection sections} (global or per player) with visibility predicates. Bars stack:
 * each alert and each visible section is its own bar.</p>
 *
 * <p>If {@link #initialize(Plugin, long)} was never called, the first call initializes the service
 * from {@link PluginContext}, which must then be bound.</p>
 */
public final class Bossbar {

    /**
     * Default duration for alerts sent without an explicit one (3 seconds).
     */
    public static final Duration DEFAULT_DURATION = Duration.ofSeconds(3);

    private static final long DEFAULT_PERIOD_TICKS = 1L;

    private Bossbar() {
    }

    /**
     * Initializes or retrieves the shared boss bar service for the given plugin.
     *
     * @param plugin      owning plugin
     * @param periodTicks interval in ticks between render passes
     * @return the active boss bar service
     */
    @NonNull
    public static BossbarService initialize(@NonNull Plugin plugin, long periodTicks) {
        BossbarService service = BossbarService.getOrCreate(plugin);
        service.start(periodTicks);
        return service;
    }

    /**
     * Creates a visibility predicate that is true for players in the same world within a radius
     * of a moving point, e.g. a boss.
     *
     * <pre>{@code
     * .visibleWhen(Bossbar.within(knight::getLocation, 60))
     * }</pre>
     *
     * @param center supplies the center, or {@code null} when it no longer exists (nobody sees the bar)
     * @param radius radius in blocks
     * @return visibility predicate
     */
    @NonNull
    public static Predicate<Player> within(@NonNull Supplier<@Nullable Location> center, double radius) {
        double radiusSquared = radius * radius;
        return player -> {
            Location location = center.get();
            return location != null
                    && location.getWorld() != null
                    && location.getWorld().equals(player.getWorld())
                    && player.getLocation().distanceSquared(location) <= radiusSquared;
        };
    }

    /**
     * Creates a visibility predicate that is true for players in the same world within a radius
     * of an entity, automatically verifying that the entity is valid and alive.
     *
     * @param entity target entity
     * @param radius radius in blocks
     * @return visibility predicate
     */
    @NonNull
    public static Predicate<Player> within(@NonNull Entity entity, double radius) {
        Objects.requireNonNull(entity, "entity cannot be null");
        double radiusSquared = radius * radius;
        return player -> entity.isValid()
                && !entity.isDead()
                && entity.getWorld() != null
                && entity.getWorld().equals(player.getWorld())
                && player.getLocation().distanceSquared(entity.getLocation()) <= radiusSquared;
    }

    /**
     * Shows a white alert that drains over {@link #DEFAULT_DURATION}.
     *
     * @param player  target player
     * @param message title text
     */
    public static void sendAlert(@NonNull Player player, @NonNull String message) {
        sendAlert(player, message, DEFAULT_DURATION);
    }

    /**
     * Shows a white alert that drains over the given duration.
     *
     * @param player   target player
     * @param message  title text
     * @param duration lifespan
     */
    public static void sendAlert(@NonNull Player player, @NonNull String message, @NonNull Duration duration) {
        sendAlert(player, BossbarAlert.of(message, duration));
    }

    /**
     * Shows a white alert with the given priority.
     *
     * @param player   target player
     * @param message  title text
     * @param priority precedence (highest first)
     * @param duration lifespan
     */
    public static void sendAlert(@NonNull Player player, @NonNull String message, int priority, @NonNull Duration duration) {
        sendAlert(player, BossbarAlert.of(message, priority, duration));
    }

    /**
     * Shows a colored alert with the given priority.
     *
     * @param player   target player
     * @param message  title text
     * @param color    bar color
     * @param priority precedence (highest first)
     * @param duration lifespan
     */
    public static void sendAlert(
            @NonNull Player player,
            @NonNull String message,
            @NonNull BarColor color,
            int priority,
            @NonNull Duration duration
    ) {
        sendAlert(player, BossbarAlert.of(message, color, priority, duration));
    }

    /**
     * Shows a white alert that drains over the given duration and plays a sound cue.
     *
     * @param player   target player
     * @param message  title text
     * @param duration lifespan
     * @param soundCue optional audio cue
     */
    public static void sendAlert(
            @NonNull Player player,
            @NonNull String message,
            @NonNull Duration duration,
            @Nullable SoundCue soundCue
    ) {
        sendAlert(player, BossbarAlert.of(message, BarColor.WHITE, 0, duration, soundCue));
    }

    /**
     * Shows a colored alert with priority and an audio cue.
     *
     * @param player   target player
     * @param message  title text
     * @param color    bar color
     * @param priority precedence (highest first)
     * @param duration lifespan
     * @param soundCue optional audio cue
     */
    public static void sendAlert(
            @NonNull Player player,
            @NonNull String message,
            @NonNull BarColor color,
            int priority,
            @NonNull Duration duration,
            @Nullable SoundCue soundCue
    ) {
        sendAlert(player, BossbarAlert.of(message, color, priority, duration, soundCue));
    }

    /**
     * Shows a fully configured alert.
     *
     * @param player target player
     * @param alert  alert to show
     */
    public static void sendAlert(@NonNull Player player, @NonNull BossbarAlert alert) {
        service().sendAlert(player, alert);
    }

    /**
     * Shows a fully configured alert to several players.
     *
     * @param players target players
     * @param alert   alert to show
     */
    public static void sendAlert(@NonNull Collection<? extends Player> players, @NonNull BossbarAlert alert) {
        BossbarService service = service();
        for (Player player : players) {
            service.sendAlert(player, alert);
        }
    }

    /**
     * Shows a white alert to every online player.
     *
     * @param message  title text
     * @param duration lifespan
     */
    public static void broadcastAlert(@NonNull String message, @NonNull Duration duration) {
        broadcastAlert(BossbarAlert.of(message, duration));
    }

    /**
     * Shows a fully configured alert to every online player.
     *
     * @param alert alert to show
     */
    public static void broadcastAlert(@NonNull BossbarAlert alert) {
        sendAlert(Bukkit.getOnlinePlayers(), alert);
    }

    /**
     * Shows a countdown that drains with the time left (e.g. {@code "Rebooting in %ds"}).
     *
     * @param player  target player
     * @param format  format string containing {@code %d}
     * @param seconds countdown duration in seconds
     */
    public static void sendCountdown(@NonNull Player player, @NonNull String format, int seconds) {
        sendCountdown(player, format, seconds, 0, null);
    }

    /**
     * Shows a countdown that drains with the time left and plays a sound cue.
     *
     * @param player   target player
     * @param format   format string containing {@code %d}
     * @param seconds  countdown duration in seconds
     * @param soundCue optional audio cue
     */
    public static void sendCountdown(@NonNull Player player, @NonNull String format, int seconds, @Nullable SoundCue soundCue) {
        sendCountdown(player, format, seconds, 0, soundCue);
    }

    /**
     * Shows a countdown with a custom priority.
     *
     * @param player   target player
     * @param format   format string containing {@code %d}
     * @param seconds  countdown duration in seconds
     * @param priority precedence (highest first)
     */
    public static void sendCountdown(@NonNull Player player, @NonNull String format, int seconds, int priority) {
        sendCountdown(player, format, seconds, priority, null);
    }

    /**
     * Shows a countdown with a custom priority and audio cue.
     *
     * @param player   target player
     * @param format   format string containing {@code %d}
     * @param seconds  countdown duration in seconds
     * @param priority precedence (highest first)
     * @param soundCue optional audio cue
     */
    public static void sendCountdown(@NonNull Player player, @NonNull String format, int seconds, int priority, @Nullable SoundCue soundCue) {
        sendAlert(player, BossbarAlert.countdown(format, priority, seconds, soundCue));
    }

    /**
     * Shows a countdown with a custom second formatter.
     *
     * @param player    target player
     * @param formatter function receiving the remaining whole seconds
     * @param seconds   countdown duration in seconds
     */
    public static void sendCountdown(@NonNull Player player, @NonNull IntFunction<String> formatter, int seconds) {
        sendCountdown(player, formatter, seconds, 0, null);
    }

    /**
     * Shows a countdown with a custom second formatter and priority.
     *
     * @param player    target player
     * @param formatter function receiving the remaining whole seconds
     * @param seconds   countdown duration in seconds
     * @param priority  precedence (highest first)
     */
    public static void sendCountdown(@NonNull Player player, @NonNull IntFunction<String> formatter, int seconds, int priority) {
        sendCountdown(player, formatter, seconds, priority, null);
    }

    /**
     * Shows a countdown with a custom second formatter, priority, and audio cue.
     *
     * @param player    target player
     * @param formatter function receiving the remaining whole seconds
     * @param seconds   countdown duration in seconds
     * @param priority  precedence (highest first)
     * @param soundCue  optional audio cue
     */
    public static void sendCountdown(
            @NonNull Player player,
            @NonNull IntFunction<String> formatter,
            int seconds,
            int priority,
            @Nullable SoundCue soundCue
    ) {
        sendAlert(player, BossbarAlert.countdown(formatter, BarColor.WHITE, priority, seconds, soundCue));
    }

    /**
     * Registers a global section owned by the service's plugin, shown to every player for whom it is visible.
     *
     * @param section section to register
     */
    public static void register(@NonNull BossbarSection section) {
        service().register(section);
    }

    /**
     * Registers a global section owned by the given plugin. It is removed by
     * {@link #unregisterAll(Plugin)} or when that plugin is disabled.
     *
     * @param plugin  owning plugin
     * @param section section to register
     */
    public static void register(@NonNull Plugin plugin, @NonNull BossbarSection section) {
        service().register(plugin, section);
    }

    /**
     * Registers a section for one player only.
     *
     * @param player  target player
     * @param section section to register
     */
    public static void register(@NonNull Player player, @NonNull BossbarSection section) {
        service().register(player, section);
    }

    /**
     * Unregisters a global section.
     *
     * @param sectionId section id
     */
    public static void unregister(@NonNull String sectionId) {
        service().unregister(sectionId);
    }

    /**
     * Unregisters a section registered for one player.
     *
     * @param player    target player
     * @param sectionId section id
     */
    public static void unregister(@NonNull Player player, @NonNull String sectionId) {
        service().unregister(player, sectionId);
    }

    /**
     * Unregisters every global section owned by a plugin.
     *
     * @param plugin owning plugin
     */
    public static void unregisterAll(@NonNull Plugin plugin) {
        service().unregisterAll(plugin);
    }

    /**
     * Forces an immediate render for a player instead of waiting for the next periodic pass.
     *
     * @param player target player
     */
    public static void update(@NonNull Player player) {
        service().renderPlayer(player);
    }

    /**
     * Clears a player's alerts, restoring the sections underneath.
     *
     * @param player target player
     */
    public static void clearAlerts(@NonNull Player player) {
        service().clearAlerts(player);
    }

    /**
     * Clears a player's alerts and player-level sections and removes every bar from their screen.
     * Global sections reappear on the next render.
     *
     * @param player target player
     */
    public static void clear(@NonNull Player player) {
        service().clear(player);
    }

    private static BossbarService service() {
        BossbarService service = BossbarService.getInstance();
        if (service != null) {
            return service;
        }
        return initialize(PluginContext.get(), DEFAULT_PERIOD_TICKS);
    }
}
