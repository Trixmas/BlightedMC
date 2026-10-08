package fr.moussax.blightedSMP.content.systems.fishing.recipes;

import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedRecipe;
import fr.moussax.blightedSMP.registry.RegistryModule;
import org.bukkit.Material;

import java.util.function.Consumer;

import static fr.moussax.blightedSMP.engine.recipes.crafting.registry.RecipeRegistry.shapedRecipe;

public final class FishingArmorsRecipes implements RegistryModule<Consumer<BlightedRecipe>> {

    @Override
    public void register(Consumer<BlightedRecipe> registry) {

        BlightedRecipe anglerHelmetRecipe = shapedRecipe("ANGLER_HELMET", 1)
                .shape("aaa", "a a", "   ")
                .bind('a', Material.COD, 2)
                .build();

        BlightedRecipe anglerChestplateRecipe = shapedRecipe("ANGLER_CHESTPLATE", 1)
                .shape("a a", "aaa", "aaa")
                .bind('a', Material.COD, 2)
                .build();

        BlightedRecipe anglerLeggingsRecipe = shapedRecipe("ANGLER_LEGGINGS", 1)
                .shape("aaa", "a a", "a a")
                .bind('a', Material.COD, 2)
                .build();

        BlightedRecipe anglerBootsRecipe = shapedRecipe("ANGLER_BOOTS", 1)
                .shape("   ", "a a", "a a")
                .bind('a', Material.COD, 2)
                .build();

        registry.accept(anglerHelmetRecipe);
        registry.accept(anglerChestplateRecipe);
        registry.accept(anglerLeggingsRecipe);
        registry.accept(anglerBootsRecipe);
    }
}
