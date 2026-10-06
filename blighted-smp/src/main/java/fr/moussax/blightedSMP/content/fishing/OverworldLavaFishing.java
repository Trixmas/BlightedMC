package fr.moussax.blightedSMP.content.fishing;

import fr.moussax.blightedSMP.engine.fishing.FishingLootTable;
import fr.moussax.blightedSMP.engine.fishing.FishingMethod;
import fr.moussax.blightedSMP.engine.fishing.registry.FishingRegistryHandler;
import fr.moussax.blightedSMP.engine.loot.LootCondition;
import fr.moussax.blightedSMP.registry.RegistryModule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.meta.OminousBottleMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.function.Consumer;

import static fr.moussax.blightedSMP.engine.loot.decorators.FishingCatchQuality.*;

public class OverworldLavaFishing implements RegistryModule<FishingRegistryHandler> {

    @Override
    public void register(FishingRegistryHandler registry) {
        registry.register(World.Environment.NORMAL, FishingMethod.LAVA, provide());
    }

    public FishingLootTable provide() {
        Consumer<LivingEntity> applyFireResistance = entity ->
                entity.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.FIRE_RESISTANCE,
                                PotionEffect.INFINITE_DURATION,
                                1,
                                false,
                                false
                        )
                );

        return FishingLootTable.builder()
                .entityRollChance(0.15)
                .entity(EntityType.MAGMA_CUBE, 50.0, GREAT_CATCH)
                .entity(EntityType.SILVERFISH, applyFireResistance, 40.0, GREAT_CATCH)
                .entity(EntityType.HUSK, applyFireResistance, 30.0, GREAT_CATCH)
                .entity(EntityType.PARCHED, applyFireResistance, 30.0, GREAT_CATCH)
                .entity(EntityType.WITCH, applyFireResistance, 20.0, GREAT_CATCH)
                .entity(EntityType.ILLUSIONER, applyFireResistance, 10.0, GREAT_CATCH)
                .item(Material.BONE, 2, 6, 120.0, COMMON)
                .item(Material.DRIED_KELP, 5, 10, 120.0, COMMON)
                .item(Material.FLINT, 2, 5, 110.0, COMMON)
                .item(Material.GUNPOWDER, 2, 5, 110.0, COMMON)
                .item(Material.COPPER_NUGGET, 5, 15, 100.0, COMMON)
                .item(Material.IRON_NUGGET, 5, 15, 100.0, COMMON)
                .item(Material.COAL, 3, 8, 100.0, COMMON)
                .item(Material.STONE, 5, 10, 100.0, COMMON)
                .item(Material.STONE_BUTTON, builder -> builder.setItemName("Pebble"), 2, 5, 100.0, COMMON)
                .item(Material.TUFF, 5, 10, 60.0, GOOD_CATCH, LootCondition.atMostY(0))
                .blight(2, 5, 60.0, GOOD_CATCH)
                .item(Material.EXPERIENCE_BOTTLE, 3, 8, 55.0, GOOD_CATCH)
                .item(Material.COOKED_BEEF, 4, 8, 55.0, GOOD_CATCH)
                .item(Material.LAPIS_LAZULI, 8, 16, 50.0, GOOD_CATCH)
                .item(Material.REDSTONE, 8, 16, 50.0, GOOD_CATCH)
                .item(Material.MAGMA_BLOCK, 5, 10, 50.0, GOOD_CATCH)
                .item(Material.OBSIDIAN, 2, 4, 55.0, GOOD_CATCH)
                .item(Material.POINTED_DRIPSTONE, 2, 8, 45.0, GOOD_CATCH, LootCondition.biome(Biome.DRIPSTONE_CAVES))
                .item("CREME_BRULEE", 1, 2, 40.0, GOOD_CATCH)
                .item(Material.GOLDEN_APPLE, 1, 35.0, GOOD_CATCH)
                .item(Material.LAVA_BUCKET, 1, 35.0, GOOD_CATCH)
                .blight(6, 10, 30.0, GREAT_CATCH)
                .item(Material.AMETHYST_CLUSTER, 2, 4, 20.0, GREAT_CATCH)
                .item(Material.EMERALD, 3, 6, 20.0, GREAT_CATCH)
                .item(Material.RAW_GOLD_BLOCK, 2, 15.0, GREAT_CATCH)
                .item(Material.DIAMOND, 1, 3, 15.0, GREAT_CATCH)
                .blight(12, 16, 10.0, OUTSTANDING_CATCH)
                .item(Material.TOTEM_OF_UNDYING, 1, 5.0, OUTSTANDING_CATCH)
                .item(Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, 1, 3.0, OUTSTANDING_CATCH)
                .item(Material.OMINOUS_BOTTLE, b -> b.setItemMeta(
                        meta -> ((OminousBottleMeta) meta).setAmplifier(2)
                ), 1, 3.0, OUTSTANDING_CATCH)
                .item(Material.DIAMOND_BLOCK, 1, 2.0, OUTSTANDING_CATCH)
                .item(Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, 1, 1.0, OUTSTANDING_CATCH, LootCondition.biome(Biome.DEEP_DARK))
                .item(Material.BUDDING_AMETHYST, b -> b.setRarity(ItemRarity.RARE), 1, 1.0, OUTSTANDING_CATCH, LootCondition.atMostY(-30))
                .item(Material.ENCHANTED_GOLDEN_APPLE, 1, 1.0, OUTSTANDING_CATCH)
                .build();
    }
}
