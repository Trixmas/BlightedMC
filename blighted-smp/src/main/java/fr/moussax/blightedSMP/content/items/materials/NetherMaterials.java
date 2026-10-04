package fr.moussax.blightedSMP.content.items.materials;

import fr.moussax.blightedSMP.engine.items.BlightedItem;
import fr.moussax.blightedSMP.engine.items.ItemRarity;
import fr.moussax.blightedSMP.engine.items.ItemType;
import fr.moussax.blightedSMP.registry.RegistryModule;
import org.bukkit.Material;

import java.util.function.Consumer;

public class NetherMaterials implements RegistryModule<Consumer<BlightedItem>> {
    @Override
    public void register(Consumer<BlightedItem> registry) {

        BlightedItem enchantedGhastTear = new BlightedItem("ENCHANTED_GHAST_TEAR", ItemType.MATERIAL, ItemRarity.COMMON, Material.GHAST_TEAR);
        enchantedGhastTear.setDisplayName("Enchanted Ghast Tear");
        enchantedGhastTear.glow();
        enchantedGhastTear.flushRarity();

        BlightedItem enchantedMagmaCream = new BlightedItem("ENCHANTED_MAGMA_CREAM", ItemType.MATERIAL, ItemRarity.COMMON, Material.MAGMA_CREAM);
        enchantedMagmaCream.setDisplayName("Enchanted Magma Cream");
        enchantedMagmaCream.glow();
        enchantedMagmaCream.flushRarity();

        BlightedItem enchantedQuartz = new BlightedItem("ENCHANTED_QUARTZ", ItemType.MATERIAL, ItemRarity.COMMON, Material.QUARTZ);
        enchantedQuartz.setDisplayName("Enchanted Quartz");
        enchantedQuartz.glow();
        enchantedQuartz.flushRarity();

        BlightedItem enchantedBlazePowder = new BlightedItem("ENCHANTED_BLAZE_POWDER", ItemType.MATERIAL, ItemRarity.COMMON, Material.BLAZE_POWDER);
        enchantedBlazePowder.setDisplayName("Enchanted Blaze Powder");
        enchantedBlazePowder.glow();
        enchantedBlazePowder.flushRarity();

        BlightedItem enchantedBlazeRod = new BlightedItem("ENCHANTED_BLAZE_ROD", ItemType.MATERIAL, ItemRarity.RARE, Material.BLAZE_ROD);
        enchantedBlazeRod.setDisplayName("Enchanted Blaze Rod");
        enchantedBlazeRod.glow();
        enchantedBlazeRod.flushRarity();

        BlightedItem enchantedGlowstoneDust = new BlightedItem("ENCHANTED_GLOWSTONE_DUST", ItemType.MATERIAL, ItemRarity.COMMON, Material.GLOWSTONE_DUST);
        enchantedGlowstoneDust.setDisplayName("Enchanted Glowstone Dust");
        enchantedGlowstoneDust.glow();
        enchantedGlowstoneDust.flushRarity();

        BlightedItem enchantedNetherWart = new BlightedItem("ENCHANTED_NETHER_WART", ItemType.MATERIAL, ItemRarity.COMMON, Material.NETHER_WART);
        enchantedNetherWart.setDisplayName("Enchanted Nether Wart");
        enchantedNetherWart.glow();
        enchantedNetherWart.flushRarity();

        BlightedItem flames = new BlightedItem("FLAMES", ItemType.MATERIAL, ItemRarity.RARE, Material.BLAZE_POWDER);
        flames.setDisplayName("Flames");
        flames.description(
                "Dredged from the deepest magma,",
                "these §6abyssal embers §7burn with",
                "a cold heat that defies nature."
        );
        flames.glow();

        BlightedItem sulfur = new BlightedItem("SULFUR", ItemType.MATERIAL, ItemRarity.COMMON, Material.GLOWSTONE_DUST);
        sulfur.setDisplayName("Sulfur");
        sulfur.description(
                "A pungent precipitate scraped",
                "from Nether vents, used to",
                "catalyze volatile reactions."
        );

        BlightedItem enchantedSulfur = new BlightedItem("ENCHANTED_SULFUR", ItemType.MATERIAL, ItemRarity.RARE, Material.GLOWSTONE_DUST);
        enchantedSulfur.setDisplayName("Enchanted Sulfur");
        enchantedSulfur.glow();
        enchantedSulfur.flushRarity();

        BlightedItem enchantedNetherrack = new BlightedItem("ENCHANTED_NETHERRACK", ItemType.MATERIAL, ItemRarity.COMMON, Material.NETHERRACK);
        enchantedNetherrack.setDisplayName("Enchanted Netherrack");
        enchantedNetherrack.glow();
        enchantedNetherrack.flushRarity();

        BlightedItem vengefulEye = new BlightedItem("VENGEFUL_EYE", ItemType.UNCATEGORIZED, ItemRarity.RARE, Material.PLAYER_HEAD);
        vengefulEye.setDisplayName("Vengeful Eye");
        vengefulEye.setCustomSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjkzZWMyODVmNWM2NzcwZTdkZmMxZjI3NzVkMGU5NTYzOTk3Mzc4Njk5OWJkZDM2Y2M4ZTk5OGIyNWU3NTNmYiJ9fX0=");
        vengefulEye.preventPlacement();

        BlightedItem cremeBrulee = new BlightedItem("CREME_BRULEE", ItemType.UNCATEGORIZED, ItemRarity.COMMON, Material.PUMPKIN_PIE);
        cremeBrulee.setDisplayName("Crème brûlée");
        cremeBrulee.description(
                "Impervious to the flame that",
                "scorched its §6caramelized",
                "§6crown§7, this custard remains",
                "velvety and untouched within.",
                "",
                "§8“Sous le feu, la promesse",
                "§8d'un instant sucré.”"
        );
        cremeBrulee.editFood(food -> {
            food.setNutrition(10);
            food.setSaturation(14.4f);
            food.setCanAlwaysEat(true);
        });
        cremeBrulee.glow();

        BlightedItem suspiciousFungus = new BlightedItem("SUSPICIOUS_FUNGUS", ItemType.UNCATEGORIZED, ItemRarity.RARE, Material.CRIMSON_FUNGUS);
        suspiciousFungus.setDisplayName("SuSpIcIoUs Fungus");
        suspiciousFungus.preventPlacement();
        suspiciousFungus.glow();

        registry.accept(enchantedGhastTear);
        registry.accept(enchantedMagmaCream);
        registry.accept(enchantedQuartz);
        registry.accept(enchantedBlazePowder);
        registry.accept(enchantedBlazeRod);
        registry.accept(enchantedGlowstoneDust);
        registry.accept(enchantedNetherWart);
        registry.accept(flames);
        registry.accept(sulfur);
        registry.accept(enchantedSulfur);
        registry.accept(enchantedNetherrack);
        registry.accept(vengefulEye);
        registry.accept(cremeBrulee);
        registry.accept(suspiciousFungus);
    }
}
