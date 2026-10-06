package fr.moussax.blightedSMP.engine.entities.rituals.menu;

import fr.moussax.bedrock.text.Formatter;
import fr.moussax.bedrock.ui.menu.Menu;
import fr.moussax.bedrock.ui.menu.interaction.MenuElementPreset;
import fr.moussax.bedrock.utils.ItemBuilder;
import fr.moussax.blightedSMP.engine.entities.rituals.AncientRitual;
import fr.moussax.blightedSMP.engine.entities.rituals.registry.RitualRegistry;
import fr.moussax.blightedSMP.engine.recipes.RecipeIngredient;
import fr.moussax.blightedSMP.utils.Utilities;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class AncientRitualsMenu extends Menu {

    private static final int[] RITUAL_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 40, 41, 42, 43
    };

    private static final int[] FILLER_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
            17, 18, 26, 27, 35, 36, 44,
            45, 46, 47, 50, 51, 52, 53
    };

    private static final int BACK_BUTTON_SLOT = 48;
    private static final int CLOSE_BUTTON_SLOT = 49;

    private final Menu previousMenu;
    private final List<AncientRitual> cachedRituals;

    public AncientRitualsMenu(Menu previousMenu) {
        super("Ancient Rituals", 54);
        this.previousMenu = previousMenu;
        this.cachedRituals = new ArrayList<>(RitualRegistry.getAll());
        this.cachedRituals.sort(Comparator.comparing(
                ritual -> ritual.getSummonedCreature() != null
                        ? ritual.getSummonedCreature().getName()
                        : "Unknown Ritual"
        ));
    }

    public AncientRitualsMenu() {
        this(null);
    }

    @Override
    public void build(@NonNull Player player) {
        fillSlots(FILLER_SLOTS, MenuElementPreset.EMPTY_SLOT_FILLER);

        for (int i = 0; i < cachedRituals.size() && i < RITUAL_SLOTS.length; i++) {
            AncientRitual ritual = cachedRituals.get(i);
            ItemStack displayItem = buildRiteDisplayItem(ritual);
            setItem(RITUAL_SLOTS[i], displayItem, (clickingPlayer, _) -> openSubMenu(new RitualAltarMenu(ritual, this)));
        }

        if (previousMenu != null) {
            setBackButton(BACK_BUTTON_SLOT, previousMenu);
        }

        setCloseButton(CLOSE_BUTTON_SLOT);
    }

    private ItemStack buildRiteDisplayItem(AncientRitual ritual) {
        ItemStack summoningItem = ritual.getDisplayedItem().clone();
        ItemBuilder builder = new ItemBuilder(summoningItem);

        if (!summoningItem.hasItemMeta() || !Objects.requireNonNull(summoningItem.getItemMeta()).hasDisplayName()) {
            if (ritual.getSummonedCreature() != null) {
                builder.setDisplayName("§5" + ritual.getSummonedCreature().getName());
            } else {
                builder.setDisplayName("§5Unknown Ritual");
            }
        }

        if (ritual.getSummonedCreature() != null) {
            String timeFormatted = Formatter.formatTime(ritual.getSummonedCreature().getTimeAllowance());
            builder.addLore("", " §7Time limit: §c" + timeFormatted, "");
        }

        builder.addLore(" §7Offerings required: ");
        for (RecipeIngredient offering : ritual.getOfferings()) {
            builder.addLore(" §8‣ " + Utilities.extractIngredientName(offering) + " §8x" + offering.getAmount());
        }

        if (ritual.getBlightCost() > 0) {
            builder.addLore(" §8‣ §3" + Formatter.formatDecimalWithCommas(ritual.getBlightCost()) + "❖ Blight");
        }
        if (ritual.getLevelCost() > 0) {
            builder.addLore(" §8‣ §3" + Formatter.formatDecimalWithCommas(ritual.getLevelCost()) + "◎ EXP Levels");
        }

        builder.addLore("", "§eClick to select!");
        return builder.toItemStack();
    }
}
