package fr.moussax.bedrock.ui.animation;

import fr.moussax.bedrock.scheduling.PluginContext;
import fr.moussax.bedrock.ui.actionbar.ActionbarService;
import fr.moussax.bedrock.ui.title.TimeableTitle;
import fr.moussax.bedrock.ui.title.TitlePacketSender;
import fr.moussax.bedrock.ui.title.TitleService;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * High-performance, flicker-free text animation engine for Minecraft user interfaces.
 *
 * <p>Supports sequenced frame animations across multiple UI channels including Action Bars,
 * Titles, Subtitles, and custom UI consumers. Eliminates visual stutter and client opacity resets
 * through in-place packet updates and automatic background HUD suspension during playback.</p>
 */
public final class TextAnimation {

    private static final Pattern COLOR_PATTERN = Pattern.compile("§[0-9a-fk-orA-FK-OR]");

    /**
     * An individual frame within a text animation sequence.
     *
     * @param text       primary frame text (rendered on action bar or main title)
     * @param subtitle   optional subtitle text (used when rendering as a title)
     * @param sound      optional sound played when this frame renders
     * @param soundPitch pitch for the frame sound
     */
    public record Frame(
            @NonNull String text,
            @Nullable String subtitle,
            @Nullable Sound sound,
            float soundPitch
    ) {
        public Frame(@NonNull String text) {
            this(text, null, null, 1.0f);
        }

        public Frame(@NonNull String text, @Nullable Sound sound, float soundPitch) {
            this(text, null, sound, soundPitch);
        }

        public Frame(@NonNull String text, @Nullable String subtitle) {
            this(text, subtitle, null, 1.0f);
        }

        /**
         * Alias for {@link #text()} suited for title-oriented contexts.
         *
         * @return primary text
         */
        public String title() {
            return text;
        }
    }

    private final List<Frame> frames;
    private final long tickInterval;
    private final TimeableTitle finalTimes;
    private final Duration finalStay;
    private final Consumer<Player> onComplete;
    private final TextAnimation nextAnimation;

    protected TextAnimation(
            List<Frame> frames,
            long tickInterval,
            TimeableTitle finalTimes,
            Duration finalStay,
            Consumer<Player> onComplete,
            TextAnimation nextAnimation
    ) {
        this.frames = List.copyOf(frames);
        this.tickInterval = Math.max(1L, tickInterval);
        this.finalTimes = finalTimes != null ? finalTimes : TimeableTitle.DEFAULT;
        this.finalStay = finalStay != null ? finalStay : Duration.ofMillis(this.finalTimes.stay() * 50L);
        this.onComplete = onComplete;
        this.nextAnimation = nextAnimation;
    }

    /**
     * Returns an unmodifiable list of all frames configured for this animation stage.
     *
     * @return animation frames
     */
    @NonNull
    public List<Frame> frames() {
        return frames;
    }

    /**
     * Returns the tick delay between consecutive frame dispatches.
     *
     * @return tick interval
     */
    public long tickInterval() {
        return tickInterval;
    }

    /**
     * Returns the timing parameters applied to title displays upon final frame completion.
     *
     * @return final title timings
     */
    @NonNull
    public TimeableTitle finalTimes() {
        return finalTimes;
    }

    /**
     * Returns the duration the completed text persists on the action bar before restoring background HUDs.
     *
     * @return final stay duration
     */
    @NonNull
    public Duration finalStay() {
        return finalStay;
    }

    /**
     * Returns the completion callback invoked for the target player upon completing this stage.
     *
     * @return completion callback or null
     */
    @Nullable
    public Consumer<Player> onComplete() {
        return onComplete;
    }

    /**
     * Returns the chained next animation stage, if configured.
     *
     * @return next animation stage or null
     */
    @Nullable
    public TextAnimation nextAnimation() {
        return nextAnimation;
    }

    /**
     * Creates a new fluent {@link Builder} to construct a custom text animation sequence.
     *
     * @return a new animation builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Chains this animation to be followed seamlessly by another animation upon completion.
     *
     * @param next the next animation to play
     * @return chained animation sequence
     */
    public TextAnimation then(@NonNull TextAnimation next) {
        Objects.requireNonNull(next, "next animation cannot be null");
        TextAnimation chainedNext = (this.nextAnimation != null) ? this.nextAnimation.then(next) : next;
        return new TextAnimation(this.frames, this.tickInterval, TimeableTitle.ANIMATION_FRAME, Duration.ZERO, this.onComplete, chainedNext);
    }

    /**
     * Constructs a typewriter text-reveal animation where characters appear incrementally.
     *
     * @param fullText     the complete text to reveal
     * @param ticksPerChar tick delay between revealing each character
     * @param clickSound   sound played when each letter appears, or null for silent
     * @return configured typewriter animation
     */
    public static TextAnimation typewriter(
            @NonNull String fullText,
            long ticksPerChar,
            @Nullable Sound clickSound
    ) {
        return typewriter(fullText, null, ticksPerChar, clickSound, TimeableTitle.DEFAULT);
    }

