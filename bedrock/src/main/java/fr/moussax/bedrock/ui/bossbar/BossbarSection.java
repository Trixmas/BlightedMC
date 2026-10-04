package fr.moussax.bedrock.ui.bossbar;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * A persistent boss bar registered under a unique id, rendered as its own bar for every player
 * for whom it is visible.
 *
 * <p>Unlike action bar sections, boss bar sections are not merged into one string: each visible
 * section is a separate bar, stacked by {@code priority} (lowest first, i.e. topmost).</p>
 *
 * <p>All suppliers and predicates are evaluated on the main thread once per render pass, so they
 * must be cheap and must not block. A {@code null} title hides the bar for that player.</p>
 *
 * <pre>{@code
 * Bossbar.register(plugin, BossbarSection.builder("ancient-knight")
 *         .title(_ -> "§f§lThe Ancient Knight")
 *         .progress(_ -> knight.getHealth() / maxHealth)
 *         .color(BarColor.RED)
 *         .visibleWhen(Bossbar.within(knight::getLocation, 60))
 *         .expireWhen(() -> !knight.isValid())
 *         .build());
 * }</pre>
 *
 * @param id               unique section identifier
 * @param priority         vertical order among sections (lowest first)
 * @param titleSupplier    title for a player, or {@code null} to hide the bar
 * @param progressSupplier fill ratio in {@code [0.0, 1.0]} for a player
 * @param colorSupplier    bar color for a player
 * @param style            segmentation style
 * @param flags            client effects (darken sky, boss music, fog)
 * @param visibility       whether the bar is shown to a player
 * @param expiry           when it returns {@code true} the section is unregistered automatically
 */
