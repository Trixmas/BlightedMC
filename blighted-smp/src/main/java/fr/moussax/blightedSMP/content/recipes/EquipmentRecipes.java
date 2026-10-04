package fr.moussax.blightedSMP.content.recipes;

import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedRecipe;
import fr.moussax.blightedSMP.registry.RegistryModule;
import java.util.function.Consumer;
import org.bukkit.Material;

import static fr.moussax.blightedSMP.engine.recipes.crafting.registry.RecipeRegistry.shapedRecipe;

public final class EquipmentRecipes implements RegistryModule<Consumer<BlightedRecipe>> {
    @Override
    public void register(Consumer<BlightedRecipe> registry) {
        BlightedRecipe rocketBoots = shapedRecipe("ROCKET_BOOTS", 1)
                .shape("aba", "cdc", "e e")
                .bind('a', Material.PHANTOM_MEMBRANE)
                .bind('b', Material.WIND_CHARGE)
                .bind('c', Material.SLIME_BLOCK)
                .bind('d', Material.COPPER_BOOTS)
                .bind('e', Material.RABBIT_FOOT)
                .attributeSource(4)
                .build();

        registry.accept(rocketBoots);
    }
}
