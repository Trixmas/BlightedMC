package fr.moussax.blightedSMP.engine.fishing;

import fr.moussax.blightedSMP.engine.fishing.hooks.CustomFishingHook;
import fr.moussax.blightedSMP.engine.fishing.modifiers.FishingSpeedCalculator;
import fr.moussax.blightedSMP.engine.loot.LootCondition;
import fr.moussax.blightedSMP.engine.loot.decorators.FishingCatchQuality;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FishingTest {

    @Test
    @DisplayName("FishingComboTracker bonuses scale correctly with combo and cap at maximum")
    void testFishingComboBonuses() {
        assertEquals(0.0, FishingComboTracker.getSeaCreatureChanceBonus(0), 1e-6);
        assertEquals(0.0125, FishingComboTracker.getSeaCreatureChanceBonus(5), 1e-6);
        assertEquals(0.025, FishingComboTracker.getSeaCreatureChanceBonus(10), 1e-6);
        assertEquals(0.0375, FishingComboTracker.getSeaCreatureChanceBonus(15), 1e-6);
        assertEquals(0.05, FishingComboTracker.getSeaCreatureChanceBonus(20), 1e-6);
        assertEquals(0.05, FishingComboTracker.getSeaCreatureChanceBonus(30), 1e-6);

        assertEquals(0, FishingComboTracker.getBonusExperience(0));
        assertEquals(0, FishingComboTracker.getBonusExperience(4));
        assertEquals(2, FishingComboTracker.getBonusExperience(5));
        assertEquals(4, FishingComboTracker.getBonusExperience(10));
        assertEquals(6, FishingComboTracker.getBonusExperience(15));
        assertEquals(8, FishingComboTracker.getBonusExperience(20));
        assertEquals(8, FishingComboTracker.getBonusExperience(30));
    }

    @Test
    @DisplayName("FishingSpeedCalculator wait time reduction applies diminishing returns correctly")
    void testFishingSpeedCalculatorWaitTicks() {
        int baseTicks = 300;

        assertEquals(300, FishingSpeedCalculator.applyToWaitTicks(baseTicks, 0));
        assertEquals(273, FishingSpeedCalculator.applyToWaitTicks(baseTicks, 10)); // 300 * (100/110) = 272.72 -> 273
        assertEquals(200, FishingSpeedCalculator.applyToWaitTicks(baseTicks, 50)); // 300 * (100/150) = 200
        assertEquals(150, FishingSpeedCalculator.applyToWaitTicks(baseTicks, 100)); // 300 * (100/200) = 150
        assertEquals(1, FishingSpeedCalculator.applyToWaitTicks(1, 1000)); // Never drops below 1 tick
    }

    @Test
    @DisplayName("CustomFishingHook launch velocity calculates expected trajectory arc")
    void testCustomFishingHookLaunchVelocity() {
        Location hookLocation = new Location(null, 0.0, 60.0, 0.0);
        Location playerLocation = new Location(null, 3.0, 64.0, 0.0);

        Vector velocity = CustomFishingHook.calculateLaunchVelocity(hookLocation, playerLocation);

        assertEquals(3.0 * 0.08, velocity.getX(), 1e-6);
        assertEquals(0.0, velocity.getZ(), 1e-6);

        double distance = 5.0; // sqrt(3^2 + 4^2) = 5
        double expectedY = (4.0 * 0.08) + (Math.sqrt(distance) * 0.05) + 0.15;
        assertEquals(expectedY, velocity.getY(), 1e-6);
        assertTrue(velocity.getY() > 0.45, "Launch velocity must include upward arc boost");
    }

    @Test
    @DisplayName("FishingLootTable.Builder constructs table successfully with rich drop helpers")
    void testFishingLootTableBuilder() {
        FishingLootTable table = FishingLootTable.builder()
                .entityRollChance(0.25)
                .entity(EntityType.ZOMBIE, 10.0, FishingCatchQuality.GOOD_CATCH)
                .entity(EntityType.SKELETON, 5.0, FishingCatchQuality.GREAT_CATCH, LootCondition.alwaysTrue())
                .entity(EntityType.SQUID, 8.0, FishingCatchQuality.COMMON, "§b§lSPLASH!")
                .entity(EntityType.TROPICAL_FISH, 4.0, FishingCatchQuality.GOOD_CATCH, "§d§lCOLORFUL!", true, LootCondition.alwaysTrue())
                .entity(EntityType.SILVERFISH, entity -> {
                }, 2.0, FishingCatchQuality.GREAT_CATCH)
                .item(Material.COD, 50.0)
                .item(Material.SALMON, 2, 30.0, FishingCatchQuality.GOOD_CATCH)
                .item(Material.PUFFERFISH, 1, 3, 10.0, FishingCatchQuality.GREAT_CATCH)
                .blight(5, 50.0, FishingCatchQuality.GOOD_CATCH)
                .blight(10, 20, 25.0, FishingCatchQuality.GREAT_CATCH)
                .damagedItem(Material.FISHING_ROD, 0.2, 0.8, 15.0)
                .build();

        assertNotNull(table, "Constructed FishingLootTable must not be null");
        assertEquals(0.25, table.getEntityRollChance(), 1e-6);
    }
}
