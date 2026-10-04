package fr.moussax.blightedSMP.content.recipes;

import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedRecipe;
import fr.moussax.blightedSMP.registry.RegistryModule;
import java.util.function.Consumer;

import static fr.moussax.blightedSMP.engine.recipes.crafting.registry.RecipeRegistry.shapedRecipe;

import fr.moussax.blightedSMP.engine.items.registry.ItemRegistry;
import org.bukkit.Material;

public class EndRecipes implements RegistryModule<Consumer<BlightedRecipe>> {
    @Override
    public void register(Consumer<BlightedRecipe> registry) {

        BlightedRecipe enchantedEnderPearlRecipe = shapedRecipe("ENCHANTED_ENDER_PEARL", 2)
                .shape(" i ", "iii", " i ")
                .bind('i', Material.ENDER_PEARL, 16)
                .build();

        BlightedRecipe enchantedEndstoneRecipe = shapedRecipe("ENCHANTED_END_STONE", 1)
                .shape(" i ", "iii", " i ")
                .bind('i', Material.END_STONE, 12)
                .build();

        BlightedRecipe enchantedChorusFruitRecipe = shapedRecipe("ENCHANTED_CHORUS_FRUIT", 1)
                .shape(" i ", "iii", " i ")
                .bind('i', Material.CHORUS_FRUIT, 12)
                .build();

        BlightedRecipe glimmeringEyeRecipe = shapedRecipe("GLIMMERING_EYE", 1)
                .shape(" i ", "iji", " i ")
                .bind('i', ItemRegistry.get("ENCHANTED_ENDER_PEARL"), 1)
                .bind('j', Material.OPEN_EYEBLOSSOM, 1)
                .build();

        registry.accept(enchantedEnderPearlRecipe);
        registry.accept(enchantedEndstoneRecipe);
        registry.accept(enchantedChorusFruitRecipe);
        registry.accept(glimmeringEyeRecipe);
    }
}
