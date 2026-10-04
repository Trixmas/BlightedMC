package fr.moussax.blightedSMP.content.items;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.ItemRarity;
import fr.moussax.blightedSMP.engine.items.ItemType;
import fr.moussax.blightedSMP.engine.loot.results.gems.GemsItem;
import fr.moussax.blightedSMP.registry.RegistryModule;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;

import java.util.List;
import java.util.function.Consumer;

public class BlightedItems implements RegistryModule<Consumer<BlightedItem>> {

    @Override
    public void register(Consumer<BlightedItem> registry) {

        BlightedItem blightedBanner = new BlightedItem(
                "BLIGHTED_BANNER", ItemType.UNCATEGORIZED, ItemRarity.UNIQUE, Material.BLACK_BANNER);
        blightedBanner.setDisplayName("Twisted Banner");
        blightedBanner.description(
                "An ancient standard woven from",
                "§5shadow-silk§7. Its fabric beats with ",
                "a faint, living pulse, drawing the",
                "corruption of the Blight like",
                "a cosmic §5lightning rod§7."
        );
        blightedBanner.addBannerPatterns(List.of(
                new Pattern(DyeColor.CYAN, PatternType.CURLY_BORDER),
                new Pattern(DyeColor.BLACK, PatternType.BRICKS),
                new Pattern(DyeColor.BLACK, PatternType.SMALL_STRIPES),
                new Pattern(DyeColor.BLACK, PatternType.GUSTER),
                new Pattern(DyeColor.CYAN, PatternType.CIRCLE),
                new Pattern(DyeColor.BLACK, PatternType.FLOW)
        ));
        blightedBanner.addItemFlag(ItemFlag.HIDE_BANNER_PATTERNS);
        blightedBanner.preventPlacement();
        blightedBanner.editEquippable(equippable -> equippable.setSlot(EquipmentSlot.HEAD));
        blightedBanner.fireResistant();
        blightedBanner.unstackable();

        BlightedItem blightedCodex = new BlightedItem("BLIGHTED_CODEX", ItemType.UNCATEGORIZED, ItemRarity.UNIQUE, Material.ENCHANTED_BOOK);
        blightedCodex.setDisplayName("Blighted Codex");
        blightedCodex.description(
                "A forbidden ledger bound in cracked",
                "leather. Its pages remain blank until",
                "they absorb the essence of the Blight."
        );
        blightedCodex.addLore(
                "",
                " &#D2A5FF§lSEALED RIDDLE!",
                "&#D2A5FF Wear the woven shadow as your crown,",
                "&#D2A5FF claim a Blighted soul for these pages.",
                "&#D2A5FF Hold the violet crystal in your left hand, ",
                "&#D2A5FF stand before the altar of runes.",
                "&#D2A5FF Lay your hand upon the ancient seal,",
                "&#D2A5FF and the hidden path shall awaken.",
                "",
                "§8 Souls trapped: §d0 ☠"
        );
        blightedCodex.fireResistant();

        BlightedItem blightedGemstone = new BlightedItem("BLIGHTED_GEMSTONE", ItemType.UNCATEGORIZED, ItemRarity.SPECIAL, Material.PLAYER_HEAD);
        blightedGemstone.setCustomSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjU1YzhhYWM4OTkwZTQ1NmFiZjM1MDMwMTE1ZTM2YzdjNTY2NzgyZTEzZDgxYjFhNzcwOTk1ZmE1ZjM3YzgyMiJ9fX0=");
        blightedGemstone.setDisplayName("Blighted Gemstone");
        blightedGemstone.description(
                "A gemstone §5corrupted§7 by shadow,",
                "stolen from the heart of a §5fallen",
                "§5abomination§7. Within its core lie §dGems ",
                "sealed and waiting for a daring",
                "hand to claim them."
        );
        blightedGemstone.addLore(
                "§8 Gems: §d1✵"
        );

        blightedGemstone.preventEquipping();
        blightedGemstone.unstackable();
        blightedGemstone.preventPlacement();
        blightedGemstone.addAbility(new GemsItem.BlightedGemstoneAbility());

        registry.accept(blightedBanner);
        registry.accept(blightedCodex);
        registry.accept(blightedGemstone);
    }
}
