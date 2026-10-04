package fr.moussax.blightedSMP.engine.player.hud;

import fr.moussax.bedrock.text.Formatter;
import fr.moussax.bedrock.ui.actionbar.Actionbar;
import fr.moussax.bedrock.ui.actionbar.ActionbarSection;
import fr.moussax.blightedSMP.engine.player.BlightedPlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

/**
 * Manages player HUD display registration and periodic action bar formatting.
 */
public final class PlayerHudManager {

    public static final String SECTION_HEALTH = "health";
    public static final String SECTION_GEMS = "gems";
    public static final String SECTION_MANA = "mana";

    private final Plugin plugin;

    /**
     * Constructs a HUD manager and registers default gems and mana display sections for the plugin.
     *
     * @param plugin owning plugin instance
     */
    public PlayerHudManager(@NonNull Plugin plugin) {
        this.plugin = plugin;
        initializeDefaultSections();
    }

    private void initializeDefaultSections() {

        ActionbarSection healthSection = ActionbarSection.builder(SECTION_HEALTH)
                .order(0)
                .render(player -> {
                    int health = (int) player.getHealth();
                    int maxHealth = (int) Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue();
                    int absorption = (int) player.getAbsorptionAmount();
                    String dynamicHealth = absorption > 0 ? "§6" + (absorption + health) : "§c" + health;
                    return dynamicHealth + "/" + maxHealth + "❤";
                })
                .build();

        ActionbarSection gemsSection = ActionbarSection.builder(SECTION_GEMS)
                .order(1)
                .render(player -> {
                    BlightedPlayer blightedPlayer = BlightedPlayer.get(player);
                    if (blightedPlayer == null) return null;
                    return "§d" + Formatter.formatDecimalWithCommas(blightedPlayer.getGems()) + "❖ Blight";
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

        Actionbar.register(plugin, healthSection);
        Actionbar.register(plugin, gemsSection);
        Actionbar.register(plugin, manaSection);
    }

    /**
     * Unregisters all sections managed by this HUD manager.
     */
    public void unregister() {
        Actionbar.unregister(SECTION_HEALTH);
        Actionbar.unregister(SECTION_GEMS);
        Actionbar.unregister(SECTION_MANA);
    }
}