    /**
     * Constructs a typewriter text-reveal animation with a custom action bar final stay duration.
     *
     * @param fullText     the complete text to reveal
     * @param ticksPerChar tick delay between revealing each character
     * @param clickSound   sound played when each letter appears, or null for silent
     * @param finalStay    duration the completed text persists before restoring normal HUD
     * @return configured typewriter animation
     */
    public static TextAnimation typewriter(
            @NonNull String fullText,
            long ticksPerChar,
            @Nullable Sound clickSound,
            @NonNull Duration finalStay
    ) {
        Builder builder = builder().interval(ticksPerChar).finalStay(finalStay);
        buildTypewriterFrames(builder, fullText, null, clickSound);
        return builder.build();
    }

    /**
     * Constructs a typewriter text-reveal animation with static subtitle and title timings.
     *
     * @param fullTitle    the complete title text to reveal
     * @param subtitle     static subtitle displayed underneath
     * @param ticksPerChar tick delay between revealing each character
     * @param clickSound   sound played when each letter appears, or null for silent
     * @param finalTimes   display timings for the completed title
     * @return configured typewriter animation
     */
    public static TextAnimation typewriter(
            @NonNull String fullTitle,
            @Nullable String subtitle,
            long ticksPerChar,
            @Nullable Sound clickSound,
            @NonNull TimeableTitle finalTimes
    ) {
        Builder builder = builder().interval(ticksPerChar).finalTimes(finalTimes);
        buildTypewriterFrames(builder, fullTitle, subtitle, clickSound);
        return builder.build();
    }

    private static void buildTypewriterFrames(Builder builder, String text, @Nullable String subtitle, @Nullable Sound sound) {
        String stripped = COLOR_PATTERN.matcher(text).replaceAll("");
        int visibleLength = stripped.length();

        if (visibleLength == 0) {
            builder.frame(text, subtitle, sound, 1.2f);
            return;
        }

        for (int i = 1; i <= visibleLength; i++) {
            String partial = substringWithColors(text, i);
            builder.frame(partial, subtitle, sound, 1.2f);
        }
    }

    /**
     * Constructs a dynamic color-shimmer wave animation sliding across the letters of a text.
     *
     * @param text           raw text
     * @param baseColor      base color code (e.g. "§5")
     * @param highlightColor highlight shine peak color code (e.g. "§f")
     * @param bold           whether the text should be bold
     * @param cycles         number of complete shimmer wave cycles
     * @return configured shimmer animation
     */
    public static TextAnimation shimmer(
            @NonNull String text,
            @NonNull String baseColor,
            @NonNull String highlightColor,
            boolean bold,
            int cycles
    ) {
        return shimmer(text, null, baseColor, "§d", highlightColor, bold, cycles, TimeableTitle.DEFAULT);
    }

    /**
     * Constructs a dynamic color-shimmer wave animation with intermediate mid-tone color.
     *
     * @param text           raw text
     * @param baseColor      base color code (e.g. "§5")
     * @param midColor       intermediate shimmer color code (e.g. "§d")
     * @param highlightColor highlight shine peak color code (e.g. "§f")
     * @param bold           whether the text should be bold
     * @param cycles         number of complete shimmer wave cycles
     * @return configured shimmer animation
     */
    public static TextAnimation shimmer(
            @NonNull String text,
            @NonNull String baseColor,
            @NonNull String midColor,
            @NonNull String highlightColor,
            boolean bold,
            int cycles
    ) {
        return shimmer(text, null, baseColor, midColor, highlightColor, bold, cycles, TimeableTitle.DEFAULT);
    }

    /**
     * Constructs a color-shimmer animation with a custom action bar final stay duration.
     *
     * @param text           raw text
     * @param baseColor      base color code
     * @param highlightColor highlight shine color code
     * @param bold           whether the text should be bold
     * @param cycles         number of complete shimmer wave cycles
     * @param finalStay      duration the completed text persists before restoring normal HUD
     * @return configured shimmer animation
     */
    public static TextAnimation shimmer(
            @NonNull String text,
            @NonNull String baseColor,
            @NonNull String highlightColor,
            boolean bold,
            int cycles,
            @NonNull Duration finalStay
    ) {
        return shimmer(text, null, baseColor, "§d", highlightColor, bold, cycles, TimeableTitle.stay(finalStay));
    }

    /**
     * Constructs a color-shimmer animation with intermediate mid-tone and a custom final stay duration.
     *
     * @param text           raw text
     * @param baseColor      base color code
     * @param midColor       intermediate shimmer color code
     * @param highlightColor highlight shine color code
     * @param bold           whether the text should be bold
     * @param cycles         number of complete shimmer wave cycles
     * @param finalStay      duration the completed text persists before restoring normal HUD
     * @return configured shimmer animation
     */
    public static TextAnimation shimmer(
            @NonNull String text,
            @NonNull String baseColor,
            @NonNull String midColor,
            @NonNull String highlightColor,
            boolean bold,
            int cycles,
            @NonNull Duration finalStay
    ) {
        return shimmer(text, null, baseColor, midColor, highlightColor, bold, cycles, TimeableTitle.stay(finalStay));
    }

