package fr.moussax.blightedSMP.content.recipes;

import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedRecipe;
import fr.moussax.blightedSMP.registry.RegistryModule;
import java.util.function.Consumer;

import static fr.moussax.blightedSMP.engine.recipes.crafting.registry.RecipeRegistry.shapedRecipe;

import org.bukkit.Material;

public class NetherMaterialRecipes implements RegistryModule<Consumer<BlightedRecipe>> {
    @Override
    public void register(Consumer<BlightedRecipe> registry) {

        BlightedRecipe enchantedGhastTearRecipe = shapedRecipe("ENCHANTED_GHAST_TEAR", 1)
            .shape(" i ", "iii", " i ")
            .bind('i', Material.GHAST_TEAR, 1)
            .build();

        BlightedRecipe enchantedMagmaCreamRecipe = shapedRecipe("ENCHANTED_MAGMA_CREAM", 1)
            .shape(" i ", "iii", " i ")
            .bind('i', Material.MAGMA_CREAM, 8)
            .build();

        BlightedRecipe enchantedQuartzRecipe = shapedRecipe("ENCHANTED_QUARTZ", 1)
            .shape(" i ", "iii", " i ")
            .bind('i', Material.QUARTZ, 8)
            .build();

        BlightedRecipe enchantedBlazePowderRecipe = shapedRecipe("ENCHANTED_BLAZE_POWDER", 1)
            .shape(" i ", "iii", " i ")
            .bind('i', Material.BLAZE_POWDER, 8)
            .build();

        BlightedRecipe enchantedBlazeRodRecipe = shapedRecipe("ENCHANTED_BLAZE_ROD", 1)
            .shape(" i ", "iii", " i ")
            .bind('i', "ENCHANTED_BLAZE_POWDER", 8)
            .build();

        BlightedRecipe enchantedGlowstoneRecipe = shapedRecipe("ENCHANTED_GLOWSTONE_DUST", 1)
            .shape(" i ", "iii", " i ")
            .bind('i', Material.GLOWSTONE_DUST, 8)
            .build();

        BlightedRecipe enchantedNetherWartRecipe = shapedRecipe("ENCHANTED_NETHER_WART", 1)
            .shape(" i ", "iii", " i ")
            .bind('i', Material.NETHER_WART, 12)
            .build();

        BlightedRecipe enchantedSulfurRecipe = shapedRecipe("ENCHANTED_SULFUR", 1)
            .shape(" i ", "iii", " i ")
            .bind('i', "SULFUR", 8)
            .build();

        BlightedRecipe enchantedNetherrackRecipe = shapedRecipe("ENCHANTED_NETHERRACK", 1)
            .shape(" i ", "iii", " i ")
            .bind('i', Material.NETHERRACK, 12)
            .build();

        registry.accept(enchantedGhastTearRecipe);
        registry.accept(enchantedMagmaCreamRecipe);
        registry.accept(enchantedQuartzRecipe);
        registry.accept(enchantedBlazePowderRecipe);
        registry.accept(enchantedBlazeRodRecipe);
        registry.accept(enchantedGlowstoneRecipe);
        registry.accept(enchantedNetherWartRecipe);
        registry.accept(enchantedSulfurRecipe);
    }
}
