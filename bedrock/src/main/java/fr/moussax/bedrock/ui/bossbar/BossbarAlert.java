package fr.moussax.bedrock.ui.bossbar;

import fr.moussax.bedrock.sound.SoundCue;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToDoubleFunction;

/**
 * An immutable specification for a temporary boss bar with a lifespan.
 *
 * <p>Alerts stack above persistent {@link BossbarSection}s, highest priority first. An
 * {@linkplain #exclusive(String, int, Duration) exclusive} alert hides all persistent sections
 * while it is active. When an alert expires, the bars underneath are restored automatically.</p>
 *
 * <p>Unless a progress function is given, the bar drains from full to empty over the alert's
 * lifespan. When queued into a composer, each viewing player's audio cue plays once.</p>
 */
public record BossbarAlert(Function<Player, @Nullable String> titleSupplier,
                           @Nullable IntFunction<@Nullable String> countdownFormatter,
                           @Nullable ToDoubleFunction<Player> progressSupplier, BarColor color, BarStyle style,
                           int priority, boolean exclusive, Duration duration,
                           @Nullable SoundCue soundCue) implements Comparable<BossbarAlert> {

    /**
     * Constructs an alert specification with explicit parameters.
     *
     * @param titleSupplier      title function; {@code null} hides the bar for that player
     * @param countdownFormatter countdown formatter receiving remaining seconds, or {@code null}
     * @param progressSupplier   fill ratio function, or {@code null} to drain over the lifespan
     * @param color              bar color
     * @param style              bar style
     * @param priority           precedence (highest first)
     * @param exclusive          whether persistent sections are hidden while active
     * @param duration           lifespan duration
     * @param soundCue           optional sound played once per player on first render
     */
    public BossbarAlert(
            @NonNull Function<Player, @Nullable String> titleSupplier,
            @Nullable IntFunction<@Nullable String> countdownFormatter,
            @Nullable ToDoubleFunction<Player> progressSupplier,
            @NonNull BarColor color,
            @NonNull BarStyle style,
            int priority,
            boolean exclusive,
            @NonNull Duration duration,
            @Nullable SoundCue soundCue
    ) {
        this.titleSupplier = Objects.requireNonNull(titleSupplier, "titleSupplier cannot be null");
        this.countdownFormatter = countdownFormatter;
        this.progressSupplier = progressSupplier;
        this.color = Objects.requireNonNull(color, "color cannot be null");
        this.style = Objects.requireNonNull(style, "style cannot be null");
        this.priority = priority;
        this.exclusive = exclusive;
        this.duration = Objects.requireNonNull(duration, "duration cannot be null");
        this.soundCue = soundCue;
    }

    /**
     * Constructs an alert specification with millisecond duration.
     */
    public BossbarAlert(
            @NonNull Function<Player, @Nullable String> titleSupplier,
            @Nullable ToDoubleFunction<Player> progressSupplier,
            @NonNull BarColor color,
            @NonNull BarStyle style,
            int priority,
            boolean exclusive,
            long durationMillis,
            @Nullable SoundCue soundCue
    ) {
        this(
                titleSupplier,
                null,
                progressSupplier,
                color,
                style,
                priority,
                exclusive,
                Duration.ofMillis(Math.max(0L, durationMillis)),
                soundCue
        );
    }

    /**
     * Creates a default-priority, white, draining alert.
     *
     * @param title    title text
     * @param duration lifespan
     * @return the alert
     */
    @NonNull
    public static BossbarAlert of(@NonNull String title, @NonNull Duration duration) {
        return of(title, BarColor.WHITE, 0, duration, null);
    }

    /**
     * Creates a default-priority, white, draining alert with an audio cue.
     *
     * @param title    title text
     * @param duration lifespan
     * @param soundCue optional sound cue
     * @return the alert
     */
    @NonNull
    public static BossbarAlert of(@NonNull String title, @NonNull Duration duration, @Nullable SoundCue soundCue) {
        return of(title, BarColor.WHITE, 0, duration, soundCue);
    }

    /**
     * Creates a white draining alert with the given priority.
     *
     * @param title    title text
     * @param priority precedence (highest first)
     * @param duration lifespan
     * @return the alert
     */
    @NonNull
    public static BossbarAlert of(@NonNull String title, int priority, @NonNull Duration duration) {
        return of(title, BarColor.WHITE, priority, duration, null);
    }

    /**
     * Creates a draining alert.
     *
     * @param title    title text
     * @param color    bar color
     * @param priority precedence (highest first)
     * @param duration lifespan
     * @return the alert
     */
    @NonNull
    public static BossbarAlert of(@NonNull String title, @NonNull BarColor color, int priority, @NonNull Duration duration) {
        return of(title, color, priority, duration, null);
    }

    /**
     * Creates a draining alert with an audio cue.
     *
     * @param title    title text
     * @param color    bar color
     * @param priority precedence (highest first)
     * @param duration lifespan
     * @param soundCue optional sound cue
     * @return the alert
     */
    @NonNull
    public static BossbarAlert of(
            @NonNull String title,
            @NonNull BarColor color,
            int priority,
            @NonNull Duration duration,
            @Nullable SoundCue soundCue
    ) {
        Objects.requireNonNull(title, "title cannot be null");
        return of(_ -> title, color, priority, duration, soundCue);
    }

    /**
     * Creates a draining alert with a per-player title.
     *
     * @param titleSupplier title function; {@code null} hides the bar for that player
     * @param color         bar color
     * @param priority      precedence (highest first)
     * @param duration      lifespan
     * @return the alert
     */
    @NonNull
    public static BossbarAlert of(
            @NonNull Function<Player, @Nullable String> titleSupplier,
            @NonNull BarColor color,
            int priority,
            @NonNull Duration duration
    ) {
        return of(titleSupplier, color, priority, duration, null);
    }

    /**
     * Creates a draining alert with a per-player title and audio cue.
     *
     * @param titleSupplier title function; {@code null} hides the bar for that player
     * @param color         bar color
     * @param priority      precedence (highest first)
     * @param duration      lifespan
     * @param soundCue      optional sound cue
     * @return the alert
     */
    @NonNull
    public static BossbarAlert of(
            @NonNull Function<Player, @Nullable String> titleSupplier,
            @NonNull BarColor color,
            int priority,
            @NonNull Duration duration,
            @Nullable SoundCue soundCue
    ) {
        Objects.requireNonNull(duration, "duration cannot be null");
        return new BossbarAlert(titleSupplier, null, null, color, BarStyle.SOLID, priority, false, duration, soundCue);
    }

    /**
     * Creates a draining alert that hides all persistent sections while it is active.
     *
     * @param title    title text
     * @param duration lifespan
     * @return the alert
     */
    @NonNull
    public static BossbarAlert exclusive(@NonNull String title, @NonNull Duration duration) {
        return exclusive(title, 0, duration, null);
    }

    /**
     * Creates an exclusive draining alert with an audio cue.
     *
     * @param title    title text
     * @param duration lifespan
     * @param soundCue optional sound cue
     * @return the alert
     */
    @NonNull
    public static BossbarAlert exclusive(@NonNull String title, @NonNull Duration duration, @Nullable SoundCue soundCue) {
        return exclusive(title, 0, duration, soundCue);
    }

    /**
     * Creates a draining alert that hides all persistent sections while it is active.
     *
     * @param title    title text
     * @param priority precedence among alerts (highest first)
     * @param duration lifespan
     * @return the alert
     */
    @NonNull
    public static BossbarAlert exclusive(@NonNull String title, int priority, @NonNull Duration duration) {
        return exclusive(title, priority, duration, null);
    }

    /**
     * Creates a draining alert that hides all persistent sections with an audio cue.
     *
     * @param title    title text
     * @param priority precedence among alerts (highest first)
     * @param duration lifespan
     * @param soundCue optional sound cue
     * @return the alert
     */
    @NonNull
    public static BossbarAlert exclusive(
            @NonNull String title,
            int priority,
            @NonNull Duration duration,
            @Nullable SoundCue soundCue
    ) {
        Objects.requireNonNull(title, "title cannot be null");
        Objects.requireNonNull(duration, "duration cannot be null");
        return new BossbarAlert(_ -> title, null, null, BarColor.WHITE, BarStyle.SOLID, priority, true, duration, soundCue);
    }

    /**
     * Creates a countdown alert. The title is derived from remaining wall-clock time and the bar drains alongside it.
     *
     * @param formatter function receiving the remaining whole seconds and returning the title
     * @param color     bar color
     * @param priority  precedence (highest first)
     * @param seconds   initial countdown duration in seconds
     * @return the alert
     */
    @NonNull
    public static BossbarAlert countdown(
            @NonNull IntFunction<@Nullable String> formatter,
            @NonNull BarColor color,
            int priority,
            int seconds
    ) {
        return countdown(formatter, color, priority, seconds, null);
    }

    /**
     * Creates a countdown alert with an audio cue.
     *
     * @param formatter function receiving the remaining whole seconds and returning the title
     * @param color     bar color
     * @param priority  precedence (highest first)
     * @param seconds   initial countdown duration in seconds
     * @param soundCue  optional sound cue
     * @return the alert
     */
    @NonNull
    public static BossbarAlert countdown(
            @NonNull IntFunction<@Nullable String> formatter,
            @NonNull BarColor color,
            int priority,
            int seconds,
            @Nullable SoundCue soundCue
    ) {
        Objects.requireNonNull(formatter, "formatter cannot be null");
        Duration duration = Duration.ofSeconds(Math.max(0, seconds));
        return new BossbarAlert(_ -> null, formatter, null, color, BarStyle.SOLID, priority, false, duration, soundCue);
    }

    /**
     * Creates a white countdown alert from a format string.
     *
     * @param format   string format containing {@code %d} (e.g. {@code "Rebooting in %ds"})
     * @param priority precedence (highest first)
     * @param seconds  initial countdown duration in seconds
     * @return the alert
     */
    @NonNull
    public static BossbarAlert countdown(@NonNull String format, int priority, int seconds) {
        return countdown(format, priority, seconds, null);
    }

    /**
     * Creates a white countdown alert from a format string with an audio cue.
     *
     * @param format   string format containing {@code %d} (e.g. {@code "Rebooting in %ds"})
     * @param priority precedence (highest first)
     * @param seconds  initial countdown duration in seconds
     * @param soundCue optional sound cue
     * @return the alert
     */
    @NonNull
    public static BossbarAlert countdown(@NonNull String format, int priority, int seconds, @Nullable SoundCue soundCue) {
        Objects.requireNonNull(format, "format cannot be null");
        return countdown(format::formatted, BarColor.WHITE, priority, seconds, soundCue);
    }

    /**
     * Returns a copy of this alert with the specified audio cue.
     *
     * @param soundCue sound cue to play on initial render
     * @return new alert instance
     */
    @NonNull
    public BossbarAlert withSound(@Nullable SoundCue soundCue) {
        return new BossbarAlert(titleSupplier, countdownFormatter, progressSupplier, color, style, priority, exclusive, duration, soundCue);
    }

    /**
     * Returns a copy of this alert with the specified bar color.
     *
     * @param color new bar color
     * @return new alert instance
     */
    @NonNull
    public BossbarAlert withColor(@NonNull BarColor color) {
        return new BossbarAlert(titleSupplier, countdownFormatter, progressSupplier, color, style, priority, exclusive, duration, soundCue);
    }

    /**
     * Returns a copy of this alert with the specified bar style.
     *
     * @param style new bar style
     * @return new alert instance
     */
    @NonNull
    public BossbarAlert withStyle(@NonNull BarStyle style) {
        return new BossbarAlert(titleSupplier, countdownFormatter, progressSupplier, color, style, priority, exclusive, duration, soundCue);
    }

    /**
     * Returns a copy of this alert with the specified precedence priority.
     *
     * @param priority precedence priority
     * @return new alert instance
     */
    @NonNull
    public BossbarAlert withPriority(int priority) {
        return new BossbarAlert(titleSupplier, countdownFormatter, progressSupplier, color, style, priority, exclusive, duration, soundCue);
    }

    /**
     * Returns a copy of this alert with the specified exclusivity flag.
     *
     * @param exclusive whether sections should be hidden
     * @return new alert instance
     */
    @NonNull
    public BossbarAlert exclusive(boolean exclusive) {
        return new BossbarAlert(titleSupplier, countdownFormatter, progressSupplier, color, style, priority, exclusive, duration, soundCue);
    }

    /**
     * Evaluates the title for the given player.
     *
     * @param player viewing player
     * @return title, or {@code null} to hide the bar for that player
     */
    public @Nullable String title(@Nullable Player player) {
        if (countdownFormatter != null) {
            int seconds = (int) Math.ceil(duration.toMillis() / 1000.0);
            return countdownFormatter.apply(seconds);
        }
        return titleSupplier.apply(player);
    }

    /**
     * Evaluates the fill ratio for the given player without an active session context.
     *
     * @param player viewing player
     * @return progress in {@code [0.0, 1.0]}
     */
    public double progress(@Nullable Player player) {
        if (progressSupplier != null) {
            return progressSupplier.applyAsDouble(player);
        }
        return 1.0;
    }

    public long durationMillis() {
        return duration.toMillis();
    }

    @Override
    public int compareTo(@NonNull BossbarAlert other) {
        return Integer.compare(other.priority, this.priority);
    }
}