    /**
     * Constructs a dynamic color-shimmer wave animation with subtitle and title timings.
     *
     * @param text           raw text
     * @param subtitle       static subtitle displayed underneath
     * @param baseColor      base color code
     * @param highlightColor highlight shine peak color code
     * @param bold           whether the text should be bold
     * @param cycles         number of complete shimmer wave cycles
     * @param finalTimes     display timings for the finale
     * @return configured shimmer animation
     */
    public static TextAnimation shimmer(
            @NonNull String text,
            @Nullable String subtitle,
            @NonNull String baseColor,
            @NonNull String highlightColor,
            boolean bold,
            int cycles,
            @NonNull TimeableTitle finalTimes
    ) {
        return shimmer(text, subtitle, baseColor, "§d", highlightColor, bold, cycles, finalTimes);
    }

    /**
     * Constructs a dynamic color-shimmer wave animation with full color palette, subtitle, and timings.
     *
     * @param text           raw text
     * @param subtitle       static subtitle displayed underneath
     * @param baseColor      base color code
     * @param midColor       intermediate shimmer color code
     * @param highlightColor highlight shine peak color code
     * @param bold           whether the text should be bold
     * @param cycles         number of complete shimmer wave cycles
     * @param finalTimes     display timings for the finale
     * @return configured shimmer animation
     */
    public static TextAnimation shimmer(
            @NonNull String text,
            @Nullable String subtitle,
            @NonNull String baseColor,
            @NonNull String midColor,
            @NonNull String highlightColor,
            boolean bold,
            int cycles,
            @NonNull TimeableTitle finalTimes
    ) {
        Builder builder = builder().interval(1L).finalTimes(finalTimes);
        String prefix = bold ? "§l" : "";
        String highlight = highlightColor + prefix;
        String mid = midColor + prefix;
        String base = baseColor + prefix;
        int length = text.length();

        for (int cycle = 0; cycle < cycles; cycle++) {
            for (int highlightIndex = -2; highlightIndex < length + 2; highlightIndex++) {
                StringBuilder frameBuilder = new StringBuilder(length * 4);
                for (int index = 0; index < length; index++) {
                    char character = text.charAt(index);
                    if (index == highlightIndex) {
                        frameBuilder.append(highlight).append(character);
                    } else if (Math.abs(index - highlightIndex) == 1) {
                        frameBuilder.append(mid).append(character);
                    } else {
                        frameBuilder.append(base).append(character);
                    }
                }
                builder.frame(frameBuilder.toString(), subtitle);
            }
        }

        return builder.build();
    }

    /**
     * Constructs a flashing pulse animation alternating between a primary and highlight color.
     *
     * @param text          message text
     * @param primaryColor  standard color code (e.g. "§c")
     * @param flashColor    bright pulse color code (e.g. "§f")
     * @param ticksPerPulse tick delay per pulse phase
     * @param pulses        number of complete pulses
     * @param sound         sound played on each flash, or null for silent
     * @return configured pulse animation
     */
    public static TextAnimation pulse(
            @NonNull String text,
            @NonNull String primaryColor,
            @NonNull String flashColor,
            long ticksPerPulse,
            int pulses,
            @Nullable Sound sound
    ) {
        Builder builder = builder().interval(ticksPerPulse);
        for (int i = 0; i < pulses; i++) {
            builder.frame(flashColor + text, sound, 1.2f);
            builder.frame(primaryColor + text, null, 1.0f);
        }
        return builder.build();
    }

    /**
     * UI channel on which text animations can be played.
     */
    public enum Channel {
        ACTIONBAR,
        TITLE
    }

    private static final Map<UUID, Playback> activeActionbars = new ConcurrentHashMap<>();
    private static final Map<UUID, Playback> activeTitles = new ConcurrentHashMap<>();

