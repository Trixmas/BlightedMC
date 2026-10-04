package fr.moussax.bedrock.ui.bossbar;

import fr.moussax.bedrock.utils.debug.Log;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages boss bar lifecycles: global sections, per-player composers, and the periodic render pass.
 *
 * <p>Unlike the title and action bar services, stopping this service also removes every bar from
 * every player's screen, since a bar stays visible on the client until it is explicitly removed.</p>
 */
public final class BossbarService implements Listener {

    @Getter
    @Setter
    private static volatile BossbarService instance;

    @Getter
    private final Plugin plugin;
    private final Map<UUID, BossbarComposer> composers = new ConcurrentHashMap<>();
    private final Map<String, BossbarSection> globalSections = new ConcurrentHashMap<>();
    private final Map<String, Plugin> globalOwners = new ConcurrentHashMap<>();
    private BukkitTask tickerTask;
    private volatile boolean running = false;
    private long currentPeriodTicks = 1L;

    /**
     * Constructs a boss bar service and registers quit and plugin-disable listeners with Bukkit.
     *
     * @param plugin owning plugin
     */
    public BossbarService(@NonNull Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        if (instance == null) {
            instance = this;
        }
        if (Bukkit.getServer() != null) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }
    }

    /**
     * Obtains the shared service, or constructs one if not yet initialized.
     *
     * @param plugin owning plugin
     * @return active boss bar service
     */
    @NonNull
    public static BossbarService getOrCreate(@NonNull Plugin plugin) {
        if (instance == null) {
            synchronized (BossbarService.class) {
                if (instance == null) {
                    new BossbarService(plugin);
                }
            }
        }
        return instance;
    }

    /**
     * Starts or updates the periodic render task. A shorter period replaces a longer running one.
     *
     * @param periodTicks interval between render passes in server ticks
     */
    public void start(long periodTicks) {
        if (running && tickerTask != null && !tickerTask.isCancelled()) {
            if (periodTicks < currentPeriodTicks) {
                this.currentPeriodTicks = periodTicks;
                tickerTask.cancel();
                this.tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, 0L, periodTicks);
            }
            return;
        }

        this.currentPeriodTicks = periodTicks;
        this.running = true;

        if (tickerTask != null) {
            tickerTask.cancel();
        }

        this.tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, 0L, periodTicks);
    }

    /**
     * Stops the render task, removes every bar from every player, and discards all state.
     */
    public void stop() {
        running = false;
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }
        composers.values().forEach(BossbarComposer::detach);
        composers.clear();
        globalSections.clear();
        globalOwners.clear();
        HandlerList.unregisterAll(this);
        synchronized (BossbarService.class) {
            if (instance == this) {
                instance = null;
            }
        }
    }

    /**
     * Retrieves or creates the composer for a player.
     *
     * @param uuid player unique id
     * @return the player's composer
     */
    @NonNull
    public BossbarComposer getOrCreateComposer(@NonNull UUID uuid) {
        return composers.computeIfAbsent(uuid, _ -> new BossbarComposer(() -> globalSections));
    }

    /**
     * Retrieves the composer for a player if present.
     *
     * @param uuid player unique id
     * @return the composer, or {@code null} if absent
     */
    public @Nullable BossbarComposer getComposer(@NonNull UUID uuid) {
        return composers.get(uuid);
    }

    /**
     * Registers a global section owned by this service's plugin.
     *
     * @param section section to register
     */
    public void register(@NonNull BossbarSection section) {
        register(plugin, section);
    }

    /**
     * Registers a global section shown to every player for whom it is visible, replacing any global
     * section with the same id. It is removed by {@link #unregisterAll(Plugin)} or when its owner is disabled.
     *
     * @param owner   plugin that owns the section
     * @param section section to register
     */
    public void register(@NonNull Plugin owner, @NonNull BossbarSection section) {
        Objects.requireNonNull(owner, "owner cannot be null");
        Objects.requireNonNull(section, "section cannot be null");
        globalSections.put(section.id(), section);
        globalOwners.put(section.id(), owner);
        renderAll();
    }

    /**
     * Registers a section for one player only.
     *
     * @param player  target player
     * @param section section to register
     */
    public void register(@NonNull Player player, @NonNull BossbarSection section) {
        getOrCreateComposer(player.getUniqueId()).registerSection(section);
        renderPlayer(player);
    }

    /**
     * Unregisters a global section.
     *
     * @param id section id
     */
    public void unregister(@NonNull String id) {
        globalSections.remove(id);
        globalOwners.remove(id);
        renderAll();
    }

    /**
     * Unregisters a section registered for one player.
     *
     * @param player target player
     * @param id     section id
     */
    public void unregister(@NonNull Player player, @NonNull String id) {
        BossbarComposer composer = composers.get(player.getUniqueId());
        if (composer != null) {
            composer.unregisterSection(id);
            renderPlayer(player);
        }
    }

    /**
     * Unregisters every global section owned by a plugin.
     *
     * @param owner owning plugin
     */
    public void unregisterAll(@NonNull Plugin owner) {
        Objects.requireNonNull(owner, "owner cannot be null");
        boolean removed = globalOwners.entrySet().removeIf(entry -> {
            if (!entry.getValue().equals(owner)) {
                return false;
            }
            globalSections.remove(entry.getKey());
            return true;
        });
        if (removed) {
            renderAll();
        }
    }

    /**
     * Queues a modal alert and immediately renders.
     *
     * @param player target player
     * @param alert  alert to queue
     */
    public void sendAlert(@NonNull Player player, @NonNull BossbarAlert alert) {
        getOrCreateComposer(player.getUniqueId()).sendModalAlert(alert);
        renderPlayer(player);
    }

    /**
     * Clears a player's alerts and restores the sections underneath.
     *
     * @param player target player
     */
    public void clearAlerts(@NonNull Player player) {
        BossbarComposer composer = composers.get(player.getUniqueId());
        if (composer != null) {
            composer.clearAlerts();
            renderPlayer(player);
        }
    }

    /**
     * Clears a player's alerts and player-level sections and removes every bar from their screen.
     * Global sections reappear on the next render.
     *
     * @param player target player
     */
    public void clear(@NonNull Player player) {
        BossbarComposer composer = composers.get(player.getUniqueId());
        if (composer != null) {
            composer.clear();
        }
    }

    /**
     * Immediately evaluates and renders bars for a player.
     *
     * @param player target player
     */
    public void renderPlayer(@NonNull Player player) {
        if (!player.isOnline()) {
            return;
        }
        getOrCreateComposer(player.getUniqueId()).render(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        BossbarComposer composer = composers.remove(event.getPlayer().getUniqueId());
        if (composer != null) {
            composer.detach();
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().equals(plugin)) {
            stop();
            return;
        }
        unregisterAll(event.getPlugin());
    }

    private void renderAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            try {
                renderPlayer(player);
            } catch (Exception exception) {
                Log.warn("BossbarService", "Failed to render boss bars for " + player.getName() + ": " + exception.getMessage());
            }
        }
    }

    private void tickAll() {
        globalSections.forEach((id, section) -> {
            if (isExpired(section)) {
                globalSections.remove(id, section);
                globalOwners.remove(id);
            }
        });
        renderAll();
    }

    private static boolean isExpired(BossbarSection section) {
        try {
            return section.isExpired();
        } catch (Exception exception) {
            Log.warn("BossbarService", "Dropping section '" + section.id() + "', expireWhen threw: " + exception.getMessage());
            return true;
        }
    }
}
