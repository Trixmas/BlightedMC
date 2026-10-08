package fr.moussax.blightedSMP.content.systems.fishing.loot;

import fr.moussax.blightedSMP.engine.fishing.FishingLootTable;
import fr.moussax.blightedSMP.engine.fishing.FishingMethod;
import fr.moussax.blightedSMP.engine.fishing.registry.FishingRegistryHandler;
import fr.moussax.blightedSMP.registry.RegistryModule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;

import static fr.moussax.blightedSMP.engine.loot.decorators.FishingCatchQuality.*;

public class EndFishing implements RegistryModule<FishingRegistryHandler> {

    @Override
    public void register(FishingRegistryHandler registry) {
        registry.register(World.Environment.THE_END, FishingMethod.VOID, provide());
    }

    public FishingLootTable provide() {
        return FishingLootTable.builder()
                .entityRollChance(0.20)
                .entity(EntityType.ENDERMITE, 3.0, GOOD_CATCH, "§b§lYUCK! §7You caught an §5Endermite§7!")
                .entity(EntityType.SHULKER, 0.8, OUTSTANDING_CATCH, "§d§lRARE CATCH! §7You caught a §5Shulker§7!")
                .item(Material.END_STONE, 3, 50.0, COMMON)
                .item(Material.CHORUS_FRUIT, 2, 40.0, GOOD_CATCH)
                .item(Material.ENDER_PEARL, 1, 10.0, GREAT_CATCH)
                .build();
    }
}
