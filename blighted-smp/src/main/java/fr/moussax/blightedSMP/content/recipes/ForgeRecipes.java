package fr.moussax.blightedSMP.content.recipes;

import fr.moussax.blightedSMP.engine.recipes.CraftingObject;
import fr.moussax.blightedSMP.engine.recipes.forging.ForgeRecipe;
import fr.moussax.blightedSMP.engine.items.registry.ItemRegistry;
import fr.moussax.blightedSMP.registry.RegistryModule;
import java.util.function.Consumer;
import org.bukkit.Material;

public class ForgeRecipes implements RegistryModule<Consumer<ForgeRecipe>> {
    @Override
    public void register(Consumer<ForgeRecipe> registry) {
        var plasmaBucket = ForgeRecipe.Builder.of("PLASMA_BUCKET", 1)
                .fuelCost(10000)
                .ingredients(
                        new CraftingObject(ItemRegistry.get("MAGMA_BUCKET"), 1),
                        new CraftingObject(ItemRegistry.get("MAGMA_BUCKET"), 1),
                        new CraftingObject(Material.NETHER_STAR, 1)
                )
                .build();

        registry.accept(plasmaBucket);
    }
}
