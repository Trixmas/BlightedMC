package fr.moussax.blightedSMP.content.systems.fishing.items;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.ItemRarity;
import fr.moussax.blightedSMP.engine.items.ItemType;
import fr.moussax.blightedSMP.registry.RegistryModule;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class FishingMaterials implements RegistryModule<Consumer<BlightedItem>> {

    @Override
    public void register(Consumer<BlightedItem> registry) {

        BlightedItem blightedAlgae = new BlightedItem("BLIGHTED_ALGAE", ItemType.MATERIAL, ItemRarity.COMMON, Material.KELP);
        blightedAlgae.setDisplayName("Blighted Algae");
        blightedAlgae.description(
                "A thick, dark strand of kelp",
                "that grows in waters heavy with",
                "corruption. Pulsates with a dim,",
                "dark glow."
        );
        blightedAlgae.preventPlacement();
        blightedAlgae.glow();

        BlightedItem smokedSalmonPlate = new BlightedItem("SMOKED_SALMON_PLATE", ItemType.UNCATEGORIZED, ItemRarity.COMMON, Material.COOKED_SALMON);
        smokedSalmonPlate.setDisplayName("Smoked Salmon Plate");
        smokedSalmonPlate.description(
                "A seasoned, perfectly smoked",
                "salmon fillet. Tastes of wild sea",
                "herbs and hickory wood."
        );
        smokedSalmonPlate.editFood(food -> {
            food.setNutrition(8);
            food.setSaturation(12.8f);
            food.setCanAlwaysEat(true);
        });

        BlightedItem saltedCod = new BlightedItem("SALTED_COD", ItemType.UNCATEGORIZED, ItemRarity.COMMON, Material.COOKED_COD);
        saltedCod.setDisplayName("Salted Cod");
        saltedCod.description(
                "Sun-dried cod heavily encrusted in",
                "coarse sea salt. Keeps indefinitely",
                "and fills the belly."
        );
        saltedCod.editFood(food -> {
            food.setNutrition(7);
            food.setSaturation(11.2f);
            food.setCanAlwaysEat(true);
        });

        BlightedItem fishermansBait = new BlightedItem("FISHERMANS_BAIT", ItemType.UNCATEGORIZED, ItemRarity.COMMON, Material.HONEY_BOTTLE);
        fishermansBait.setDisplayName("Fisherman's Bait");
        fishermansBait.description(
                "A sweet, pungent mixture of",
                "golden honey and ground bugs.",
                "Irresistible to aquatic life."
        );
        fishermansBait.preventConsume();

        BlightedItem fishermansStew = new BlightedItem("FISHERMANS_STEW", ItemType.UNCATEGORIZED, ItemRarity.COMMON, Material.RABBIT_STEW);
        fishermansStew.setDisplayName("Fisherman's Stew");
        fishermansStew.description(
                "A hearty stew prepared from",
                "fresh catches. Provides the",
                "consumer with aquatic affinity."
        );
        fishermansStew.addLore(
                "",
                " §8When Consumed:",
                " §8 ‣ &#8EBAFFWater Breathing I §8(2:00)"
        );
        fishermansStew.editFood(food -> {
            food.setNutrition(10);
            food.setSaturation(14.4f);
            food.setCanAlwaysEat(true);
        });
        fishermansStew.onConsume(player -> player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 2400, 0)));

        BlightedItem barnacleCluster = new BlightedItem("BARNACLE_CLUSTER", ItemType.MATERIAL, ItemRarity.RARE, Material.NAUTILUS_SHELL);
        barnacleCluster.setDisplayName("Barnacle Cluster");
        barnacleCluster.description(
                "A hardened mass of razor-sharp",
                "barnacles clinging to a glowing",
                "fossilized shell."
        );
        barnacleCluster.glow();

        BlightedItem coralFragment = new BlightedItem("CORAL_FRAGMENT", ItemType.MATERIAL, ItemRarity.RARE, Material.FIRE_CORAL);
        coralFragment.setDisplayName("Coral Fragment");
        coralFragment.description(
                "A bright, warm fragment of coral",
                "retrieved from the depths. Retains",
                "a mystical heat."
        );
        coralFragment.preventPlacement();
        coralFragment.glow();

        BlightedItem messageInABottle = new BlightedItem("MESSAGE_IN_A_BOTTLE", ItemType.UNCATEGORIZED, ItemRarity.RARE, Material.GLASS_BOTTLE);
        messageInABottle.setDisplayName("Message in a Bottle");
        messageInABottle.description(
                "A sealed glass bottle containing",
                "a worn, waterlogged note. Perhaps",
                "someone's final words."
        );
        messageInABottle.addLore(
                "",
                " §fRight click to shatter!"
        );
        messageInABottle.preventPlacement();

        BlightedItem blightedSushi = new BlightedItem("BLIGHTED_SUSHI", ItemType.UNCATEGORIZED, ItemRarity.RARE, Material.SUSPICIOUS_STEW);
        blightedSushi.setDisplayName("Blighted Sushi");
        blightedSushi.description(
                "A questionable sushi roll",
                "wrapped in algae. It hums with",
                "unstable entropic energy."
        );
        blightedSushi.addLore(
                "",
                " §8When Consumed:",
                " §8 ‣ &#8EBAFFWater Breathing I §8(1:00)",
                " §8 ‣ &#FFCEB8Hunger I §8(0:15) §7(∼50%)"
        );
        blightedSushi.editFood(food -> {
            food.setNutrition(6);
            food.setSaturation(4.8f);
            food.setCanAlwaysEat(true);
        });
        blightedSushi.onConsume(player -> {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 1200, 0));
            if (ThreadLocalRandom.current().nextDouble() < 0.5) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 300, 0));
            }
        });
        blightedSushi.glow();

        BlightedItem abyssalPearl = new BlightedItem("ABYSSAL_PEARL", ItemType.MATERIAL, ItemRarity.RARE, Material.ENDER_PEARL);
        abyssalPearl.setDisplayName("Abyssal Pearl");
        abyssalPearl.description(
                "A dark, swirling orb pulled from",
                "the abyss. Its core feels freezing",
                "cold and infinitely deep."
        );
        abyssalPearl.preventProjectileLaunch();
        abyssalPearl.glow();

        BlightedItem drownedResearchCodex = new BlightedItem("DROWNED_RESEARCH_CODEX", ItemType.UNCATEGORIZED, ItemRarity.RARE, Material.KNOWLEDGE_BOOK);
        drownedResearchCodex.setDisplayName("Drowned Research Codex");
        drownedResearchCodex.description(
                "A preserved collection of research",
                "behind by an unknown explorer. Its",
                "pages reveal forgotten secrets",
                "of the abyssal currents and the",
                "creatures lurking beneath."
        );
        drownedResearchCodex.setEnchantmentGlint(false);

        registry.accept(blightedAlgae);
        registry.accept(smokedSalmonPlate);
        registry.accept(saltedCod);
        registry.accept(fishermansBait);
        registry.accept(fishermansStew);
        registry.accept(barnacleCluster);
        registry.accept(coralFragment);
        registry.accept(messageInABottle);
        registry.accept(drownedResearchCodex);
        registry.accept(blightedSushi);
        registry.accept(abyssalPearl);
    }
}
