package fr.moussax.blightedSMP.engine.items.lore;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.ItemRarity;
import fr.moussax.blightedSMP.engine.items.ItemType;
import fr.moussax.blightedSMP.engine.items.abilities.FullSetBonus;
import fr.moussax.blightedSMP.engine.items.abilities.ItemAbility;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders standardized, canonical BlightedMC item lore.
 *
 * <p>Formats descriptions, custom stat blocks, ability triggers and resource costs,
 * set bonuses, and the canonical rarity and item-type footer.</p>
 */
public final class ItemLoreRenderer {

    private ItemLoreRenderer() {
    }

    /**
     * Renders complete lore lines for a custom item.
     *
     * @param item item definition to render lore for
     * @return ordered list of formatted lore lines
     */
    public static List<String> render(BlightedItem item) {
        List<String> lore = new ArrayList<>();

        // 1. Description lines
        List<String> description = item.getDescription();
        if (!description.isEmpty()) {
            lore.add("");
            for (String line : description) {
                lore.add(line.isEmpty() ? "" : "§7 " + line);
            }
        }

        // 2. Abilities
        for (ItemAbility<?> ability : item.getAbilities()) {
            if (!ability.hasLore()) {
                continue;
            }

            lore.add("");
            lore.add("§3 Ability: " + ability.getName() + "  " + ability.getTrigger().getDisplayName());

            for (String line : ability.getDescription()) {
                lore.add(line.isEmpty() ? "" : "§7 " + line);
            }

            int mana = ability.getManaCost();
            int cooldown = ability.getCooldownSeconds();

            if (mana > 0) {
                lore.add("§8 Mana Cost: §3" + mana);
            }
            if (cooldown > 0) {
                lore.add("§8 Cooldown: §a" + cooldown + "s");
            }
        }

        // 3. Set Bonus / Piece Bonus
        FullSetBonus setBonus = item.getFullSetBonus();
        if (setBonus != null) {
            List<String> bonusLore = setBonus.getBonusLore();
            lore.addAll(bonusLore);
        }

        // 4. Custom lore lines appended directly
        List<String> customLore = item.getCustomLoreLines();
        if (!customLore.isEmpty()) {
            lore.addAll(customLore);
        }

        // 5. Canonical Rarity & Type Footer
        if (!lore.isEmpty()) {
            if (item.isSpacedRarity()) {
                if (!lore.getLast().isEmpty()) {
                    lore.add("");
                }
            } else {
                while (!lore.isEmpty() && lore.getLast().isEmpty()) {
                    lore.removeLast();
                }
            }
        }

        ItemRarity rarity = item.getItemRarity();
        ItemType type = item.getItemType();
        String typeSuffix = (type != null && type.getDisplayName() != null) ? " " + type.getDisplayName() : "";

        lore.add(rarity.getName() + typeSuffix);

        return lore;
    }
}
