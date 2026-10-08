package fr.moussax.blightedSMP.content.systems.fishing.loot;

import fr.moussax.blightedSMP.engine.fishing.FishingLootTable;
import fr.moussax.blightedSMP.engine.fishing.FishingMethod;
import fr.moussax.blightedSMP.engine.fishing.registry.FishingRegistryHandler;
import fr.moussax.blightedSMP.engine.loot.LootCondition;
import fr.moussax.blightedSMP.registry.RegistryModule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.meta.OminousBottleMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.List;

import static fr.moussax.blightedSMP.engine.loot.decorators.FishingCatchQuality.*;

public class NetherFishing implements RegistryModule<FishingRegistryHandler> {

    @Override
    public void register(FishingRegistryHandler registry) {
        registry.register(World.Environment.NETHER, FishingMethod.LAVA, provide());
    }

    public FishingLootTable provide() {
        return FishingLootTable.builder()
                .entityRollChance(0.15)
                .entity(EntityType.MAGMA_CUBE, 2.0, GREAT_CATCH)
                .entity(EntityType.GHAST, 20.0, GREAT_CATCH, LootCondition.biome(Biome.SOUL_SAND_VALLEY))
                .entity(EntityType.HOGLIN, 30.0, GREAT_CATCH, LootCondition.biome(Biome.CRIMSON_FOREST))
                .entity(EntityType.ENDERMAN, 30.0, GREAT_CATCH, LootCondition.biome(Biome.WARPED_FOREST))
                .entity(EntityType.SKELETON, 30.0, GREAT_CATCH, LootCondition.biome(Biome.SOUL_SAND_VALLEY))
                .item(Material.ROTTEN_FLESH, 2, 6, 120.0, COMMON)
                .item(Material.QUARTZ, 4, 12, 110.0, COMMON)
                .item(Material.MUSHROOM_STEW, 1, 110.0, COMMON)
                .item(Material.GLOWSTONE_DUST, 4, 12, 110.0, COMMON)
                .item(Material.COAL, 3, 8, 100.0, COMMON)
                .item(Material.GOLD_NUGGET, 5, 15, 100.0, COMMON)
                .item(Material.IRON_NUGGET, 3, 9, 100.0, COMMON)
                .item(Material.COOKED_CHICKEN, 2, 6, 100.0, COMMON)
                .item(Material.COOKED_PORKCHOP, 2, 6, 100.0, COMMON)
                .item(Material.BAKED_POTATO, 3, 8, 90.0, COMMON)
                .item(Material.STRING, 1, 4, 80.0, COMMON)
                .item(Material.LEATHER, 1, 3, 80.0, COMMON)
                .item("SULFUR", 2, 4, 60.0, COMMON)
                .blight(2, 5, 60.0, GOOD_CATCH)
                .item(Material.EXPERIENCE_BOTTLE, 3, 8, 55.0, GOOD_CATCH)
                .item(Material.GOLD_INGOT, 2, 5, 55.0, GOOD_CATCH)
                .item("ENCHANTED_QUARTZ", 1, 3, 50.0, GOOD_CATCH)
                .item("ENCHANTED_GLOWSTONE_DUST", 1, 3, 50.0, GOOD_CATCH)
                .item(Material.GOLDEN_CARROT, 3, 8, 50.0, GOOD_CATCH)
                .item(Material.OBSIDIAN, 2, 4, 45.0, GOOD_CATCH)
                .item(Material.BLAZE_ROD, 1, 3, 45.0, GOOD_CATCH)
                .item(Material.SPECTRAL_ARROW, 8, 16, 40.0, GOOD_CATCH, LootCondition.biome(Biome.SOUL_SAND_VALLEY))
                .item(Material.GILDED_BLACKSTONE, 1, 4, 40.0, GOOD_CATCH, LootCondition.biome(Biome.BASALT_DELTAS))
                .item("CREME_BRULEE", 1, 2, 40.0, GOOD_CATCH)
                .damagedItem(Material.CROSSBOW, 0.10, 0.80, 35.0, GOOD_CATCH)
                .damagedItem(Material.GOLDEN_HELMET, 0.20, 0.75, 35.0, GOOD_CATCH)
                .item("FLAMES", 1, 35.0, GOOD_CATCH)
                .item(Material.SUSPICIOUS_STEW, b ->
                                b.setItemMeta(meta -> ((SuspiciousStewMeta) meta).addCustomEffect(
                                        new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0), false)
                                ),
                        1,
                        35.0,
                        GOOD_CATCH
                )
                .item(Material.WARPED_FUNGUS_ON_A_STICK, 1, 30.0, GOOD_CATCH, LootCondition.biome(Biome.WARPED_FOREST))
                .item(Material.SOUL_SAND, 1, 2, 30.0, GOOD_CATCH, LootCondition.biome(Biome.SOUL_SAND_VALLEY))
                .item(Material.ENDER_PEARL, 1, 2, 30.0, GOOD_CATCH)
                .blight(6, 10, 30.0, GREAT_CATCH)
                .item(Material.GOLDEN_APPLE, 1, 2, 25.0, GREAT_CATCH)
                .item(Material.CRYING_OBSIDIAN, 1, 3, 25.0, GREAT_CATCH)
                .item(Material.GHAST_TEAR, 1, 3, 25.0, GREAT_CATCH)
                .item(Material.POTION, b -> b.setItemMeta(meta ->
                        ((PotionMeta) meta).setBasePotionType(PotionType.LONG_FIRE_RESISTANCE)
                ), 1, 20.0, GREAT_CATCH)
                .enchantedBook(
                        List.of(
                                Enchantment.SMITE,
                                Enchantment.PROTECTION,
                                Enchantment.FIRE_PROTECTION,
                                Enchantment.SHARPNESS,
                                Enchantment.FEATHER_FALLING
                        ),
                        3,
                        5,
                        20.0,
                        GREAT_CATCH
                )
                .item(Material.SKELETON_SKULL, 1, 15.0, GREAT_CATCH, LootCondition.biome(Biome.SOUL_SAND_VALLEY))
                .item(Material.DRIED_GHAST, 1, 15.0, GREAT_CATCH, LootCondition.biome(Biome.SOUL_SAND_VALLEY))
                .item("SUSPICIOUS_FUNGUS", 1, 15.0, GREAT_CATCH, LootCondition.biome(Biome.SOUL_SAND_VALLEY))
                .item(Material.RESPAWN_ANCHOR, 1, 15.0, GREAT_CATCH)
                .item(Material.PIGLIN_HEAD, 1, 15.0, GREAT_CATCH)
                .item(Material.NETHERITE_SCRAP, 1, 2, 15.0, GREAT_CATCH)
                .item(Material.ANCIENT_DEBRIS, 1, 10.0, GREAT_CATCH)
                .blight(12, 16, 10.0, OUTSTANDING_CATCH)
                .item("FLAMES", 8, 16, 8.0, OUTSTANDING_CATCH)
                .item(Material.MUSIC_DISC_PIGSTEP, 1, 5.0, OUTSTANDING_CATCH, LootCondition.biome(Biome.CRIMSON_FOREST))
                .item(Material.MUSIC_DISC_TEARS, 1, 5.0, OUTSTANDING_CATCH, LootCondition.biome(Biome.SOUL_SAND_VALLEY))
                .item(Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, 1, 3.0, OUTSTANDING_CATCH)
                .item(Material.WITHER_SKELETON_SKULL, 1, 3.0, OUTSTANDING_CATCH)
                .item(Material.OMINOUS_BOTTLE, b -> b.setItemMeta(
                        meta -> ((OminousBottleMeta) meta).setAmplifier(4)
                ), 1, 3.0, OUTSTANDING_CATCH)
                .item("VENGEFUL_EYE", 1, 2.0, OUTSTANDING_CATCH)
                .item(Material.ENCHANTED_GOLDEN_APPLE, 1, 1.0, OUTSTANDING_CATCH)
                .item(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1, 1.0, OUTSTANDING_CATCH)
                .build();
    }
}
