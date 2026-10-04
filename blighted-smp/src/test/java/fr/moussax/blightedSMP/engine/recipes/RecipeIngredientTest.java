package fr.moussax.blightedSMP.engine.recipes;

import fr.moussax.blightedSMP.engine.recipes.crafting.BlightedRecipe;
import fr.moussax.blightedSMP.engine.recipes.crafting.registry.RecipeRegistry;
import org.bukkit.Material;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecipeIngredientTest {

    @Test
    @DisplayName("Vanilla ingredient stores material, amount, and formatted id correctly")
    void testVanillaIngredient() {
        RecipeIngredient ingredient = RecipeIngredient.of(Material.NETHERITE_INGOT, 4);

        assertTrue(ingredient.isVanilla());
        assertFalse(ingredient.isCustom());
        assertEquals(Material.NETHERITE_INGOT, ingredient.getMaterial());
        assertEquals(4, ingredient.getAmount());
        assertEquals("vanilla:NETHERITE_INGOT", ingredient.getId());
    }

    @Test
    @DisplayName("Value equality and hashCode depend on ingredient id and amount")
    void testEqualityAndHashCode() {
        RecipeIngredient first = RecipeIngredient.of(Material.DIAMOND, 2);
        RecipeIngredient second = RecipeIngredient.of(Material.DIAMOND, 2);
        RecipeIngredient differentAmount = RecipeIngredient.of(Material.DIAMOND, 3);
        RecipeIngredient differentMaterial = RecipeIngredient.of(Material.EMERALD, 2);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());

        assertNotEquals(first, differentAmount);
        assertNotEquals(first, differentMaterial);
        assertNotEquals(first, null);
    }

    @Test
    @DisplayName("RecipeRegistry empty grid returns empty matching recipes")
    void testRecipeRegistryEmptyGrid() {
        assertTrue(RecipeRegistry.findMatchingRecipes(null).isEmpty());
        assertTrue(RecipeRegistry.findMatchingRecipes(List.of()).isEmpty());
        assertTrue(RecipeRegistry.findFirstMatching(List.of()).isEmpty());
    }
}