public record BossbarSection(
        @NonNull String id,
        int priority,
        @NonNull Function<Player, @Nullable String> titleSupplier,
        @NonNull ToDoubleFunction<Player> progressSupplier,
        @NonNull Function<Player, BarColor> colorSupplier,
        @NonNull BarStyle style,
        @NonNull Set<BarFlag> flags,
        @NonNull Predicate<Player> visibility,
        @NonNull BooleanSupplier expiry
) {

    /**
     * Color used when none is specified.
     */
    public static final BarColor DEFAULT_COLOR = BarColor.WHITE;

    /**
     * Style used when none is specified.
     */
    public static final BarStyle DEFAULT_STYLE = BarStyle.SOLID;

    public BossbarSection {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(titleSupplier, "titleSupplier cannot be null");
        Objects.requireNonNull(progressSupplier, "progressSupplier cannot be null");
        Objects.requireNonNull(colorSupplier, "colorSupplier cannot be null");
        Objects.requireNonNull(style, "style cannot be null");
        Objects.requireNonNull(visibility, "visibility cannot be null");
        Objects.requireNonNull(expiry, "expiry cannot be null");
        flags = Set.copyOf(flags);
    }

    /**
     * Creates an always-visible, white, solid section.
     *
     * @param id               unique section identifier
     * @param priority         vertical order (lowest first)
     * @param titleSupplier    title for a player, or {@code null} to hide the bar
     * @param progressSupplier fill ratio in {@code [0.0, 1.0]}
     * @return the section
     */
    @NonNull
    public static BossbarSection of(
            @NonNull String id,
            int priority,
            @NonNull Function<Player, @Nullable String> titleSupplier,
            @NonNull ToDoubleFunction<Player> progressSupplier
    ) {
        return builder(id).priority(priority).title(titleSupplier).progress(progressSupplier).build();
    }

    /**
     * Starts building a section with custom properties.
     *
     * @param id unique section identifier
     * @return new builder
     */
    @NonNull
    public static Builder builder(@NonNull String id) {
        return new Builder(id);
    }

    /**
     * Evaluates {@link #expiry()}.
     *
     * @return {@code true} if the section should be unregistered
     */
    public boolean isExpired() {
        return expiry.getAsBoolean();
    }

    /**
     * Fluent builder for {@link BossbarSection}.
     */
    public static final class Builder {

        private final String id;
        private int priority = 0;
        private Function<Player, @Nullable String> titleSupplier;
        private ToDoubleFunction<Player> progressSupplier = _ -> 1.0;
        private Function<Player, BarColor> colorSupplier = _ -> DEFAULT_COLOR;
        private BarStyle style = DEFAULT_STYLE;
        private final Set<BarFlag> flags = EnumSet.noneOf(BarFlag.class);
        private Predicate<Player> visibility = _ -> true;
        private BooleanSupplier expiry = () -> false;

        private Builder(@NonNull String id) {
            this.id = Objects.requireNonNull(id, "id cannot be null");
        }

        /**
         * Sets the vertical order among sections (lowest first).
         *
         * @param priority order weight
         * @return this builder
         */
        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Sets a static title.
         *
         * @param title title text
         * @return this builder
         */
        public Builder title(@NonNull String title) {
            Objects.requireNonNull(title, "title cannot be null");
            this.titleSupplier = _ -> title;
            return this;
        }

        /**
         * Sets a per-player title; returning {@code null} hides the bar for that player.
         *
         * @param titleSupplier title function
         * @return this builder
         */
        public Builder title(@NonNull Function<Player, @Nullable String> titleSupplier) {
            this.titleSupplier = Objects.requireNonNull(titleSupplier, "titleSupplier cannot be null");
            return this;
        }

        /**
         * Sets a constant fill ratio clamped to {@code [0.0, 1.0]}.
         *
         * @param progress static fill ratio
         * @return this builder
         */
        public Builder progress(double progress) {
            double clamped = Math.clamp(progress, 0.0, 1.0);
            this.progressSupplier = _ -> clamped;
            return this;
        }

        /**
         * Sets a progress supplier not dependent on the viewing player. Values are clamped to {@code [0.0, 1.0]}.
         *
         * @param progressSupplier progress supplier
         * @return this builder
         */
        public Builder progress(@NonNull DoubleSupplier progressSupplier) {
            Objects.requireNonNull(progressSupplier, "progressSupplier cannot be null");
            this.progressSupplier = _ -> progressSupplier.getAsDouble();
            return this;
        }

        /**
         * Sets the per-player fill ratio. Values are clamped to {@code [0.0, 1.0]}.
         *
         * @param progressSupplier progress function
         * @return this builder
         */
        public Builder progress(@NonNull ToDoubleFunction<Player> progressSupplier) {
            this.progressSupplier = Objects.requireNonNull(progressSupplier, "progressSupplier cannot be null");
            return this;
        }

        /**
         * Sets a static color.
         *
         * @param color bar color
         * @return this builder
         */
        public Builder color(@NonNull BarColor color) {
            Objects.requireNonNull(color, "color cannot be null");
            this.colorSupplier = _ -> color;
            return this;
        }

        /**
         * Sets a per-player color, re-evaluated every render pass (e.g. red when a boss is enraged).
         *
         * @param colorSupplier color function
         * @return this builder
         */
        public Builder color(@NonNull Function<Player, BarColor> colorSupplier) {
            this.colorSupplier = Objects.requireNonNull(colorSupplier, "colorSupplier cannot be null");
            return this;
        }

        /**
         * Sets the segmentation style.
         *
         * @param style bar style
         * @return this builder
         */
        public Builder style(@NonNull BarStyle style) {
            this.style = Objects.requireNonNull(style, "style cannot be null");
            return this;
        }

        /**
         * Adds client effects shown while this bar is visible.
         *
         * @param barFlags flags to add
         * @return this builder
         */
        public Builder flags(@NonNull BarFlag... barFlags) {
            Collections.addAll(flags, barFlags);
            return this;
        }

        /**
         * Adds a client effect shown while this bar is visible.
         *
         * @param barFlag flag to add
         * @return this builder
         */
        public Builder flag(@NonNull BarFlag barFlag) {
            Objects.requireNonNull(barFlag, "barFlag cannot be null");
            this.flags.add(barFlag);
            return this;
        }

        /**
         * Restricts which players see the bar. Evaluated every render pass.
         *
         * @param visibility visibility predicate
         * @return this builder
         */
        public Builder visibleWhen(@NonNull Predicate<Player> visibility) {
            this.visibility = Objects.requireNonNull(visibility, "visibility cannot be null");
            return this;
        }

        /**
         * Unregisters the section automatically once the condition becomes {@code true}.
         * Evaluated every render pass; use it to tie a bar to the lifetime of an entity or an event.
         *
         * @param expiry expiry condition
         * @return this builder
         */
        public Builder expireWhen(@NonNull BooleanSupplier expiry) {
            this.expiry = Objects.requireNonNull(expiry, "expiry cannot be null");
            return this;
        }

        /**
         * Builds the section.
         *
         * @return immutable section
         * @throws IllegalStateException if no title was set
         */
        @NonNull
        public BossbarSection build() {
            if (titleSupplier == null) {
                throw new IllegalStateException("A title is required for boss bar section '" + id + "'");
            }
            return new BossbarSection(id, priority, titleSupplier, progressSupplier, colorSupplier, style, flags, visibility, expiry);
        }
    }
}
