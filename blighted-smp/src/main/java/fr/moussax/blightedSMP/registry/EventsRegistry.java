package fr.moussax.blightedSMP.registry;

import fr.moussax.blightedSMP.BlightedSMP;
import fr.moussax.blightedSMP.engine.entities.listeners.BlightedEntitiesListener;
import fr.moussax.blightedSMP.engine.entities.listeners.EntityComponentListener;
import fr.moussax.blightedSMP.engine.entities.listeners.SpawnableEntitiesListener;
import fr.moussax.blightedSMP.engine.entities.registry.EntitiesRegistry;
import fr.moussax.blightedSMP.engine.fishing.FishingListener;
import fr.moussax.blightedSMP.engine.blocks.BlightedBlockListener;
import fr.moussax.blightedSMP.engine.items.listeners.ItemAbilityListener;
import fr.moussax.blightedSMP.engine.items.listeners.ItemRestrictionListener;
import fr.moussax.blightedSMP.engine.items.listeners.UnsafeAnvilListener;
import fr.moussax.blightedSMP.engine.items.listeners.VanillaRecipeProtectionListener;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import fr.moussax.blightedSMP.engine.player.PlayerListener;
import fr.moussax.blightedSMP.engine.player.hud.PlayerHudManager;
import fr.moussax.blightedSMP.engine.quest.BlightedQuestListener;
import fr.moussax.bedrock.ui.actionbar.Actionbar;
import fr.moussax.bedrock.ui.menu.system.MenuListener;
import fr.moussax.bedrock.ui.menu.system.MenuSystem;
import fr.moussax.bedrock.ui.sign.SignInput;
import fr.moussax.bedrock.ui.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

/**
 * Registers and manages the event listeners used by BlightedMC.
 *
 * <p>This registry is responsible for constructing listener-dependent
 * subsystems, registering Bukkit event listeners, and managing the lifecycle
 * of listeners that require explicit initialization or cleanup.</p>
 */
public final class EventsRegistry {

    private final BlightedSMP instance = BlightedSMP.getInstance();
    private MenuSystem menuSystem;
    private PlayerHudManager playerHudManager;
    private SpawnableEntitiesListener spawnableEntitiesListener;

    /**
     * Initializes the event-driven subsystems and registers all BlightedMC
     * event listeners with Bukkit.
     *
     * <p>The spawnable entity listener is also registered as a callback on
     * {@link EntitiesRegistry} so that its cache can be invalidated whenever
     * the entity registry changes.</p>
     */
    public void initializeListeners() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        menuSystem = new MenuSystem(instance);
        SignInput.initialize(instance);
        Actionbar.initialize(instance, 20L);
        Title.initialize(instance, 10L);
        playerHudManager = new PlayerHudManager(instance);

        Bukkit.getScheduler().runTaskTimer(instance, () -> {
            for (BlightedPlayer player : BlightedPlayer.getPlayers()) {
                player.tick();
            }
        }, 20L, 20L);

        spawnableEntitiesListener = new SpawnableEntitiesListener();
        EntitiesRegistry.addOnRegisterCallback(spawnableEntitiesListener::invalidateCache);

        pluginManager.registerEvents(new MenuListener(menuSystem), instance);
        pluginManager.registerEvents(new BlightedEntitiesListener(), instance);
        pluginManager.registerEvents(new EntityComponentListener(), instance);
        pluginManager.registerEvents(spawnableEntitiesListener, instance);
        pluginManager.registerEvents(new BlightedBlockListener(), instance);
        pluginManager.registerEvents(new PlayerListener(), instance);
        pluginManager.registerEvents(new ItemRestrictionListener(), instance);
        pluginManager.registerEvents(new ItemAbilityListener(), instance);
        pluginManager.registerEvents(new FishingListener(), instance);
        pluginManager.registerEvents(new UnsafeAnvilListener(), instance);
        pluginManager.registerEvents(new VanillaRecipeProtectionListener(), instance);
        pluginManager.registerEvents(new BlightedQuestListener(), instance);
    }

    /**
     * Builds the spawnable entity cache after the entity registry has been initialized.
     *
     * <p>If the spawnable entity listener has not been initialized, this method has no effect.</p>
     */
    public void buildSpawnCache() {
        if (spawnableEntitiesListener != null) {
            spawnableEntitiesListener.rebuildCache();
        }
    }

    /**
     * Cleans up listener-specific resources that require explicit disposal.
     */
    public void cleanup() {
        SignInput.cleanup(instance);
        Actionbar.unregisterAll(instance);
    }

    /**
     * Shuts down the menu system and releases its associated resources.
     *
     * <p>If the menu system has not been initialized, this method has no effect.</p>
     */
    public void shutdownMenus() {
        if (menuSystem != null) {
            menuSystem.shutdown();
        }
    }
}
