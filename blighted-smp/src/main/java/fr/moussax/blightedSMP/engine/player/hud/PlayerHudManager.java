package fr.moussax.blightedSMP.engine.player.hud;

import fr.moussax.bedrock.text.Formatter;
import fr.moussax.bedrock.ui.actionbar.Actionbar;
import fr.moussax.bedrock.ui.actionbar.ActionbarSection;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

/**
 * Manages player HUD display registration and periodic action bar formatting.
 */
public final class PlayerHudManager {

    public static final String SECTION_BLIGHT = "blight";
    public static final String SECTION_MANA = "mana";

    private final Plugin plugin;

    /**
     * Constructs a HUD manager and registers default blight and mana display sections for the plugin.
     *
     * @param plugin owning plugin instance
     */
    public PlayerHudManager(@NonNull Plugin plugin) {
        this.plugin = plugin;
        initializeDefaultSections();
    }

    private void initializeDefaultSections() {

        ActionbarSection blightSection = ActionbarSection.builder(SECTION_BLIGHT)
                .order(0)
                .render(player -> {
                    BlightedPlayer blightedPlayer = BlightedPlayer.get(player);
                    if (blightedPlayer == null) return null;
                    return "§3" + Formatter.formatDecimalWithCommas(blightedPlayer.getBlight()) + "❖ Blight";
                })
                .build();

        ActionbarSection manaSection = ActionbarSection.builder(SECTION_MANA)
                .order(10)
                .render(player -> {
                    BlightedPlayer blightedPlayer = BlightedPlayer.get(player);
                    if (blightedPlayer == null) return null;
                    return "§b" + Formatter.formatDouble(blightedPlayer.getCurrentMana(), 0) + "/"
                            + Formatter.formatDouble(blightedPlayer.getMaxMana(), 0) + "✎ Mana";
                })
                .build();

        Actionbar.register(plugin, blightSection);
        Actionbar.register(plugin, manaSection);
    }

    /**
     * Unregisters all sections managed by this HUD manager.
     */
    public void unregister() {
        Actionbar.unregister(SECTION_BLIGHT);
        Actionbar.unregister(SECTION_MANA);
    }
}
