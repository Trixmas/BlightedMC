package fr.moussax.blightedSMP.content.artifacts;

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
public class BlightedArtifacts implements RegistryModule<Consumer<BlightedItem>> {

    @Override
    public void register(Consumer<BlightedItem> registry) {

        BlightedItem twistedBanner = new BlightedItem(
                "TWISTED_BANNER",
                ItemType.UNCATEGORIZED,
                ItemRarity.SPECIAL,
                Material.BLACK_BANNER
        );
        twistedBanner.setDisplayName("Twisted Banner");
        twistedBanner.description(
                "Once a §3war-standard§7 of the Forgotten,",
                "the banner was buried beneath the Keep.",
                "The cloth has since §3twisted§7 into something ",
                "living, and still hums with the §3resonance§7",
                "of the deep."
        );
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
        twistedBanner.soulbound();

        BlightedItem resonantBlightstone = new BlightedItem(
                "RESONANT_BLIGHTSTONE",
                ItemType.UNCATEGORIZED,
                ItemRarity.RARE,
                Material.PLAYER_HEAD
        );
        resonantBlightstone.setCustomSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjU1YzhhYWM4OTkwZTQ1NmFiZjM1MDMwMTE1ZTM2YzdjNTY2NzgyZTEzZDgxYjFhNzcwOTk1ZmE1ZjM3YzgyMiJ9fX0=");
        resonantBlightstone.setDisplayName("Resonant Blightstone");
        resonantBlightstone.description(
                "Buried deep beneath the Trenches, this ",
                "blightstone still pulses with a forgotten",
                "resonance. A single strike was one",
                "enough to reshape living matter."
        );
        resonantBlightstone.addLore("§8 Blight Sealed: §31❖");
        resonantBlightstone.preventEquipping();
        resonantBlightstone.unstackable();
        resonantBlightstone.preventPlacement();
        resonantBlightstone.addAbility(new ResonantBlightstoneItem.ResonantBlightstoneAbility());

        BlightedItem echoingTwistedOrb = new BlightedItem(
                "ECHOING_TWISTED_ORB",
                ItemType.UNCATEGORIZED,
                ItemRarity.SPECIAL,
                Material.PLAYER_HEAD
        );
        echoingTwistedOrb.setCustomSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTIwZTU5OWI1YjIzMDExNTdjYjE0ZWYwMGZmM2FmY2Y2ZjI3NWMzMjMwNjQwZDM0ZGI1NzU4MjI3MDQzY2Y0In19fQ==");
        echoingTwistedOrb.setDisplayName("Echoing Twisted Orb");
        echoingTwistedOrb.description(
                "A fragment of something older than",
                "the hands that carried it. It echoes ",
                "with the souls of the Blighted, yet",
                "never seems full."
        );
        echoingTwistedOrb.addLore("", "§8 Souls Bound: §30 ☠");
        echoingTwistedOrb.fireResistant();
        echoingTwistedOrb.preventPlacement();
        echoingTwistedOrb.preventEquipping();
        echoingTwistedOrb.soulbound();

        BlightedItem forgottenPattern = new BlightedItem(
                "FORGOTTEN_PATTERN",
                ItemType.UNCATEGORIZED,
                ItemRarity.UNIQUE,
                Material.BORDURE_INDENTED_BANNER_PATTERN
        );
        forgottenPattern.setDisplayName("Forgotten Pattern");
        forgottenPattern.description(
                "A pattern worn by hands long",
                "forgotten. Its markings speak",
                "of a path buried deep.",
                "",
                " §8§lThe Forgotten Verse",
                " §8Wear what was woven.",
                " §8Release what echoes within.",
                " §8Let pale ink wake the darkness.",
                " §8At the old table, let it remember. "
        );
        forgottenPattern.soulbound();

        registry.accept(twistedBanner);
        registry.accept(echoingTwistedOrb);
        registry.accept(forgottenPattern);
        registry.accept(resonantBlightstone);
    }
}
