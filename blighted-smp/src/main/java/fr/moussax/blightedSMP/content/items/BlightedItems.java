package fr.moussax.blightedSMP.content.items;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.ItemRarity;
import fr.moussax.blightedSMP.engine.items.ItemType;
import fr.moussax.blightedSMP.engine.loot.results.blightstone.ResonantBlightstoneItem;
import fr.moussax.blightedSMP.registry.RegistryModule;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;

import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public class BlightedItems implements RegistryModule<Consumer<BlightedItem>> {

    @Override
    public void register(Consumer<BlightedItem> registry) {

        var twistedBanner = new BlightedItem("TWISTED_BANNER", ItemType.UNCATEGORIZED, ItemRarity.RARE, Material.BLACK_BANNER);
        twistedBanner.setDisplayName("Twisted Banner");
        twistedBanner.description(
                "Once a §3war-standard§7 of the Forgotten,",
                "the banner was buried beneath the Keep.",
                "The cloth has since §3twisted§7 into something ",
                "living, and still hums with the §3resonance§7",
                "of the deep.");
        twistedBanner.addBannerPatterns(List.of(
                new Pattern(DyeColor.CYAN, PatternType.CURLY_BORDER),
                new Pattern(DyeColor.BLACK, PatternType.BRICKS),
                new Pattern(DyeColor.BLACK, PatternType.SMALL_STRIPES),
                new Pattern(DyeColor.BLACK, PatternType.GUSTER),
                new Pattern(DyeColor.CYAN, PatternType.CIRCLE),
                new Pattern(DyeColor.BLACK, PatternType.FLOW)));
        twistedBanner.addItemFlag(ItemFlag.HIDE_BANNER_PATTERNS);
        twistedBanner.preventPlacement();
        twistedBanner.editEquippable(equippable -> {
            equippable.setSlot(EquipmentSlot.HEAD);
            equippable.setEquipSound(Sound.ENTITY_WARDEN_LISTENING_ANGRY);
        });
        twistedBanner.fireResistant();
        twistedBanner.unstackable();

        var resonantBlightstone = new BlightedItem("RESONANT_BLIGHTSTONE", ItemType.UNCATEGORIZED, ItemRarity.RARE, Material.PLAYER_HEAD);
        resonantBlightstone.setCustomSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjU1YzhhYWM4OTkwZTQ1NmFiZjM1MDMwMTE1ZTM2YzdjNTY2NzgyZTEzZDgxYjFhNzcwOTk1ZmE1ZjM3YzgyMiJ9fX0=");
        resonantBlightstone.setDisplayName("Resonant Blightstone");
        resonantBlightstone.description(
                "Buried deep beneath the Trenches, this",
                "this blightstone still pulses with a forgotten ",
                "resonance. A single strike was once enough",
                "to reshape living matter."
        );
        resonantBlightstone.addLore("§8 Blight Sealed: §31❖");

        resonantBlightstone.preventEquipping();
        resonantBlightstone.unstackable();
        resonantBlightstone.preventPlacement();
        resonantBlightstone.addAbility(new ResonantBlightstoneItem.ResonantBlightstoneAbility());


        BlightedItem blightedCodex =
                new BlightedItem("BLIGHTED_CODEX", ItemType.UNCATEGORIZED, ItemRarity.UNIQUE, Material.ENCHANTED_BOOK);
        blightedCodex.setDisplayName("Blighted Codex");
        blightedCodex.description(
                "A forbidden ledger bound in cracked",
                "leather. Its pages remain blank until",
                "they absorb the essence of the Blight.");
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
                "§8 Souls trapped: §d0 ☠");
        blightedCodex.fireResistant();

        registry.accept(twistedBanner);
        registry.accept(blightedCodex);
        registry.accept(resonantBlightstone);
    }
}