    /**
     * Retrieves the currently active playback for a player on the specified channel, if any.
     *
     * @param player  target player
     * @param channel playback channel
     * @return active playback or null
     */
    public static @Nullable Playback getActivePlayback(@NonNull Player player, @NonNull Channel channel) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(channel, "channel cannot be null");
        return getActivePlayback(player.getUniqueId(), channel);
    }

    /**
     * Retrieves the currently active playback for a player UUID on the specified channel, if any.
     *
     * @param uuid    target player UUID
     * @param channel playback channel
     * @return active playback or null
     */
    public static @Nullable Playback getActivePlayback(@NonNull UUID uuid, @NonNull Channel channel) {
        Objects.requireNonNull(uuid, "uuid cannot be null");
        Objects.requireNonNull(channel, "channel cannot be null");
        Map<UUID, Playback> map = (channel == Channel.ACTIONBAR) ? activeActionbars : activeTitles;
        return map.get(uuid);
    }

    /**
     * Cancels all active text animation playbacks across all channels and clears tracking.
     */
    public static void clearActivePlaybacks() {
        activeActionbars.values().forEach(Playback::cancel);
        activeActionbars.clear();
        activeTitles.values().forEach(Playback::cancel);
        activeTitles.clear();
    }

    /**
     * Represents an active playback handle for a text animation on an action bar or title channel.
     *
     * <p>A playback handle owns all stages in chained sequences and any final stay task.
     * Cancelling the handle stops the active task, clears the animation state, restores the player's
     * persistent HUD, and invokes the supplied completion callback exactly once.</p>
     */
    public static final class Playback implements BukkitTask {
        private final Plugin plugin;
        private final Player player;
        private final Channel channel;
        private final Runnable extraOnComplete;
        private final AtomicBoolean finished = new AtomicBoolean(false);
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private volatile BukkitTask currentTask;

        Playback(
                @NonNull Plugin plugin,
                @NonNull Player player,
                @NonNull Channel channel,
                @Nullable Runnable extraOnComplete
        ) {
            this.plugin = plugin;
            this.player = player;
            this.channel = channel;
            this.extraOnComplete = extraOnComplete;
        }

        void setCurrentTask(@Nullable BukkitTask task) {
            this.currentTask = task;
        }

        /**
         * Returns the target player receiving this animation playback.
         *
         * @return target player
         */
        @NonNull
        public Player getPlayer() {
            return player;
        }

        /**
         * Returns the UI channel on which this animation is playing.
         *
         * @return playback channel
         */
        @NonNull
        public Channel getChannel() {
            return channel;
        }

        @Override
        public int getTaskId() {
            BukkitTask task = this.currentTask;
            return task != null ? task.getTaskId() : -1;
        }

        @Override
        @NonNull
        public Plugin getOwner() {
            return plugin;
        }

        @Override
        public boolean isSync() {
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }

        /**
         * Checks whether this playback has completed or was cancelled.
         *
         * @return true if finished
         */
        public boolean isFinished() {
            return finished.get();
        }

        @Override
        public void cancel() {
            if (!cancelled.compareAndSet(false, true)) {
                return;
            }
            BukkitTask task = this.currentTask;
            if (task != null) {
                task.cancel();
            }
            cleanupAndComplete();
        }

        void completeNormally() {
            if (cancelled.get()) {
                return;
            }
            cleanupAndComplete();
        }

        private void cleanupAndComplete() {
            if (!finished.compareAndSet(false, true)) {
                return;
            }

            UUID uuid = player.getUniqueId();
            Map<UUID, Playback> activeMap = (channel == Channel.ACTIONBAR) ? activeActionbars : activeTitles;
            activeMap.remove(uuid, this);

            if (channel == Channel.ACTIONBAR) {
                ActionbarService service = ActionbarService.getInstance();
                if (service != null) {
                    service.setAnimating(uuid, false);
                    service.renderPlayer(player);
                }
            } else {
                if (cancelled.get()) {
                    player.resetTitle();
                }
                TitleService service = TitleService.getInstance();
                if (service != null) {
                    service.setAnimating(uuid, false);
                    service.invalidateCache(uuid);
                    service.renderPlayer(player);
                }
            }

            if (extraOnComplete != null) {
                extraOnComplete.run();
            }
        }
    }

    /**
     * Plays this text animation on the target player's action bar.
     *
     * @param player target player
     * @return the running playback handle
     */
    public Playback playActionbar(@NonNull Player player) {
        return playActionbar(player, null);
    }

    /**
     * Plays this text animation on the target player's action bar with a completion callback.
     *
     * @param player          target player
     * @param extraOnComplete additional action executed upon completion
     * @return the running playback handle
     */
    public Playback playActionbar(@NonNull Player player, @Nullable Runnable extraOnComplete) {
        return playActionbar(resolvePlugin(), player, extraOnComplete);
    }

    /**
     * Plays this text animation on the target player's action bar using the specified plugin for scheduling.
     *
     * @param plugin owning plugin
     * @param player target player
     * @return the running playback handle
     */
    public Playback playActionbar(@NonNull Plugin plugin, @NonNull Player player) {
        return playActionbar(plugin, player, null);
    }

    /**
     * Plays this text animation on the target player's action bar using the specified plugin and completion callback.
     *
     * @param plugin          owning plugin
     * @param player          target player
     * @param extraOnComplete additional action executed upon completion
     * @return the running playback handle
     */
    public Playback playActionbar(@NonNull Plugin plugin, @NonNull Player player, @Nullable Runnable extraOnComplete) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Objects.requireNonNull(player, "player cannot be null");

        UUID uuid = player.getUniqueId();
        Playback previous = activeActionbars.get(uuid);
        if (previous != null) {
            previous.cancel();
        }

        if (!player.isOnline()) {
            if (extraOnComplete != null) {
                extraOnComplete.run();
            }
            return null;
        }

        Playback playback = new Playback(plugin, player, Channel.ACTIONBAR, extraOnComplete);
        activeActionbars.put(uuid, playback);
        ActionbarService service = ActionbarService.getInstance();
        if (service != null) {
            service.setAnimating(uuid, true);
        }

        executeActionbarStage(playback, this);
        return playback;
    }

    private void executeActionbarStage(@NonNull Playback playback, @NonNull TextAnimation stage) {
        if (playback.isCancelled()) {
            return;
        }

        Player player = playback.getPlayer();
        Plugin plugin = playback.getOwner();

        if (!player.isOnline()) {
            playback.cancel();
            return;
        }

        if (stage.frames.isEmpty()) {
            if (stage.nextAnimation != null) {
                executeActionbarStage(playback, stage.nextAnimation);
                return;
            }
            playback.completeNormally();
            return;
        }

        Frame firstFrame = stage.frames.getFirst();
        sendRawActionbar(player, firstFrame.text());
        if (firstFrame.sound() != null) {
            player.playSound(player.getLocation(), firstFrame.sound(), 1.0f, firstFrame.soundPitch());
        }

        if (stage.frames.size() == 1) {
            if (stage.onComplete != null) {
                stage.onComplete.accept(player);
            }

            if (stage.nextAnimation != null) {
                executeActionbarStage(playback, stage.nextAnimation);
                return;
            }

            long stayTicks = Math.max(0L, stage.finalStay.toMillis() / 50L);
            if (stayTicks > 0) {
                if (Bukkit.getServer() != null) {
                    BukkitTask stayTask = Bukkit.getScheduler().runTaskLater(plugin, playback::completeNormally, stayTicks);
                    playback.setCurrentTask(stayTask);
                }
            } else {
                playback.completeNormally();
            }
            return;
        }

        BukkitTask task = new BukkitRunnable() {
            private int frameIndex = 1;

            @Override
            public void run() {
                if (playback.isCancelled()) {
                    cancel();
                    return;
                }

                if (!player.isOnline()) {
                    cancel();
                    playback.cancel();
                    return;
                }

                if (frameIndex < stage.frames.size() - 1) {
                    Frame frame = stage.frames.get(frameIndex);
                    sendRawActionbar(player, frame.text());
                    if (frame.sound() != null) {
                        player.playSound(player.getLocation(), frame.sound(), 1.0f, frame.soundPitch());
                    }
                    frameIndex++;
                    return;
                }

                Frame last = stage.frames.getLast();
                cancel();

                sendRawActionbar(player, last.text());
                if (last.sound() != null) {
                    player.playSound(player.getLocation(), last.sound(), 1.0f, last.soundPitch());
                }
                if (stage.onComplete != null) {
                    stage.onComplete.accept(player);
                }

                if (stage.nextAnimation != null) {
                    executeActionbarStage(playback, stage.nextAnimation);
                    return;
                }

                long stayTicks = Math.max(0L, stage.finalStay.toMillis() / 50L);
                if (stayTicks > 0) {
                    if (Bukkit.getServer() != null) {
                        BukkitTask stayTask = Bukkit.getScheduler().runTaskLater(plugin, playback::completeNormally, stayTicks);
                        playback.setCurrentTask(stayTask);
                    }
                } else {
                    playback.completeNormally();
                }
            }
        }.runTaskTimer(plugin, stage.tickInterval, stage.tickInterval);

        playback.setCurrentTask(task);
    }

    /**
     * Plays this text animation across a collection of players' action bars.
     *
     * @param players target players
     */
    public void playActionbar(@NonNull Collection<? extends Player> players) {
        playActionbar(players, null);
    }

    /**
     * Plays this text animation across a collection of players' action bars with a completion callback.
     *
     * @param players         target players
     * @param extraOnComplete action executed when all players complete the animation
     */
    public void playActionbar(@NonNull Collection<? extends Player> players, @Nullable Runnable extraOnComplete) {
        playActionbar(resolvePlugin(), players, extraOnComplete);
    }

    /**
     * Plays this text animation across a collection of players' action bars using the specified plugin.
     *
     * @param plugin          owning plugin
     * @param players         target players
     * @param extraOnComplete action executed when all players complete the animation
     */
    public void playActionbar(
            @NonNull Plugin plugin,
            @NonNull Collection<? extends Player> players,
            @Nullable Runnable extraOnComplete
    ) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Objects.requireNonNull(players, "players cannot be null");
        dispatchCollection(players, extraOnComplete, (player, barrier) -> playActionbar(plugin, player, barrier));
    }

    /**
     * Delegate interface for custom action bar packet dispatching or test assertions.
     */
    @FunctionalInterface
    public interface ActionbarHandler {
        void sendActionbar(@NonNull Player player, @NonNull String text);
    }

    private static volatile ActionbarHandler customActionbarHandler;

    /**
     * Overrides the action bar packet handler for testing or custom packet pipelines.
     *
     * @param handler custom action bar handler, or null to revert to default Spigot dispatch
     */
    public static void setCustomActionbarHandler(@Nullable ActionbarHandler handler) {
        customActionbarHandler = handler;
    }

    private static void sendRawActionbar(@NonNull Player player, @NonNull String text) {
        ActionbarHandler handler = customActionbarHandler;
        if (handler != null) {
            handler.sendActionbar(player, text);
            return;
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text));
    }

    /**
     * Plays this text animation on the target player as an in-place title sequence.
     *
     * @param player target player
     * @return the running playback handle
     */
    public Playback playTitle(@NonNull Player player) {
        return playTitle(player, null);
    }

    /**
     * Plays this text animation on the target player as a title sequence with a completion callback.
     *
     * @param player          target player
     * @param extraOnComplete additional action executed upon completion
     * @return the running playback handle
     */
    public Playback playTitle(@NonNull Player player, @Nullable Runnable extraOnComplete) {
        return playTitle(resolvePlugin(), player, extraOnComplete);
    }

    /**
     * Plays this text animation on the target player as a title sequence using the specified plugin for scheduling.
     *
     * @param plugin owning plugin
     * @param player target player
     * @return the running playback handle
     */
    public Playback playTitle(@NonNull Plugin plugin, @NonNull Player player) {
        return playTitle(plugin, player, null);
    }

    /**
     * Plays this text animation on the target player as a title sequence using the specified plugin and completion callback.
     *
     * @param plugin          owning plugin
     * @param player          target player
     * @param extraOnComplete additional action executed upon completion
     * @return the running playback handle
     */
    public Playback playTitle(@NonNull Plugin plugin, @NonNull Player player, @Nullable Runnable extraOnComplete) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Objects.requireNonNull(player, "player cannot be null");

        UUID uuid = player.getUniqueId();
        Playback previous = activeTitles.get(uuid);
        if (previous != null) {
            previous.cancel();
        }

        if (!player.isOnline()) {
            if (extraOnComplete != null) {
                extraOnComplete.run();
            }
            return null;
        }

        Playback playback = new Playback(plugin, player, Channel.TITLE, extraOnComplete);
        activeTitles.put(uuid, playback);
        TitleService titleService = TitleService.getInstance();
        if (titleService != null) {
            titleService.setAnimating(uuid, true);
        }

        executeTitleStage(playback, this);
        return playback;
    }

    private void executeTitleStage(@NonNull Playback playback, @NonNull TextAnimation stage) {
        if (playback.isCancelled()) {
            return;
        }

        Player player = playback.getPlayer();
        Plugin plugin = playback.getOwner();

        if (!player.isOnline()) {
            playback.cancel();
            return;
        }

        if (stage.frames.isEmpty()) {
            if (stage.nextAnimation != null) {
                executeTitleStage(playback, stage.nextAnimation);
                return;
            }
            playback.completeNormally();
            return;
        }

        Frame firstFrame = stage.frames.getFirst();
        if (firstFrame.sound() != null) {
            player.playSound(player.getLocation(), firstFrame.sound(), 1.0f, firstFrame.soundPitch());
        }

        if (stage.frames.size() == 1) {
            if (stage.onComplete != null) {
                stage.onComplete.accept(player);
            }

            if (stage.nextAnimation != null) {
                int totalTicks = (int) (stage.frames.size() * stage.tickInterval);
                TitlePacketSender.sendFull(player, firstFrame.title(), firstFrame.subtitle(), 0, totalTicks + 60, 0);
                executeTitleStage(playback, stage.nextAnimation);
                return;
            }

            TitlePacketSender.sendFull(player, firstFrame.title(), firstFrame.subtitle(),
                    stage.finalTimes.fadeIn(), stage.finalTimes.stay(), stage.finalTimes.fadeOut());

            long finishDelay = stage.finalTimes.stay() + stage.finalTimes.fadeOut();
            if (finishDelay > 0) {
                if (Bukkit.getServer() != null) {
                    BukkitTask stayTask = Bukkit.getScheduler().runTaskLater(plugin, playback::completeNormally, finishDelay);
                    playback.setCurrentTask(stayTask);
                }
            } else {
                playback.completeNormally();
            }
            return;
        }

        int totalTicks = (int) (stage.frames.size() * stage.tickInterval);
        TitlePacketSender.sendFull(player, firstFrame.title(), firstFrame.subtitle(), 0, totalTicks + 60, 0);

        BukkitTask task = new BukkitRunnable() {
            private int frameIndex = 1;

            @Override
            public void run() {
                if (playback.isCancelled()) {
                    cancel();
                    return;
                }

                if (!player.isOnline()) {
                    cancel();
                    playback.cancel();
                    return;
                }

                if (frameIndex < stage.frames.size() - 1) {
                    Frame frame = stage.frames.get(frameIndex);
                    TitlePacketSender.sendTextOnly(player, frame.title(), frame.subtitle());
                    if (frame.sound() != null) {
                        player.playSound(player.getLocation(), frame.sound(), 1.0f, frame.soundPitch());
                    }
                    frameIndex++;
                    return;
                }

                Frame last = stage.frames.getLast();
                cancel();

                if (last.sound() != null) {
                    player.playSound(player.getLocation(), last.sound(), 1.0f, last.soundPitch());
                }
                if (stage.onComplete != null) {
                    stage.onComplete.accept(player);
                }

                if (stage.nextAnimation != null) {
                    TitlePacketSender.sendTextOnly(player, last.title(), last.subtitle());
                    executeTitleStage(playback, stage.nextAnimation);
                    return;
                }

                TitlePacketSender.sendFull(player, last.title(), last.subtitle(),
                        stage.finalTimes.fadeIn(), stage.finalTimes.stay(), stage.finalTimes.fadeOut());

                long finishDelay = stage.finalTimes.stay() + stage.finalTimes.fadeOut();
                if (finishDelay > 0) {
                    if (Bukkit.getServer() == null) return;
                    {
                        BukkitTask stayTask = Bukkit.getScheduler().runTaskLater(plugin, playback::completeNormally, finishDelay);
                        playback.setCurrentTask(stayTask);
                    }
                } else {
                    playback.completeNormally();
                }
            }
        }.runTaskTimer(plugin, stage.tickInterval, stage.tickInterval);

        playback.setCurrentTask(task);
    }

    /**
     * Plays this text animation as a title across a collection of players.
     *
     * @param players target players
     */
    public void playTitle(@NonNull Collection<? extends Player> players) {
        playTitle(players, null);
    }

    /**
     * Plays this text animation as a title across a collection of players with a completion callback.
     *
     * @param players         target players
     * @param extraOnComplete action executed when all players complete the animation
     */
    public void playTitle(@NonNull Collection<? extends Player> players, @Nullable Runnable extraOnComplete) {
        playTitle(resolvePlugin(), players, extraOnComplete);
    }

    /**
     * Plays this text animation as a title across a collection of players using the specified plugin.
     *
     * @param plugin          owning plugin
     * @param players         target players
     * @param extraOnComplete action executed when all players complete the animation
     */
    public void playTitle(
            @NonNull Plugin plugin,
            @NonNull Collection<? extends Player> players,
            @Nullable Runnable extraOnComplete
    ) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Objects.requireNonNull(players, "players cannot be null");
        dispatchCollection(players, extraOnComplete, (player, barrier) -> playTitle(plugin, player, barrier));
    }

    private static void dispatchCollection(
            @NonNull Collection<? extends Player> players,
            @Nullable Runnable extraOnComplete,
            @NonNull BiConsumer<Player, Runnable> dispatcher
    ) {
        List<? extends Player> targetPlayers = players.stream()
                .filter(p -> p != null && p.isOnline())
                .toList();

        if (targetPlayers.isEmpty()) {
            if (extraOnComplete != null) extraOnComplete.run();
            return;
        }

        AtomicInteger remaining = new AtomicInteger(targetPlayers.size());
        Runnable barrier = (extraOnComplete != null) ? () -> {
            if (remaining.decrementAndGet() == 0) {
                extraOnComplete.run();
            }
        } : null;

        for (Player player : targetPlayers) {
            dispatcher.accept(player, barrier);
        }
    }

    /**
     * Plays this text animation dispatching each rendered frame to a custom consumer.
     *
     * @param player        target player
     * @param frameConsumer consumer receiving each frame during playback
     * @return the running Bukkit task handle
     */
    public BukkitTask play(@NonNull Player player, @NonNull BiConsumer<Player, Frame> frameConsumer) {
        return play(player, frameConsumer, null);
    }

    /**
     * Plays this text animation dispatching each rendered frame to a custom consumer with completion callback.
     *
     * @param player          target player
     * @param frameConsumer   consumer receiving each frame during playback
     * @param extraOnComplete action executed upon completion
     * @return the running Bukkit task handle
     */
    public BukkitTask play(
            @NonNull Player player,
            @NonNull BiConsumer<Player, Frame> frameConsumer,
            @Nullable Runnable extraOnComplete
    ) {
        return play(resolvePlugin(), player, frameConsumer, extraOnComplete);
    }

    /**
     * Plays this text animation dispatching each rendered frame to a custom consumer using the specified plugin.
     *
     * @param plugin          owning plugin
     * @param player          target player
     * @param frameConsumer   consumer receiving each frame during playback
     * @param extraOnComplete action executed upon completion
     * @return the running Bukkit task handle
     */
    public BukkitTask play(
            @NonNull Plugin plugin,
            @NonNull Player player,
            @NonNull BiConsumer<Player, Frame> frameConsumer,
            @Nullable Runnable extraOnComplete
    ) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(frameConsumer, "frameConsumer cannot be null");

        if (frames.isEmpty()) {
            if (nextAnimation != null) {
                return nextAnimation.play(plugin, player, frameConsumer, extraOnComplete);
            }
            if (extraOnComplete != null) extraOnComplete.run();
            return null;
        }

        if (!player.isOnline()) {
            if (extraOnComplete != null) extraOnComplete.run();
            return null;
        }

        Frame firstFrame = frames.getFirst();
        frameConsumer.accept(player, firstFrame);
        if (firstFrame.sound() != null) {
            player.playSound(player.getLocation(), firstFrame.sound(), 1.0f, firstFrame.soundPitch());
        }

        if (frames.size() == 1) {
            if (onComplete != null) {
                onComplete.accept(player);
            }
            if (nextAnimation != null) {
                return nextAnimation.play(plugin, player, frameConsumer, extraOnComplete);
            }
            if (extraOnComplete != null) {
                extraOnComplete.run();
            }
            return null;
        }

        return new BukkitRunnable() {
            private int frameIndex = 1;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    if (extraOnComplete != null) {
                        extraOnComplete.run();
                    }
                    return;
                }

                if (frameIndex < frames.size() - 1) {
                    Frame frame = frames.get(frameIndex);
                    frameConsumer.accept(player, frame);
                    if (frame.sound() != null) {
                        player.playSound(player.getLocation(), frame.sound(), 1.0f, frame.soundPitch());
                    }
                    frameIndex++;
                    return;
                }

                Frame last = frames.getLast();
                cancel();
                frameConsumer.accept(player, last);
                if (last.sound() != null) {
                    player.playSound(player.getLocation(), last.sound(), 1.0f, last.soundPitch());
                }
                if (onComplete != null) {
                    onComplete.accept(player);
                }
                if (nextAnimation != null) {
                    nextAnimation.play(plugin, player, frameConsumer, extraOnComplete);
                } else if (extraOnComplete != null) {
                    extraOnComplete.run();
                }
            }
        }.runTaskTimer(plugin, tickInterval, tickInterval);
    }

    private static Plugin resolvePlugin() {
        ActionbarService actionbarService = ActionbarService.getInstance();
        if (actionbarService != null) {
            return actionbarService.getPlugin();
        }
        TitleService titleService = TitleService.getInstance();
        if (titleService != null) {
            return titleService.getPlugin();
        }
        return PluginContext.get();
    }

    /**
     * Extracts a substring containing a given number of visible characters while preserving color formatting codes.
     *
     * @param text         formatted string
     * @param visibleChars maximum visible character count
     * @return truncated formatted string
     */
    public static String substringWithColors(@NonNull String text, int visibleChars) {
        StringBuilder result = new StringBuilder();
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                result.append(c).append(text.charAt(i + 1));
                i++;
                continue;
            }
            result.append(c);
            count++;
            if (count >= visibleChars) {
                break;
            }
        }
        return result.toString();
    }

    /**
     * Fluent builder for {@link TextAnimation}.
     */
    public static class Builder {
        protected final List<Frame> frames = new ArrayList<>();
        protected long tickInterval = 1L;
        protected TimeableTitle finalTimes = TimeableTitle.DEFAULT;
        protected Duration finalStay = null;
        protected Consumer<Player> onComplete;

        public Builder frame(@NonNull String text) {
            return frame(text, null);
        }

        public Builder frame(@NonNull String text, @Nullable Sound sound, float pitch) {
            frames.add(new Frame(text, null, sound, pitch));
            return this;
        }

        public Builder frame(@NonNull String text, @Nullable String subtitle) {
            frames.add(new Frame(text, subtitle, null, 1.0f));
            return this;
        }

        public Builder frame(@NonNull String text, @Nullable String subtitle, @Nullable Sound sound, float pitch) {
            frames.add(new Frame(text, subtitle, sound, pitch));
            return this;
        }

        public Builder interval(long tickInterval) {
            this.tickInterval = tickInterval;
            return this;
        }

        public Builder interval(@NonNull Duration interval) {
            Objects.requireNonNull(interval, "interval cannot be null");
            this.tickInterval = Math.max(1L, interval.toMillis() / 50L);
            return this;
        }

        public Builder finalTimes(@NonNull TimeableTitle finalTimes) {
            this.finalTimes = Objects.requireNonNull(finalTimes, "finalTimes cannot be null");
            return this;
        }

        public Builder finalTimes(@NonNull Duration stayDuration) {
            return finalTimes(TimeableTitle.stay(stayDuration));
        }

        public Builder finalStay(@NonNull Duration finalStay) {
            this.finalStay = Objects.requireNonNull(finalStay, "finalStay cannot be null");
            return this;
        }

        public Builder onComplete(@Nullable Consumer<Player> onComplete) {
            this.onComplete = onComplete;
            return this;
        }

        public TextAnimation build() {
            Duration stay = (finalStay != null) ? finalStay : Duration.ofMillis(finalTimes.stay() * 50L);
            return new TextAnimation(frames, tickInterval, finalTimes, stay, onComplete, null);
        }
    }
}
