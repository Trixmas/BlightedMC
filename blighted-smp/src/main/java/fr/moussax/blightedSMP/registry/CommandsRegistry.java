package fr.moussax.blightedSMP.registry;

import fr.moussax.bedrock.commands.CommandRegistrar;
import fr.moussax.bedrock.commands.TabSuggestionRegistry;
import fr.moussax.blightedSMP.commands.impl.*;
import fr.moussax.blightedSMP.engine.entities.BlightedEntity;
import fr.moussax.blightedSMP.engine.entities.registry.EntitiesRegistry;
import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.registry.ItemRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Central registry for all plugin commands.
 *
 * <p>Registers player-facing, administrative, and utility commands with the
 * plugin's {@link CommandRegistrar}. It also initializes the shared tab
 * completion suggestions used by registered commands.</p>
 */
public final class CommandsRegistry {

    private CommandsRegistry() {
    }

    /**
     * Registers all plugin commands and their associated tab completion suggestions.
     *
     * @param plugin plugin instance used by the command registrar
     */
    public static void register(JavaPlugin plugin) {
        TabSuggestionRegistry suggestions = createSuggestionRegistry();
        CommandRegistrar registrar = new CommandRegistrar(plugin, suggestions);

        registrar.register("altar", new AltarCommand());
        registrar.register("craft", new CraftCommand());
        registrar.register("forge", new ForgeCommand());

        // Administrator Commands
        registrar.register("spawncustommob", new SpawnCustomMobCommand());
        registrar.register("giveitem", new GiveItemCommand());
        registrar.register("blight", new BlightCommand());
        registrar.register("tppos", new TeleportPositionCommand());
        GamemodeCommands gamemodeCommands = new GamemodeCommands();
        registrar.register("gmc", gamemodeCommands);
        registrar.register("gms", gamemodeCommands);
        registrar.register("gma", gamemodeCommands);
        registrar.register("gmspec", gamemodeCommands);
        registrar.register("fly", new FlyCommand());
        registrar.register("god", new GodCommand());
        registrar.register("nightvision", new NightVisionCommand());
        registrar.register("speed", new SpeedCommand());
        registrar.register("butcher", new ButcherCommand());
        registrar.register("loop", new LoopCommand());
        registrar.register("test", new TestCommand());
    }

    /**
     * Creates and configures the shared tab completion suggestion registry.
     *
     * <p>The registry exposes dynamic suggestions for online players,
     * registered custom items, and registered custom entities.</p>
     *
     * @return configured tab suggestion registry
     */
    private static TabSuggestionRegistry createSuggestionRegistry() {
        TabSuggestionRegistry suggestions = new TabSuggestionRegistry();

        suggestions.register("$players", sender -> Bukkit.getOnlinePlayers()
                .stream()
                .filter(target -> !(sender instanceof Player player) || player.canSee(target))
                .map(Player::getName)
                .toList()
        );

        suggestions.register("$items", () -> ItemRegistry.getAllItems()
                .stream()
                .map(BlightedItem::getItemId)
                .toList()
        );

        suggestions.register("$entities", () -> EntitiesRegistry.getAll()
                .stream()
                .map(BlightedEntity::getEntityId)
                .toList()
        );

        return suggestions;
    }
}
