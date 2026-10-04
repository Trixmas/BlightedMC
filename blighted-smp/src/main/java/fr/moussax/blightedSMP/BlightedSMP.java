package fr.moussax.blightedSMP;

import fr.moussax.blightedSMP.engine.entities.spawnable.engine.BlightedSpawnEngine;
import fr.moussax.blightedSMP.engine.fishing.hooks.CustomFishingHook;
import fr.moussax.blightedSMP.registry.CommandsRegistry;
import fr.moussax.blightedSMP.registry.EventsRegistry;
import fr.moussax.blightedSMP.registry.RegistrySystem;
import fr.moussax.blightedSMP.server.BlightedServer;
import fr.moussax.blightedSMP.server.PluginFiles;
import fr.moussax.blightedSMP.server.PluginSettings;
import fr.moussax.blightedSMP.server.database.PluginDatabase;
import fr.moussax.bedrock.scheduling.PluginContext;
import fr.moussax.bedrock.utils.debug.Log;
import fr.moussax.blightedSMP.engine.player.cinematic.FirstJoinCinematic;
import lombok.Getter;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

/**
 * Main plugin entry point for BlightedMC.
 *
 * <p>Coordinates initialization and shutdown of the plugin's core systems,
 * including server configuration, persistence, registries, commands, event
 * listeners, entity spawning, and custom fishing hooks.</p>
 */
public final class BlightedSMP extends JavaPlugin {

    @Getter
    private static BlightedSMP instance;
    @Getter
    private PluginSettings settings;
    @Getter
    private PluginDatabase database;
    @Getter
    private EventsRegistry eventsRegistry;

    @Override
    public void onEnable() {
        instance = this;
        PluginContext.bind(this);

        Log.info("Plugin", "Initializing BlightedMC plugin...");
        BlightedServer.configureServer();

        String config = PluginFiles.CONFIG.getFileName();
        saveResource(config, false);

        settings = PluginSettings.load(this);
        if (!initializeDatabase()) {
            return;
        }

        CommandsRegistry.register(this);
        RegistrySystem.initialize();
        eventsRegistry = new EventsRegistry();
        eventsRegistry.initializeListeners();
        eventsRegistry.buildSpawnCache();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            BlightedPlayer.get(onlinePlayer);
        }

        BlightedServer.rehydrateEntitiesOnLoadedChunks(this);
        new BlightedSpawnEngine().runTaskTimer(this, 100L, 20L);
    }

    @Override
    public void onDisable() {
        FirstJoinCinematic.cleanupAll();
        CustomFishingHook.cleanupAll();
        BlightedPlayer.saveAllSync();
        if (database != null) {
            database.closeConnection();
        }
        if (eventsRegistry != null) {
            eventsRegistry.shutdownMenus();
            eventsRegistry.cleanup();
        }
        RegistrySystem.clear();
    }

    private boolean initializeDatabase() {
        try {
            database = new PluginDatabase(getDataFolder().getAbsolutePath() + "/" + PluginFiles.DATABASE.getFileName());
            Log.success("Database", "Successfully connected to the database.");
            return true;
        } catch (SQLException error) {
            Log.debug(error.getMessage());
            Log.error("Database", "Unable to connect to the database.");
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }
    }
}
