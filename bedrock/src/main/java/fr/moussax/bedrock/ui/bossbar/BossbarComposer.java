package fr.moussax.bedrock.ui.bossbar;

import fr.moussax.bedrock.sound.SoundCue;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Manages one player's boss bars: per-player sections, modal alerts, and the
 * global sections shared by every player.
 *
 * <p>Each visible section or alert is backed by its own Bukkit {@link BossBar}, because a Bukkit
 * bar has a single title and progress for its whole audience while suppliers are per player.
 * Bars are only touched when their content changed, and are re-added in order only when the
 * visible stack order changes.</p>
 *
 * <p>{@link #render(Player)}, {@link #clear()} and {@link #detach()} must run on the main thread.
 * Registration and alert methods are thread-safe.</p>
 */
public final class BossbarComposer {

    /**
     * Prevents redundant packet dispatch from floating-point jitter while preserving smooth sub-frame lerping.
     */
    private static final double PROGRESS_EPSILON = 1e-5;

    private final Supplier<Map<String, BossbarSection>> globalSectionsSupplier;
    private final Map<String, BossbarSection> playerSections = new ConcurrentHashMap<>();
    private final PriorityQueue<ActiveAlert> modalAlerts = new PriorityQueue<>();
    private final Object alertLock = new Object();

    /**
     * Insertion order mirrors the order bars were added on the client. Keys: section id or active alert instance.
     */
    private Map<Object, Handle> handles = new LinkedHashMap<>();

    /**
     * Constructs a composer with no global sections.
     */
    public BossbarComposer() {
        this(Map::of);
    }

    /**
     * Constructs a composer that also renders the given global sections.
     *
     * @param globalSectionsSupplier supplier of the current global sections by id
     */
    public BossbarComposer(@NonNull Supplier<Map<String, BossbarSection>> globalSectionsSupplier) {
        this.globalSectionsSupplier = Objects.requireNonNull(globalSectionsSupplier, "globalSectionsSupplier cannot be null");
    }

    /**
     * Registers a section for this player only, replacing any player section with the same id.
     * A player section overrides a global section with the same id.
     *
     * @param section section to register
     */
    public void registerSection(@NonNull BossbarSection section) {
        Objects.requireNonNull(section, "section cannot be null");
        playerSections.put(section.id(), section);
    }

    /**
     * Unregisters this player's section.
     *
     * @param id section id
     */
    public void unregisterSection(@NonNull String id) {
        playerSections.remove(id);
    }

    /**
     * Queues a modal alert. An active session is created with a fresh lifespan.
     *
     * @param alert alert to queue
     */
    public void sendModalAlert(@NonNull BossbarAlert alert) {
        Objects.requireNonNull(alert, "alert cannot be null");
        synchronized (alertLock) {
            modalAlerts.add(new ActiveAlert(alert));
        }
    }

    /**
     * Clears all modal alerts. Persistent sections reappear on the next render.
     */
    public void clearAlerts() {
        synchronized (alertLock) {
            modalAlerts.clear();
        }
    }

    /**
     * Clears alerts and this player's sections, and removes every bar from the player's screen.
     */
    public void clear() {
        clearAlerts();
        playerSections.clear();
        detach();
    }

    /**
     * Removes every bar from the player's screen without discarding registered state.
     * The next render re-adds whatever is visible.
     */
    public void detach() {
        handles.values().forEach(Handle::detach);
        handles = new LinkedHashMap<>();
    }

    /**
     * Forces the next render to resend title, progress, color and style of every bar.
     */
    public void invalidateCache() {
        handles.values().forEach(Handle::invalidate);
    }

    /**
     * Renders the current composite state to the given player.
     *
     * @param player target player
     */
    public void render(@NonNull Player player) {
        if (!player.isOnline()) {
            return;
        }

        if (handles.isEmpty() && modalAlerts.isEmpty() && playerSections.isEmpty() && globalSectionsSupplier.get().isEmpty()) {
            return;
        }

        List<ActiveAlert> alerts;
        synchronized (alertLock) {
            modalAlerts.removeIf(ActiveAlert::isExpired);
            alerts = new ArrayList<>(modalAlerts);
        }
        alerts.sort(null);
        playerSections.values().removeIf(BossbarSection::isExpired);

        List<Entry> desired = new ArrayList<>();
        boolean exclusive = false;

        for (ActiveAlert alert : alerts) {
            String title = alert.title(player);
            if (title == null) {
                continue;
            }
            alert.playSoundIfNeeded(player);
            desired.add(new Entry(alert, title, clamp(alert.progress(player)), alert.alert().color(), alert.alert().style(), Set.of()));
            exclusive |= alert.alert().exclusive();
        }

        if (!exclusive) {
            for (BossbarSection section : orderedSections()) {
                if (!section.visibility().test(player)) {
                    continue;
                }
                String title = section.titleSupplier().apply(player);
                if (title == null) {
                    continue;
                }
                desired.add(new Entry(
                        section.id(),
                        title,
                        clamp(section.progressSupplier().applyAsDouble(player)),
                        section.colorSupplier().apply(player),
                        section.style(),
                        section.flags()
                ));
            }
        }

        reconcile(player, desired);
    }

    private List<BossbarSection> orderedSections() {
        Map<String, BossbarSection> merged = new HashMap<>(globalSectionsSupplier.get());
        merged.putAll(playerSections);
        List<BossbarSection> sections = new ArrayList<>(merged.values());
        sections.sort(Comparator.comparingInt(BossbarSection::priority).thenComparing(BossbarSection::id));
        return sections;
    }

    private void reconcile(Player player, List<Entry> desired) {
        Set<Object> desiredKeys = new HashSet<>();
        for (Entry entry : desired) {
            desiredKeys.add(entry.key());
        }

        handles.entrySet().removeIf(existing -> {
            if (desiredKeys.contains(existing.getKey())) {
                return false;
            }
            existing.getValue().detach();
            return true;
        });

        // New bars are appended on the client, so existing bars are only kept in place when they
        // already form the head of the desired stack. Otherwise, the stack is rebuilt in order.
        boolean appendOnly = true;
        int index = 0;
        for (Object key : handles.keySet()) {
            if (index >= desired.size() || !desired.get(index).key().equals(key)) {
                appendOnly = false;
                break;
            }
            index++;
        }
        if (!appendOnly) {
            handles.values().forEach(Handle::detach);
        }

        Map<Object, Handle> ordered = new LinkedHashMap<>();
        for (Entry entry : desired) {
            Handle handle = handles.get(entry.key());
            if (handle == null) {
                handle = new Handle(entry);
            }
            handle.apply(entry);
            if (!handle.attached) {
                handle.bar.addPlayer(player);
                handle.attached = true;
            }
            ordered.put(entry.key(), handle);
        }
        handles = ordered;
    }

    private static double clamp(double progress) {
        return Double.isNaN(progress) ? 0.0 : Math.clamp(progress, 0.0, 1.0);
    }

    /**
     * Active stateful session for a queued alert.
     */
    public static final class ActiveAlert implements Comparable<ActiveAlert> {

        private final BossbarAlert alert;
        private final long createdAt;
        private final long durationMillis;
        private final long expiresAt;
        private final Set<UUID> playedPlayers = ConcurrentHashMap.newKeySet();

        public ActiveAlert(@NonNull BossbarAlert alert) {
            this.alert = Objects.requireNonNull(alert, "alert cannot be null");
            this.createdAt = System.currentTimeMillis();
            this.durationMillis = Math.max(0L, alert.durationMillis());
            this.expiresAt = this.createdAt + this.durationMillis;
        }

        public BossbarAlert alert() {
            return alert;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }

        public long remainingMillis() {
            return Math.max(0L, expiresAt - System.currentTimeMillis());
        }

        public @Nullable String title(Player player) {
            IntFunction<@Nullable String> formatter = alert.countdownFormatter();
            if (formatter != null) {
                int remainingSeconds = (int) Math.ceil(remainingMillis() / 1000.0);
                return formatter.apply(remainingSeconds);
            }
            return alert.titleSupplier().apply(player);
        }

        public double progress(Player player) {
            if (alert.progressSupplier() != null) {
                return alert.progressSupplier().applyAsDouble(player);
            }
            return durationMillis == 0L ? 0.0 : (double) remainingMillis() / durationMillis;
        }

        public void playSoundIfNeeded(@NonNull Player player) {
            SoundCue cue = alert.soundCue();
            if (cue != null && playedPlayers.add(player.getUniqueId())) {
                cue.play(player);
            }
        }

        @Override
        public int compareTo(@NonNull ActiveAlert other) {
            int precedence = Integer.compare(other.alert.priority(), this.alert.priority());
            if (precedence != 0) {
                return precedence;
            }
            return Long.compare(other.createdAt, this.createdAt);
        }
    }

    private record Entry(
            Object key,
            String title,
            double progress,
            BarColor color,
            BarStyle style,
            Set<BarFlag> flags
    ) {
    }

    private static final class Handle {

        private final BossBar bar;
        private boolean attached;
        private String title;
        private double progress = Double.NaN;
        private BarColor color;
        private BarStyle style;

        private Handle(Entry entry) {
            this.bar = Bukkit.createBossBar(entry.title(), entry.color(), entry.style());
        }

        private void apply(Entry entry) {
            if (!entry.title().equals(title)) {
                bar.setTitle(entry.title());
                title = entry.title();
            }
            if (progressChanged(progress, entry.progress())) {
                bar.setProgress(entry.progress());
                progress = entry.progress();
            }
            if (entry.color() != color) {
                bar.setColor(entry.color());
                color = entry.color();
            }
            if (entry.style() != style) {
                bar.setStyle(entry.style());
                style = entry.style();
            }
            for (BarFlag flag : BarFlag.values()) {
                boolean wanted = entry.flags().contains(flag);
                if (bar.hasFlag(flag) == wanted) {
                    continue;
                }
                if (wanted) {
                    bar.addFlag(flag);
                } else {
                    bar.removeFlag(flag);
                }
            }
        }

        private static boolean progressChanged(double last, double next) {
            if (Double.isNaN(last)) {
                return true;
            }
            if (next == last) {
                return false;
            }
            return Math.abs(next - last) >= PROGRESS_EPSILON || next == 0.0 || next == 1.0;
        }

        private void detach() {
            bar.removeAll();
            attached = false;
        }

        private void invalidate() {
            title = null;
            progress = Double.NaN;
            color = null;
            style = null;
        }
    }
}
